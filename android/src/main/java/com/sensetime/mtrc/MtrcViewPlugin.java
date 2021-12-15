package com.sensetime.mtrc;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.shim.ShimPluginRegistry;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformViewRegistry;

public class MtrcViewPlugin implements FlutterPlugin {
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        BinaryMessenger messenger = flutterPluginBinding.getBinaryMessenger();
        flutterPluginBinding.getPlatformViewRegistry()
                .registerViewFactory("MtrcPlugin", new MtrcViewFactory(messenger));
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    }

    static public void registerWith(PluginRegistry.Registrar registar) {
        registar.platformViewRegistry()
                .registerViewFactory(
                        "MtrcPlugin",
                        new MtrcViewFactory(registar.messenger()));

    }


    public static void registerWith(FlutterEngine flutterEngine) {
        final String key = MtrcPlugin.class.getCanonicalName();
        ShimPluginRegistry shimPluginRegistry = new ShimPluginRegistry(flutterEngine);

        if (shimPluginRegistry.hasPlugin(key)) {
            return;
        }

        PluginRegistry.Registrar registrar = shimPluginRegistry.registrarFor(key);
        registrar.platformViewRegistry().registerViewFactory("MtrcPlugin", new MtrcViewFactory(registrar.messenger()));

    }
}
