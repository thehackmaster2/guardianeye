package com.example.managers

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject
import org.json.JSONArray
import java.net.URISyntaxException
import com.example.services.RemoteControlAccessibilityService

object SocketManager {
    private const val TAG = "SocketManager"
    
    private var signalingUrl = "https://parental-control-server-production-fef5.up.railway.app"
    private var socket: Socket? = null

    // Standard signaling and authentication flows
    private val _joinedFlow = MutableSharedFlow<JoinedEvent>(extraBufferCapacity = 64)
    val joinedFlow: SharedFlow<JoinedEvent> = _joinedFlow

    private val _startCallFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val startCallFlow: SharedFlow<String> = _startCallFlow

    private val _offerFlow = MutableSharedFlow<OfferEvent>(extraBufferCapacity = 64)
    val offerFlow: SharedFlow<OfferEvent> = _offerFlow

    private val _answerFlow = MutableSharedFlow<AnswerEvent>(extraBufferCapacity = 64)
    val answerFlow: SharedFlow<AnswerEvent> = _answerFlow

    private val _iceCandidateFlow = MutableSharedFlow<IceCandidateEvent>(extraBufferCapacity = 64)
    val iceCandidateFlow: SharedFlow<IceCandidateEvent> = _iceCandidateFlow

    private val _peerDisconnectedFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val peerDisconnectedFlow: SharedFlow<String> = _peerDisconnectedFlow

    // Advanced Remote interactions flow
    private val _remoteControlFlow = MutableSharedFlow<RemoteControlEvent>(extraBufferCapacity = 64)
    val remoteControlFlow: SharedFlow<RemoteControlEvent> = _remoteControlFlow

    // Features flows - Parent commands receivers
    private val _requestFilesFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val requestFilesFlow: SharedFlow<String> = _requestFilesFlow

    private val _requestFileDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val requestFileDataFlow: SharedFlow<String> = _requestFileDataFlow

    private val _lockScreenFlow = MutableSharedFlow<LockEvent>(extraBufferCapacity = 64)
    val lockScreenFlow: SharedFlow<LockEvent> = _lockScreenFlow

    private val _playAudioFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val playAudioFlow: SharedFlow<String> = _playAudioFlow

    private val _speakTextFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val speakTextFlow: SharedFlow<String> = _speakTextFlow

    private val _requestAppsFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val requestAppsFlow: SharedFlow<Unit> = _requestAppsFlow

    // Feature flows - Parent metadata collectors
    private val _fileListFlow = MutableSharedFlow<JSONArray>(extraBufferCapacity = 64)
    val fileListFlow: SharedFlow<JSONArray> = _fileListFlow

    private val _fileDataFlow = MutableSharedFlow<FileDataEvent>(extraBufferCapacity = 64)
    val fileDataFlow: SharedFlow<FileDataEvent> = _fileDataFlow

    private val _appListFlow = MutableSharedFlow<JSONArray>(extraBufferCapacity = 64)
    val appListFlow: SharedFlow<JSONArray> = _appListFlow

    private val _notificationFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val notificationFlow: SharedFlow<JSONObject> = _notificationFlow

    fun setSignalingUrl(url: String) {
        if (url.isNotEmpty() && url.startsWith("http")) {
            signalingUrl = url
        }
    }

    fun isConnected(): Boolean {
        return socket?.connected() == true
    }

