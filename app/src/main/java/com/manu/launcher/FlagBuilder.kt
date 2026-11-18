package com.manu.launcher

import org.json.JSONObject

/**
 * Bloxstrap-inspired Engine Settings → ClientAppSettings.json
 * Only flags that are commonly used on mobile; many PC-only flags omitted.
 * Note: since late 2025 Roblox allowlists flags — some may be ignored by client.
 */
object FlagBuilder {

    data class State(
        var fps: Int = 120,
        var showFps: Boolean = true,
        var preferVulkan: Boolean = true,
        var graySky: Boolean = false,
        var noMsaa: Boolean = false,
        var msaaLevel: Int = 0,
        var frmLevel: Int = -1,
        var renderMode: Int = 2, // 0 auto 1 gl 2 vulkan
        var dynamicRes: String = "auto",
        var minGraphics: Boolean = false,
        var reduceMotion: Boolean = false,
        var haptic: Boolean = true,
        var oneClickBoost: Boolean = false,
        var simplifyModels: Boolean = false,
        var lowTextures: Boolean = false,
        var meshOpt: Boolean = false,
        var preloadModels: Boolean = false,
        var lessShadows: Boolean = false,
        var noPostFx: Boolean = false,
        var noGrass: Boolean = false,
        var hideNames: Boolean = false,
        var noBlurMenu: Boolean = false,
        var noVfx: Boolean = false,
        var noTextures: Boolean = false,
        var animCache: Boolean = false,
        var physics60: Boolean = false,
        // Bloxstrap-style extras
        var disableTelemetry: Boolean = false,
        var pauseVoxelizer: Boolean = false,
        var noFog: Boolean = false,
        var lowWater: Boolean = false,
        var shadowIntensity: Int = -1, // -1 default, 0 off
        var textureOverride: Int = -1, // -1 off, 0..3
        var lightingMode: Int = 0 // 0 default, 1 voxel, 2 shadowmap, 3 future
    )

    fun build(s: State): String {
        val o = JSONObject()

        o.put("DFIntTaskSchedulerTargetFps", s.fps)

        if (s.showFps) o.put("FFlagDebugDisplayFPS", true)

        when (s.renderMode) {
            1 -> o.put("FFlagDebugGraphicsPreferOpenGL", true)
            2 -> o.put("FFlagDebugGraphicsPreferVulkan", true)
        }
        if (s.preferVulkan && s.renderMode == 0) {
            o.put("FFlagDebugGraphicsPreferVulkan", true)
        }

        if (s.graySky) o.put("FFlagDebugSkyGray", true)

        if (s.noMsaa || s.msaaLevel == 0) {
            o.put("FIntDebugForceMSAASamples", 0)
            o.put("FFlagDebugForceMSAADisabled", true)
        } else if (s.msaaLevel > 0) {
            o.put("FIntDebugForceMSAASamples", s.msaaLevel)
        }

        if (s.frmLevel > 0) {
            o.put("DFIntDebugFRMQualityLevelOverride", s.frmLevel)
        } else if (s.minGraphics || s.oneClickBoost) {
            o.put("DFIntDebugFRMQualityLevelOverride", 1)
        }

        if (s.dynamicRes != "auto") {
            val lvl = when (s.dynamicRes) {
                "144", "240" -> 1
                "360", "480" -> 5
                "720" -> 10
                else -> 15
            }
            if (!o.has("DFIntDebugFRMQualityLevelOverride"))
                o.put("DFIntDebugFRMQualityLevelOverride", lvl)
        }

        val tex = when {
            s.textureOverride >= 0 -> s.textureOverride
            s.noTextures -> 0
            s.lowTextures || s.oneClickBoost -> 1
            else -> -1
        }
        if (tex >= 0) {
            o.put("DFFlagTextureQualityOverrideEnabled", true)
            o.put("DFIntTextureQualityOverride", tex)
        }

        if (s.lessShadows || s.oneClickBoost || s.shadowIntensity == 0) {
            o.put("FIntRenderShadowIntensity", 0)
            o.put("FIntRenderLocalLightUpdatesMax", 1)
        } else if (s.shadowIntensity > 0) {
            o.put("FIntRenderShadowIntensity", s.shadowIntensity)
        }

        if (s.noPostFx || s.oneClickBoost || s.noVfx) {
            o.put("FFlagDisablePostFx", true)
        }

        if (s.noGrass) {
            o.put("FIntFRMMinGrassDistance", 0)
            o.put("FIntFRMMaxGrassDistance", 0)
            o.put("FIntRenderGrassDetailStrands", 0)
            o.put("FIntRenderGrassHeightScaler", 0)
        }

        if (s.pauseVoxelizer || s.oneClickBoost) {
            o.put("DFFlagDebugPauseVoxelizer", true)
        }

        if (s.lowWater) {
            o.put("FFlagRenderWaterReflectionType", 0)
        }

        if (s.disableTelemetry) {
            o.put("FFlagDebugDisableTelemetryEphemeralCounter", true)
            o.put("FFlagDebugDisableTelemetryEventIngest", true)
            o.put("FFlagDebugDisableTelemetryPoint", true)
            o.put("FFlagDebugDisableTelemetryV2Counter", true)
            o.put("FFlagDebugDisableTelemetryV2Event", true)
        }

        when (s.lightingMode) {
            1 -> o.put("DFFlagDebugRenderForceTechnologyVoxel", true)
            2 -> { /* shadowmap - often default */ }
            3 -> o.put("FFlagDebugForceFutureIsBrightPhase3", true)
        }

        if (s.hideNames) o.put("FFlagDebugDisplayNames", false)
        if (s.noBlurMenu) o.put("FFlagDebugDisableMenuBlur", true)
        if (s.reduceMotion) o.put("FFlagDebugForceReducedMotion", true)
        if (s.animCache) o.put("FFlagDebugEnableAnimationCache", true)
        if (s.haptic) o.put("FFlagDebugEnableHaptics", true)
        if (s.physics60) o.put("DFIntTaskSchedulerTargetFps", minOf(s.fps, 60))

        if (s.simplifyModels || s.meshOpt) {
            o.put("DFIntCSGLevelOfDetailSwitchingDistance", 0)
            o.put("DFIntCSGLevelOfDetailSwitchingDistanceL12", 0)
            o.put("DFIntCSGLevelOfDetailSwitchingDistanceL23", 0)
            o.put("DFIntCSGLevelOfDetailSwitchingDistanceL34", 0)
        }

        return o.toString(2)
    }

