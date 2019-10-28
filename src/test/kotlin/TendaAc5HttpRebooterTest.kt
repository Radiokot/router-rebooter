import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import ua.com.radiokot.routerreboot.rebooter.TendaAc5HttpRebooter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

class TendaAc5HttpRebooterTest {
    companion object {
        private var port: Int = 0
        private lateinit var server: HttpServer
        private var authToken: String = ""
        var isRebooted = false

        @JvmStatic
        @BeforeClass
        fun setUpTestServer() {
            val socket = ServerSocket(0).apply { reuseAddress = true }
            port = socket.localPort
            socket.close()
            server = HttpServer.create(InetSocketAddress(port), 0)

            val getBodyKeyValue = { http: HttpExchange ->
                http.requestBody
                    .bufferedReader(Charsets.UTF_8)
                    .readText()
                    .split('&')
                    .map { it.split('=').run { get(0) to get(1) } }
                    .toMap()
            }

            server.createContext("/login/Auth") { http ->
                if (http.requestMethod != "POST") {
                    http.close()
                    println("Invalid request method ${http.requestMethod}")
                }

                val bodyKeyValue = getBodyKeyValue(http)

                val username = bodyKeyValue["username"]
                if (username != DEFAULT_USERNAME) {
                    http.close()
                    println("Invalid username $username")
                }

                val passwordHash = bodyKeyValue["password"]
                val defaultPasswordHash = MessageDigest
                    .getInstance("MD5")
                    .digest(DEFAULT_PASSWORD.toByteArray(Charsets.UTF_8))
                    .let(DatatypeConverter::printHexBinary)
                    .toLowerCase()
                if (passwordHash != defaultPasswordHash) {
                    http.close()
                    println("Invalid password $passwordHash")
                }

                authToken = passwordHash + SERVER_KEY

                Thread.sleep(1000)

                http.responseHeaders.add("Content-type", "text/html")
                http.responseHeaders.add("Set-Cookie", "password=$authToken; path=/")
                http.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_TEMP, 0)
                http.close()
            }

            server.createContext("/goform/SysToolReboot") { http ->
                if (http.requestMethod != "POST") {
                    http.close()
                    println("Invalid request method ${http.requestMethod}")
                }

                val authToken = http.requestHeaders.getFirst("Cookie")
                    .substringAfter("password=")
                    .substringBefore(';')
                if (authToken != this.authToken) {
                    http.close()
                    print("Invalid auth token $authToken")
                }

                val bodyKeyValue = getBodyKeyValue(http)

                val action = bodyKeyValue["action"]
                if (action != REBOOT_ACTION) {
                    http.close()
                    print("Invalid action $REBOOT_ACTION")
                }

                isRebooted = true

                Thread.sleep(1000)
                
                http.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1)
                http.close()
            }

            server.start()
        }

        @JvmStatic
        @AfterClass
        fun shutDownTestServer() {
            server.stop(0)
        }

        const val SERVER_KEY = "+server_key"
        const val DEFAULT_USERNAME = "admin"
        const val DEFAULT_PASSWORD = "qwe123"
        const val REBOOT_ACTION = "0"
    }

    @Test
    fun tendaAc5HttpRebooterSequence() {
        TendaAc5HttpRebooter(
            ip = "127.0.0.1",
            port = port,
            password = DEFAULT_PASSWORD
        ).reboot()


        Thread.sleep(100)

        Assert.assertTrue(isRebooted)
    }
}