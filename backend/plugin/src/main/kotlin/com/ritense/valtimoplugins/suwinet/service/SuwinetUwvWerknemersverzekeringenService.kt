package com.ritense.valtimoplugins.suwinet.service

import com.ritense.valtimoplugins.dkd.uwvdossierwerknemersverzekeringengsd.FWI
import com.ritense.valtimoplugins.dkd.uwvdossierwerknemersverzekeringengsd.ObjectFactory
import com.ritense.valtimoplugins.dkd.uwvdossierwerknemersverzekeringengsd.UWVInfo
import com.ritense.valtimoplugins.dkd.uwvdossierwerknemersverzekeringengsd.UWVPersoonsWvInfoResponse
import com.ritense.valtimoplugins.suwinet.client.SuwinetSOAPClient
import com.ritense.valtimoplugins.suwinet.client.SuwinetSOAPClientConfig
import com.ritense.valtimoplugins.suwinet.dynamic.DynamicResponseFactory
import com.ritense.valtimoplugins.suwinet.error.SuwinetError
import com.ritense.valtimoplugins.suwinet.exception.SuwinetResultFWIException
import com.ritense.valtimoplugins.suwinet.exception.SuwinetResultNotFoundException
import com.ritense.valtimoplugins.suwinet.model.DynamicResponseDto
import com.ritense.valtimoplugins.suwinet.util.PeriodFilter
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.xml.ws.WebServiceException
import jakarta.xml.ws.soap.SOAPFaultException
import java.time.LocalDate
import org.springframework.util.StringUtils

/**
 * Retrieves UWV werknemersverzekeringen data (uitkeringsverhoudingen, uitkeringsperioden with
 * maatregelen/kortingen, arbeidsongeschiktheid, werkloosheid, aanvragen) via
 * UWVDossierWerknemersverzekeringenGSD-v0200, operation UWVPersoonsWvInfo.
 */
