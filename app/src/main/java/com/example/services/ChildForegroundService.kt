package com.example.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.managers.SocketManager
import com.example.managers.WebRTCManager
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import org.webrtc.*
import android.os.Handler
import android.os.Looper
import android.os.Environment
import android.util.Base64
import java.io.File
import java.net.URISyntaxException
import java.util.Locale
import android.speech.tts.TextToSpeech
import android.media.MediaPlayer
import android.media.AudioManager
import android.view.WindowManager
import android.graphics.PixelFormat
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.view.Gravity
import android.graphics.Color

class ChildForegroundService : Service() {

    companion object {
        private const val TAG = "ChildForegroundService"
        private const val CHANNEL_ID = "GuardianEyeChannel"
        private const val NOTIFICATION_ID = 88291

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_PROJECTION_INTENT = "EXTRA_PROJECTION_INTENT"
        const val EXTRA_ROOM_CODE = "EXTRA_ROOM_CODE"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var webRtcManager: WebRTCManager? = null
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var cameraCapturer: CameraVideoCapturer? = null
    
    private var eglContext: EglBase.Context? = null
    private var localStream: MediaStream? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ChildForegroundService created")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")

        // Start default passive protection foreground notification first
        startForeground(NOTIFICATION_ID, buildNotification())

        if (action == ACTION_START) {
            val projectionIntent = intent.getParcelableExtra<Intent>(EXTRA_PROJECTION_INTENT)
            val roomCode = intent.getStringExtra(EXTRA_ROOM_CODE) ?: ""

            if (projectionIntent != null) {
                startForegroundWithNotification()
                initializeStreaming(roomCode, projectionIntent)
            } else {
                Log.e(TAG, "Cannot start streaming: projection intent is null")
                stopSelf()
            }
        } else if (action == ACTION_STOP) {
            stopStreaming()
            stopSelf()
        }

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            "guardian_channel",
            "GuardianEye Protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "GuardianEye is actively protecting this device"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notificationIntent = Intent(this, Class.forName("com.example.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "guardian_channel")
            .setContentTitle("GuardianEye Active")
            .setContentText("This device is being monitored by a parent")
            .setSmallIcon(android.R.drawable.presence_video_busy)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, ChildForegroundService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pendingIntent)
        super.onTaskRemoved(rootIntent)
    }

    private fun startForegroundWithNotification() {
        val notificationIntent = Intent(this, Class.forName("com.example.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, run { 0 }, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GuardianEye Active")
            .setContentText("Device is currently monitored and secured in the background.")
            .setSmallIcon(android.R.drawable.presence_video_busy)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initializeStreaming(roomCode: String, projectionIntent: Intent) {
        serviceScope.launch {
            // 1. Establish Signaling Connection
            Log.d(TAG, "Connecting socket and joining room: $roomCode")
            SocketManager.connect()
            SocketManager.emitJoinRoom(roomCode, "child")

            // Wait a moment for socket association
            delay(1000)

            // 2. Setup WebRTC
            eglContext = WebRTCManager.eglBase.eglBaseContext
            
            webRtcManager = WebRTCManager(
                context = this@ChildForegroundService,
                peerId = "parent", // Sending to Parent
                isParent = false,
                onIceCandidateReady = { _, candidate ->
                    SocketManager.emitIceCandidate("parent", candidate)
                },
                onLocalSdpReady = { _, sdp ->
                    // Answer or Offer generated locally
                    SocketManager.emitAnswer("parent", sdp.description)
                }
            )

            // Ensure we create a media capture streams container
            localStream = webRtcManager?.peerConnection?.let { pc ->
                val stream = webRtcManager?.peerConnection?.let {
                    // Create media stream
                    null // or standard tracking
                }
                stream
            }

            // 3. Setup Screen Capture
            setupScreenCapturer(projectionIntent)

            // 4. Setup Front Camera Capture (WebRTC-optimized)
            setupFrontCameraCapturer()

            // 5. Setup Audio capture
            setupMicrophone()

            // 6. Monitor signaling socket requests
            listenToSignalingEvents()
        }
    }

    private fun setupScreenCapturer(projectionIntent: Intent) {
        val manager = webRtcManager ?: return
        try {
            val videoSource = manager.createVideoSourceShared(true) ?: return
            
            screenCapturer = ScreenCapturerAndroid(projectionIntent, object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection Screen Capturer stopped")
                }
            })

            val surfaceHelper = SurfaceTextureHelper.create("ScreenCaptureThread", eglContext)
            screenCapturer?.initialize(surfaceHelper, this, videoSource.capturerObserver)
            screenCapturer?.startCapture(1280, 720, 30)

            val screenTrack = manager.createVideoTrackShared(videoSource, "ARDAMSv0_screen")
            if (screenTrack != null) {
                val rtpSender = manager.peerConnection?.createSender("video", "ARDAMSv0_screen")
                rtpSender?.setTrack(screenTrack, true)
                Log.d(TAG, "Screen Capture WebRTC Track registered successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up screen capturer: ${e.message}", e)
        }
    }

    private fun setupFrontCameraCapturer() {
        val manager = webRtcManager ?: return
        try {
            val enumerator = Camera2Enumerator(this)
            val deviceNames = enumerator.deviceNames
            var frontCameraName: String? = null
            
            for (name in deviceNames) {
                if (enumerator.isFrontFacing(name)) {
                    frontCameraName = name
                    break
                }
            }

            if (frontCameraName == null) {
                Log.e(TAG, "Front camera not found on device")
                return
            }

            val videoSource = manager.createVideoSourceShared(false) ?: return
            cameraCapturer = enumerator.createCapturer(frontCameraName, null) as CameraVideoCapturer
            
            val surfaceHelper = SurfaceTextureHelper.create("CameraCaptureThread", eglContext)
            cameraCapturer?.initialize(surfaceHelper, this, videoSource.capturerObserver)
            cameraCapturer?.startCapture(640, 480, 30)

            val cameraTrack = manager.createVideoTrackShared(videoSource, "ARDAMSv1_camera")
            if (cameraTrack != null) {
                // Add second video track
                manager.peerConnection?.addTrack(cameraTrack, listOf("ARDAMSv1_camera_stream"))
                Log.d(TAG, "Front Camera WebRTC Track registered successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up CameraX front-camera capturer: ${e.message}", e)
        }
    }

    private fun setupMicrophone() {
        val manager = webRtcManager ?: return
        try {
            val audioSource = manager.createAudioSourceShared(MediaConstraints()) ?: return
            val audioTrack = manager.createAudioTrackShared(audioSource, "ARDAMSa0")
            
            if (audioTrack != null) {
                manager.peerConnection?.addTrack(audioTrack, listOf("ARDAMSa0_stream"))
                Log.d(TAG, "Audio Recording Micro-track registered successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up microphone: ${e.message}", e)
        }
    }

    private var lockView: android.view.View? = null
    private var textToSpeech: android.speech.tts.TextToSpeech? = null

    private fun listenToSignalingEvents() {
        serviceScope.launch {
            SocketManager.offerFlow.collect { event ->
                Log.d(TAG, "WebRTC SDP Offer received from Parent. Handling...")
                webRtcManager?.handleRemoteSdp("offer", event.sdp)
            }
        }

        serviceScope.launch {
            SocketManager.iceCandidateFlow.collect { event ->
                try {
                    val mid = event.candidate.getString("sdpMid")
                    val index = event.candidate.getInt("sdpMLineIndex")
                    val sdp = event.candidate.getString("candidate")
                    webRtcManager?.handleRemoteIceCandidate(mid, index, sdp)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply remote ICE candidate: ${e.message}")
                }
            }
        }

        serviceScope.launch {
            SocketManager.remoteControlFlow.collect { event ->
                Log.d(TAG, "Executing simulated gesture coordinates: x=${event.x}, y=${event.y}")
                val success = RemoteControlAccessibilityService.performTap(event.x, event.y)
                Log.d(TAG, "Gesture execution result success: $success")
            }
        }

        serviceScope.launch {
            SocketManager.requestFilesFlow.collect { type ->
                Log.d(TAG, "Parent requested files of type: $type")
                val files = scanFiles(type)
                SocketManager.emitFileList(JSONArray(files))
            }
        }

        serviceScope.launch {
            SocketManager.requestFileDataFlow.collect { path ->
                Log.d(TAG, "Parent requested file data for path: $path")
                try {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                        SocketManager.emitFileData(path, base64)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading file data: ${e.message}")
                }
            }
        }

        serviceScope.launch {
            SocketManager.lockScreenFlow.collect { event ->
                Log.d(TAG, "Parent requested screen lock: locked=${event.locked}, message=${event.message}")
                if (event.locked) {
                    showLockOverlay(event.message)
                } else {
                    hideLockOverlay()
                }
            }
        }

        serviceScope.launch {
            SocketManager.playAudioFlow.collect { base64 ->
                Log.d(TAG, "Parent sent voice broadcast message")
                playAudioPayload(base64)
            }
        }

        serviceScope.launch {
            SocketManager.speakTextFlow.collect { text ->
                Log.d(TAG, "Parent sent text-to-speech instructions")
                speakTextLocally(text)
            }
        }

        serviceScope.launch {
            SocketManager.requestAppsFlow.collect {
                Log.d(TAG, "Parent requested installed applications metadata")
                val apps = getInstalledAppsJson()
                SocketManager.emitAppList(apps)
            }
        }
    }

    private fun showLockOverlay(message: String) {
        if (lockView != null) return
        val context = this@ChildForegroundService
        val wm = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        val alertIcon = android.widget.ImageView(context).apply {
            setImageResource(android.R.drawable.ic_dialog_alert)
            setColorFilter(android.graphics.Color.WHITE)
            val lp = android.widget.LinearLayout.LayoutParams(120, 120)
            lp.setMargins(0, 0, 0, 40)
            layoutParams = lp
        }
        layout.addView(alertIcon)

        val label = android.widget.TextView(context).apply {
            text = "GUARDIANEYE SUSPENSION"
            setTextColor(android.graphics.Color.RED)
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setPadding(40, 0, 40, 20)
        }
        layout.addView(label)

        val mainMsg = if (message.isNotEmpty()) message else "Device usage has been suspended temporarily by your guardian."
        val textView = android.widget.TextView(context).apply {
            text = mainMsg
            setTextColor(android.graphics.Color.WHITE)
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setPadding(60, 10, 60, 60)
        }
        layout.addView(textView)
        
        val params = android.view.WindowManager.LayoutParams(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                android.view.WindowManager.LayoutParams.TYPE_PHONE
            },
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.graphics.PixelFormat.OPAQUE
        )
        
        Handler(Looper.getMainLooper()).post {
            try {
                wm.addView(layout, params)
                lockView = layout
            } catch (e: Exception) {
                Log.e(TAG, "Error showing lock overlay display: ${e.message}")
            }
        }
    }

    private fun hideLockOverlay() {
        val view = lockView ?: return
        val wm = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        Handler(Looper.getMainLooper()).post {
            try {
                wm.removeView(view)
                lockView = null
            } catch (e: Exception) {
                Log.e(TAG, "Error removing lock overlay: ${e.message}")
            }
        }
    }

    private fun speakTextLocally(text: String) {
        if (textToSpeech == null) {
            textToSpeech = android.speech.tts.TextToSpeech(applicationContext) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    textToSpeech?.language = java.util.Locale.US
                    performSpeak(text)
                }
            }
        } else {
            performSpeak(text)
        }
    }

    private fun performSpeak(text: String) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC),
                0
            )
            textToSpeech?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "guardian_tts")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing speak: ${e.message}")
        }
    }

    private fun playAudioPayload(base64Data: String) {
        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val tempFile = java.io.File(cacheDir, "parent_broadcast.mp3")
            if (tempFile.exists()) tempFile.delete()
            tempFile.writeBytes(bytes)
            
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC),
                0
            )
            
            android.media.MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setVolume(1.0f, 1.0f)
                start()
                setOnCompletionListener {
                    it.release()
                    try { tempFile.delete() } catch(e: java.lang.Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio payload: ${e.message}")
        }
    }

    private fun getInstalledAppsJson(): JSONArray {
        val array = JSONArray()
        val pm = packageManager
        try {
            val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            val nonSystemApps = apps.filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
            for (app in nonSystemApps) {
                val label = pm.getApplicationLabel(app).toString()
                val pkgName = app.packageName
                val obj = JSONObject().apply {
                    put("name", label)
                    put("package", pkgName)
                }
                array.put(obj)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching installation payload: ${e.message}")
        }
        return array
    }

    private fun scanFiles(type: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        val rootDir = Environment.getExternalStorageDirectory() ?: return results
        
        val targetDirs = when (type) {
            "photos" -> listOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
            "videos" -> listOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
            "documents" -> listOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            else -> listOf(rootDir)
        }

        val extensions = when (type) {
            "photos" -> listOf("jpg", "jpeg", "png", "webp", "gif")
            "videos" -> listOf("mp4", "3gp", "mkv", "avi")
            "documents" -> listOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
            else -> emptyList()
        }

        for (dir in targetDirs) {
            if (dir.exists()) {
                scanDirectory(dir, extensions, results)
            }
        }
        
        // General search backup list
        if (results.size < 5) {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dlDir.exists()) {
                scanDirectory(dlDir, extensions, results)
            }
        }
        
        return results.take(100)
    }

    private fun scanDirectory(dir: java.io.File, extensions: List<String>, results: MutableList<JSONObject>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirectory(file, extensions, results)
                }
            } else {
                val extension = file.extension.lowercase()
                if (extensions.isEmpty() || extensions.contains(extension)) {
                    val obj = JSONObject().apply {
                        put("name", file.name)
                        put("path", file.absolutePath)
                        put("size", file.length())
                        put("mimeType", extension)
                        put("lastModified", file.lastModified())
                    }
                    results.add(obj)
                }
            }
        }
    }

    private fun stopStreaming() {
        Log.d(TAG, "Stopping live captures and WebRTC session")
        try {
            screenCapturer?.stopCapture()
            screenCapturer?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing screen capturer: ${e.message}")
        }
        
        try {
            cameraCapturer?.stopCapture()
            cameraCapturer?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing camera capturer: ${e.message}")
        }

        webRtcManager?.close()
        webRtcManager = null
        
        SocketManager.disconnect()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GuardianEye Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Secures real-time streaming operations in child's device."
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopStreaming()
        super.onDestroy()
        Log.d(TAG, "ChildForegroundService destroyed")
    }
}
