package com.sensetime.lib_rtc

import android.view.TextureView

interface IRtcEngineCallback {
    fun onUserRemoteSpeaking()
    fun onUserRemoteStoppedSpeaking()
    fun addLocalVideoSurface(surface: TextureView)
}