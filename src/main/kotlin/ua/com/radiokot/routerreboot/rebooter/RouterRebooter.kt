package ua.com.radiokot.routerreboot.rebooter

abstract class RouterRebooter(protected val ip: String,
                              protected val login: String,
                              protected val password: String) {
    abstract fun reboot()
}