package com.example.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import android.os.Bundle

class RemoteControlAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RemoteAccessibility"
        private var instance: RemoteControlAccessibilityService? = null
        private var blockedApps: Set<String> = emptySet()

        fun performTap(x: Float, y: Float): Boolean {
            val service = instance
            if (service == null) {
                Log.e(TAG, "Accessibility service instance not active")
                return false
            }
            return service.tapAt(x, y)
        }

        fun performLongPress(x: Float, y: Float): Boolean {
            val service = instance
            if (service == null) {
                Log.e(TAG, "Accessibility service instance not active")
                return false
            }
            return service.longPressAt(x, y)
        }

        fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
            val service = instance
            if (service == null) {
                Log.e(TAG, "Accessibility service instance not active")
                return false
            }
            return service.swipe(x1, y1, x2, y2, duration)
        }

        fun performText(value: String): Boolean {
            val service = instance
            if (service == null) {
                Log.e(TAG, "Accessibility service instance not active")
                return false
            }
            return service.inputText(value)
        }

        fun updateBlockedApps(apps: Set<String>) {
            blockedApps = apps
            Log.d(TAG, "Blocked applications database synchronized. Active blacklist length: ${apps.size}")
        }

        fun isActive(): Boolean {
            return instance != null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "RemoteControlAccessibilityService Linked successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // 1. App Blocker validation intercept
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            if (packageName.isNotEmpty() && blockedApps.contains(packageName) && packageName != "com.example") {
                Log.w(TAG, "Child attempted access to blocked application: $packageName. Redirecting to home screen...")
                
                // Trigger home screen redirection
                performGlobalAction(GLOBAL_ACTION_HOME)
                
                // Display feedback dialog
                Toast.makeText(
                    this,
                    "GuardianEye: Access to this app has been suspended.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }
        
        // 2. Parental settings protection intercept
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.contains("settings", ignoreCase = true)) {
            val texts = mutableListOf<String>()
            event.text?.forEach { text -> texts.add(text.toString()) }
            val textString = texts.joinToString(" ")
            val contentDesc = event.contentDescription?.toString() ?: ""
            
            var containsGuardian = textString.contains("GuardianEye", ignoreCase = true) ||
                                  contentDesc.contains("GuardianEye", ignoreCase = true)
                                
            if (!containsGuardian) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val nodes = rootNode.findAccessibilityNodeInfosByText("GuardianEye")
                    if (!nodes.isNullOrEmpty()) {
                        containsGuardian = true
                    }
                }
            }
            
            if (containsGuardian) {
                Toast.makeText(
                    this,
                    "Contact your parent to change GuardianEye settings",
                    Toast.LENGTH_LONG
                ).show()
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "RemoteControlAccessibilityService onInterrupt")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.d(TAG, "RemoteControlAccessibilityService Destroyed")
    }

    fun tapAt(x: Float, y: Float): Boolean {
        Log.d(TAG, "Accessibility dispatching Click coordinate ($x, $y)")
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gestureBuilder = GestureDescription.Builder().addStroke(stroke)
        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun longPressAt(x: Float, y: Float): Boolean {
        Log.d(TAG, "Accessibility dispatching LongPress coordinate ($x, $y)")
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 1000)
        val gestureBuilder = GestureDescription.Builder().addStroke(stroke)
        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
        Log.d(TAG, "Accessibility dispatching Swipe coordinates: ($x1,$y1)->($x2,$y2), elapsed=${duration}ms")
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gestureBuilder = GestureDescription.Builder().addStroke(stroke)
        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun inputText(text: String): Boolean {
        Log.d(TAG, "Accessibility dispatching simulated keyboard text entry: '$text'")
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            val bundle = Bundle()
            bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        }
        return false
    }
}
