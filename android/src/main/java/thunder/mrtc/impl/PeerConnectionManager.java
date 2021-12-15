/*
 * @Author: lijianhong
 * @Date: 2021-06-04 11:19:16
 * @LastEditTime: 2021-06-17 19:40:49
 * @LastEditors: Please set LastEditors
 * @Description: PeerConnection service interface
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/service/PeerConnectionManager.java
 */
package thunder.mrtc.impl; 

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;

import thunder.mrtc.model.MediaAttributes;
import org.webrtc.PeerConnection;

import java.nio.ByteBuffer;

public interface PeerConnectionManager {

    /**
     * @description: 
     * @param {String} addr
     * @return {*}
     */
    public void setStunAddr(String addr);

    /**
     * @description: 
     * @param {MediaAttributes} mediaAttributes
     * @return {*}
     */
    public void setMediaAttributes(MediaAttributes mediaAttributes);


    /**
     * @description: release resource
     * @param {*}
     * @return {*}
     */
    void release();


    /**
     * @description: 
     * @param {*}
     * @return {*}
     */
    PeerConnection getPeerConnection();
    
    /**
     * @description: 
     * @param {[]byte} data
     * @return {*}
     */
    void onExternalFrame(byte[] data);

    /**
     * @description: 
     * @param {[]byte} data
     * @return {*}
     */
    void sendMsg(ByteBuffer data);

    /**
     * @description: 
     * @param {boolean} sw
     * @return {*}
     */
    void setLocalAudioTrack(boolean sw);

    /**
     * @description: 
     * @param {boolean} sw
     * @return {*}
     */
    void setRemoteAudioTrack(boolean sw);
}