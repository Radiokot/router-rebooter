package ua.com.radiokot.routerreboot.rebooter

import ua.com.radiokot.routerreboot.rebooter.TendaAc5HttpRebooter.Companion.PORT
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.security.MessageDigest
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
    private val port = port ?: PORT

    private val webClientRootUrl = "http://$ip:${this.port}"

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

        val request = sendForm(
            "/login/Auth",
            mapOf(
                "username" to login,
                "password" to passwordHash
            )
        )

       return request.getHeaderField("Set-Cookie")
           .substringAfter("password=")
           .substringBeforeLast(';')
    }

    private fun sendRebootRequest(authToken: String) {
        sendForm(
            "/goform/SysToolReboot",
            mapOf("action" to 0),
            authToken
        ).disconnect()
    }

    private fun sendForm(
        path: String,
        data: Map<String, Any>,
        authToken: String? = null
    ): HttpURLConnection {
        val url = URL(webClientRootUrl + path)

        val formData = data.entries.joinToString(separator = "&", transform = { (key, value) ->
            "$key=$value"
        })

        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false

            requestMethod = "POST"
            setRequestProperty("Content-Type", FORM_CONTENT_TYPE)

            if (authToken != null) {
                setRequestProperty("Cookie", "password=$authToken")
            }

            println("$requestMethod --> $url")
            println(formData)

            doOutput = true
            outputStream.use { it.write(formData.toByteArray(Charset.forName(FORM_CONTENT_CHARSET))) }

            inputStream.close()

            println("$responseCode <-- $url")
        }
    }

    companion object {
        const val PORT = 80
        private const val TIMEOUT_MS = 60 * 1000
        private const val FORM_CONTENT_CHARSET = "utf-8"
        private const val FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=$FORM_CONTENT_CHARSET"
    }
}