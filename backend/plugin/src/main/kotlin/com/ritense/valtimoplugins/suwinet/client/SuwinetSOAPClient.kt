package com.ritense.valtimoplugins.suwinet.client

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.cxf.configuration.jsse.TLSClientParameters
import org.apache.cxf.configuration.security.AuthorizationPolicy
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.interceptor.LoggingInInterceptor
import org.apache.cxf.interceptor.LoggingOutInterceptor
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.transports.http.configuration.ConnectionType
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy
import org.apache.cxf.ws.addressing.WSAddressingFeature
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

class SuwinetSOAPClient {
    private var keystoreManagerFactory: KeyManagerFactory? = null
    private var trustManagerFactory: TrustManagerFactory? = null
    private var basicAuthName: String? = null
    private var basicAuthSecret: String? = null
    private var basicAuth: Boolean = false

    fun configureKeystore(
        keystoreCertificate: String? = null,
        keystoreKey: String? = null,
    ): SuwinetSOAPClient {
        keystoreManagerFactory = buildKeyManagerFactory(keystoreCertificate, keystoreKey)
        return this
    }

    fun configureTruststore(
        truststoreCertificate: String? = null,
        truststoreKey: String? = null,
    ): SuwinetSOAPClient {
        trustManagerFactory = buildTrustManagerFactory(truststoreCertificate, truststoreKey)
        return this
    }

    fun configureBasicAuth(
        basicAuthName: String? = null,
        basicAuthSecret: String? = null,
    ): SuwinetSOAPClient {
        this.basicAuthName = basicAuthName
        this.basicAuthSecret = basicAuthSecret
        basicAuth = this.basicAuthName?.isNotBlank() == true && this.basicAuthSecret?.isNotBlank() == true
        return this
    }

    inline fun <reified T : Any> getService(
        url: String,
        connectionTimeout: Int?,
        receiveTimeout: Int?,
    ): T {
        val clazz = T::class.java

        val soapService =
            with(JaxWsProxyFactoryBean()) {
                this.serviceClass = clazz
                address = url

                // WS-Addressing feature for including SOAPAction in tag
                val addressingFeature =
                    WSAddressingFeature().apply {
                        isAddressingRequired = false
                    }

                val loggingFeature: LoggingFeature = LoggingFeature()
                loggingFeature.setPrettyLogging(true)
                loggingFeature.setVerbose(true)
                loggingFeature.setLogMultipart(true)
                loggingFeature.addSensitiveElementNames(
                    setOf<String>(
                        "Burgerservicenr",
                        "Voornamen",
                        "SignificantDeelVanDeAchternaam",
                        "ANr",
                        "Geboortedat",
                        "Postcd",
                        "Straatnaam",
                        "Huisnr",
                        "Woonplaatsnaam",
                    ),
                )
                loggingFeature.addSensitiveProtocolHeaderNames(
                    setOf<String>(
                        "Authorization",
                        "x-opentunnel-api-key",
                    ),
                )
                this.features.add(addressingFeature)
                this.features.add(loggingFeature)

                create() as T
            }

        setDefaultPolicies(soapService, connectionTimeout, receiveTimeout)

        return soapService
    }

    fun setDefaultPolicies(
        service: Any,
        connectionTimeout: Int?,
        receiveTimeout: Int?,
    ) {
        val client = ClientProxy.getClient(service)
        with(client.requestContext) {
            // Disable strict action checking
            this["ws-addressing.strict.action.checking"] = false

            // Disable validation
            this["ws-addressing.validation.enabled"] = false

            // Use default action
            this["ws-addressing.using.default.action"] = true

            // Disable message ID requirement
            this["ws-addressing.messageId.required"] = false

            // Disable checks
            this["ws-addressing.disable.addressing.checks"] = true

            // Allow anonymous RelatesTo
            this["allow.anonymous"] = true

            // Suwinet service does not properly implement WS-Addressing and requires relaxed validation.
            // All checks disabled to prevent request failures.
            this["org.apache.cxf.ws.addressing.MAPAggregator.addressingDisabled"] = true
        }

        if (logger.isDebugEnabled()) {
            // deprecated loggers do log RAW messages
            client.inInterceptors.add(LoggingInInterceptor())
            client.outInterceptors.add(LoggingOutInterceptor())

            client.inFaultInterceptors.add(LoggingInInterceptor())
            client.outFaultInterceptors.add(LoggingOutInterceptor())
        }

        val conduit: HTTPConduit = client.conduit as HTTPConduit
        client.requestContext[Message.PROTOCOL_HEADERS] =
            mapOf("Expect" to listOf("100-continue"))

        client.outInterceptors.add(StripSoapActionQuotesInterceptor())

        val httpPolicy = HTTPClientPolicy()
        httpPolicy.connectionTimeout = (connectionTimeout ?: 10) * 1000L
        httpPolicy.isAllowChunking = true
        httpPolicy.receiveTimeout = (receiveTimeout ?: 10) * 1000L
        httpPolicy.connection = ConnectionType.KEEP_ALIVE

        val tlsParameters = TLSClientParameters()
        tlsParameters.keyManagers = keystoreManagerFactory?.keyManagers
        tlsParameters.trustManagers = trustManagerFactory?.trustManagers

        conduit.tlsClientParameters = tlsParameters
        conduit.client = httpPolicy

        if (basicAuth) {
            conduit.authorization = basicAuthorization()
            logger.info { "set conduit.authorization type to ${conduit.authorization.authorizationType}" }
        }
    }

    fun basicAuthorization(): AuthorizationPolicy {
        val authorizationPolicy = AuthorizationPolicy()
        authorizationPolicy.userName = basicAuthName
        authorizationPolicy.password = basicAuthSecret
        authorizationPolicy.authorizationType = "Basic"
        return authorizationPolicy
    }

    private fun buildKeyManagerFactory(
        keystoreCertificate: String? = null,
        keystoreKey: String? = null,
    ): KeyManagerFactory? =
        if (keystoreCertificate.isNullOrEmpty() || keystoreKey.isNullOrEmpty()) {
            logger.info("Keystore not set")
            null
        } else {
            logger.info("wsgKeyManagerFactory certificate: $keystoreCertificate")
            val keyStore = KeyStore.getInstance("jks")
            keyStore.load(FileInputStream(keystoreCertificate), keystoreKey.toCharArray())
            keystoreManagerFactory = KeyManagerFactory.getInstance("SunX509")
            keystoreManagerFactory?.init(keyStore, keystoreKey.toCharArray())
            keystoreManagerFactory
        }

    private fun buildTrustManagerFactory(
        truststoreCertificate: String? = null,
        truststoreKey: String? = null,
    ): TrustManagerFactory? =
        if (truststoreCertificate.isNullOrEmpty() || truststoreKey.isNullOrEmpty()) {
            logger.info("Truststore not set.")
            null
        } else {
            val trustStore = KeyStore.getInstance("jks")
            logger.info("wsgTrustManagerFactory certificate: $truststoreCertificate")

            trustStore.load(FileInputStream(truststoreCertificate), truststoreKey.toCharArray())
            trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
            trustManagerFactory?.init(trustStore)
            trustManagerFactory
        }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
