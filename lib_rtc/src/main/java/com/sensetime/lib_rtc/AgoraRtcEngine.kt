package com.sensetime.lib_rtc

import android.content.Context
import android.text.TextUtils
import android.view.TextureView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.sensetime.lib_rtc.util.RtcVideoSizeUtil
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.LowLightEnhanceOptions
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class AgoraRtcEngine(
    private val context: Context,
    private val callback: IRtcEngineCallback,
    private val socketManager: WebSocketManager
) : IRtcEngineEventHandler(), DefaultLifecycleObserver, IRtcEngine {
    private var mRtcEngine: RtcEngine? = null
    private val userVolumeMap = mutableMapOf<Int, Int>() // 记录远端用户静音计数
    private val MAX_SILENT_COUNT = 5 // 连续静音次数的阈值
    private var muteLocalAudio: Boolean = false

    override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndication(speakers, totalVolume)
        speakers?.forEach { speaker ->
            val uid = speaker.uid
            val volume = speaker.volume

            if (uid != 0) {
                // 0 表示本地用户
                handleRemoteUserVolume(uid, volume)
            }
        }
    }

    private fun handleRemoteUserVolume(uid: Int, volume: Int) {
        if (volume == 0) {
            // 增加静音计数
            userVolumeMap[uid] = (userVolumeMap[uid] ?: 0) + 1
        } else {
            // 如果音量不为 0，重置计数
            userVolumeMap[uid] = 0
//            LogUtil.d("正在讲话")
            callback.onUserRemoteSpeaking()
        }

        // 检查静音计数是否超过阈值
        if ((userVolumeMap[uid] ?: 0) >= MAX_SILENT_COUNT) {
            onUserStoppedSpeaking(uid)
        }
    }

    private fun onUserStoppedSpeaking(uid: Int) {
//        LogUtil.d("停止讲话")
        callback.onUserRemoteStoppedSpeaking()
        userVolumeMap[uid] = 0
    }

    // 成功加入频道回调
    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        super.onJoinChannelSuccess(channel, uid, elapsed)
