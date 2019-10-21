package ua.com.radiokot.routerreboot.rebooter

import ua.com.radiokot.routerreboot.rebooter.TelnetRouterRebooter.Companion.PORT
import java.io.InputStream
import java.io.PrintWriter
import java.net.Socket
import java.nio.BufferOverflowException

/**
 * Reboots router over a Telnet protocol.
 *
 * @param loginPrompt ending of a login prompt, i.e. "router login: "
 * @param login router login
 * @param passwordPrompt ending of a password prompt, i.e. "router password: "
 * @param password router password
 * @param commandPrompt ending of a command prompt, i.e. "#"
 * @param rebootCommand command to perform in order to reboot the router
 * @param port Telnet port of the router, [PORT] by default
 */
open class TelnetRouterRebooter(
    ip: String,
    protected val loginPrompt: String,
    protected val login: String,
    protected val passwordPrompt: String,
    protected val password: String,
    protected val commandPrompt: String,
    protected val rebootCommand: String,
    port: Int? = null
) : RouterRebooter(ip) {
    protected val port = port ?: PORT

    override fun reboot() {
        val socket = getSocket()

        val inputStream = socket.getInputStream()
        val writer = PrintWriter(socket.getOutputStream(), true)

        // Read login prompt.
        readPrompt(loginPrompt, inputStream)

        // Send login, it will be echoed.
        writer.println(login)
        skipEcho(login.length, inputStream)

        // Read password prompt.
        readPrompt(passwordPrompt, inputStream)

        // Send password, won't be echoed.
        writer.println(password)

        // Read command enter prompt.
        readPrompt(commandPrompt, inputStream)

        // Send reboot command.
        writer.println(rebootCommand)

        inputStream.close()
        socket.close()
    }

    protected open fun getSocket(): Socket {
        return Socket(ip, port)
    }

    protected open fun readPrompt(expectedEnding: String, stream: InputStream) {
        val buffer = CharArray(2048)
        var i = 0
        while (true) {
            waitForInputStreamData(stream)

            buffer[i++] = stream.read().toChar().also { print(it) }
            val bufferedString = buffer.slice(0 until i).joinToString("")
            if (bufferedString.endsWith(expectedEnding)) {
                return
            } else if (i == buffer.size) {
                throw BufferOverflowException()
            }
        }
    }

    protected open fun skipEcho(charsCount: Int, stream: InputStream) {
        // Telnet char is a single-byte one.
        var i = 0
        while (i < charsCount) {
            waitForInputStreamData(stream)

            stream.read()
            i++
        }
    }

    protected open fun waitForInputStreamData(stream: InputStream) {
        while (true) {
            if (stream.available() == 0) {
                Thread.sleep(500)
            } else {
                return
            }
        }
    }

    companion object {
        const val PORT = 23
    }
}