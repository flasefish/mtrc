/*
 * @Author: lijianhong
 * @Date: 2021-06-16 09:48:05
 * @LastEditTime: 2021-06-16 11:01:31
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/model/MrtcSetupParam.java
 */
package thunder.mrtc.model;


import android.content.Context;

// Mrtc初始化类
public class MrtcSetupParam {

    /*
     * 信令服务器地址，如"192.168.0.11:8080"
     */
    private String signalServerAddress;
 
    /*
     * stun服务器地址，如"192.168.0.11:8080"
     */
    private String stunServerAddress;

    /*
     * 是否分流，如果分流了，需要业务侧调用Call.onExternalFrame方法向SDK传入音视频数据
     */
    private boolean rawStream;

    // Application的Context, PeerConnection会用到
    private Context context;


    public MrtcSetupParam(String signalServerAddress, String stunServerAddress, boolean rawStream, Context context) {
        this.signalServerAddress = signalServerAddress;
        this.stunServerAddress = stunServerAddress;
        this.rawStream = rawStream;
        this.context = context;
    }

    public MrtcSetupParam(MrtcSetupParam mrtcSetupParam) {
        this.signalServerAddress = mrtcSetupParam.getSignalServerAddress();
        this.stunServerAddress = mrtcSetupParam.getStunServerAddress();
        this.rawStream = mrtcSetupParam.getRawStream();
        this.context = mrtcSetupParam.getContext();
    }


    public void setSignalServerAddress(String signalServerAddress) {
        this.signalServerAddress = signalServerAddress;
    }

    public String getSignalServerAddress() {
        return this.signalServerAddress;
    }

    public void setStunServerAddress(String stunServerAddress) {
        this.stunServerAddress = stunServerAddress;
    }

    public String getStunServerAddress() {
        return this.stunServerAddress;
    }

    public void setRawStream(boolean rawStream) {
        this.rawStream = rawStream;
    }

    public boolean getRawStream() {
        return this.rawStream;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return this.context;
    }

};