//        LogUtil.d("onJoinChannelSuccess:$uid")
    }

    override fun onLeaveChannel(stats: RtcStats?) {
        super.onLeaveChannel(stats)
//        LogUtil.d("onLeaveChannel")
    }

    // 远端用户或主播离开当前频道回调
    override fun onUserOffline(uid: Int, reason: Int) {
        super.onUserOffline(uid, reason)
//        LogUtil.d("onUserOffline:$uid")
    }

    override fun onError(err: Int) {
        super.onError(err)
//        LogUtil.e("onError:$err")
    }

    override fun onConnectionLost() {
        super.onConnectionLost()
//        LogUtil.d("onConnectionLost")
    }

    override fun onConnectionStateChanged(state: Int, reason: Int) {
        super.onConnectionStateChanged(state, reason)
//        LogUtil.d("onConnectionStateChanged:$state,reason:$reason")
    }

    override fun setCameraFocusPositionInPreview(positionX: Float, positionY: Float) {
        mRtcEngine?.setCameraFocusPositionInPreview(positionX, positionY)
    }

    fun initializeAndJoinChannel(
        agoraAppId: String?,
        agoraToken: String?,
        agoraChannelId: String?
    ) {
        if (TextUtils.isEmpty(agoraAppId) || TextUtils.isEmpty(agoraToken) || TextUtils.isEmpty(
                agoraChannelId
            )
        ) {
            return
        }

        try {
            // 创建 RtcEngineConfig 对象，并进行配置
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = agoraAppId
            config.mEventHandler = this
            // 创建并初始化 RtcEngine
            mRtcEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
        mRtcEngine?.enableAudioVolumeIndication(200, 3, true)
//        mRtcEngine?.muteLocalAudioStream(muteLocalAudio)
        adjustVolume(!muteLocalAudio)
        mRtcEngine?.enableAudio()
        mRtcEngine?.setAINSMode(true, 0)

        if (!socketManager.isAudioOnly()) {
            val videoEncoder = VideoEncoderConfiguration()
            videoEncoder.codecType = VideoEncoderConfiguration.VIDEO_CODEC_TYPE.VIDEO_CODEC_H264
            val model = RtcVideoSizeUtil.convert(context)
//            val h = AppManager.getScreenHeightPx() * 1f / AppManager.getScreenWidthPx() * 720
            videoEncoder.dimensions =
                VideoEncoderConfiguration.VideoDimensions(model.width, model.height)
//            videoEncoder.dimensions = VideoEncoderConfiguration.VideoDimensions(1280, 720)
            videoEncoder.orientationMode =
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE
//                if (AppConfig.isPad) {
//                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
//            } else {
//                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
//            }
            videoEncoder.frameRate = 30
            mRtcEngine?.setVideoEncoderConfiguration(videoEncoder)

            // 启用视频模块
            mRtcEngine?.enableVideo()
            val cameraCaptureConfiguration =
                CameraCapturerConfiguration(CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR)
            mRtcEngine?.setCameraCapturerConfiguration(cameraCaptureConfiguration)
            // 增强暗光场景光线
            val lowLightOptions = LowLightEnhanceOptions()
            mRtcEngine?.setLowlightEnhanceOptions(true, lowLightOptions)

            setupLocalVideo()
            // 开启本地预览
            mRtcEngine?.startPreview()
        }

        // 创建 ChannelMediaOptions 对象，并进行配置
        val options = ChannelMediaOptions()
        // 根据场景将用户角色设置为 BROADCASTER (主播) 或 AUDIENCE (观众)
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // 直播场景下，设置频道场景为 BROADCASTING (直播场景)
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        options.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY

        // 发布麦克风采集的音频
        options.publishMicrophoneTrack = true
        // 发布摄像头采集的视频
        options.publishCameraTrack = true
        // 自动订阅所有音频流
        options.autoSubscribeAudio = true
        // 自动订阅所有视频流
        options.autoSubscribeVideo = true

        // 使用临时 Token 加入频道，自行指定用户 ID 并确保其在频道内的唯一性
        mRtcEngine?.joinChannel(agoraToken, agoraChannelId, 1, options)
    }

    // 静音
    override fun switchMicro(enable: Boolean) {
        // 不上传本地采集的音频数据
//        mRtcEngine?.enableLocalAudio(enable)
//        mRtcEngine?.muteLocalAudioStream(!enable)
        adjustVolume(enable)
        muteLocalAudio = !enable
    }

    private fun adjustVolume(enable: Boolean) {
        if (enable) {
            mRtcEngine?.adjustRecordingSignalVolume(100)
        } else {
            mRtcEngine?.adjustRecordingSignalVolume(0)
        }
    }

    // 翻转摄像头
    override fun flipCamera() {
        mRtcEngine?.switchCamera()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        mRtcEngine?.disableAudio()
        if (!socketManager.isAudioOnly()) {
            stopVideo()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
//        if (engineCallback.isErrorPage()) return
        mRtcEngine?.enableAudio()
        if (!socketManager.isAudioOnly()) {
            startVideo()
        }
    }

    override fun startVideo() {
        mRtcEngine?.enableVideo()
        mRtcEngine?.startPreview()
    }

    override fun stopVideo() {
        mRtcEngine?.disableVideo()
        mRtcEngine?.stopPreview()
    }

    private fun setupLocalVideo() {
        // 创建一个 SurfaceView 对象，并将其作为 FrameLayout 的子对象
        val surfaceView = TextureView(context)
        callback.addLocalVideoSurface(surfaceView)
        // 将 SurfaceView 对象传入声网实时互动 SDK，设置本地视图
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 1))
    }

    // 关闭rtc
    override fun close() {
        if (mRtcEngine == null) {
            return
        }
        // 停止本地视频预览
        mRtcEngine?.stopPreview()
        leaveChannel()
        mRtcEngine = null
        RtcEngine.destroy()
    }

    // 离开频道
    override fun leaveChannel() {
        mRtcEngine?.leaveChannel()
    }
}