    /** Ready-made Bloxstrap-style presets */
    fun applyPreset(name: String, s: State) {
        // reset soft defaults first
        s.oneClickBoost = false
        s.minGraphics = false
        s.graySky = false
        s.noMsaa = false
        s.noGrass = false
        s.noPostFx = false
        s.noVfx = false
        s.noTextures = false
        s.lowTextures = false
        s.lessShadows = false
        s.pauseVoxelizer = false
        s.disableTelemetry = false
        s.preferVulkan = true
        s.renderMode = 2
        s.shadowIntensity = -1
        s.textureOverride = -1
        s.frmLevel = -1

        when (name) {
            "fps_max" -> {
                s.fps = 240
                s.showFps = true
                s.oneClickBoost = true
                s.noMsaa = true
                s.noPostFx = true
                s.lessShadows = true
                s.pauseVoxelizer = true
                s.preferVulkan = true
            }
            "potato" -> {
                s.fps = 60
                s.oneClickBoost = true
                s.minGraphics = true
                s.graySky = true
                s.noMsaa = true
                s.noGrass = true
                s.noPostFx = true
                s.noVfx = true
                s.lowTextures = true
                s.lessShadows = true
                s.pauseVoxelizer = true
                s.textureOverride = 0
                s.frmLevel = 1
            }
            "low" -> {
                s.fps = 90
                s.lowTextures = true
                s.lessShadows = true
                s.noPostFx = true
                s.noGrass = true
                s.frmLevel = 1
                s.msaaLevel = 0
                s.noMsaa = true
            }
            "balanced" -> {
                s.fps = 120
                s.showFps = true
                s.frmLevel = 10
                s.preferVulkan = true
            }
            "quality" -> {
                s.fps = 60
                s.frmLevel = 21
                s.msaaLevel = 4
                s.noMsaa = false
                s.preferVulkan = true
                s.shadowIntensity = 50
                s.textureOverride = 3
            }
            "gray_fps" -> {
                s.fps = 120
                s.graySky = true
                s.showFps = true
            }
            "compet" -> {
                s.fps = 240
                s.showFps = true
                s.noMsaa = true
                s.noPostFx = true
                s.lessShadows = true
                s.noGrass = true
                s.disableTelemetry = true
                s.preferVulkan = true
                s.pauseVoxelizer = true
            }
            "reset" -> {
                val d = State()
                s.fps = d.fps
                s.showFps = d.showFps
                s.preferVulkan = d.preferVulkan
                s.renderMode = d.renderMode
            }
        }
    }
}
