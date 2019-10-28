package ua.com.radiokot.routerreboot.rebooter

import okhttp3.*
import ua.com.radiokot.routerreboot.rebooter.TendaAc5HttpRebooter.Companion.PORT
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.xml.bind.DatatypeConverter

/**
 * Reboots Tenda AC5 router through it's HTTP API
 *
 * @param password web interface password
 * @param port web interface port, [PORT] if null
 */
class TendaAc5HttpRebooter(
    ip: String,
    password: String,
    port: Int? = null
) : RouterRebooter(ip, "admin", password) {
    private val webClientRootUrl = HttpUrl.Builder()
        .host(ip)
        .port(port ?: PORT)
        .scheme("http")
        .build()

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .connectTimeout(2, TimeUnit.MINUTES)
        .cookieJar(CookieJar.NO_COOKIES)
        .addInterceptor {
            println("${it.request().method} ${it.request().url}")
            it.proceed(it.request())
        }
        .build()

    override fun reboot() {
        println("Authorizing...")
        val authToken = getAuthToken()

        println("Requesting reboot...")
        sendRebootRequest(authToken)
    }

    private fun getAuthToken(): String {
        val passwordHash = MessageDigest
            .getInstance("MD5")
            .digest(password.toByteArray(Charsets.UTF_8))
            .let(DatatypeConverter::printHexBinary)
            .toLowerCase()

        val request = Request.Builder()
            .url(webClientRootUrl.newBuilder().encodedPath("/login/Auth").build())
            .post(
                FormBody.Builder()
                    .add("username", login)
                    .add("password", passwordHash)
                    .build()
            )
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val response = httpClient.newCall(request).execute()

        return response.headers("Set-Cookie")[0].substringAfter("password=")
    }

    private fun sendRebootRequest(authToken: String) {
        val request = Request.Builder()
            .url(webClientRootUrl.newBuilder().encodedPath("/goform/SysToolReboot").build())
            .post(
                FormBody.Builder()
                    .add("action", "0")
                    .build()
            )
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", "password=$authToken")
            .build()

        httpClient.newCall(request).execute()
    }

    companion object {
        const val PORT = 80
    }
}