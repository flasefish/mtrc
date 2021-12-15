/*
 * @Author: lijianhong
 * @Date: 2021-06-04 11:06:29
 * @LastEditTime: 2021-07-08 19:03:08
 * @LastEditors: Please set LastEditors
 * @Description: signaling manager
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/impl/SignalingManagerImpl.java
 */
package thunder.mrtc.impl; 

import android.text.TextUtils;
import android.util.Log;


import thunder.mrtc.callback.OnLineEventExecutor;
import thunder.mrtc.callback.CallEventExecutor;
import thunder.mrtc.common.ErrCode;
import thunder.mrtc.common.SignalingType;
import thunder.mrtc.model.DeviceInfo;
import thunder.mrtc.MrtcOperator;
import thunder.mrtc.common.ByeReason;
import thunder.mrtc.common.SignalingValue;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;


public class SignalingManagerImpl implements SignalingManager {

    private final static String TAG = "MRTC_SIGNALING";

    private final static boolean TOKEN_FLAG = false;

    private static final int MIN_HEARTBEAT_INTERVAL = 10;

    private static final int MAX_HEARTBEAT_INTERVAL = 300;

    // WebSocket connection timeout
    private static final int WS_CONNECT_TIMEOUT = 0;

    private URI serverAddress;

    private WebSocketClient client;

    private OnLineEventExecutor onLineEventExecutor;

    private boolean isConnected = false;

    private boolean isReg = false;

    private DeviceInfo deviceInfo;

    private String token;

    private int retryTimes;

    private int retryInterval;

    private boolean isIceChecking;

    private int mHeartBeatIntervalInSec = MIN_HEARTBEAT_INTERVAL;

    private Timer mHeartBeatTimer = null;

    private TimerTask mHeartBeatTask = null;

    private ConcurrentLinkedQueue<IceCandidate> recIceDelayQueue = new ConcurrentLinkedQueue<>();

    private ConcurrentLinkedQueue<IceCandidate> sendIceDelayQueue = new ConcurrentLinkedQueue<>();

    /**
     * @description: set server address 
     * @param {String} addr
     * @return {*}
     */
    @Override
    public void setServerAddress(String addr) {
        if (TextUtils.isEmpty(addr)) {
            Log.e(TAG, "Signaling Server is Empty");
            return;
        }
        
        try {
            this.serverAddress = new URI(addr);
        } catch (Exception e) {
            Log.e(TAG, "URI parser error: " + e.toString());
        }
    }

    /**
     * @description: 
     * @param {*}
     * @return {*}
     */
    @Override
    public void setIceChecking(boolean isChecking){
        this.isIceChecking = isChecking;
    }

    /**
     * @description: get online event callback
     * @param {*} 
     * @return {OnLineEventExecutor}
     */
    public OnLineEventExecutor getOnLineEventExecutor() {
        return onLineEventExecutor;
    }

    /**
     * @description: set online event callback
     * @param {OnLineEventExecutor} onLineEventExecutor
     * @return {*}
     */
    public void setOnLineEventExecutor(OnLineEventExecutor onLineEventExecutor) {
        if (this.onLineEventExecutor != null) {
            return;
        }
        this.onLineEventExecutor = onLineEventExecutor;
    }

     /**
     * @description: 
     * @param {*}
     * @return {*}
     */
    public DeviceInfo getDeviceInfo() {
        return this.deviceInfo;
    }

