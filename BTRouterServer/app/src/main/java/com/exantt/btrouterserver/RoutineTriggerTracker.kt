package com.exantt.btrouterserver

/**
 * Трекер жизненного цикла сервисного уведомления-триггера для Samsung Modes & Routines:
 * позволяет держать уведомление до таймаута и досрочно убирать его,
 * как только реальное состояние Hotspot / Mobile Data совпало с целевым.
 */
class RoutineTriggerTracker {

    private var activeNotifId: Int = -1
    private var targetState: Int = -1

    fun onNotificationPosted(notifId: Int, targetState: Int): Int {
        val previousId = activeNotifId
        activeNotifId = notifId
        this.targetState = targetState
        return previousId
    }

    fun dismissIfMatched(actualState: Int): Int {
        if ((actualState == 0 || actualState == 1) && activeNotifId >= 0 && targetState == actualState) {
            val id = activeNotifId
            activeNotifId = -1
            targetState = -1
            return id
        }
        return -1
    }

    fun onTimeout(notifId: Int): Boolean {
        if (activeNotifId >= 0 && activeNotifId == notifId) {
            activeNotifId = -1
            targetState = -1
            return true
        }
        return false
    }

    fun isPending(): Boolean = activeNotifId >= 0

    fun clear(): Int {
        val previousId = activeNotifId
        activeNotifId = -1
        targetState = -1
        return previousId
    }
}
