/*
 * @Author: your name
 * @Date: 2021-06-08 11:10:25
 * @LastEditTime: 2021-06-08 19:51:54
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/listener/SdpObserver.java
 */
package thunder.mrtc.listener;

import android.util.Log;


import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * sdp create 与offer 回调warp
 */
public class MrtcSdpObserver implements SdpObserver {
    private final static String TAG = "MRTC_SDP_OBSERVER";

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.i(TAG, "SdpObserver: onCreateSuccess !");
    }

    @Override
    public void onSetSuccess() {
        Log.i(TAG, "SdpObserver: onSetSuccess");
    }

    @Override
    public void onCreateFailure(String msg) {
        Log.e(TAG, "SdpObserver onCreateFailure: " + msg);
    }

    @Override
    public void onSetFailure(String msg) {
        Log.e(TAG, "SdpObserver onSetFailure: " + msg);
    }
}
