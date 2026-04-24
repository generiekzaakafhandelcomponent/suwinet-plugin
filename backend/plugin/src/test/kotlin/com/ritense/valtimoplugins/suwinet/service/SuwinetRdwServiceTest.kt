package com.ritense.valtimoplugins.suwinet.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ritense.valtimo.TestHelper
import com.ritense.valtimoplugins.BaseTest
import com.ritense.valtimoplugins.dkd.rdwdossier.KentekenInfo
import com.ritense.valtimoplugins.dkd.rdwdossier.KentekenInfoResponse
import com.ritense.valtimoplugins.dkd.rdwdossier.RDW
import com.ritense.valtimoplugins.dkd.rdwdossier.VoertuigbezitInfoPersoonResponse
import com.ritense.valtimoplugins.suwinet.client.SuwinetSOAPClient
import com.ritense.valtimoplugins.suwinet.client.SuwinetSOAPClientConfig
import com.ritense.valtimoplugins.suwinet.dynamic.DynamicResponseFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import kotlin.test.junit5.JUnit5Asserter.assertEquals

@MockitoSettings(strictness = Strictness.LENIENT)
internal class SuwinetRdwServiceTest : BaseTest() {
    private val logger = KotlinLogging.logger {}

    @Mock
    lateinit var rdwService: RDW

    @Mock
    lateinit var suwinetSOAPClient: SuwinetSOAPClient

    @Mock
    lateinit var suwinetSOAPClientConfig: SuwinetSOAPClientConfig

    private lateinit var suwinetRdwService: SuwinetRdwService

    lateinit var testHelper: TestHelper

    @BeforeEach
    fun setup() {
        testHelper = TestHelper
        suwinetSOAPClient = Mockito.mock()
        val dynamicResponseFactory = DynamicResponseFactory(jacksonObjectMapper())
        suwinetRdwService = SuwinetRdwService(suwinetSOAPClient, dynamicResponseFactory)
        suwinetRdwService.setConfig(suwinetSOAPClientConfig, "")
    }

    @Test
    fun `retrieving voertuig bezit with 2 voertuigen from RDW to Motorvoertuigen lijst`() {
        // given
        val bsn = "111111110"
        val kentekenMh74dz = "MH74DZ"
        val kenteken16zdlx = "16ZDLX"
        // when
        whenever(rdwService.voertuigbezitInfoPersoon(any())).thenReturn(
            testHelper.unmarshal<VoertuigbezitInfoPersoonResponse>(
                "RDWDossierGSD_VoertuigbezitInfoPersoon_111111110.xml",
            ),
        )
        val paramKentekenInfo = ArgumentCaptor.forClass(KentekenInfo::class.java)
        whenever(
            rdwService.kentekenInfo(paramKentekenInfo.capture()),
        ).thenAnswer {
            val kentekenInfo = it.arguments[0] as KentekenInfo
            testHelper.unmarshal<KentekenInfoResponse>(
                "RDWDossierGSD_KentekenInfo_${kentekenInfo.kentekenVoertuig}.xml",
            )
        }
        val result =
            suwinetRdwService.getVoertuigbezitInfoPersoonByBsn(
                bsn,
                rdwService,
                dynamicProperties = listOf("*"),
            )
        // then
        val aansprakelijken = (result.dynamicProperties as Map<*, *>)["aansprakelijken"] as List<*>
        assertEquals("found motorvoertuigen should be 2", 2, aansprakelijken.size)
        assertEquals(
            "found motorvoertuig should have kenteken",
            kentekenMh74dz,
            ((aansprakelijken[0] as Map<*, *>)["voertuig"] as Map<*, *>)["kentekenVoertuig"],
        )
        assertEquals(
            "found motorvoertuig should have kenteken",
            kenteken16zdlx,
            ((aansprakelijken[1] as Map<*, *>)["voertuig"] as Map<*, *>)["kentekenVoertuig"],
        )
    }

