/*
 * @Author: your name
 * @Date: 2021-06-24 17:32:26
 * @LastEditTime: 2021-07-08 17:34:19
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/common/SignalingValue.java
 */
package thunder.mrtc.common;

public class SignalingValue {
    public final static String TAG = "MRTC_SIGNALING";

    public final static String MSG_ID = "msg_id";

    public final static String VERSION = "version";

    public final static String VERSION_TAG = "1.0";

    public final static String BODY = "data";

    public final static String ONLINE_ID = "online_id";

    public final static String TOKEN = "token";

    public final static String EXTRA_DATA = "extra_data";

    public final static String ERR_CODE = "err_code";

    public final static String ERR_MSG = "err_msg";

    public final static String SESSION_ID = "session_id";

    public final static String CALLER_ID = "caller_id";

    public final static String CALLEE_ID = "callee_id";

    public final static String CALLEE_IDS = "callee_ids";

    public final static String SDP = "sdp";

    public final static String CANDIDATE = "candidate";

    public final static String SDP_MID = "sdp_mid";

    public final static String SDP_MLINE_INDEX = "sdp_mline_index"; 

    public final static int CALL_ACCEPTED = 200;

    public final static int CALL_ERROR = 400;

    public final static int CALL_UNAUTH = 401;

    public final static int CALL_REJECTED = 403;

    public final static int CALL_UNAVAILABLE = 404;

    public final static int CALL_BUSY = 406;

    public final static int CALL_TIMEOUT = 408;

    public final static int CALL_INTERNAL_ERROR = 500;

    public final static String STATUS = "status";

    public final static String STATUS_TRING = "TRING";

    public final static String STATUS_RINGING = "RINGING";

    public final static String PEER_OFFLINE = "PEER_OFFLINE";

    public final static String PEER_ONLINE = "PEER_ONLINE";

    public final static String TIME_OUT = "time_out";

    public final static String RINGING_TIMEOUT = "ringing_timeout";

    public final static String CALLER_NUMBER = "caller_number";

    public final static String CALLEE_NUMBER = "callee_number";

    public final static String CALLER_CALL_EXTRA_DATA = "caller_call_extra_data";

    public final static String CALLEE_CALL_EXTRA_DATA = "callee_call_extra_data";
}