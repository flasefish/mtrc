/*
 * @Author: your name
 * @Date: 2021-07-07 16:20:16
 * @LastEditTime: 2021-07-07 16:43:45
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/thunder/mrtc/Call.java
 */
package thunder.mrtc;

import java.util.List;

import thunder.mrtc.model.MediaAttributes;
import thunder.mrtc.model.MrtcException;
import thunder.mrtc.model.MrtcSetupParam;
import thunder.mrtc.model.DeviceInfo;
import thunder.mrtc.model.FrameColorSpaceType;
import thunder.mrtc.common.ByeReason;
import thunder.mrtc.model.MediaParam;
import thunder.mrtc.callback.AcceptEventExecutor;
import thunder.mrtc.callback.HangupEventExecutor;

import java.nio.ByteBuffer;

public interface Call {

    /**
     * 主叫身份的数据结构(一对多需求)
     */
    DeviceInfo getCaller();

    /**
     * 被叫身份的数据结构
     */
    DeviceInfo getCallee();

    /**
     * 自己是否是主叫
     */
    boolean isCaller();

    /**
     * 呼叫开始时间
     */
    long getMediaStartTime();


    /**
     * 主动挂断(呼叫时长需求增加ByeReason入参)
     * @throws MrtcException
     */
    void hangup(HangupEventExecutor hangupEventExecutor, ByeReason byeReason) throws MrtcException;

    /**
     * 通话过程中，随时可以更新媒体流方向以控制音视频是否传输
     * @param direction
     */
    void updateMediaAttributes(MediaAttributes direction);

    /**
     * 设备侧媒体流设置，如果MrtcSetupParam.isRawStream等于true，则必须调用此方法
     * @param width
     * @param height
     * @param framePerSecond
     */
    void onExternalSourceInit(int width, int height, int framePerSecond);

    /**
     * 设备侧外部视频帧数据输入接口
     * @param array
     * @param frameColorSpaceType
     */
    void onExternalFrame(byte[] array, FrameColorSpaceType frameColorSpaceType);

    /**
     * 呼叫前的全局媒体参数设置
     * @param mediaParam
     */
    void setupMedia(MediaParam mediaParam);

    /**
     * 呼叫过程中，设置麦克风开关
     */
    void setMicSwitch(boolean sw);

    /**
     * 呼叫过程中，获取麦克风开关
     */
    void getMicSwitch();

    /**
     * 呼叫过程中，设置声音开关
     */
    void setAudioSwitch(boolean sw);

    /**
     * 呼叫过程中，获取声音开关
     */
    void getAudioSwitch();

    /**
     * 作为被叫的时候，接受远端呼叫后，业务层需要调用此方法
     * @param mediaAttributes 音频和视频流的方向
     * @param acceptEventExecutor 接受的异步回调
     * @param callExtraData 被叫设备绑定的额外信息，信令服务器只负责透传，该字段的生命周期是本次呼叫期间
     */
    void accept(MediaAttributes mediaAttributes, AcceptEventExecutor acceptEventExecutor, String callExtraData);

    /**
     * 收到发送DataChannel消息
     */
    void sendDataChannelMsg(ByteBuffer msg);

}