    /**
     * @description: 
     * @param {DeviceInfo} deviceInfo
     * @return {*}
     */
    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }


    /**
     * @description: 
     * @param {int} heartBeatInterval
     * @return {*}
     */
    public void setHeartBeatInterval(int heartBeatInterval) {
        if (heartBeatInterval < MIN_HEARTBEAT_INTERVAL) {
            this.mHeartBeatIntervalInSec = MIN_HEARTBEAT_INTERVAL;
        }
        else if (heartBeatInterval > MAX_HEARTBEAT_INTERVAL) {
            this.mHeartBeatIntervalInSec = MAX_HEARTBEAT_INTERVAL;
        }
        else {
            this.mHeartBeatIntervalInSec = heartBeatInterval;
        }
    }


    /**
     * @description: 
     * @param {int} retryInterval
     * @return {*}
     */
    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    /**
     * @description: 
     * @param {int} retryTimes
     * @return {*}
     */
    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    /**
     * @description: 
     * @param {String} token
     * @return {*}
     */
    public void setToken(String token) {
        this.token = token;
    }

    
    @Override
    /**
     * @description: connect to the websocket
     * @param {DeviceInfo} deviceInfo
     * @return {*}
     */
    public boolean connect() {
        if (this.serverAddress == null) {
            Log.e(TAG, "WebSocket Address is null");
            return false;
        }
        if (this.client != null && this.client.isOpen()) {
            Log.e(TAG, "WebSocket Client is error");
            return false;
        }
        
        if (this.isConnected) {
            Log.w(TAG, "WebSocket Connection is connected, can not new connection");
            return true;
        }
        
        new Thread(() -> {
            try {
                doConnect();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }).start();
            
        return true;
    }
    
    /**
     * @description: 
     * @param {*}
     * @return {*}
     */
    public boolean close() {
        if (this.client != null && this.client.isOpen()) {
            this.client.close();
        }

        stopHeartBeatTimer();

        return true;
    }

    private void startHeartBeatTimer() {
        stopHeartBeatTimer();

        mHeartBeatTimer = new Timer();
        mHeartBeatTask = new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        };
        mHeartBeatTimer.schedule(mHeartBeatTask, this.mHeartBeatIntervalInSec * 1000 / 2,
                (this.mHeartBeatIntervalInSec - 2) * 1000);
    }

    private void stopHeartBeatTimer() {
        if (mHeartBeatTimer != null) {
            mHeartBeatTimer.cancel();
            mHeartBeatTimer = null;
        }

        if (mHeartBeatTask != null) {
            mHeartBeatTask.cancel();
            mHeartBeatTask = null;
        }
    }

    private boolean doConnect() {
        this.client = new WebSocketClient(this.serverAddress) {
            @Override
            public void onOpen(ServerHandshake handShake) {
                Log.d(TAG, "WebSocket OnOpen: " + handShake.getHttpStatus());
                sendOnlineReq();
            }

            @Override
            public void onMessage(String str) {
                onRemoteMessage(str);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.d(TAG, "WebSocket connect closed: " + s);

                stopHeartBeatTimer();

                CallImpl call = (CallImpl)CallManager.getOnlineCall();
                if (call != null) {
                    call.getCallEventExecutor().onException(ErrCode.NETWORK_ERROR.getValue(), "network disconnected");
                    onLineEventExecutor.onCloseWebsocket();
                    call.release();
                    CallManager.clearAllCall();
                } else {
                    onLineEventExecutor.onCloseWebsocket();
                    //call.release();
                    CallManager.clearAllCall();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "WebSocket receive error: " + e.toString());
                onLineEventExecutor.onException(ErrCode.DEVICE_OFFLINE.getValue(), e.toString());
            }
        };
        Log.d(TAG, "Begin connect with uri: " + this.serverAddress.toString());
        
        try {
            this.client.setConnectionLostTimeout(WS_CONNECT_TIMEOUT);
            boolean isConnected = this.client.connectBlocking();
            if (isConnected) {
                Log.i(TAG, "WebSocket with uri: " +  this.serverAddress.toString() + " is connected");
                
                return true;
            } else {
                Log.e(TAG, "WebSocket with uri: " +  this.serverAddress.toString() + " is not connected");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "WebSocket connect error: " +  this.serverAddress.toString() + " " + e.toString());
           return false;
        }
    }

    @Override
    public boolean getRegState() {
        return this.isReg;
    }

    /**
     * @description: send online message 
     * @param {JSONObject*} onlineReq 
     * @return {*}
     */
    @Override
    public void sendOnlineReq() {
        try {
            JSONObject header = new JSONObject();
            header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
            header.put(SignalingValue.MSG_ID, SignalingType.online);
            JSONObject body = new JSONObject();
            if (TOKEN_FLAG) {
                body.put(SignalingValue.ONLINE_ID, "");
            } else {
                body.put(SignalingValue.ONLINE_ID, deviceInfo.getOnlineId());
            }
            body.put(SignalingValue.EXTRA_DATA, deviceInfo.getOnlineExtraData());
            body.put(SignalingValue.TOKEN, token);
            if (this.mHeartBeatIntervalInSec > 0) {
                body.put(SignalingValue.TIME_OUT, this.mHeartBeatIntervalInSec);
            }
            header.put(SignalingValue.BODY, body);
            sendSignalMessage(header);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    
    /**
     * @description: process online message  
     * @param {String} onlineResp
     * @return {*}
     */
    @Override
    public void processOnlineResp(JSONObject msg) {
        try {
            JSONObject body = msg.getJSONObject(SignalingValue.BODY);
            int errCode = msg.getInt(SignalingValue.ERR_CODE);
            String errMsg = msg.getString(SignalingValue.ERR_MSG);
            if (errCode == 200) {
                this.isReg = true;
                this.deviceInfo.setOnlineId(body.getString(SignalingValue.ONLINE_ID));
                this.onLineEventExecutor.onRegSuccess();

                startHeartBeatTimer();
            } else {
                this.isReg = false;
                Log.e(TAG, "online fail: " + errMsg);
                switch (errCode) {
                    case SignalingValue.CALL_ERROR:
                        this.onLineEventExecutor.onException(ErrCode.ONLINE_ERROR.getValue(), errMsg);
                        break;
                    case SignalingValue.CALL_UNAUTH:
                        this.onLineEventExecutor.onException(ErrCode.ONLINE_UNAUTH.getValue(), errMsg);
                        break;
                    case SignalingValue.CALL_INTERNAL_ERROR:
                        Log.e(TAG, errMsg);
                        break;
                    default:
                        Log.e(TAG, "unrecognized online response");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
    * @description: send offline message 
    * @param {String} offlineReq
    * @return {*}
    */
    @Override
    public void sendOfflineReq() {
        try {
            JSONObject header = new JSONObject();
            header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
            header.put(SignalingValue.MSG_ID, SignalingType.offline);
            JSONObject body = new JSONObject();
            body.put(SignalingValue.ONLINE_ID, this.deviceInfo.getOnlineId());
            header.put(SignalingValue.BODY, body);
            sendSignalMessage(header);
            this.isReg = false;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * @description: send ping
     * @param {JSONObject} pingMessage
     * @return {*}
     */
    @Override
    public void sendPing() {
        try {
            JSONObject header = new JSONObject();
            header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
            header.put(SignalingValue.MSG_ID, SignalingType.ping);
            JSONObject body = new JSONObject();
            body.put(SignalingValue.ONLINE_ID, deviceInfo.getOnlineId());
            header.put(SignalingValue.BODY, body);
            sendSignalMessage(header);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * @description: process pong
     * @param {JSONObject} pongMessage
     * @return {*}
     */
    @Override
    public void processPong(JSONObject pongMessage) {
        Log.d(TAG, "get the pong message");
    }

    /**
     * @description: call sdp candidate message sender
     * @param {JSONObject} bizMessage
     * @return {*}
     */
    @Override
    public void sendSignalMessage(JSONObject bizMessage) {
        Log.d(TAG, "[Signaling] send message:" + bizMessage.toString());
        try {
            this.client.send(bizMessage.toString()); 
        } catch (Exception e) {
            Log.e(TAG, "send message fail: " + e.getMessage());
            CallImpl call = (CallImpl)CallManager.getOnlineCall();
            if (call != null) {
                call.getCallEventExecutor().onException(ErrCode.NETWORK_ERROR.getValue(), "network disconnected");
            } else {
                onLineEventExecutor.onException(ErrCode.NETWORK_ERROR.getValue(), "network disconnected");
            }
        }
    }

    private void onRemoteMessage(String message) {
        if (TextUtils.isEmpty(message)) {
            Log.e(TAG, "Remote essage is Empty");
            return;
        }
        try {
            dispatchMessage(message);
        } catch (Exception e) {
            Log.e(TAG, "Dispatch message fail : " + e.getMessage());
        }
    }

    private void dispatchMessage(String message) throws Exception{
        JSONObject root = new JSONObject(message);
        SignalingType type = SignalingType.valueOf(root.getString(SignalingValue.MSG_ID));
        switch (type) {
            case onlineResponse:
                Log.d(TAG, "[Signaling] online resp: " + message);
                processOnlineResp(root);
                break;
            case notifyCallStatus:
                Log.d(TAG, "[Signaling] notify callStatus: " + message);
                processCallStatus(root);
                break;
            case call:
                Log.d(TAG, "[Signaling] callIn request: " + message);
                processCallIn(root);
                break;
            case cancelCall:
                Log.d(TAG, "[Signaling] cancelCall request: " + message);
                processCancelCall(root);
                break;
            case callResponse:
                Log.d(TAG, "[Signaling] callOut resp: " + message);
                processCallResp(root);
                break;
            case cancelCallResponse:
                Log.d(TAG, "[Signaling] cancelCall resp: " + message);
                break;
            case notifyIceCandidate:
                Log.d(TAG, "[Signaling] notify iceCandidate: " + message);
                processIceCandidate(root);
                break;
            case bye:
                Log.d(TAG, "[Signaling] call Bye: " + message);
                processByeResp(root);
                break;
            case pong:
                Log.d(TAG, "[Signaling] heart beat: " + message);
                break;
            default:
                Log.e(TAG, "[Signaling] Unknown msg: " + message);
        }
    }

    /**
     * @description: websocket send message method 
     * @param {String} message
     * @return {*}
     */
    private void sendMessage(String message) {
        if (this.client.isOpen() && !TextUtils.isEmpty(message)) {
            Log.d(TAG, "send message: " + message);
            try {
                this.client.send(message);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            Log.e(TAG, "send message fail");
        }
    }

    /**
     * @description: 
     * @param {String} offer
     * @param {String} calleeNumber
     * @return {*}
     */
    public void sendOfferSdp(String offer, String calleeNumber, long timeout, String sessionId) {
        try {
            JSONObject header = new JSONObject();
            header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
            header.put(SignalingValue.MSG_ID, SignalingType.call);
            JSONObject body = new JSONObject();
            body.put(SignalingValue.SESSION_ID, sessionId);
            body.put(SignalingValue.CALLER_ID, this.deviceInfo.getOnlineId());
            JSONObject extraData = new JSONObject();
            extraData.put(SignalingValue.CALLER_CALL_EXTRA_DATA, deviceInfo.getCallExtraData());
            if (!TOKEN_FLAG) {
                body.put(SignalingValue.CALLEE_IDS, new JSONArray(new Object[]{ calleeNumber }));
            } else {
                extraData.put(SignalingValue.CALLEE_NUMBER, calleeNumber);
            }
            body.put(SignalingValue.SDP, offer);
            body.put(SignalingValue.RINGING_TIMEOUT, timeout);
            header.put(SignalingValue.BODY, body);
            header.put(SignalingValue.EXTRA_DATA, extraData);
            sendSignalMessage(header);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void sendAnswerSdp(String answer, DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId) {
        try {
            JSONObject header = new JSONObject();
            header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
            header.put(SignalingValue.MSG_ID, SignalingType.callResponse);
            header.put(SignalingValue.ERR_CODE, SignalingValue.CALL_ACCEPTED);
            header.put(SignalingValue.ERR_MSG, "");
            JSONObject body = new JSONObject();
            body.put(SignalingValue.SESSION_ID, sessionId);
            body.put(SignalingValue.CALLER_ID, callerInfo.getOnlineId());
            body.put(SignalingValue.CALLEE_IDS, new JSONArray(new Object[]{ calleeInfo.getOnlineId() }));
            body.put(SignalingValue.SDP, answer);
            JSONObject extraData = new JSONObject();
            extraData.put(SignalingValue.CALLEE_CALL_EXTRA_DATA, deviceInfo.getCallExtraData());
            header.put(SignalingValue.BODY, body);
            header.put(SignalingValue.EXTRA_DATA, extraData);
            sendSignalMessage(header);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * @description: 
     * @param {DeviceInfo} callerInfo
     * @param {DeviceInfo} calleeInfo
     * @param {String} sessionId
     * @return {*}
     */
    public void sendRejectMsg(DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId) {
        try {
            JSONObject header = new JSONObject();
            header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
            header.put(SignalingValue.MSG_ID, SignalingType.callResponse);
            header.put(SignalingValue.ERR_CODE, SignalingValue.CALL_REJECTED);
            header.put(SignalingValue.ERR_MSG, "");
            JSONObject body = new JSONObject();
            body.put(SignalingValue.SESSION_ID, sessionId);
            body.put(SignalingValue.CALLER_ID, callerInfo.getOnlineId());
            body.put(SignalingValue.CALLEE_IDS, new JSONArray(new Object[]{ calleeInfo.getOnlineId() }));
            header.put(SignalingValue.BODY, body);
            sendSignalMessage(header);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } 
    }

    @Override
    /**
     * @description: 
     * @param {IceCandidate} iceCandidate
     * @param {DeviceInfo} callerInfo
     * @param {DeviceInfo} calleeInfo
     * @param {String} sessionId
     * @return {*}
     */
    public void sendIceCandidate(IceCandidate iceCandidate, DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId) {
        try {
            if (null != iceCandidate) {
                sendIceDelayQueue.offer(iceCandidate); 
            }

            if (isIceChecking) {
                while (!sendIceDelayQueue.isEmpty()) {
                    IceCandidate candidate = sendIceDelayQueue.poll();
                    Log.i(TAG, "send candidate: " + candidate);
                    JSONObject header = new JSONObject();
                    header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
                    header.put(SignalingValue.MSG_ID, SignalingType.notifyIceCandidate);
                    JSONObject body = new JSONObject();
                    body.put(SignalingValue.SESSION_ID, sessionId);
                    body.put(SignalingValue.CALLER_ID, callerInfo.getOnlineId());
                    body.put(SignalingValue.CALLEE_ID, calleeInfo.getOnlineId());
                    body.put(SignalingValue.CANDIDATE, candidate.sdp);
                    body.put(SignalingValue.SDP_MID, candidate.sdpMid);
                    body.put(SignalingValue.SDP_MLINE_INDEX, candidate.sdpMLineIndex);
                    header.put(SignalingValue.BODY, body);
                    sendSignalMessage(header);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } 
    }

    /**
     * @description: 
     * @param {DeviceInfo} callerInfo
     * @param {DeviceInfo} calleeInfo
     * @param {String} sessionId
     * @return {*}
     */
    public void sendBye(DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId) {
        try {
            JSONObject header = new JSONObject();
            header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
            header.put(SignalingValue.MSG_ID, SignalingType.bye);
            JSONObject body = new JSONObject();
            body.put(SignalingValue.SESSION_ID, sessionId);
            body.put(SignalingValue.CALLER_ID, callerInfo.getOnlineId());
            body.put(SignalingValue.CALLEE_ID, calleeInfo.getOnlineId());
            header.put(SignalingValue.BODY, body);
            sendSignalMessage(header);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } 
    }

    /**
     * @description: 
     * @param {DeviceInfo} callerInfo
     * @param {DeviceInfo} calleeInfo
     * @param {String} sessionId
     * @return {*}
     */
    public void sendCancel(DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId) {
        try {
            JSONObject header = new JSONObject();
            header.put(SignalingValue.VERSION, SignalingValue.VERSION_TAG);
            header.put(SignalingValue.MSG_ID, SignalingType.cancelCall);
            JSONObject body = new JSONObject();
            body.put(SignalingValue.SESSION_ID, sessionId);
            body.put(SignalingValue.CALLER_ID, callerInfo.getOnlineId());
            body.put(SignalingValue.CALLEE_ID, "");
            header.put(SignalingValue.BODY, body);
            sendSignalMessage(header);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } 
    }

    /**
     * @description: 
     * @param {JSONObject} msg
     * @return {*}
     */
    private void processCallStatus(JSONObject msg) {
        try {
            JSONObject body = msg.getJSONObject(SignalingValue.BODY);
            String callStatus = body.getString(SignalingValue.STATUS);
            String sessionId =  body.getString(SignalingValue.SESSION_ID);
            switch (callStatus) {
                case SignalingValue.STATUS_RINGING:
                    CallImpl call = (CallImpl) CallManager.getCall(sessionId);
                    call.getCallEventExecutor().onCalleeRinging();
                    JSONArray arr = body.getJSONArray(SignalingValue.CALLEE_IDS);
                    if (arr != null) {
                        Log.i(TAG, "the size of callee client is " + String.valueOf(arr.length()));
                    }
                    break;
                case SignalingValue.PEER_OFFLINE:
                    break;
                case SignalingValue.PEER_ONLINE:
                    break;
                case SignalingValue.STATUS_TRING:
                    Log.d(TAG, "CALL TRYING");
                    break;
                default:
                    Log.d(TAG, "unrecognized call status");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void grabIceCandidate(String sessionId, IceCandidate iceCandidate) {
        if (null != iceCandidate) {
           recIceDelayQueue.offer(iceCandidate); 
        }
        CallImpl call = (CallImpl) CallManager.getCall(sessionId);
        if (call == null) {
            while (!recIceDelayQueue.isEmpty()) {
                Log.i(TAG, "candidate size: " + recIceDelayQueue.size());
                IceCandidate candidate = recIceDelayQueue.poll();
            }
            return;
        }
        PeerConnectionManager peerConnectionManager = call.getPeerConnectionManager();
        if (peerConnectionManager == null) {
            Log.d(TAG, "peer connection manager not init");
            return;
        }
        PeerConnection peerConnection = peerConnectionManager.getPeerConnection();
        if (peerConnection == null) {
            Log.d(TAG, "peer connection not init");
           return; 
        }
        if (isIceChecking) {
            while (!recIceDelayQueue.isEmpty()) {
                IceCandidate candidate = recIceDelayQueue.poll();
                Log.i(TAG, "add candidate: " + candidate);
                peerConnection.addIceCandidate(candidate);
            }
        } 
    }

    /**
     * @description: 
     * @param {JSONObject} msg
     * @return {*}
     */
    private void processCallIn(JSONObject msg) {
        try {
            JSONObject body = msg.getJSONObject(SignalingValue.BODY);
            String sessionId = body.getString(SignalingValue.SESSION_ID);

            CallImpl call = new CallImpl(sessionId);
            call.setCalleeInfo(this.deviceInfo);
            call.setCallee();
            
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.setOnlineId(body.getString(SignalingValue.CALLER_ID));
            call.setCallerInfo(deviceInfo);

            SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, body.getString(SignalingValue.SDP));
            call.setOfferSdp(sdp);

            call.setSignalingManager(this);
            this.onLineEventExecutor.onCallIn(call);

            CallManager.putCall(sessionId, call);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * @description:i
     * @param {JSONObject} msg
     * @return {*}
     */
    private void processCallResp(JSONObject msg) {
        try {
            int code = msg.getInt(SignalingValue.ERR_CODE);
            JSONObject body = msg.getJSONObject(SignalingValue.BODY);
            String sessionId = body.getString(SignalingValue.SESSION_ID);
            CallImpl call = (CallImpl) CallManager.getCall(sessionId);
            DeviceInfo calleeInfo;
            String errMsg;
            Log.d(TAG, "call response is: " + String.valueOf(code));
            switch (code) {
                case SignalingValue.CALL_ACCEPTED: 
                    calleeInfo =  new DeviceInfo();
                    JSONArray arr = body.getJSONArray(SignalingValue.CALLEE_IDS);
                    calleeInfo.setOnlineId((String)arr.get(0));
                    call.setCalleeInfo(calleeInfo);
                    SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER, body.getString(SignalingValue.SDP));
                    call.acceptAnswerSdp(sdp);
                    call.setIsCalling(true);
                    call.getCallEventExecutor().onCalleeAccept(calleeInfo);
                    break;
                case SignalingValue.CALL_REJECTED:
                    call.getCallEventExecutor().onCalleeReject();
                    errMsg = msg.getString(SignalingValue.ERR_MSG);
                    // call.getCallEventExecutor().onException(ErrCode.CALLEE_REJECT.getValue(), errMsg);
                    call.release();
                    CallManager.removeCall(sessionId);
                    break;
                case SignalingValue.CALL_TIMEOUT:
                    call.getCallEventExecutor().onCallTimeout();
                    call.release();
                    CallManager.removeCall(sessionId);
                    break;
                case SignalingValue.CALL_UNAVAILABLE:
                    errMsg = msg.getString(SignalingValue.ERR_MSG);
                    call.getCallEventExecutor().onException(ErrCode.CALLEE_OFFLINE.getValue(), errMsg);
                    call.release();
                    CallManager.removeCall(sessionId);
                    break;
                case SignalingValue.CALL_BUSY:
                    errMsg = msg.getString(SignalingValue.ERR_MSG);
                    call.getCallEventExecutor().onException(ErrCode.CALLEE_BUSY.getValue(), errMsg);
                    call.release();
                    CallManager.removeCall(sessionId);
                    break;
                case SignalingValue.CALL_ERROR:
                    errMsg = msg.getString(SignalingValue.ERR_MSG);
                    Log.e(TAG, errMsg);
                    call.release();
                    CallManager.removeCall(sessionId);
                    break;
                case SignalingValue.CALL_UNAUTH:
                    errMsg = msg.getString(SignalingValue.ERR_MSG);
                    call.getCallEventExecutor().onException(ErrCode.CALLEE_UNAUTH.getValue(), errMsg);
                    call.release();
                    CallManager.removeCall(sessionId);
                    break;
                case SignalingValue.CALL_INTERNAL_ERROR:
                    errMsg = msg.getString(SignalingValue.ERR_MSG);
                    Log.e(TAG, errMsg);
                    call.release();
                    CallManager.removeCall(sessionId);
                    break;
                default:
                    Log.d(TAG, "call response is: " + String.valueOf(code));
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * @description: 
     * @param {JSONObject} msg
     * @return {*}
     */
    private void processIceCandidate(JSONObject msg) {
        try {
            JSONObject body = msg.getJSONObject(SignalingValue.BODY);
            String sdpMid = body.getString(SignalingValue.SDP_MID);
            int sdpMLineIndex = body.getInt(SignalingValue.SDP_MLINE_INDEX);
            String sdp =  body.getString(SignalingValue.CANDIDATE);
            String sessionId = body.getString(SignalingValue.SESSION_ID);
            IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
            grabIceCandidate(sessionId, iceCandidate);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void processByeResp(JSONObject msg) {
        try {
            JSONObject body = msg.getJSONObject(SignalingValue.BODY); 
            String sessionId = body.getString(SignalingValue.SESSION_ID);
            CallImpl call = (CallImpl) CallManager.getCall(sessionId); 
            if (call.isCaller()) {
                call.getCallEventExecutor().onRemoteBye(ByeReason.INITIATIVE);
            } else {
                onLineEventExecutor.onRemoteBye(call, ByeReason.INITIATIVE);

            }
            call.setIsCalling(false);
            call.release();
            CallManager.removeCall(sessionId);
            
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void processCancelCall(JSONObject msg) {
        try {
            JSONObject body = msg.getJSONObject(SignalingValue.BODY);

            String sessionId = body.getString(SignalingValue.SESSION_ID);
            CallImpl call = (CallImpl) CallManager.getCall(sessionId);
            
            call.setCalleeInfo(this.deviceInfo);
            call.setCallee();
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.setOnlineId(body.getString(SignalingValue.CALLER_ID));
            call.setCallerInfo(deviceInfo);

            call.setSignalingManager(this);
            
            this.onLineEventExecutor.onCancelCall(call);
            call.release();
            CallManager.removeCall(sessionId);
            clearIceBuffer();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } 
    }

    @Override
    public void clearIceBuffer() {
        Log.d(TAG, "clear ice buffer");
        isIceChecking = false;

        while (!recIceDelayQueue.isEmpty()) {
            Log.i(TAG, "rec candidate size: " + recIceDelayQueue.size());
            recIceDelayQueue.poll();
        }

        while (!sendIceDelayQueue.isEmpty()) {
            Log.i(TAG, "send candidate size: " + sendIceDelayQueue.size());
            sendIceDelayQueue.poll();
        }
    }
    
}
