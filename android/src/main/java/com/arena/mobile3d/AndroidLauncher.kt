package com.arena.mobile3d

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = true
            useWakelock = true
            r = 8
            g = 8
            b = 8
            a = 8
            depth = 16
            stencil = 0
            numSamples = 0
        }
        initialize(AdventureGame(), config)
    }
}
