/*
 * @Author: lijianhong
 * @Date: 2021-06-04 10:15:28
 * @LastEditTime: 2021-07-08 18:33:36
 * @LastEditors: Please set LastEditors
 * @Description: signaling manager interface
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/impl/SignalingManager.java
 */
package thunder.mrtc.impl; 

import android.content.Context;

import thunder.mrtc.callback.OnLineEventExecutor;
import thunder.mrtc.model.DeviceInfo;


import org.webrtc.IceCandidate;
import org.webrtc.SurfaceViewRenderer;

import org.json.JSONObject;


public interface SignalingManager {

    /**
     * @description: set server address 
     * @param {String} addr
     * @return {*}
     */
    void setServerAddress(String addr);

    
    /**
     * @description: 
     * @param {boolean} isChecking
     * @return {*}
     */
    void setIceChecking(boolean isChecking);

    OnLineEventExecutor getOnLineEventExecutor();

    /**
     * @description: set online event callback
     * @param {OnLineEventExecutor} onLineEventExecutor
     * @return {*}
     */
    void setOnLineEventExecutor(OnLineEventExecutor onLineEventExecutor);

    /**
     * @description: 
     * @param {DeviceInfo} deviceInfo
     * @return {*}
     */
    public void setDeviceInfo(DeviceInfo deviceInfo);


    /**
     * @description: 
     * @param {*}
     * @return {*}
     */
    public DeviceInfo getDeviceInfo();


    public boolean getRegState();

    /**
     * @description: 
     * @param {int} heartBeatInterval
     * @return {*}
     */
    public void setHeartBeatInterval(int heartBeatInterval);

    /**
     * @description: 
     * @param {int} retryInterval
     * @return {*}
     */
    public void setRetryInterval(int retryInterval);

    /**
     * @description: 
     * @param {int} retryTimes
     * @return {*}
     */
    public void setRetryTimes(int retryTimes);

    
    /**
     * @description: 
     * @param {String} token
     * @return {*}
     */
    public void setToken(String token);

    /**
     * @description: connect websocket with signal server
     * @param {*}
     * @return {*}
     */
    boolean connect();

    /**
     * @description: 
     * @param {*}
     * @return {*}
     */
    boolean close();

    /**
     * @description: send online message 
     * @param {*}
     * @return {*}
     */
    void sendOnlineReq();

    
    /**
     * @description: process online message  
     * @param {String} msg
     * @return {*}
     */
    void processOnlineResp(JSONObject msg);

    /**
    * @description: send offline message 
    * @param {String} offlineReq
    * @return {*}
    */
    void sendOfflineReq();

    /**
     * @description: send ping
     * @param {*}
     * @return {*}
     */
    void sendPing();

    /**
     * @description: process pong
     * @param {JSONObject} pongMessage
     * @return {*}
     */
    void processPong(JSONObject pongMessage);

    /**
     * @description: call sdp candidate message sender
     * @param {JSONObject} bizMessage
     * @return {*}
     */
    void sendSignalMessage(JSONObject bizMessage);
    
    /**
     * @description: 
     * @param {String} offer
     * @param {String} calleeNum
     * @return {*}
     */
    void sendOfferSdp(String offer, String calleeNumber, long timeout, String sessionId);


    /**
     * @description: 
     * @param {String} answer
     * @param {DeviceInfo} callerInfo
     * @param {Device} calleeInfo
     * @param {String} sessionId
     * @return {*}
     */
    void sendAnswerSdp(String answer, DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId);

    /**
     * @description: 
     * @param {IceCandidate} ice
     * @return {*}
     */
    void sendIceCandidate(IceCandidate iceCandidate, DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId);

    /**
     * @description: 
     * @param {IceCandidate} iceCandidate
     * @return {*}
     */
    void grabIceCandidate(String sessionId, IceCandidate iceCandidate);


    /**
        * @description: 
        * @param {DeviceInfo} callerInfo
        * @param {DeviceInfo} calleeInfo
        * @param {String} sessionId
        * @return {*}
        */
    void sendBye(DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId);
    
    /**
     * @description: 
     * @param {DeviceInfo} callerInfo
     * @param {DeviceInfo} calleeInfo
     * @param {String} sessionId
     * @return {*}
     */
    void sendRejectMsg(DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId);


    /**
     * @description: 
     * @param {DeviceInfo} callerInfo
     * @param {DeviceInfo} calleeInfo
     * @param {String} sessionId
     * @return {*}
     */
    void sendCancel(DeviceInfo callerInfo, DeviceInfo calleeInfo, String sessionId);

    void clearIceBuffer();
}
