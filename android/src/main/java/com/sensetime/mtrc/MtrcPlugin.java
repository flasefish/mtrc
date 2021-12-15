package com.sensetime.mtrc;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformView;
import thunder.mrtc.MrtcRender;

/** MtrcPlugin */
public class MtrcPlugin implements FlutterPlugin, MethodCallHandler, PlatformView {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  Context context;
  public MrtcRender mrtcView;
  public  String TAG = "MtrcPlugin";
  private MethodChannel channel;

  public MtrcPlugin(Context context, BinaryMessenger messenger, int id){
    this.context = context;
    mrtcView =  new MrtcRender(context);
    Log.d(TAG,"MtrcView init");
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "mtrc");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public View getView() {
    return this.mrtcView;
  }

  @Override
  public void dispose() {

  }
}
