/*
 * @Author: your name
 * @Date: 2021-06-24 18:47:09
 * @LastEditTime: 2021-07-01 18:30:20
 * @LastEditors: Please set LastEditors
 * @Description: In User Settings Edit
 * @FilePath: /mrtc-sdk-android/mrtc/src/main/java/com/mercury/mrtc/MrtcRender.java
 */
package thunder.mrtc;

import android.content.Context;
import android.graphics.Bitmap;

import android.graphics.Rect;
import android.os.Environment;
import android.util.Size;
import android.widget.FrameLayout;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.graphics.Canvas;
import android.graphics.Color;

import org.webrtc.EglRenderer;
import org.webrtc.Logging;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class MrtcRender extends FrameLayout implements VideoSink {
    private static String TAG = "MrtcRender";
	private SurfaceViewRenderer remoteRender;
	private boolean mIsRenderEnabled;
	private boolean mIsForceRotated;
	private int mForceRotation;
	private boolean mSavingBitmap;
	private boolean mAddedListener;
    private SnapshotListener mSnapshotListener;

	public MrtcRender(Context context) {
        super(context);
        init();
    }

    public MrtcRender(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MrtcRender(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public MrtcRender(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

	public SurfaceViewRenderer getRemoteRender() {
		return this.remoteRender;
	}

	private void init() {
        remoteRender = new SurfaceViewRenderer(getContext());
        mAddedListener = false;
        mSnapshotListener = null;

        addView(remoteRender, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        remoteRender.setZOrderOnTop(true);
        remoteRender.setZOrderMediaOverlay(true);
        mIsRenderEnabled = true;
        mIsForceRotated = false;
        mForceRotation = 0;
        mSavingBitmap = false;
    }

    public boolean isRenderEnabled() {
	    return mIsRenderEnabled;
    }

    public void setRenderEnabled(boolean enable) {
        mIsRenderEnabled = enable;
    }

    public void setForceRotation(boolean enable, int rotation) {
        mIsForceRotated = enable;
        mForceRotation = rotation;
    }

    public boolean isForceRotated() {
	    return mIsForceRotated;
    }

    public int getForceRotation() {
	    if (mIsForceRotated)
	        return mForceRotation;
	    else
	        return 0;
    }

    public void setMirror(final boolean mirror) {
        remoteRender.setMirror(mirror);
    }

    public void setEnableRoi(boolean enableRoi) {
        remoteRender.setEnableRoi(enableRoi);
    }

    public void setRoiRect(Rect rect, Size frameSize) {
	    org.webrtc.Size size = new org.webrtc.Size(frameSize.getWidth(), frameSize.getHeight());
        remoteRender.setRoiRect(rect, size);
    }

    @Override
    synchronized public void onFrame(VideoFrame frame) {
        if (remoteRender == null) {
            Logging.d(TAG, "Dropping frame in proxy because target is null.");
            return;
        }

        if (!mIsRenderEnabled) {
            Logging.d(TAG, "Dropping frame in proxy because render is disabled.");
            return;
        }

        if (mIsForceRotated) {
            VideoFrame outVideoFrame = new VideoFrame(
                    frame.getBuffer(),
                    mForceRotation, frame.getTimestampNs());
            remoteRender.onFrame(outVideoFrame);
        }
        else {
            remoteRender.onFrame(frame);
        }
    }

    public interface SnapshotListener {
        void onResult(boolean result, String saveFilePath);
    }

    public void snapshot(String savePath, SnapshotListener snapshotListener) {
	    if (mSavingBitmap)
	        return;

        mSavingBitmap = true;
        mSnapshotListener = snapshotListener;
        if (!mAddedListener) {
            remoteRender.addFrameListener(new EglRenderer.FrameListener() {
                @Override
                public void onFrame(Bitmap bitmap) {
                    if (mSavingBitmap) {
                        String savedFilePath = saveBitmapToFile(savePath, bitmap);
                        if (mSnapshotListener != null) {
                            mSnapshotListener.onResult((savedFilePath != null) ? true : false, savedFilePath);
                        }
                        mSavingBitmap = false;
                        mSnapshotListener = null;
                    }
                }
            }, 1.0f);
        }
    }

    private String saveBitmapToFile(String savePath, Bitmap bitmap) {
	    if (bitmap != null) {
            try {
                if (savePath == null)
                    savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                String filePath = savePath + "/" + System.currentTimeMillis()+".jpg";
                FileOutputStream fileOutputStream = new FileOutputStream(filePath);
                boolean result = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                if (result) {
                    return filePath;
                }
                else {
                    return null;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
	    else {
	        return null;
        }
    }
}