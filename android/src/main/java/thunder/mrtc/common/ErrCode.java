/*
 * @Author: lijianhong
 * @Date: 2021-06-05 11:12:35
 * @LastEditTime: 2021-06-05 11:28:35
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/common/ErrCode.java
 */
package thunder.mrtc.common;

public enum ErrCode {

    // 被叫不在线
    CALLEE_OFFLINE(1001),
    // 被叫占线
    CALLEE_BUSY(1002),
    // 被叫拒接
    // CALLEE_REJECT(1003),
    // 设备离线
    DEVICE_OFFLINE(1004),
    // 被叫未认证
    CALLEE_UNAUTH(1005),
    // 上线请求错误
    ONLINE_ERROR(2001),
    // 上线未认证
    ONLINE_UNAUTH(2002),
    // 网络异常
    NETWORK_ERROR(3001),

    P2P_NETWORK_ERROR(3002);
    
    private int value;

    private ErrCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