    @Test
    fun `unhappy path return nietgevonden retrieving voertuig bezit`() {
        // given
        val bsn = "333333330"

        // when
        whenever(rdwService.voertuigbezitInfoPersoon(any())).thenReturn(
            testHelper.unmarshal<VoertuigbezitInfoPersoonResponse>(
                "RDWDossierGSD_VoertuigbezitInfoPersoon_Nietsgevonden.xml",
            ),
        )
        val result =
            suwinetRdwService.getVoertuigbezitInfoPersoonByBsn(
                bsn,
                rdwService,
                dynamicProperties = listOf("*"),
            )

        // then
        assertEquals("List motorvoertuigen should be empty", true, result.properties.isEmpty())
    }

    @Test
    fun `retrieving voertuig bezit with 3 voertuigen and missing kenteken to Motorvoertuigen lijst`() {
        // given
        val bsn = "111111110"
        val kentekenMh74dz = "MH74DZ"
        val kentekenAa00bb = "ONBEKE"
        val kenteken16zdlx = "16ZDLX"

        // when
        whenever(rdwService.voertuigbezitInfoPersoon(any())).thenReturn(
            testHelper.unmarshal<VoertuigbezitInfoPersoonResponse>(
                "RDWDossierGSD_VoertuigbezitInfoPersoon_111111110_voertuig_ontbreekt.xml",
            ),
        )

        val paramKentekenInfo = ArgumentCaptor.forClass(KentekenInfo::class.java)
        whenever(
            rdwService.kentekenInfo(paramKentekenInfo.capture()),
        ).thenAnswer {
            val kentekenInfo = it.arguments[0] as KentekenInfo
            testHelper.unmarshal<KentekenInfoResponse>(
                "RDWDossierGSD_KentekenInfo_${kentekenInfo.kentekenVoertuig}.xml",
            )
        }
        val result =
            suwinetRdwService.getVoertuigbezitInfoPersoonByBsn(
                bsn,
                rdwService,
                dynamicProperties = listOf("*"),
            )
        result.let { logger.info { "voertuig: $it" } }
        // then
        val aansprakelijken = (result.dynamicProperties as Map<*, *>)["aansprakelijken"] as List<*>
        assertEquals("found motorvoertuigen should be 3", 3, aansprakelijken.size)
        assertEquals(
            "found motorvoertuig should have kenteken",
            kentekenMh74dz,
            ((aansprakelijken[0] as Map<*, *>)["voertuig"] as Map<*, *>)["kentekenVoertuig"],
        )
        assertEquals(
            "found motorvoertuig should have kenteken",
            kentekenAa00bb,
            ((aansprakelijken[1] as Map<*, *>)["voertuig"] as Map<*, *>)["kentekenVoertuig"],
        )
        assertEquals(
            "found motorvoertuig should have kenteken",
            kenteken16zdlx,
            ((aansprakelijken[2] as Map<*, *>)["voertuig"] as Map<*, *>)["kentekenVoertuig"],
        )
    }

    @Test
    fun `retrieving voertuig bezit with 1 voertuig from RDW to Motorvoertuigen lijst`() {
        // given
        val bsn = "444444440"
        val kenteken = "MH74DZ"

        // when
        whenever(rdwService.voertuigbezitInfoPersoon(any())).thenReturn(
            testHelper.unmarshal<VoertuigbezitInfoPersoonResponse>(
                "RDWDossierGSD_VoertuigbezitInfoPersoon_444444440.xml",
            ),
        )
        whenever(rdwService.kentekenInfo(any())).thenReturn(
            testHelper.unmarshal<KentekenInfoResponse>(
                "RDWDossierGSD_KentekenInfo_MH74DZ.xml",
            ),
        )

        val result =
            suwinetRdwService.getVoertuigbezitInfoPersoonByBsn(
                bsn,
                rdwService,
                dynamicProperties = listOf("*"),
            )

        // then
        val aansprakelijken = (result.dynamicProperties as Map<*, *>)["aansprakelijken"] as List<*>
        assertEquals("found motorvoertuigen should be 1", 1, aansprakelijken.size)
        assertEquals(
            "found motorvoertuig should have kenteken",
            kenteken,
            ((aansprakelijken[0] as Map<*, *>)["voertuig"] as Map<*, *>)["kentekenVoertuig"],
        )
    }
}
