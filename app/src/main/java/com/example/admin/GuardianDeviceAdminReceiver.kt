package com.example.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class GuardianDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "GuardianEye protection enabled", Toast.LENGTH_SHORT).show()
    }
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "GuardianEye is protecting this device. Contact your parent to disable."
    }
    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "GuardianEye protection disabled", Toast.LENGTH_SHORT).show()
    }
}
