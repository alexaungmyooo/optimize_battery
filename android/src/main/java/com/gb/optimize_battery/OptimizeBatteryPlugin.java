package com.gb.optimize_battery;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import static android.content.Context.POWER_SERVICE;

enum Status {OK, NO_ACTIVITY, ACTIVITY_NOT_FOUND}

/** OptimizeBatteryPlugin */
public class OptimizeBatteryPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android

  private MethodChannel channel;
  private Context context;
  private Activity activity;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "optimize_battery");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "openBatteryOptimizationSettings":
        Status status = openBatteryOptimizationSettings();
        if (status == Status.NO_ACTIVITY) {
          result.error("NO_ACTIVITY", "Launching a setting requires a foreground activity.", null);
        } else if (status == Status.ACTIVITY_NOT_FOUND) {
          result.error(
                  "ACTIVITY_NOT_FOUND",
                  "No Activity found to handle intent",
                  null);
        } else {
          result.success(true);
        }
        break;
      case "isIgnoringBatteryOptimizations":
        boolean isIgnoring = isIgnoringBatteryOptimizations();
        result.success(isIgnoring);
        return;
      case "stopOptimizingBatteryUsage":
        Boolean response = stopOptimizingBatteryUsage();
        result.success(response);
      default:
        result.notImplemented();
        break;
    }
  }

  Boolean stopOptimizingBatteryUsage() {
    if(android.os.Build.VERSION.SDK_INT<23)
      return true;
    Intent intent = new Intent();
    String packageName = context.getPackageName();
    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
    intent.setData(Uri.parse("package:" + packageName));
    activity.startActivity(intent);
    return true;
  }

  boolean isIgnoringBatteryOptimizations() {
    if(android.os.Build.VERSION.SDK_INT<23)
      return true;
    String packageName = context.getPackageName();
    PowerManager mPowerManager = (PowerManager) (context.getSystemService(POWER_SERVICE));
    return mPowerManager.isIgnoringBatteryOptimizations(packageName);
  }

  Status openBatteryOptimizationSettings() {
    if(android.os.Build.VERSION.SDK_INT<23)
      return Status.OK;

    if (activity == null) {
      return Status.NO_ACTIVITY;
    }
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
    try {
      activity.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      return Status.ACTIVITY_NOT_FOUND;
    }
    return Status.OK;
  }

  //Activity aware methods
  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }
}
