import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import ua.com.radiokot.routerreboot.rebooter.TelnetRouterRebooter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket

class TelnetRebooterTest {
    companion object {
        var port: Int = 0
        lateinit var server: ServerSocket
        var isRebooted = false

        @BeforeClass
        @JvmStatic
        fun setUpTestServer() {
            val socket = ServerSocket(0).apply { reuseAddress = true }
            port = socket.localPort
            socket.close()

            server = ServerSocket(port)
            Thread {
                val client = server.accept()

                val writer = PrintWriter(client.getOutputStream())
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))

                // We have a poor connection.
                Thread.sleep(1000)

                writer.print(LOGIN_PROMPT)
                writer.flush()

                val login = reader.readLine()

                // A really poor one.
                login.toCharArray().forEach {
                    Thread.sleep(400)
                    writer.print(it)
                }
                writer.flush()
                Thread.sleep(1000)

                println(login)

                writer.print(PASSWORD_PROMPT)
                writer.flush()

                val password = reader.readLine()
                println(password)

                Thread.sleep(1000)

                writer.println(MOTD)
                writer.print(COMMAND_PROMPT)
                writer.flush()

                val command = reader.readLine()
                println(command)

                if (login == LOGIN && password == PASSWORD && command == REBOOT_COMMAND) {
                    isRebooted = true
                }

                writer.close()
                reader.close()
                client.close()
            }.start()
        }

        @AfterClass
        @JvmStatic
        fun shutdownTestServer() {
            server.close()
        }

        const val LOGIN_PROMPT = "Enter login: "
        const val LOGIN = "root"
        const val PASSWORD_PROMPT = "Enter password: "
        const val PASSWORD = "qwe123"
        const val COMMAND_PROMPT = "$ "
        const val MOTD = "\nWelcome to our awesome router!\nBusyBox v1.19.2\n"
        const val REBOOT_COMMAND = "reboot"
    }

    @Test
    fun telnetRebooterSequence() {
        TelnetRouterRebooter(
            ip = "127.0.0.1",
            port = port,
            loginPrompt = LOGIN_PROMPT,
            login = LOGIN,
            passwordPrompt = PASSWORD_PROMPT,
            password = PASSWORD,
            commandPrompt = COMMAND_PROMPT,
            rebootCommand = REBOOT_COMMAND
        ).reboot()

        Thread.sleep(100)

        Assert.assertTrue(isRebooted)
    }
}