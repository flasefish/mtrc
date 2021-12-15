package com.sensetime.mtrc;

import android.content.Context;
import android.view.View;

import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MessageCodec;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class MtrcViewFactory extends PlatformViewFactory {
    private BinaryMessenger messenger;

    public MtrcViewFactory(BinaryMessenger messenger) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;

    }

    @Override
    public PlatformView create(Context context, int i, Object o) {
        MtrcPlugin view = new MtrcPlugin(context,messenger,i);
        return view;
    }


}
