/*
 * @Author: your name
 * @Date: 2021-07-02 11:01:41
 * @LastEditTime: 2021-07-06 21:37:40
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/thunder/mrtc/impl/CallManagerImpl.java
 */
package thunder.mrtc.impl;

import android.util.Log;


import thunder.mrtc.Call;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class CallManager{

	private final static String TAG = "MRTC_CALL_MANAGER"; 

	private static Map<String, Call> callPool = new ConcurrentHashMap<String, Call>();

	public static Call getCall(String sessionId) {
		Log.d(TAG, "get session: " + sessionId);
		return callPool.get(sessionId);
	}

	public static void putCall(String sessionId, Call call) {
		Log.d(TAG, "put session: " + sessionId);
		callPool.put(sessionId, call);
	}
	
	public static void removeCall(String sessionId) {
		Log.d(TAG, "remove session: " + sessionId);
		callPool.remove(sessionId);
	}

	public static Call getOnlineCall() {
		Call call = null;
		if (callPool.size() == 1) {
			for(Map.Entry<String, Call> entry : callPool.entrySet()) {
				Log.d(TAG, "get only one call: " + entry.getKey());
				call = entry.getValue();
			}
		}
		return call;
	}

	public static void clearAllCall() {
		callPool.clear();
	}

}