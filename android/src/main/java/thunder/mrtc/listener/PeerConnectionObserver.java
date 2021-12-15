/*
 * @Author: your name
 * @Date: 2021-06-08 11:08:07
 * @LastEditTime: 2021-07-05 21:56:50
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/listener/SdpObserver.java
 */
package thunder.mrtc.listener;

import android.util.Log;


import thunder.mrtc.impl.CallManager;
import thunder.mrtc.impl.CallImpl;
import thunder.mrtc.impl.SignalingManager;
import thunder.mrtc.common.ErrCode;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;

public class PeerConnectionObserver implements PeerConnection.Observer {
    

    private static final String TAG = "MRTC_PEER_OBSERVER";

    private String sessionId;

    public PeerConnectionObserver(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(TAG, "onSignalingChange: " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.i(TAG, "onIceConnectionChange: " + iceConnectionState);
        CallImpl call = (CallImpl) CallManager.getCall(sessionId);
        SignalingManager signaling = call.getSignalingManager();
        switch(iceConnectionState) {
            case FAILED:
                signaling.getOnLineEventExecutor().onException(ErrCode.P2P_NETWORK_ERROR.getValue(), "p2p network can not established");
                break;
            case COMPLETED:
                signaling.clearIceBuffer();
            default:
        } 
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.i(TAG, "onIceConnectionChange: " + b);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.i(TAG, "onIceGatheringChange: " + iceGatheringState);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.i(TAG, "onIceCandidate: " + iceCandidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        for (int i = 0; i < iceCandidates.length; i++) {
            Log.i(TAG, "onIceCandidatesRemoved: " + iceCandidates[i]);
        }
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.i(TAG, "onAddStream: " + mediaStream.videoTracks.size());
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.i(TAG, "onRemoveStream " + mediaStream.getId());
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.i(TAG, "onDataChannel " + dataChannel.label());
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.i(TAG, "onRenegotiationNeeded");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        MediaStreamTrack track = rtpReceiver.track();
        if (track != null) {
            Log.d(TAG, "onAddRemoteTrack " + track.kind());
        }
    }
}
