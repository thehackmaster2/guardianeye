package com.example.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.managers.SocketManager
import org.json.JSONObject

class GuardianNotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListener"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        try {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val pkg = sbn.packageName ?: ""
            
            // Filter out our own self-monitored notifications or empty noise
            if (pkg == packageName || (title.trim().isEmpty() && text.trim().isEmpty())) {
                return
            }
            
            val appName = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (e: Exception) {
                // Return cleaned representation of final word block
                pkg.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
            }

            val data = JSONObject().apply {
                put("app", appName)
                put("title", title)
                put("text", text)
                put("time", System.currentTimeMillis())
                put("package", pkg)
            }
            
            Log.d(TAG, "Guardian Eye Captured Notification from $appName: $title")
            SocketManager.emitChildNotification(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading StatusBarNotification payload: ${e.message}", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional tracking lifecycle
    }
}
