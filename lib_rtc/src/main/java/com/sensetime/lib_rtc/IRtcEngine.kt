package com.sensetime.lib_rtc

interface IRtcEngine {
    fun close()
    fun leaveChannel()
    fun startVideo()
    fun stopVideo()
    fun flipCamera()
    fun switchMicro(enable: Boolean)
    fun setCameraFocusPositionInPreview(positionX: Float, positionY: Float)
}