class SuwinetUwvWerknemersverzekeringenService(
    private val suwinetSOAPClient: SuwinetSOAPClient,
    private val dynamicResponseFactory: DynamicResponseFactory
) {
    private lateinit var soapClientConfig: SuwinetSOAPClientConfig

    var suffix: String? = ""

    fun setConfig(soapClientConfig: SuwinetSOAPClientConfig, suffix: String?) {
        this.soapClientConfig = soapClientConfig
        this.suffix = suffix
    }

    fun createUwvInfoService(): UWVInfo {
        var completeUrl = this.soapClientConfig.baseUrl + SERVICE_PATH

        if (StringUtils.hasText(suffix)) {
            completeUrl = completeUrl.plus(suffix)
        }

        return suwinetSOAPClient
            .getService<UWVInfo>(
                completeUrl,
                soapClientConfig.connectionTimeout,
                soapClientConfig.receiveTimeout,
                soapClientConfig.authConfig
            )
    }

    fun getUwvWerknemersverzekeringenByBsn(
        bsn: String,
        uwvInfoService: UWVInfo,
        dynamicProperties: List<String> = listOf(),
        periodStart: LocalDate? = null,
        periodEnd: LocalDate? = null
    ): DynamicResponseDto? {
        logger.info { "Getting UWV werknemersverzekeringen info from ${soapClientConfig.baseUrl + SERVICE_PATH + (this.suffix ?: "")}" }
        try {
            val request = objectFactory
                .createUWVPersoonsWvInfo()
                .apply {
                    burgerservicenr = bsn
                }

            return uwvInfoService
                .uwvPersoonsWvInfo(request)
                .unwrapResponse(dynamicProperties, periodStart, periodEnd)

        } catch (e: SOAPFaultException) {
            logger.error(e) { "SOAPFaultException - Error getting UWV werknemersverzekeringen info" }
            throw SuwinetError(e, "SUWINET_CONNECT_ERROR")
        } catch (e: WebServiceException) {
            logger.error(e) { "WebServiceException - Error getting UWV werknemersverzekeringen info" }
            throw SuwinetError(e, "SUWINET_CONNECT_ERROR")
        } catch (e: Exception) {
            logger.error(e) { "Other Exception - Error getting UWV werknemersverzekeringen info" }
            throw SuwinetError(e, "SUWINET_CONNECT_ERROR")
        }
    }

    private fun UWVPersoonsWvInfoResponse.unwrapResponse(
        dynamicProperties: List<String>,
        periodStart: LocalDate? = null,
        periodEnd: LocalDate? = null
    ): DynamicResponseDto? {
        val responseValue = content
            .firstOrNull()
            ?.value
            ?: throw IllegalStateException("UWVPersoonsWvInfoResponse contains no value")

        return when (responseValue) {
            is UWVPersoonsWvInfoResponse.ClientSuwi -> {
                if (periodStart != null && periodEnd != null) {
                    responseValue.applyPeriodFilter(periodStart, periodEnd)
                }
                DynamicResponseDto(
                    properties = getAvailableProperties(responseValue),
                    dynamicProperties = getDynamicProperties(responseValue, dynamicProperties)
                )
            }

            is FWI -> {
                throw SuwinetResultFWIException(responseValue.foutOrWaarschuwingOrInformatie.joinToString { "${it.name} / ${it.value}\n" })
            }

            else -> {
                val nietsGevonden = objectFactory.createNietsGevonden("test")
                if (nietsGevonden.name.equals(content[0].name)) {
                    // Same as UWV inkomsten: an empty result, so the result variable is always set.
                    DynamicResponseDto(emptyList(), emptyMap<String, Any>())
                } else {
                    throw SuwinetResultNotFoundException("SuwiNet response: $responseValue")
                }
            }
        }
    }

    /**
     * Mutates the ClientSuwi response in-place: removes uitkeringsverhoudingen, uitkeringsperioden
     * and maatregelen that do not overlap with [periodStart, periodEnd].
     *
     * XSD date fields use yyyyMMdd format (sml:Datum).
     * Field mapping from BodyReaction.xsd:
     *   Uitkeringsverhouding:          DatBUitkeringsverhouding / DatEUitkeringsverhouding
     *   Uitkeringsperiode:             DatBUitkeringsperiode / DatEUitkeringsperiode
     *   RedenMaatregelMbtUitkeringUwv: DatBMaatregelMbtUitkering / DatEMaatregelMbtUitkering
     */
    private fun UWVPersoonsWvInfoResponse.ClientSuwi.applyPeriodFilter(
        periodStart: LocalDate,
        periodEnd: LocalDate
    ) {
        uitkeringsverhouding.removeIf { uv ->
            !PeriodFilter.overlaps(uv.datBUitkeringsverhouding, uv.datEUitkeringsverhouding, periodStart, periodEnd)
        }
        uitkeringsverhouding.forEach { uv ->
            uv.uitkeringsperiode.removeIf { up ->
                !PeriodFilter.overlaps(up.datBUitkeringsperiode, up.datEUitkeringsperiode, periodStart, periodEnd)
            }
            uv.uitkeringsperiode.forEach { up ->
                up.redenMaatregelMbtUitkeringUwv.removeIf { m ->
                    !PeriodFilter.overlaps(m.datBMaatregelMbtUitkering, m.datEMaatregelMbtUitkering, periodStart, periodEnd)
                }
            }
        }
    }

    private fun getAvailableProperties(info: Any): List<String> =
        dynamicResponseFactory.toFlatMap(info).keys.toList()

    private fun getDynamicProperties(info: Any, dynamicProperties: List<String>): Any {
        val propertiesMap: MutableMap<String, Any?> = mutableMapOf()
        val flatMap = dynamicResponseFactory.toFlatMap(info)

        dynamicProperties.forEach { prop ->
            if (flatMap.containsKey(prop)) {
                propertiesMap[prop] = flatMap[prop]
            }
            if (prop.endsWith('*')) {
                val prefixValue = prop.trimEnd('*')
                flatMap.keys.forEach {
                    if (it.startsWith(prefixValue)) {
                        propertiesMap[it] = flatMap[it]
                    }
                }
            }
        }

        return dynamicResponseFactory.flatMapToNested(propertiesMap)
    }

    companion object {
        const val SERVICE_PATH = "UWVDossierWerknemersverzekeringenGSD-v0200"
        private val objectFactory = ObjectFactory()
        private val logger = KotlinLogging.logger {}
    }
}
