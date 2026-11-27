package com.exantt.btrouterclient

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast

class OtpCopyReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BT_OTP_CLIP"
        private val mainHandler = Handler(Looper.getMainLooper())

        fun copyOtpToClipboard(context: Context, code: String, showToast: Boolean = true) {
            if (code.isEmpty()) return
            val appCtx = context.applicationContext ?: context
            val task = Runnable {
                try {
                    val locCtx = AppLocale.wrap(appCtx)
                    val cm = appCtx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(
                        locCtx.getString(R.string.otp_label_default),
                        code
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        clip.description.extras = PersistableBundle().apply {
                            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                        }
                    }
                    cm.setPrimaryClip(clip)
                    if (showToast && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(
                            appCtx,
                            locCtx.getString(R.string.otp_copied_toast_safe),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (_: Exception) {
                    Log.w(TAG, "Не удалось скопировать код в буфер")
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                task.run()
            } else {
                mainHandler.post(task)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != BTRouterClientService.ACTION_COPY_OTP) return
        val code = intent.getStringExtra(BTRouterClientService.EXTRA_OTP_CODE) ?: return
        if (code.isEmpty() || code.length > 16 || !code.all { it.isDigit() }) return
        copyOtpToClipboard(context, code, showToast = true)
    }
}
