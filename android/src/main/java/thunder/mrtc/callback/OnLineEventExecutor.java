/*
 * @Author: your name
 * @Date: 2021-07-05 21:43:15
 * @LastEditTime: 2021-07-05 21:43:16
 * @LastEditors: your name
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/thunder/mrtc/callback/OnLineEventExecutor.java
 */
package thunder.mrtc.callback;

import thunder.mrtc.Call;
import thunder.mrtc.common.ByeReason;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface OnLineEventExecutor {
    /**
     * 注册到信令服务器成功
     */
    void onRegSuccess();
 
    /**
     * 信令服务器主动关闭websocket连接
     */
    void onCloseWebsocket();
 
    /**
     * 作为被叫，有远程呼叫到达
     */
    void onCallIn(Call mrtcCall);
 
    /**
     * 作为被叫，远程呼叫在振铃期间取消呼叫
     */
    void onCancelCall(Call mrtcCall);
 
    /**
     * 作为被叫，在通话中被挂断(call入参)
     */
    void onRemoteBye(Call mrtcCall, ByeReason byeReason);
 
    /**
     * 重试时的回调
     */
    void onReconnect();
 
    /**
     * 遇到异常后的回调，例如网络断开
     * @param code
     * @param message
     */
    void onException(int code, String message);

    /**
     * datachannel 打开事件
     */
    void onChannelOpen();

    /**
     * datachannel 关闭事件
     */
    void onChannelClose();

    /**
     * 收到datachannel消息
     */
    void onChannelMessage(ByteBuffer msg);
}