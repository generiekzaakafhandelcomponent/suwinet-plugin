package com.ritense.valtimoplugins.suwinet.plugin

import com.ritense.plugin.PluginFactory
import com.ritense.plugin.service.PluginService
import com.ritense.valtimoplugins.suwinet.service.SuwinetBrpInfoService

class SuwiNetPluginFactory(
    pluginService: PluginService,
    private val suwinetBrpInfoService: SuwinetBrpInfoService
) : PluginFactory<SuwiNetPlugin>(pluginService) {

    override fun create(): SuwiNetPlugin {
        return SuwiNetPlugin(
            suwinetBrpInfoService
        )
    }
}