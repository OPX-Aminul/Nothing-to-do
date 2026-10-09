package com.hairgrowth.os.ui.fragments

/** Contract between the hosting activity and the four screens. */
interface Host {
    fun showToast(msg: String)
    fun celebrate()
    fun playTone(kind: String)
    fun dockOnScroll(deltaY: Int)
    fun requestNotifPermission()
    fun openExactAlarmSettings()
    /** Jumps to the Home tab, where routines can be edited. */
    fun goHomeForEditing()
}
