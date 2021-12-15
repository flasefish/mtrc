/*
 * @Author: your name
 * @Date: 2021-06-07 17:10:41
 * @LastEditTime: 2021-07-07 11:08:21
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/MrtcOperator.java
 */
package thunder.mrtc;

import android.content.Context;

import thunder.mrtc.callback.OnLineEventExecutor;
import thunder.mrtc.callback.CallEventExecutor;
import thunder.mrtc.model.MediaAttributes;
import thunder.mrtc.model.MrtcException;
import thunder.mrtc.model.MrtcSetupParam;
import thunder.mrtc.impl.MrtcOperatorImpl;
import thunder.mrtc.MrtcRender;


public interface MrtcOperator {

    static MrtcOperator impl = new MrtcOperatorImpl();

    static MrtcOperator getInstance() {
        return impl;
    }

    /**
     * 初始化阶段执行一次的全局初始化方法
     * @param mrtcSetupParam
     */
    void setup(MrtcSetupParam mrtcSetupParam);
 
    /**
     * 设备主动建立和信令服务器的websocket连接
     * @param token 设备token，信令服务器会用此字段去link验证设备身份
     * @param onlineExtraData 设备绑定的额外信息，信令服务器只负责透传，该字段的生命周期是本次websocket连接
     * @param heartBeatInterval 保活心跳间隔
     * @param retryTimes websocket连接出现异常时，SDK主动重试的次数，达到该次数如果还连不上，就放弃重试并执行异常的callback
     * @param retryInterval websocket连接出现异常时，SDK主动重试前的等待时间
     * @param onLineEventExecutor online阶段异步事件处理类的实现
     * @throws MrtcException
     */
    void online(String onlineId, String token, String onlineExtraData, int heartBeatInterval, int retryTimes, int retryInterval, OnLineEventExecutor onLineEventExecutor) throws MrtcException;
 
    /**
     * 呼叫
     * @param calleeNumber 被叫号码，信令服务器用此号码去link换onlineId，可能换到一个或者多个onlineId
     * @param callExtraData 设备绑定的额外信息，信令服务器只负责透传，该字段的生命周期是本次呼叫期间
     * @param mediaAttributes 音频和视频流的方向
     * @param timeout 振铃时间 (根据信令配置时间配置，暂无用到)
     * @param callEventExecutor 异步事件处理类的实现，call阶段支持以下消息类型
     * @throws MrtcException
     */
    Call call(String calleeNumber, String callExtraData, MediaAttributes mediaAttributes, long timeout, CallEventExecutor callEventExecutor) throws MrtcException;

    /**
     * 离线服务
     */
    void offline();

    /**
     * @description: 主叫或者被叫在各自合适的时候设置远端视频的view
     * @param {MrtcRender} remoteRender
     * @return {*}
     */
    void setRemoteRender(MrtcRender remoteRender);

}