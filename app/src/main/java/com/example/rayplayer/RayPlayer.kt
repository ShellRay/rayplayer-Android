package com.example.rayplayer

import android.view.Surface


class RayPlayer {

    companion object {
        init {
            System.loadLibrary("RayPlayer")
        }
    }

    external fun nativeInit()
    external fun nativePrepare(path: String)
    external fun nativeSetSurface(surface: Surface?)

    external fun nativeUpdateSurface(surface: Surface?)
    external fun nativeStart()
    external fun nativePause()
    external fun nativeStop()
    external fun nativeRelease()
}