package com.sensetime.lib_rtc

import androidx.appcompat.app.AppCompatActivity

class SenseEngine constructor(
    private val activity: AppCompatActivity,
    private val callback: ISenseEngineCallback
) : IRtcEngine {
    private var rtcEngine: AgoraRtcEngine? = null
    private var webSocketManager: WebSocketManager? = null

    fun start(config: WebSocketConfig? = null) {
        val socketConfig = config ?: WebSocketConfig.Builder().build(activity)
        rtcEngine = AgoraRtcEngine(activity, callback, WebSocketManager().apply {
            webSocketManager = this
        })
        webSocketManager?.rtcInit = { appId, token, channelId ->
            activity.runOnUiThread {
                initializeAndJoinChannel(appId, token, channelId)
            }
        }
        webSocketManager?.startWithCallback(socketConfig, callback)
    }

    override fun close() {
        rtcEngine?.close()
        webSocketManager?.close()
    }

    override fun leaveChannel() {
        rtcEngine?.leaveChannel()
    }

    override fun startVideo() {
        rtcEngine?.startVideo()
    }

    override fun stopVideo() {
        rtcEngine?.stopVideo()
    }

    override fun flipCamera() {
        rtcEngine?.flipCamera()
    }

    override fun switchMicro(enable: Boolean) {
        rtcEngine?.switchMicro(enable)
    }

    override fun setCameraFocusPositionInPreview(positionX: Float, positionY: Float) {
        rtcEngine?.setCameraFocusPositionInPreview(positionX, positionY)
    }

    private fun initializeAndJoinChannel(
        agoraAppId: String?,
        agoraToken: String?,
        agoraChannelId: String?
    ) {
        rtcEngine?.initializeAndJoinChannel(agoraAppId, agoraToken, agoraChannelId)
    }
}