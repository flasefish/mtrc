/*
 * @Author: lijianhong
 * @Date: 2021-06-04 14:37:48
 * @LastEditTime: 2021-07-12 16:57:11
 * @LastEditors: Please set LastEditors
 * @Description: Mrt Opertaror
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/impl/MrtcOperatorimpl.java
 */
package thunder.mrtc.impl;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceView;

import thunder.mrtc.callback.OnLineEventExecutor;
import thunder.mrtc.callback.CallEventExecutor;
import thunder.mrtc.model.DeviceInfo;
import thunder.mrtc.model.MediaAttributes;
import thunder.mrtc.model.MrtcException;
import thunder.mrtc.model.MrtcSetupParam;
import thunder.mrtc.model.MediaParam;
import thunder.mrtc.MrtcOperator;
import thunder.mrtc.Call;
import thunder.mrtc.MrtcRender;

import org.webrtc.SurfaceViewRenderer;

import java.util.UUID;


public class MrtcOperatorImpl implements MrtcOperator {

    private final static String TAG = "MRTC_OPERATOR";

    private MrtcSetupParam mrtcSetupParam;

    private Call call;

    private SignalingManager signaling;

    private MrtcRender remoteMrtcRender;

    private SurfaceViewRenderer remoteRender;
    
    /**
     * 初始化阶段执行一次的全局初始化方法
     * @param mrtcSetupParam
     */
    @Override
    public void setup(MrtcSetupParam mrtcSetupParam) {
        this.mrtcSetupParam = mrtcSetupParam;
    }
 
    /**
     * 设备主动建立和信令服务器的websocket连接
     * @param onlineId
     * @param token 设备token，信令服务器会用此字段去link验证设备身份，测试过程
     * @param onlineExtraData 设备绑定的额外信息，信令服务器只负责透传，该字段的生命周期是本次websocket连接
     * @param heartBeatInterval 保活心跳间隔
     * @param retryTimes websocket连接出现异常时，SDK主动重试的次数，达到该次数如果还连不上，就放弃重试并执行异常的callback
     * @param retryInterval websocket连接出现异常时，SDK主动重试前的等待时间
     * @param onLineEventExecutor online阶段异步事件处理类的实现
     * @throws MrtcException
     */
    @Override
    public void online(String onlineId, String token, String onlineExtraData, int heartBeatInterval, int retryTimes, int retryInterval, OnLineEventExecutor onLineEventExecutor) throws MrtcException {
        if ( mrtcSetupParam.getSignalServerAddress() == null || token == null ) {
            throw new MrtcException("address or token is empty");
        }
        this.signaling = new SignalingManagerImpl();
        this.signaling.setServerAddress(mrtcSetupParam.getSignalServerAddress());
        this.signaling.setOnLineEventExecutor(onLineEventExecutor);
        DeviceInfo deviceInfo = new DeviceInfo(onlineId);
        deviceInfo.setOnlineExtraData(onlineExtraData);
        this.signaling.setDeviceInfo(deviceInfo);
        this.signaling.setToken(token);

        this.signaling.setHeartBeatInterval(heartBeatInterval);
        this.signaling.setRetryTimes(retryTimes);
        this.signaling.setRetryInterval(retryInterval);

        this.signaling.connect();
    }
 
    /**
     * 呼叫
     * @param calleeNumber 被叫号码，信令服务器用此号码去link换onlineId，可能换到一个或者多个onlineId
     * @param callExtraData 设备绑定的额外信息，信令服务器只负责透传，该字段的生命周期是本次呼叫期间
     * @param mediaAttributes 音频和视频流的方向
     * @param callEventExecutor 异步事件处理类的实现，call阶段支持以下消息类型
     * @throws MrtcException
     */
    @Override
    public Call call(String calleeNumber, String callExtraData, MediaAttributes mediaAttributes, long timeout, CallEventExecutor callEventExecutor) throws MrtcException {  
        if (!this.signaling.getRegState()) {
            throw new MrtcException("signaling not connected");
        }
        
        String sessionId = UUID.randomUUID().toString();

        CallImpl callImpl = new CallImpl(sessionId);
        
        callImpl.initPeerConnectionManager();
        callImpl.setCaller();
        callImpl.setCallerInfo(this.signaling.getDeviceInfo());
        this.signaling.setIceChecking(false);
        callImpl.setSignalingManager(this.signaling);
        callImpl.setCallExtraData(callExtraData);
        callImpl.setMediaAttributes(mediaAttributes);
        callImpl.setCallEventExecutor(callEventExecutor);
        callImpl.setTimeout(timeout);
        CallManager.putCall(sessionId, callImpl);

        callImpl.startCall(calleeNumber);
        this.call = callImpl; 
        return this.call;
    }

    @Override
    public void offline() {
        if (this.signaling != null) {
            this.signaling.sendOfflineReq();
            this.signaling.close();
        }
    }

    /**
     * @description: 
     * @param {MrtcRender} remoteRender
     * @return {*}
     */
    @Override
    public void setRemoteRender(MrtcRender mrtcRender) {
        this.remoteMrtcRender = mrtcRender;
        this.remoteRender =  mrtcRender.getRemoteRender();
    }
    
    public MrtcSetupParam getMrtcSetupParam() {
        return this.mrtcSetupParam;
    }

    public SurfaceViewRenderer getRemoteRender() {
        return this.remoteRender;
    }

    public MrtcRender getRemoteMrtcRender() {
        return this.remoteMrtcRender;
    }
}