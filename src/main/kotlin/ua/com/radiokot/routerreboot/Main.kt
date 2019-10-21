package ua.com.radiokot.routerreboot

import ua.com.radiokot.routerreboot.rebooter.TelnetRouterRebooter
import java.io.File
import java.util.*

object Main {
    fun main(args: Array<String>) {
        val mode = if (args.size <= 1) {
            printHelp()
            return
        } else {
            args[0]
        }

        when (mode) {
            "telnet" -> telnet(args)
            else -> printHelp()
        }
    }

    private fun printHelp() {
        println("Usage: rebooter.jar mode <config.properties>")
        println("Available modes:")
        println("""| - telnet: reboot a router over Telnet protocol.""".trimMargin())
        println(
            """|   * ip: IP address of the router
            |   * login_prompt: ending of a login prompt, i.e. "router login: "
            |   * login: router login
            |   * password_prompt: ending of a password prompt, i.e. "router password: "
            |   * password: router password
            |   * command_prompt: ending of a command prompt, i.e. "#"
            |   * reboot_command: command to perform in order to reboot the router
            |   * [port]: non-standard port
        """.trimMargin()
        )
    }

    private fun telnet(args: Array<String>) {
        val configFile = args.getOrNull(1)
        if (configFile == null) {
            println("Config file is required for this mode")
        }

        val properties = Properties().apply { load(File(configFile).inputStream()) }

        TelnetRouterRebooter(
            ip = properties.getProperty("ip"),
            loginPrompt = properties.getProperty("login_prompt"),
            login = properties.getProperty("login"),
            passwordPrompt = properties.getProperty("password_prompt"),
            password = properties.getProperty("password"),
            commandPrompt = properties.getProperty("command_prompt"),
            rebootCommand = properties.getProperty("reboot_command"),
            port = properties["port"]?.toString()?.toIntOrNull()
        ).reboot()
    }
}