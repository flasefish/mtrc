/*
 * @Author: your name
 * @Date: 2021-06-04 11:28:04
 * @LastEditTime: 2021-07-07 22:10:27
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/impl/PeerConnectionManagerImpl.java
 */
package thunder.mrtc.impl; 
import org.webrtc.RTCStatsCollectorCallback;
import org.webrtc.RTCStatsReport;
import org.webrtc.SurfaceViewRenderer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.os.SystemClock;


import thunder.mrtc.MrtcRender;
import thunder.mrtc.model.MediaAttributes;
import thunder.mrtc.listener.PeerConnectionObserver;
import thunder.mrtc.MrtcOperator;
import thunder.mrtc.impl.MrtcOperatorImpl;

import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.VideoTrack;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.NV21Buffer;
import org.webrtc.VideoFrame;
import org.webrtc.RendererCommon;
import org.webrtc.RtpSender;
import org.webrtc.Logging;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeerConnectionManagerImpl implements PeerConnectionManager {

    private static final String TAG = "MRTC_PEERCONNECTION";

    private static final String VIDEO_TRACK_ID = "1";
    
    private static final String AUDIO_TRACK_ID = "2";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String stunAddr;

    private MediaAttributes mediaAttributes;

    private Context context;

    private EglBase eglBase;

    private PeerConnectionFactory peerConnectionFactory;

    private PeerConnection peerConnection;

    private AudioDeviceModule audioDeviceModule;

    private AudioTrack audioTrack;

    private VideoTrack videoTrack;

    private AudioTrack remoteAudioTrack;

    private VideoTrack remoteVideoTrack;

    private boolean enableLocalAudio;

    private boolean enableRemoteAudio;

    private boolean enableLocalVideo;

    private boolean enableRemoteVideo;

    private PeerConnectionObserver peerConnectionObserver;

    private MrtcRender remoteMrtcRender;

    private SurfaceViewRenderer remoteRender;

    private VideoSource videoSource;

    private String sessionId;

    private DataChannel dataChannel;

    private DataChannel.Observer dataChannelObserver;

    private Handler mHandler = new Handler();

    private Timer mStatsTimer = null;

    private TimerTask mStatsTimerTask = new TimerTask() {
        @Override
        public void run() {
            getStats();
        }
    };

    public PeerConnectionManagerImpl(String sessionId) {
        this.sessionId = sessionId;
        this.context = ((MrtcOperatorImpl)MrtcOperator.getInstance()).getMrtcSetupParam().getContext();
        this.stunAddr = ((MrtcOperatorImpl)MrtcOperator.getInstance()).getMrtcSetupParam().getStunServerAddress();
        initEGL();

        this.peerConnectionFactory = createPeerConnectionFactory(context);
        initVideo();
        initAudio();

        initPeerConnectionObserver();
        this.peerConnection = createPeerConnection();

        Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);

        initDataChannel();
        initRemoteRender();
        startStatsTimer();
    }

    private void initDataChannel() {
        if (this.dataChannel != null) return;
        DataChannel.Init init = new DataChannel.Init();
        this.dataChannel = peerConnection.createDataChannel("Hello Baby", init);
    }

    private void initEGL() {
        Log.i(TAG, "init EGL");
        eglBase = EglBase.create();
    }

    private void startStatsTimer() {
        stopStatsTimer();

        mStatsTimer = new Timer();
        mStatsTimer.schedule(mStatsTimerTask, 0, 5*1000);
    }

    private void stopStatsTimer() {
        if (mStatsTimer != null) {
            mStatsTimer.cancel();
            mStatsTimer = null;
        }
    }

    private void getStats() {
        if (this.peerConnection != null) {
            this.peerConnection.getStats(new RTCStatsCollectorCallback() {
                @Override
                public void onStatsDelivered(RTCStatsReport report) {
                    Log.d(TAG, "stats: " + report.toString());
                }
            });
        }
    }

    /**
     * 创建peer connection factory
     *
     * @param context
     */
    private PeerConnectionFactory createPeerConnectionFactory(Context context) {
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;
        encoderFactory = new DefaultVideoEncoderFactory(
                this.eglBase.getEglBaseContext(),
                true,
                true);
        decoderFactory = new DefaultVideoDecoderFactory(this.eglBase.getEglBaseContext());

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context.getApplicationContext())
                .setEnableInternalTracer(true)
                .createInitializationOptions());
        this.audioDeviceModule = JavaAudioDeviceModule.builder(context.getApplicationContext())
                .setSampleRate(16000)
                .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule();
        this.audioDeviceModule.setMicrophoneMute(false);
        this.audioDeviceModule.setSpeakerMute(false);
        PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder()
                .setAudioDeviceModule(this.audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory);
        builder.setOptions(null);

        //config audio manager
        AudioManager audioManager = ((AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE));
        int result = audioManager.requestAudioFocus(null,
                AudioManager.STREAM_MUSIC,  //AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "Audio focus request granted for VOICE_CALL streams");
        } else {
            Log.e(TAG, "Audio focus request failed");
        }
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        audioManager.setMicrophoneMute(false);
        return builder.createPeerConnectionFactory();
    }

    private void initVideo() {
        this.videoSource = this.peerConnectionFactory.createVideoSource(false);
        SurfaceTextureHelper textureHelper = SurfaceTextureHelper.create("CaptureThread", this.eglBase.getEglBaseContext());

        Camera2Enumerator enumerator = new Camera2Enumerator(this.context);
        String[] names = enumerator.getDeviceNames();
        if(names == null || names.length < 0) {
            Log.e(TAG, "can't get camera device");
            this.videoSource = null;
            return;
        } 
        CameraVideoCapturer capturer = enumerator.createCapturer(names[0], null);
        capturer.initialize(textureHelper, this.context, this.videoSource.getCapturerObserver());
        
        this.videoTrack = this.peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, this.videoSource);
        this.enableLocalVideo = true;
        this.videoTrack.setEnabled(this.enableLocalVideo);
    }
    
    private void initAudio()  {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("echoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("noiseSuppression", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("autoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("channelCount", "1"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("sampleRate", "16000"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("sampleSize", "16"));
        AudioSource audioSource = this.peerConnectionFactory.createAudioSource(audioConstraints);
        this.audioTrack = this.peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        this.enableLocalAudio = true;
        this.audioTrack.setEnabled(this.enableLocalAudio);
    }

    /**mStunAddr
     * create peer connection
     */
    private PeerConnection createPeerConnection() {
        Log.i(TAG, "Create PeerConnection ...");
        Log.d(TAG, "Stun server: " + this.stunAddr);
        LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();

        PeerConnection.IceServer ice_server =
                PeerConnection.IceServer.builder(this.stunAddr)
                        .createIceServer();
        
        iceServers.add(PeerConnection.IceServer.builder("turn:14.215.130.139:3478")
                       .setUsername("username1")
                       .setPassword("key1")
                       .createIceServer());

        iceServers.add(ice_server);

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.enableDtlsSrtp = true;
        // rtcConfig.enableRtpDataChannel = true;
        PeerConnection connection =
                this.peerConnectionFactory.createPeerConnection(rtcConfig,
                        this.peerConnectionObserver);
        if (connection == null) {
            Log.e(TAG, "Failed to createPeerConnection!");
            return null;
        }

        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
        connection.addTrack(this.videoTrack, mediaStreamLabels);
        connection.addTrack(this.audioTrack, mediaStreamLabels);
        return connection;
    }


    /**
     * @description: 
     * @param {*}
     * @return {*}
     */
    private void initPeerConnectionObserver() {
        this.peerConnectionObserver = new PeerConnectionObserver(this.sessionId) {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.i(TAG, "session id: " + sessionId + "onIceCandidate: " + iceCandidate);
                CallImpl call = (CallImpl) CallManager.getCall(sessionId);
                SignalingManager signalingManager =  call.getSignalingManager();
                signalingManager.sendIceCandidate(iceCandidate, call.getCallerInfo(), call.getCalleeInfo(), call.getSessionId());
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                for (int i = 0; i < iceCandidates.length; i++) {
                    Log.i(TAG, "onIceCandidatesRemoved: " + iceCandidates[i]);
                }
                peerConnection.removeIceCandidates(iceCandidates);
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                MediaStreamTrack track = rtpReceiver.track();
                if (track != null) {
                    Log.d(TAG, "onAddRemoteTrack " + track.kind());
                }
                if (track instanceof VideoTrack) {
                    VideoTrack remoteVideo = (VideoTrack) track;
                    remoteVideoTrack = remoteVideo;
                    enableRemoteVideo = true;
                    remoteVideo.setEnabled(enableRemoteVideo);
//                    if ( null != remoteRender) {
//                       remoteVideo.addSink(remoteRender);
//                    }
                    if (remoteMrtcRender != null) {
                        remoteVideoTrack.addSink(remoteMrtcRender);
                    }
                }
                if (track instanceof AudioTrack) {
                    AudioTrack remoteAudio = (AudioTrack) track;
                    remoteAudioTrack = remoteAudio;
                    enableRemoteAudio = true;
                    remoteAudio.setEnabled(enableRemoteAudio);
                }
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.i(TAG, "onDataChannel " + dataChannel.label());
                CallImpl call = (CallImpl) CallManager.getCall(sessionId);
                SignalingManager signalingManager =  call.getSignalingManager();
                dataChannel.registerObserver(new DataChannel.Observer(){
                    @Override 
                    public void onMessage(final DataChannel.Buffer buffer) {
                        if (buffer.binary) {
                            Log.d(TAG, "Received binary msg over " + dataChannel);
                            return;
                        }
                        ByteBuffer data = buffer.data;
                        signalingManager.getOnLineEventExecutor().onChannelMessage(data);
                    }

                    @Override
                    public void onStateChange() {
                        Log.d(TAG, "Data channel state changed: " + dataChannel.label() + ": " + dataChannel.state());
                        if (call == null) {
                            Log.e(TAG, "call not existed");
                            return;
                        }
                        if (dataChannel.state() == DataChannel.State.OPEN) {
                            signalingManager.getOnLineEventExecutor().onChannelOpen();
                        }
                        if (dataChannel.state() == DataChannel.State.CLOSED) {
                            signalingManager.getOnLineEventExecutor().onChannelClose();
                        }
                    }

                    @Override
                    public void onBufferedAmountChange(long l) {
                        Log.d(TAG, "Data channel buffered amount changed: " + dataChannel.label() + ": " + dataChannel.state());
                    }
                });
            }
        };
    }


    /**
     * @description: 
     * @param {String} addr
     * @return {*}
     */
    @Override
    public void setStunAddr(String addr) {
       this.stunAddr = addr; 
    }

    /**
     * @description: 
     * @param {MediaAttributes} mediaAttributes
     * @return {*}
     */
    @Override
    public void setMediaAttributes(MediaAttributes mediaAttributes) {
        this.mediaAttributes = mediaAttributes;
    }

    /**
     * @description: init remote video view
     * @param {*} 
     * @return {*}
     */
    public void initRemoteRender() {
        this.remoteMrtcRender = ((MrtcOperatorImpl)MrtcOperator.getInstance()).getRemoteMrtcRender();
        if (this.remoteMrtcRender == null) {
            return;
        }

        this.remoteRender = ((MrtcOperatorImpl)MrtcOperator.getInstance()).getRemoteRender();
        if (this.remoteRender == null) {
            return;
        }
//        this.remoteRender.init(this.eglBase.getEglBaseContext(), null);
        remoteRender.init(eglBase.getEglBaseContext(), new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {

            }

            @Override
            public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
                if (remoteRender != null) {
                    int w = (rotation == 0 || rotation == 180) ? videoWidth : videoHeight;
                    int h = (rotation == 0 || rotation == 180) ? videoHeight : videoWidth;
                    if (w > h) {
                        mHandler.post(() -> {
                            remoteRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                        });
                    }
                    else {
                        mHandler.post(() -> {
                            remoteRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
                        });
                    }
                }
            }
        });

        this.remoteRender.setEnableHardwareScaler(false);
        this.remoteRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }


    /**
     * @description: release resource
     * @param {*}
     * @return {*}
     */
    @Override
    public void release() {
        stopStatsTimer();

        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
        } 
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }

        if (remoteRender != null) {
            remoteRender.release();
        } 

        if (eglBase != null) {
            eglBase.release();
        }
    }

    /**
     * @description: 
     * @param {*}
     * @return {*}
     */
    @Override
    public PeerConnection getPeerConnection() {
        return this.peerConnection;
    }

    public static String setSdpPreferredCodec(String sdp, String codec, boolean isAudio) {
        final String[] lines = sdp.split("\r\n");
        final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
        if (mLineIndex == -1) {
            Log.w(TAG, "No mediaDescription line, so can't prefer " + codec);
            return sdp;
        }
        // A list with all the payload types with name |codec|. The payload types are integers in the
        // range 96-127, but they are stored as strings here.
        final List<String> codecPayloadTypes = new ArrayList<>();
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
        for (String line : lines) {
            Matcher codecMatcher = codecPattern.matcher(line);
            if (codecMatcher.matches()) {
                codecPayloadTypes.add(codecMatcher.group(1));
            }
        }
        if (codecPayloadTypes.isEmpty()) {
            Log.w(TAG, "No payload types with name " + codec);
            return sdp;
        }

        final String newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]);
        if (newMLine == null) {
            return sdp;
        }
        Log.d(TAG, "Change media description from: " + lines[mLineIndex] + " to " + newMLine);
        lines[mLineIndex] = newMLine;
        return joinString(Arrays.asList(lines), "\r\n", true /* delimiterAtEnd */);
    }

    public static String removeSdpCodec(String sdp, String codec, boolean isAudio) {
        final String[] lines = sdp.split("\r\n");
        final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
        if (mLineIndex == -1) {
            Log.w(TAG, "[RemoveSdpCodec] No mediaDescription line, so can't remove " + codec);
            return sdp;
        }

        int nextMLineIndex = lines.length;
        for (int i = mLineIndex+1; i < lines.length; ++i) {
            if (lines[i].startsWith("m=")) {
                nextMLineIndex = i;
                break;
            }
        }

        // A list with all the payload types with name |codec|. The payload types are integers in the
        // range 96-127, but they are stored as strings here.
        final List<String> codecPayloadTypes = new ArrayList<>();
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
        for (int i = mLineIndex+1; i < nextMLineIndex; ++i) {
            String line = lines[i];
            Matcher codecMatcher = codecPattern.matcher(line);
            if (codecMatcher.matches()) {
                codecPayloadTypes.add(codecMatcher.group(1));
            }
        }
        if (codecPayloadTypes.isEmpty()) {
            Log.w(TAG, "[RemoveSdpCodec] No payload types with name " + codec);
            return sdp;
        }

        // find a=fmtp:xxx apt=xxx line
        // webrtc red payload type
        final List<String> codecPayloadTypes2 = new ArrayList<>();
        for(String payloadType: codecPayloadTypes) {
            final Pattern payloadTypePattern = Pattern.compile("^a=fmtp:(\\d+) apt=" + payloadType + "[\r]?$");
            for (int i = mLineIndex+1; i < nextMLineIndex; ++i) {
                String line = lines[i];
                Matcher codecMatcher = payloadTypePattern.matcher(line);
                if (codecMatcher.matches()) {
                    codecPayloadTypes2.add(codecMatcher.group(1));
                }
            }
        }

        for(String line : codecPayloadTypes2) {
            codecPayloadTypes.add(line);
        }

        // Remove codec payload type from m=audio/video line
        final String newMLine = removePayloadTypes(codecPayloadTypes, lines[mLineIndex]);
        if (newMLine == null) {
            return sdp;
        }
        Log.d(TAG, "[RemoveSdpCodec] Change media description from: " + lines[mLineIndex] + " to " + newMLine);
        lines[mLineIndex] = newMLine;

        final List<String> newLines = new ArrayList<String>();
        for(int i = 0; i <= mLineIndex; ++i) {
            newLines.add(lines[i]);
        }

        // Remove a=rtpmap: and a=rtcp-fb: line
        for (int i = mLineIndex+1; i < nextMLineIndex; ++i) {
            String line = lines[i];
            boolean removeLine = false;
            for(String payloadType : codecPayloadTypes) {
                if (line.startsWith("a=rtpmap:" + payloadType)
                        || line.startsWith("a=rtcp-fb:" + payloadType)
                        || line.startsWith("a=fmtp:" + payloadType))
                {
                    removeLine = true;
                    break;
                }
            }
            if (!removeLine) {
                newLines.add(lines[i]);
            }
        }

        for (int i = nextMLineIndex; i < lines.length; ++i) {
            newLines.add(lines[i]);
        }

        return joinString(newLines, "\r\n", true /* delimiterAtEnd */);
    }

    /** Returns the line number containing "m=audio|video", or -1 if no such line exists. */
    private static int findMediaDescriptionLine(boolean isAudio, String[] sdpLines) {
        final String mediaDescription = isAudio ? "m=audio " : "m=video ";
        for (int i = 0; i < sdpLines.length; ++i) {
            if (sdpLines[i].startsWith(mediaDescription)) {
                return i;
            }
        }
        return -1;
    }

    private static String movePayloadTypesToFront(
            List<String> preferredPayloadTypes, String mLine) {
        // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
        final List<String> origLineParts = Arrays.asList(mLine.split(" "));
        if (origLineParts.size() <= 3) {
            Log.e(TAG, "Wrong SDP media description format: " + mLine);
            return null;
        }
        final List<String> header = origLineParts.subList(0, 3);
        final List<String> unpreferredPayloadTypes =
                new ArrayList<>(origLineParts.subList(3, origLineParts.size()));
        unpreferredPayloadTypes.removeAll(preferredPayloadTypes);
        // Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
        // types.
        final List<String> newLineParts = new ArrayList<>();
        newLineParts.addAll(header);
        newLineParts.addAll(preferredPayloadTypes);
        newLineParts.addAll(unpreferredPayloadTypes);
        return joinString(newLineParts, " ", false /* delimiterAtEnd */);
    }

    private static String removePayloadTypes(List<String> toRemovePayloadTypes, String mLine) {
        // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
        final List<String> origLineParts = Arrays.asList(mLine.split(" "));
        if (origLineParts.size() <= 3) {
            Log.e(TAG, "Wrong SDP media description format: " + mLine);
            return null;
        }
        final List<String> header = origLineParts.subList(0, 3);
        final List<String> keepPayloadTypes =
                new ArrayList<>(origLineParts.subList(3, origLineParts.size()));
        keepPayloadTypes.removeAll(toRemovePayloadTypes);
        // Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
        // types.
        final List<String> newLineParts = new ArrayList<>();
        newLineParts.addAll(header);
        newLineParts.addAll(keepPayloadTypes);
        return joinString(newLineParts, " ", false /* delimiterAtEnd */);
    }

    private static String joinString(
            Iterable<? extends CharSequence> s, String delimiter, boolean delimiterAtEnd) {
        Iterator<? extends CharSequence> iter = s.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        if (delimiterAtEnd) {
            buffer.append(delimiter);
        }
        return buffer.toString();
    }

    /**
     * @description: 
     * @param {[]byte} data
     * @return {*}
     */
    @Override
    public void onExternalFrame(byte[] data) {
        long timestampNS = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
        NV21Buffer buffer = new NV21Buffer(data, 1280, 720, null);

        VideoFrame videoFrame = new VideoFrame(buffer, 90, timestampNS);
        if (null != this.videoSource) {
            this.videoSource.getCapturerObserver().onFrameCaptured(videoFrame); 
        } 
        
        videoFrame.release();
    }

    @Override
    public void sendMsg(ByteBuffer data) {
        if (dataChannel == null) {
            Log.e(TAG, "send data fail cause by datachannel null");
            return;
        }
        dataChannel.send(new DataChannel.Buffer(data, false));
    }

    @Override
    public void setLocalAudioTrack(boolean sw){
        executor.execute(() -> {
            if (audioTrack != null) {
                Log.d(TAG, "set local audio tack:" + String.valueOf(sw));
                enableLocalAudio = sw ;
                audioTrack.setEnabled(sw);
            }
        });
    }

    @Override
    public void setRemoteAudioTrack(boolean sw){
        executor.execute(() -> {
            if (remoteAudioTrack != null) {
                Log.d(TAG, "set remote audio tack:" + String.valueOf(sw));
                enableRemoteAudio = sw;
                remoteAudioTrack.setEnabled(sw);
            }
        });
    }
}
