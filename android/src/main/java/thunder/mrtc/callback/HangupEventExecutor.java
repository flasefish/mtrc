/*
 * @Author: your name
 * @Date: 2021-06-16 11:07:21
 * @LastEditTime: 2021-06-29 20:20:35
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/callback/HanpupEventExecutor.java
 */

package thunder.mrtc.callback;

public interface HangupEventExecutor {
 
    /**
     * 挂断成功
     */
    void onSuccess();
 
    /**
     * 挂断失败
     */
    void onFail(int code, String message);
}