package com.sensetime.lib_rtc.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.sensetime.lib_rtc.model.RtcVideoModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object RtcVideoSizeUtil {
    fun convert(context: Context?): RtcVideoModel {
        val windowManager =
            context?.applicationContext?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val h = max(metrics.heightPixels, metrics.widthPixels)
        val w = min(metrics.heightPixels, metrics.widthPixels)
//        val w = AppManager.getScreenWidthPx()
//        val h = AppManager.getScreenHeightPx()
        val scale = w * 1.0f / h
        var x = sqrt(720 * 1280 * scale).toInt()
        x -= x % 4
        var y = (x * 1.0f / scale).toInt()
        y -= y % 4
//        if (AppConfig.isPad) {
        // 横屏的话需要二次处理
        if (isLandscape(context)) {
            // 横屏
            val temp = y
            y = x
            x = temp
        }
//        }
        return RtcVideoModel(x, y)
    }

    private fun isLandscape(context: Context?): Boolean {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val rotation = display.rotation
        return rotation == 1 || rotation == 3  // 1 and 3 represent landscape mode
    }
}