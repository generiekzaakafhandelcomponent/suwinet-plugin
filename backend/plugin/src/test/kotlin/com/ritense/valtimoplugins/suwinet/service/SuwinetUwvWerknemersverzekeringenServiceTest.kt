package com.ritense.valtimoplugins.suwinet.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ritense.valtimo.TestHelper
import com.ritense.valtimoplugins.BaseTest
import com.ritense.valtimoplugins.dkd.uwvdossierwerknemersverzekeringengsd.UWVInfo
import com.ritense.valtimoplugins.dkd.uwvdossierwerknemersverzekeringengsd.UWVPersoonsWvInfo
import com.ritense.valtimoplugins.dkd.uwvdossierwerknemersverzekeringengsd.UWVPersoonsWvInfoResponse
import com.ritense.valtimoplugins.suwinet.client.SuwinetSOAPClient
import com.ritense.valtimoplugins.suwinet.client.SuwinetSOAPClientConfig
import com.ritense.valtimoplugins.suwinet.dynamic.DynamicResponseFactory
import com.ritense.valtimoplugins.suwinet.error.SuwinetError
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@MockitoSettings(strictness = Strictness.LENIENT)
internal class SuwinetUwvWerknemersverzekeringenServiceTest : BaseTest() {

    @Mock
    lateinit var uwvService: UWVInfo

    @Mock
    lateinit var suwinetSOAPClientConfig: SuwinetSOAPClientConfig

    private lateinit var service: SuwinetUwvWerknemersverzekeringenService

    @BeforeEach
    fun setup() {
        val suwinetSOAPClient: SuwinetSOAPClient = Mockito.mock()
        service = SuwinetUwvWerknemersverzekeringenService(
            suwinetSOAPClient,
            DynamicResponseFactory(jacksonObjectMapper()),
        )
        service.setConfig(suwinetSOAPClientConfig, "")
    }

    private fun givenResponse(fixture: String) {
        whenever(uwvService.uwvPersoonsWvInfo(any(UWVPersoonsWvInfo::class.java))).thenReturn(
            TestHelper.unmarshal<UWVPersoonsWvInfoResponse>(fixture),
        )
    }

    @Test
    fun `wildcard returns uitkeringsverhoudingen including maatregel fields`() {
        givenResponse("UWVDossierWerknemersverzekeringenGSD_UWVPersoonsWvInfo_maatregel.xml")

        val result = service.getUwvWerknemersverzekeringenByBsn(
            "999993653", uwvService, dynamicProperties = listOf("*"),
        )

        val dynamic = result!!.dynamicProperties as Map<*, *>
        val verhoudingen = dynamic["uitkeringsverhouding"] as List<*>
        assertEquals(2, verhoudingen.size)

        val perioden = (verhoudingen[0] as Map<*, *>)["uitkeringsperiode"] as List<*>
        val maatregel = ((perioden[1] as Map<*, *>)["redenMaatregelMbtUitkeringUwv"] as List<*>)[0] as Map<*, *>
        assertEquals("2025-04-01", maatregel["datBMaatregelMbtUitkering"])
        assertEquals("2025-04-30", maatregel["datEMaatregelMbtUitkering"])
        assertEquals(12, maatregel["cdRedenMaatregelMbtUitkeringUwv"])
        val korting = maatregel["percKortingUitkering"] as Map<*, *>
        assertEquals("02500", korting["percentage"])
        assertEquals("+", korting["cdPositiefNegatief"])

        assertTrue(
            result.properties.contains(
                "uitkeringsverhouding[0].uitkeringsperiode[1].redenMaatregelMbtUitkeringUwv[0].percKortingUitkering.percentage"
            )
        )
    }

    @Test
    fun `prefix selection returns only the requested subtree`() {
        givenResponse("UWVDossierWerknemersverzekeringenGSD_UWVPersoonsWvInfo_maatregel.xml")

        val result = service.getUwvWerknemersverzekeringenByBsn(
            "999993653", uwvService, dynamicProperties = listOf("werkloosheidClient*"),
        )

        val dynamic = result!!.dynamicProperties as Map<*, *>
        assertEquals(setOf("werkloosheidClient"), dynamic.keys)
    }

    @Test
    fun `period filter removes non-overlapping verhoudingen, perioden and maatregelen`() {
        givenResponse("UWVDossierWerknemersverzekeringenGSD_UWVPersoonsWvInfo_maatregel.xml")

        val result = service.getUwvWerknemersverzekeringenByBsn(
            "999993653",
            uwvService,
            dynamicProperties = listOf("*"),
            periodStart = LocalDate.of(2025, 3, 1),
            periodEnd = LocalDate.of(2025, 3, 31),
        )

        val verhoudingen = (result!!.dynamicProperties as Map<*, *>)["uitkeringsverhouding"] as List<*>
        assertEquals(1, verhoudingen.size)
        val perioden = (verhoudingen[0] as Map<*, *>)["uitkeringsperiode"] as List<*>
        assertEquals(1, perioden.size)
        assertEquals("2025-03-01", (perioden[0] as Map<*, *>)["datBUitkeringsperiode"])
        assertTrue(result.properties.none { it.contains("redenMaatregelMbtUitkeringUwv") })
    }

    @Test
    fun `niets gevonden returns an empty result like UWV inkomsten`() {
        givenResponse("UWVDossierWerknemersverzekeringenGSD_UWVPersoonsWvInfo_Nietsgevonden.xml")

        val result = service.getUwvWerknemersverzekeringenByBsn(
            "999993653", uwvService, dynamicProperties = listOf("*"),
        )

        assertNotNull(result)
        assertTrue(result.properties.isEmpty())
        assertTrue((result.dynamicProperties as Map<*, *>).isEmpty())
    }

    @Test
    fun `FWI response results in a connection error`() {
        givenResponse("UWVDossierWerknemersverzekeringenGSD_UWVPersoonsWvInfo_FWI.xml")

        val error = assertThrows<SuwinetError> {
            service.getUwvWerknemersverzekeringenByBsn(
                "999993653", uwvService, dynamicProperties = listOf("*"),
            )
        }
        assertEquals("SUWINET_CONNECT_ERROR", error.errorCode)
    }
}
