/*
 * @Author: your name
 * @Date: 2021-06-22 14:37:16
 * @LastEditTime: 2021-07-07 11:17:17
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/callback/CallEventExecutor.java
 */
package thunder.mrtc.callback;

import thunder.mrtc.common.ByeReason;
import thunder.mrtc.model.DeviceInfo;

public interface CallEventExecutor {
    /**
     * 被叫接听(一呼多需求)
     * @param callee 被叫信息
     */
    void onCalleeAccept(DeviceInfo callee);

    /**
     * 被叫拒绝，在一呼多场景，当所有被叫都拒绝后，信令服务器给主叫发送一次拒绝命令，SDK回调此方法
     */
    void onCalleeReject();

    /**
     * 被叫振铃
     */
    void onCalleeRinging();

    /**
     * 呼叫超时
     */
    void onCallTimeout();

    /**
     * 作为主叫，在通话中被挂断(呼叫时长需求增加ByeReason入参)
     */
    void onRemoteBye(ByeReason byeReason);

    /**
     * 遇到异常后的回调，例如被叫通信异常
     * @param code
     * @param message
     */
    void onException(int code, String message);
}