    fun connect() {
        if (socket != null && socket?.connected() == true) {
            Log.d(TAG, "Already connected to signaling server")
            return
        }

        try {
            Log.d(TAG, "Connecting to signaling server: $signalingUrl")
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
            }
            socket = IO.socket(signalingUrl, opts)
            setupListeners()
            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Signaling URL invalid syntax: ${e.message}", e)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from signaling server")
        socket?.disconnect()
        socket = null
    }

    private fun setupListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Connected to signaling server successfully")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Connection error: ${args.firstOrNull() ?: "unknown"}")
        }

        socket?.on("joined") { args ->
            try {
                val data = args[0] as JSONObject
                val roomCode = data.getString("roomCode")
                val role = data.getString("role")
                val socketId = data.optString("socketId", "")
                Log.d(TAG, "Joined room successfully: code=$roomCode, role=$role")
                _joinedFlow.tryEmit(JoinedEvent(roomCode, role, socketId))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'joined' content: ${e.message}")
            }
        }

        socket?.on("start-call") { args ->
            try {
                val data = args[0] as JSONObject
                val childId = data.getString("childId")
                _startCallFlow.tryEmit(childId)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'start-call': ${e.message}")
            }
        }

        socket?.on("webrtc-offer") { args ->
            try {
                val data = args[0] as JSONObject
                val from = data.getString("from")
                val sdp = data.getString("sdp")
                _offerFlow.tryEmit(OfferEvent(from, sdp))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'webrtc-offer'")
            }
        }

        socket?.on("webrtc-answer") { args ->
            try {
                val data = args[0] as JSONObject
                val from = data.getString("from")
                val sdp = data.getString("sdp")
                _answerFlow.tryEmit(AnswerEvent(from, sdp))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'webrtc-answer'")
            }
        }

        socket?.on("ice-candidate") { args ->
            try {
                val data = args[0] as JSONObject
                val from = data.getString("from")
                val candidateObj = data.getJSONObject("candidate")
                _iceCandidateFlow.tryEmit(IceCandidateEvent(from, candidateObj))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'ice-candidate'")
            }
        }

        socket?.on("remote-control") { args ->
            try {
                val data = args[0] as JSONObject
                val actionObj = data.getJSONObject("action")
                val type = actionObj.getString("type")
                if (type == "swipe") {
                    val x1 = actionObj.getDouble("x1").toFloat()
                    val y1 = actionObj.getDouble("y1").toFloat()
                    val x2 = actionObj.getDouble("x2").toFloat()
                    val y2 = actionObj.getDouble("y2").toFloat()
                    val duration = actionObj.getLong("duration")
                    _remoteControlFlow.tryEmit(RemoteControlEvent(type = "swipe", x1 = x1, y1 = y1, x2 = x2, y2 = y2, duration = duration))
                } else {
                    val x = actionObj.getDouble("x").toFloat()
                    val y = actionObj.getDouble("y").toFloat()
                    _remoteControlFlow.tryEmit(RemoteControlEvent(type = type, x = x, y = y))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'remote-control': ${e.message}")
            }
        }

        socket?.on("peer-disconnected") { args ->
            try {
                val data = args[0] as JSONObject
                val role = data.getString("role")
                _peerDisconnectedFlow.tryEmit(role)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing 'peer-disconnected'")
            }
        }

        // Feature listeners
        socket?.on("request-files") { args ->
            val type = args.getOrNull(0) as? String ?: "all"
            _requestFilesFlow.tryEmit(type)
        }

        socket?.on("file-list") { args ->
            val list = args.getOrNull(0) as? JSONArray
            if (list != null) {
                _fileListFlow.tryEmit(list)
            }
        }

        socket?.on("request-file-data") { args ->
            val path = args.getOrNull(0) as? String ?: ""
            _requestFileDataFlow.tryEmit(path)
        }

        socket?.on("file-data") { args ->
            try {
                val data = args[0] as JSONObject
                val path = data.getString("path")
                val base64 = data.getString("base64")
                _fileDataFlow.tryEmit(FileDataEvent(path, base64))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing file-data: ${e.message}")
            }
        }

        socket?.on("lock-screen") { args ->
            try {
                val data = args[0] as JSONObject
                val locked = data.getBoolean("locked")
                val message = data.optString("message", "")
                _lockScreenFlow.tryEmit(LockEvent(locked, message))
                
                if (data.has("blockedApps")) {
                    val appsArray = data.getJSONArray("blockedApps")
                    val set = mutableSetOf<String>()
                    for (i in 0 until appsArray.length()) {
                        set.add(appsArray.getString(i))
                    }
                    RemoteControlAccessibilityService.updateBlockedApps(set)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error lock-screen layout: ${e.message}")
            }
        }

        socket?.on("play-audio") { args ->
            val base64 = args.getOrNull(0) as? String ?: ""
            _playAudioFlow.tryEmit(base64)
        }

        socket?.on("speak-text") { args ->
            val text = args.getOrNull(0) as? String ?: ""
            _speakTextFlow.tryEmit(text)
        }

        socket?.on("request-apps") {
            _requestAppsFlow.tryEmit(Unit)
        }

        socket?.on("app-list") { args ->
            val list = args.getOrNull(0) as? JSONArray
            if (list != null) {
                _appListFlow.tryEmit(list)
            }
        }

        socket?.on("block-apps") { args ->
            try {
                val array = args[0] as JSONArray
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    set.add(array.getString(i))
                }
                RemoteControlAccessibilityService.updateBlockedApps(set)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing block-apps packet: ${e.message}")
            }
        }

        socket?.on("child-notification") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) {
                _notificationFlow.tryEmit(data)
            }
        }
    }

    // EMITTERS

    fun emitJoinRoom(roomCode: String, role: String) {
        val data = JSONObject().apply {
            put("roomCode", roomCode)
            put("role", role)
        }
        socket?.emit("join-room", data)
    }

    fun emitOffer(to: String, sdp: String) {
        val data = JSONObject().apply {
            put("to", to)
            put("sdp", sdp)
        }
        socket?.emit("webrtc-offer", data)
    }

    fun emitAnswer(to: String, sdp: String) {
        val data = JSONObject().apply {
            put("to", to)
            put("sdp", sdp)
        }
        socket?.emit("webrtc-answer", data)
    }

    fun emitIceCandidate(to: String, candidate: JSONObject) {
        val data = JSONObject().apply {
            put("to", to)
            put("candidate", candidate)
        }
        socket?.emit("ice-candidate", data)
    }

    fun emitRemoteControl(type: String, x: Float, y: Float) {
        val action = JSONObject().apply {
            put("type", type)
            put("x", x)
            put("y", y)
        }
        val data = JSONObject().apply {
            put("action", action)
        }
        socket?.emit("remote-control", data)
    }

    fun emitRemoteControlSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long) {
        val action = JSONObject().apply {
            put("type", "swipe")
            put("x1", x1)
            put("y1", y1)
            put("x2", x2)
            put("y2", y2)
            put("duration", duration)
        }
        val data = JSONObject().apply {
            put("action", action)
        }
        socket?.emit("remote-control", data)
    }

    fun emitFileList(list: JSONArray) {
        socket?.emit("file-list", list)
    }

    fun emitFileData(path: String, base64: String) {
        val data = JSONObject().apply {
            put("path", path)
            put("base64", base64)
        }
        socket?.emit("file-data", data)
    }

    fun emitChildNotification(data: JSONObject) {
        socket?.emit("child-notification", data)
    }

    fun emitRequestApps() {
        socket?.emit("request-apps")
    }

    fun emitAppList(list: JSONArray) {
        socket?.emit("app-list", list)
    }

    fun emitBlockApps(packages: JSONArray) {
        socket?.emit("block-apps", packages)
    }

    fun emitLockScreen(locked: Boolean, message: String) {
        val data = JSONObject().apply {
            put("locked", locked)
            put("message", message)
        }
        socket?.emit("lock-screen", data)
    }

    fun emitSpeakText(text: String) {
        socket?.emit("speak-text", text)
    }

    fun emitPlayAudio(base64: String) {
        socket?.emit("play-audio", base64)
    }

    fun emitRequestFiles(type: String) {
        socket?.emit("request-files", type)
    }

    fun emitRequestFileData(path: String) {
        socket?.emit("request-file-data", path)
    }
}

// Event Data Classes
data class JoinedEvent(val roomCode: String, val role: String, val socketId: String)
data class OfferEvent(val from: String, val sdp: String)
data class AnswerEvent(val from: String, val sdp: String)
data class IceCandidateEvent(val from: String, val candidate: JSONObject)
data class RemoteControlEvent(
    val type: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val x1: Float = 0f,
    val y1: Float = 0f,
    val x2: Float = 0f,
    val y2: Float = 0f,
    val duration: Long = 0L
)
data class LockEvent(val locked: Boolean, val message: String)
data class FileDataEvent(val path: String, val base64Data: String)
