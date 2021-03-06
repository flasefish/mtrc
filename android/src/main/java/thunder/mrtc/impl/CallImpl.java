/*
 * @Author: lijianhong
 * @Date: 2021-06-04 09:49:49
 * @LastEditTime: 2021-07-08 19:18:46
 * @LastEditors: Please set LastEditors
 * @Description: Call Impl
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/impl/CallImpl.java
 */
package thunder.mrtc.impl;

import android.content.Context;
import android.util.Log;


import thunder.mrtc.Call;
import thunder.mrtc.MrtcOperator;
import thunder.mrtc.common.ErrCode;
import thunder.mrtc.impl.MrtcOperatorImpl;
import thunder.mrtc.model.MediaAttributes;
import thunder.mrtc.model.MrtcException;
import thunder.mrtc.model.MrtcSetupParam;
import thunder.mrtc.model.DeviceInfo;
import thunder.mrtc.model.FrameColorSpaceType;
import thunder.mrtc.common.ByeReason;
import thunder.mrtc.model.MediaParam;
import thunder.mrtc.callback.AcceptEventExecutor;
import thunder.mrtc.callback.CallEventExecutor;
import thunder.mrtc.callback.HangupEventExecutor;
import thunder.mrtc.listener.MrtcSdpObserver;

import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;


import java.util.List;
import java.util.UUID;
import java.nio.ByteBuffer;

public class CallImpl implements Call {
    
    private final static String TAG = "MRTC_CALL";

    private SignalingManager signaling;
    
    private PeerConnectionManager peerConnectionManager;

    private boolean isCaller;

    private long mediaStartTime;

    private CallEventExecutor callEventExecutor;

    private long timeout;

    private String sessionId;

    private DeviceInfo callerInfo;

    private DeviceInfo calleeInfo;

    private SessionDescription offerSdp;

    private MediaParam mediaParam;

    private SurfaceViewRenderer remoteRender;

    private boolean isCalling = false;

    public CallImpl(String sessionId) {
        this.sessionId = sessionId;
    }

    public void release() {
        Log.i(TAG, "release call info for " +  sessionId);
        if (peerConnectionManager != null) {
            peerConnectionManager.release();
        }
        peerConnectionManager = null;
    }

    public void initPeerConnectionManager() {
        this.peerConnectionManager = new PeerConnectionManagerImpl(this.sessionId);
    }

    public PeerConnectionManager getPeerConnectionManager() {
        return this.peerConnectionManager;
    }

    public void setSignalingManager(SignalingManager signalingManager) {
        this.signaling = signalingManager;
    }
    
    public SignalingManager getSignalingManager() {
        return this.signaling;
    }

    public void setCallExtraData(String callExtraData) {
       DeviceInfo deviceInfo = this.signaling.getDeviceInfo();
       deviceInfo.setCallExtraData(callExtraData);
    }

    public void setMediaAttributes(MediaAttributes mediaAttributes) {
        if (this.peerConnectionManager != null) {
            this.peerConnectionManager.setMediaAttributes(mediaAttributes);
        }
    }

    public void setCaller() {
        this.isCaller = true;
    }

    public void setCallee() {
        this.isCaller = false;
    }

    public void setCallEventExecutor(CallEventExecutor callEventExecutor) {
        this.callEventExecutor = callEventExecutor;
    }

