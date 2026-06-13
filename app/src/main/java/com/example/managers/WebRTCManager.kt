package com.example.managers

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*
import java.util.*

class WebRTCManager(
    private val context: Context,
    private val peerId: String,
    private val isParent: Boolean,
    private val onIceCandidateReady: (toUserId: String, candidate: JSONObject) -> Unit,
    private val onLocalSdpReady: (toUserId: String, sdp: SessionDescription) -> Unit,
    private val onRemoteTrackAdded: ((MediaStream) -> Unit)? = null
) {
    companion object {
        private const val TAG = "WebRTCManager"
        
        val eglBase: EglBase by lazy {
            EglBase.create()
        }

        private var isInitialized = false

        fun initializeWebRTC(context: Context) {
            if (isInitialized) return
            try {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions()
                )
                isInitialized = true
                Log.d(TAG, "WebRTC successfully initialized")
            } catch (e: Exception) {
                Log.e(TAG, "WebRTC initialization failure: ${e.message}", e)
            }
        }
    }

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:openrelay.metered.ca:80").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
        PeerConnection.IceServer.builder("turns:openrelay.metered.ca:443")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer()
    )

    private val peerConnectionConfig: PeerConnection.RTCConfiguration by lazy {
        val config = PeerConnection.RTCConfiguration(iceServers)
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        config.iceTransportsType = PeerConnection.IceTransportsType.ALL
        config
    }

    private var factory: PeerConnectionFactory? = null
    var peerConnection: PeerConnection? = null
        private set

    init {
        initializeWebRTC(context)
        createFactory()
        createPeerConnection()
    }

    private fun createFactory() {
        val options = PeerConnectionFactory.Options()
        val builder = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))

        factory = builder.createPeerConnectionFactory()
        Log.d(TAG, "PeerConnectionFactory created successfully")
    }

    private fun createPeerConnection() {
        peerConnection = factory?.createPeerConnection(peerConnectionConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "onSignalingChange: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "onIceConnectionChange: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "onIceConnectionReceivingChange: $receiving")
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "onIceGatheringChange: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    Log.d(TAG, "Generated local ICE candidate: ${candidate.sdpMid}")
                    val candObj = JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("candidate", candidate.sdp)
                    }
                    onIceCandidateReady(peerId, candObj)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "onIceCandidatesRemoved")
            }

            override fun onAddStream(stream: MediaStream?) {
                if (stream != null) {
                    Log.d(TAG, "onAddStream: stream=${stream.id}")
                    onRemoteTrackAdded?.invoke(stream)
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "onRemoveStream: stream=${stream?.id}")
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                Log.d(TAG, "onDataChannel")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded")
                if (isParent) {
                    createOffer()
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                Log.d(TAG, "onAddTrack")
            }
        })
    }

    // Capture/Media Methods (Child side)

    fun createVideoTrackShared(videoSource: VideoSource, trackId: String): VideoTrack? {
        val track = factory?.createVideoTrack(trackId, videoSource)
        track?.setEnabled(true)
        return track
    }

    fun createAudioTrackShared(audioSource: AudioSource, trackId: String): AudioTrack? {
        val track = factory?.createAudioTrack(trackId, audioSource)
        track?.setEnabled(true)
        return track
    }

    fun createVideoSourceShared(isScreencast: Boolean): VideoSource? {
        return factory?.createVideoSource(isScreencast)
    }

    fun createAudioSourceShared(constraints: MediaConstraints): AudioSource? {
        return factory?.createAudioSource(constraints)
    }

    // Call Actions (Offer/Answer execution)

    fun createOffer() {
        val sdpMediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    Log.d(TAG, "Local SDP Offer created successfully")
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "Local SDP Offer set successfully")
                            onLocalSdpReady(peerId, desc)
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(err: String?) {
                            Log.e(TAG, "Failed to set local SDP Offer description: $err")
                        }
                    }, desc)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {
                Log.e(TAG, "Failed to create local SDP Offer: $err")
            }
            override fun onSetFailure(p0: String?) {}
        }, sdpMediaConstraints)
    }

    fun createAnswer() {
        val sdpMediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    Log.d(TAG, "Local SDP Answer created successfully")
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "Local SDP Answer set successfully")
                            onLocalSdpReady(peerId, desc)
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(err: String?) {
                            Log.e(TAG, "Failed to set local SDP Answer description: $err")
                        }
                    }, desc)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {
                Log.e(TAG, "Failed to create local SDP Answer: $err")
            }
            override fun onSetFailure(p0: String?) {}
        }, sdpMediaConstraints)
    }

    fun handleRemoteSdp(sdpTypeString: String, sdpDescription: String) {
        val type = if (sdpTypeString.equals("offer", ignoreCase = true)) {
            SessionDescription.Type.OFFER
        } else {
            SessionDescription.Type.ANSWER
        }
        val sessionDescription = SessionDescription(type, sdpDescription)
        
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote SDP ($sdpTypeString) set successfully")
                if (type == SessionDescription.Type.OFFER) {
                    createAnswer()
                }
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(err: String?) {
                Log.e(TAG, "Failed to set remote SDP ($sdpTypeString): $err")
            }
        }, sessionDescription)
    }

    fun handleRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection?.addIceCandidate(candidate)
        Log.d(TAG, "Successfully injected remote ICE candidate: $sdpMid")
    }

    fun close() {
        try {
            peerConnection?.dispose()
            factory?.dispose()
            Log.d(TAG, "WebRTC resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up WebRTC resources: ${e.message}")
        }
    }
}
