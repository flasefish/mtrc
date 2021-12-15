/*
 * @Author: device Info
 * @Date: 2021-06-07 16:42:14
 * @LastEditTime: 2021-07-08 17:22:30
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/model/DeviceInfo.java
 */
package thunder.mrtc.model;

public class DeviceInfo {
    /**
     * 设备ID
     */
    private String onlineId;

    /**
     * online extra data
     */
    private String onlineExtraData;

    /**
     * 设备绑定的额外信息，信令服务器只负责透传，该字段的生命周期是本次呼叫期间
     */
    private String callExtraData;

    public DeviceInfo() {
    }

    public DeviceInfo(String onlineId) {
        this.onlineId = onlineId;
    }

    public String getOnlineId() {
        return this.onlineId;
    }

    public void setOnlineId(String onlineId) {
        this.onlineId = onlineId;
    }

    public String getCallExtraData() {
        return this.callExtraData;
    }

    public void setCallExtraData(String callExtraData) {
        this.callExtraData = callExtraData;
    }

    public void setOnlineExtraData(String onlineExtraData) {
        this.onlineExtraData = onlineExtraData;
    }

    public String getOnlineExtraData() {
        return this.onlineExtraData;
    }
}
