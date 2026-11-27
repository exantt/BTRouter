package com.exantt.btrouterserver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.app.ActivityCompat

object CompatUtils {

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            true
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBluetoothScanPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            true
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getSmsManager(context: Context): SmsManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java) ?: return null
            var subId = SubscriptionManager.getDefaultSmsSubscriptionId()
            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                subId = SubscriptionManager.getDefaultSubscriptionId()
            }
            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val sm = context.getSystemService(SubscriptionManager::class.java)
                    @Suppress("DEPRECATION")
                    val firstActive = sm?.activeSubscriptionInfoList?.firstOrNull()?.subscriptionId
                    if (firstActive != null && firstActive != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        subId = firstActive
                    }
                } catch (_: Exception) {
                }
            }
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                base.createForSubscriptionId(subId)
            } else {
                base
            }
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }
}
