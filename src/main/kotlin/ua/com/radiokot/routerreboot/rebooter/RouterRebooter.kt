package ua.com.radiokot.routerreboot.rebooter

abstract class RouterRebooter(protected val ip: String) {
    abstract fun reboot()
}