    public CallEventExecutor getCallEventExecutor() {
        return this.callEventExecutor;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public void setIsCalling(boolean isCalling) {
        this.isCalling = isCalling;
    }

    public void setMediaStartTime(long mediaStartTime) {
        this.mediaStartTime = mediaStartTime;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setOfferSdp(SessionDescription sdp) {
        this.offerSdp = sdp;
    }

    public void acceptAnswerSdp(SessionDescription sdp) {
        if (peerConnectionManager == null) {
            Log.e(TAG, "Peer connection is nul for session: " + sessionId);
            return;
        }
        PeerConnection peerConnection = this.peerConnectionManager.getPeerConnection();
        peerConnection.setRemoteDescription(new MrtcSdpObserver(){
            @Override
            public void onSetSuccess() {
                super.onSetSuccess();
                signaling.setIceChecking(true);
                signaling.grabIceCandidate(sessionId, null);
                signaling.sendIceCandidate(null, callerInfo, calleeInfo, sessionId);
            }
        } , sdp); 
    }

   
    public void startCall(String calleeNumber) {
        this.callerInfo = this.signaling.getDeviceInfo();
        this.calleeInfo = new DeviceInfo();
        
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        PeerConnection peerConnection = this.peerConnectionManager.getPeerConnection();
        peerConnection.createOffer(new MrtcSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);

                String sdp = sessionDescription.description;
                sdp = PeerConnectionManagerImpl.setSdpPreferredCodec(sdp, "PCMA", true);
                sdp = PeerConnectionManagerImpl.removeSdpCodec(sdp, "opus", true);
                sdp = PeerConnectionManagerImpl.removeSdpCodec(sdp, "ISAC", true);
                sdp = PeerConnectionManagerImpl.removeSdpCodec(sdp, "ILBC", true);
                sdp = PeerConnectionManagerImpl.removeSdpCodec(sdp, "G722", true);

                sdp = PeerConnectionManagerImpl.setSdpPreferredCodec(sdp, "H264", false);
                sdp = PeerConnectionManagerImpl.removeSdpCodec(sdp, "H265", false);
                sdp = PeerConnectionManagerImpl.removeSdpCodec(sdp, "VP8", false);
                sdp = PeerConnectionManagerImpl.removeSdpCodec(sdp, "VP9", false);
//                BILog.d(TAG, "sdp: \r\n" + sdp);
                final SessionDescription newDesc = new SessionDescription(sessionDescription.type, sdp);

                peerConnection.setLocalDescription(new MrtcSdpObserver(), newDesc);
                signaling.sendOfferSdp(sdp, calleeNumber, timeout, sessionId);
            } 
        }, mediaConstraints);
    }

    public void setCallerInfo(DeviceInfo deviceInfo) {
        this.callerInfo = deviceInfo;
    }

    public void setCalleeInfo(DeviceInfo deviceInfo) {
        this.calleeInfo = deviceInfo;
    }

    public DeviceInfo getCallerInfo() {
        return this.callerInfo;
    }

    public DeviceInfo getCalleeInfo() {
        return this.calleeInfo;
    }
    
    /**
     * ???????????????????????????
     */
    @Override
    public DeviceInfo getCaller() {
        return this.callerInfo;
    }

    /**
     * ???????????????????????????
     */
    @Override
    public DeviceInfo getCallee() {
        return this.calleeInfo;
    }

    /**
     * ?????????????????????
     */
    @Override
    public boolean isCaller() {
        return this.isCaller;
    }

    /**
     * ??????????????????
     */
    @Override
    public long getMediaStartTime() {
        return 0;
    }

    /**
     * ????????????(????????????????????????ByeReason??????)
     * @throws MrtcException
     */
    @Override
    public void hangup(HangupEventExecutor hangupEventExecutor, ByeReason byeReason) throws MrtcException {
        Log.d(TAG, "start to hangup the call");
        if (!signaling.getRegState() || hangupEventExecutor == null) {
            hangupEventExecutor.onFail(1002, "signaling not register");
        } 
        
        if (isCalling) {
            this.signaling.sendBye(this.callerInfo, this.calleeInfo, this.sessionId);
        } else {
            if (isCaller) {
                this.signaling.sendCancel(this.callerInfo, this.calleeInfo, this.sessionId);
            } else {
                this.signaling.sendRejectMsg(this.callerInfo, this.calleeInfo, this.sessionId);
            }
        }
        this.release();
        CallManager.removeCall(this.sessionId);
        if (hangupEventExecutor != null) {
            hangupEventExecutor.onSuccess();
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????
     * @param direction
     */
    @Override
    public void updateMediaAttributes(MediaAttributes direction) {
        
    }

    /**
     * ?????????????????????????????????MrtcSetupParam.isRawStream??????true???????????????????????????
     * @param width
     * @param height
     * @param framePerSecond
     */
    @Override
    public void onExternalSourceInit(int width, int height, int framePerSecond) {

    }

    /**
     * ??????????????????????????????????????????
     * @param array
     * @param frameColorSpaceType
     */
    @Override
    public void onExternalFrame(byte[] array, FrameColorSpaceType frameColorSpaceType) {
        if (this.peerConnectionManager !=  null) {
            this.peerConnectionManager.onExternalFrame(array); 
        }
    }

    /**
     * ????????????????????????????????????
     * @param mediaParam
     */
    @Override
    public void setupMedia(MediaParam mediaParam) {
        this.mediaParam = mediaParam;
    }

    /**
     * ???????????????????????????????????????
     */
    @Override
    public void setMicSwitch(boolean sw) {
        if (this.peerConnectionManager != null) {
            this.peerConnectionManager.setLocalAudioTrack(sw);
        }
    }

    /**
     * ???????????????????????????????????????
     */
    @Override
    public void getMicSwitch() {

    }

    /**
     * ????????????????????????????????????
     */
    @Override
    public void setAudioSwitch(boolean sw) {
        if (this.peerConnectionManager != null) {
            this.peerConnectionManager.setRemoteAudioTrack(sw);
        }
    }

    /**
     * ????????????????????????????????????
     */
    @Override
    public void getAudioSwitch() {
        
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????
     * @param mediaAttributes ???????????????????????????
     * @param acceptEventExecutor ?????????????????????
     * @param callExtraData ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    @Override
    public void accept(MediaAttributes mediaAttributes, AcceptEventExecutor acceptEventExecutor, String callExtraData) {
        Log.d(TAG, "start to accept the call");
        if (!signaling.getRegState() && acceptEventExecutor != null) {
            acceptEventExecutor.onFail(ErrCode.DEVICE_OFFLINE.getValue(), "signaling not register");
        }
        initPeerConnectionManager();
        setCallExtraData(callExtraData);
        setMediaAttributes(mediaAttributes);
        isCalling = true;

        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        PeerConnection peerConnection = this.peerConnectionManager.getPeerConnection();
        peerConnection.setRemoteDescription(new MrtcSdpObserver() {
            @Override
            public void onSetSuccess() {
                super.onSetSuccess();
                signaling.setIceChecking(true);
                signaling.grabIceCandidate(sessionId, null);
                peerConnection.createAnswer(new MrtcSdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        super.onCreateSuccess(sessionDescription);
                        peerConnection.setLocalDescription(new MrtcSdpObserver(), sessionDescription);
                        signaling.sendAnswerSdp(sessionDescription.description, callerInfo, calleeInfo, sessionId);
                    } 
                }, mediaConstraints);

            }
        }, this.offerSdp);
        if (acceptEventExecutor != null) {
            acceptEventExecutor.onSuccess();
        }
    }

     /**
     * ??????Datachannel??????
     */
    @Override
    public void sendDataChannelMsg(ByteBuffer msg) {
        if (this.peerConnectionManager == null) {
            Log.e(TAG, "sendDataChannelMsg: PeerConnection Manager is null!");
            return;
        }

        Log.i(TAG, "send data channel msg");
        PeerConnection peerConnection = this.peerConnectionManager.getPeerConnection();
        if (peerConnection == null) {
            Log.e(TAG, "send data fail cause by peer connection null");
            return;
        }
        peerConnectionManager.sendMsg(msg);
    }
}