package com.sensetime.lib_rtc

import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.sensetime.lib_rtc.util.JwtTokenUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject
import kotlin.concurrent.thread

class WebSocketManager {
    private val TAG = "WebSocketManager"
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null

    private var agoraAppId: String? = null
    private var agoraToken: String? = null
    private var agoraChannelId: String? = null
    private var sessionId: String? = null

    private var showInitPrompt = true
    private var mChannelInit = false

    private var config: WebSocketConfig? = null
    private var callback: MessageCallback? = null

    var rtcInit: ((String?, String?, String?) -> Unit)? = null

    fun isAudioOnly(): Boolean {
        if (config == null) {
            throw RuntimeException("请先启动socket")
        }
        return config!!.audioOnly
    }

    private fun start(config: WebSocketConfig) {
        this.config = config
        val url = config.url
        try {
            val originalUri = Uri.parse(url)
            val builder = originalUri.buildUpon()
                .appendQueryParameter("audio_only", config.audioOnly.toString())
                .appendQueryParameter("voice_type", config.voiceType)
                .appendQueryParameter(
                    "jwt",
                    JwtTokenUtil.generateToken(config.iss, config.secretKey)
                )
                .appendQueryParameter("system_prompt", config.systemPrompt)
                .appendQueryParameter("vad_neg_threshold", config.vadNegThreshold.toString())
                .appendQueryParameter("vad_pos_threshold", config.vadPosThreshold.toString())
                .appendQueryParameter(
                    "vad_min_speech_duration_ms",
                    config.vadMinSpeechDurationMs.toString()
                )
                .appendQueryParameter(
                    "vad_min_silence_duration_ms",
                    config.vadMinSilenceDurationMs.toString()
                )
                .appendQueryParameter("video_width", config.videoWidth.toString())
                .appendQueryParameter("video_height", config.videoHeight.toString())
            if (!config.modelId.isNullOrEmpty()) {
                builder.appendQueryParameter("model", config.modelId)
            }
//            if (showInitPrompt) {
            builder.appendQueryParameter("init_prompt", "你好")
//            } else {
//                showInitPrompt = true
//            }

            val newUri = builder.build()
            client = OkHttpClient()
            val request = Request.Builder().url(newUri.toString()).build()
            webSocket = client?.newWebSocket(request, MyWebSocketListener())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSessionId(): String? = sessionId

    fun close() {
        thread {
            Log.d(TAG, "ws close")
            if (webSocket != null) {
                webSocket?.close(1000, "Closing the WebSocket connection")
                client?.dispatcher?.executorService?.shutdown()
                client?.connectionPool?.evictAll()
            }
            agoraAppId = ""
            agoraToken = ""
            agoraChannelId = ""
        }
    }

    private inner class MyWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.d(TAG, "onOpen")
            callback?.onOpen()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            try {
                val msg = JSONObject(text)
                Log.d(TAG, "Message from server: $msg")
                when (msg.getString("type")) {
                    "SetSessionId" -> {
                        sessionId = msg.getString("session_id")
                        Log.d(TAG, "got agora session_id: $sessionId")
                    }

                    "SetAgoraAppid" -> {
                        agoraAppId = msg.getString("agora_appid")
                        Log.d(TAG, "got agora appid: $agoraAppId")
                    }

                    "SetAgoraToken" -> {
                        agoraToken = msg.getString("agora_token")
                        Log.d(TAG, "got agora token: $agoraToken")
                    }

                    "SetAgoraChannelId" -> {
                        agoraChannelId = msg.getString("agora_channel_id")
                        Log.d(TAG, "got agora channel: $agoraChannelId")
                    }

                    "StartSpeaking" -> {
                        callback?.onStartSpeaking()
                    }

                    "StopSpeaking" -> {
                        callback?.onStopSpeaking()
                    }

                    "ResponseTextSegment" -> {
                        callback?.onResponseTextSegment(msg.getString("text"))
                    }

                    "ResponseEndTextStream" -> {
                        callback?.onResponseFullText(msg.getString("text"))
                    }

                    "AudioAccepting" -> {
                        callback?.audioAccepting()
                    }

                    "VideoAccepting" -> {
                        callback?.videoAccepting()
                    }
                }

                // 调用回调接口的方法，将接收到的消息传递出去
                if (!mChannelInit
                    && !TextUtils.isEmpty(agoraAppId)
                    && !TextUtils.isEmpty(agoraToken)
                    && !TextUtils.isEmpty(agoraChannelId)
                ) {
                    rtcInit?.invoke(
                        agoraAppId,
                        agoraToken,
                        agoraChannelId
                    )
                    mChannelInit = true
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                Log.e(TAG, "Json parse error")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e(TAG, "WebSocket error: ${t.message}")
            callback?.onWebRtcError()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.e(TAG, "WebSocket connection closed, code: $code, reason: $reason")
        }
    }

    fun startWithCallback(config: WebSocketConfig, callback: MessageCallback?) {
        this.callback = callback
        start(config)
    }
}