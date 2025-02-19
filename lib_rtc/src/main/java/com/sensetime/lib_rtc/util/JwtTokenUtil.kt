package com.sensetime.lib_rtc.util

import android.os.Build
import androidx.annotation.RequiresApi
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.SignatureException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

object JwtTokenUtil {
    /**
     * 获取时间戳，模拟根据给定秒数偏移后的时间，这里以获取当前时间加上偏移量后的时间为例
     *
     * @param deltaSeconds 时间偏移的秒数
     * @return 对应的Date类型时间对象
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getTimestamp(deltaSeconds: Int): Date {
        val instant = Instant.now().plusSeconds(deltaSeconds.toLong())
        val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"))
        return Date.from(zonedDateTime.toInstant())
    }

    fun generateToken(iss: String, secret_key: String): String {
        val header: MutableMap<String, Any> = HashMap()
        header["alg"] = "HS256"
        header["typ"] = "JWT"

        val currentTimeSeconds = Date().time / 1000
        val payload: MutableMap<String, Any> = HashMap()
        payload["iss"] = iss
        // 设置过期时间，这里设置为当前时间往后推24小时（和原代码逻辑一致，24 * 60 * 60秒）
        payload["exp"] = currentTimeSeconds + 23 * 60 * 60
        // 设置在此时间之前不可用，这里设置为当前时间往前推10秒（和原代码逻辑一致）
        payload["nbf"] = currentTimeSeconds - 60 * 60

        return Jwts.builder()
            .setHeader(header)
            .setClaims(payload)
            .signWith(SignatureAlgorithm.HS256, secret_key.toByteArray())
            .compact()
    }


    /**
     * 判断JWT是否有效
     *
     * @param token 要验证的JWT字符串
     * @return 如果JWT有效返回true，否则返回false
     */
    fun isValid(token: String?, secret_key: String): Boolean {
        try {
            val claims: Claims =
                Jwts.parser().setSigningKey(secret_key).parseClaimsJws(token).getBody()
            // 检查是否过期
            val expiration: Date = claims.getExpiration()
            val now = Date()
            if (expiration.before(now)) {
                return false
            }
            return true
        } catch (e: SignatureException) {
            // 签名验证失败，直接返回false
            return false
        } catch (e: Exception) {
            // 其他解析异常等情况，也返回false
            return false
        }
    }
}