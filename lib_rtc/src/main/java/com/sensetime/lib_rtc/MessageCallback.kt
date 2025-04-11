package com.sensetime.lib_rtc

interface MessageCallback {
    fun onStartSpeaking()
    fun onStopSpeaking()
    fun onResponseTextSegment(string: String?)
    fun audioAccepting()
    fun videoAccepting()
    fun onResponseFullText(string: String?)
    fun onOpen()
    fun onWebRtcError()
}