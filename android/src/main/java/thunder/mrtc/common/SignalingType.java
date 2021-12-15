/*
 * @Author: lijianhong
 * @Date: 2021-06-06 08:39:59
 * @LastEditTime: 2021-06-07 19:08:45
 * @LastEditors: Please set LastEditors
 * @Description: Siga
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/common/SignalMessageType.java
 */
package thunder.mrtc.common;

public enum SignalingType {

    // 上线
    online,
    // 离线
    offline,
    // 上线应答
    onlineResponse,
    // 通话呼叫
    call,
    // 呼叫状态通知
    notifyCallStatus,
    // 呼叫应答
    callResponse,
    // 下发Ice Candidate
    notifyIceCandidate,
    // 心跳ping
    ping,
    // 心跳pong
    pong,
    // 取消呼叫
    cancelCall,
    // 取消呼叫应答
    cancelCallResponse,
    // 挂断通话
    bye
}