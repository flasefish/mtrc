/*
 * @Author: your name
 * @Date: 2021-06-07 22:13:00
 * @LastEditTime: 2021-06-07 22:13:01
 * @LastEditors: your name
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/model/MediaAttributes.java
 */
package thunder.mrtc.model;


import thunder.mrtc.common.AttributeValue;

public class MediaAttributes {
    /**
     * 视频方向
     */
    private AttributeValue videoAttributeValue;
 
    /**
     * 音频方向
     */
    private AttributeValue audioAttributeValue;


    public AttributeValue getVideoAttributeValue() {
        return this.videoAttributeValue;
    }

    public void setVideoAttributeValue(AttributeValue videoAttributeValue) {
        this.videoAttributeValue = videoAttributeValue;
    }

    public AttributeValue getAudioAttributeValue() {
        return this.audioAttributeValue;
    }

    public void setAudioAttributeValue(AttributeValue audioAttributeValue) {
        this.audioAttributeValue = audioAttributeValue;
    }

}