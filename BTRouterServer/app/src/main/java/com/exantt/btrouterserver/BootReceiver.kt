package com.exantt.btrouterserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!CompatUtils.hasBluetoothConnectPermission(context)) {
            Log.w("BT_SERVER_BOOT", "Нет разрешения BLUETOOTH_CONNECT при загрузке")
            return
        }
        try {
            val serviceIntent = Intent(context, BTRouterServerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("BT_SERVER_BOOT", "Не удалось автозапустить серверную службу")
        }
    }
}
