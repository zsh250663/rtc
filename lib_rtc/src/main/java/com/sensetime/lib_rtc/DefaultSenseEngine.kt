package com.sensetime.lib_rtc

import android.view.TextureView

abstract class DefaultSenseEngine: ISenseEngineCallback {
    override fun onUserRemoteSpeaking() {
    }

    override fun onUserRemoteStoppedSpeaking() {
    }

    override fun addLocalVideoSurface(surface: TextureView) {
    }

    override fun onStartSpeaking() {
    }

    override fun onStopSpeaking() {
    }

    override fun onResponseTextSegment(string: String?) {
    }

    override fun audioAccepting() {
    }

    override fun videoAccepting() {
    }

    override fun onResponseFullText(string: String?) {
    }

    override fun onOpen() {
    }

    override fun onWebRtcError() {
    }
}