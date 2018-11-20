package com.geer2.xiaokuai.lib.ezviz;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.videogo.errorlayer.ErrorInfo;
import com.videogo.exception.BaseException;
import com.videogo.exception.InnerException;
import com.videogo.openapi.EZConstants;
import com.videogo.openapi.EZOpenSDKListener;
import com.videogo.realplay.RealPlayStatus;
import com.videogo.util.MediaScanner;
import com.videogo.util.SDCardUtil;

public class EzvizPlayModule extends BaseModule implements LifecycleEventListener{
    private static final String REACT_CLASS = "EzvizPlayModule";

    private static EzvizView mSurfaceView;
    private static ReactApplicationContext staticContext;

    private Application mApplication;
    private AudioPlayUtil mAudioPlayUtil;

    final BroadcastReceiver receiver;

    private Integer mode = EZConstants.EZWiFiConfigMode.EZWiFiConfigSmart; //配网模式
    private EZOpenSDKListener.EZStartConfigWifiCallback mEZStartConfigWifiCallback; //回调

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public void initialize() {
        super.initialize();
        mApplication = (Application) getReactApplicationContext().getBaseContext();
        mAudioPlayUtil = AudioPlayUtil.getInstance(mApplication);

        mEZStartConfigWifiCallback = new EZOpenSDKListener.EZStartConfigWifiCallback(){
            @Override
            public void onStartConfigWifiCallback(String s, EZConstants.EZWifiConfigStatus ezWifiConfigStatus) {
                if (ezWifiConfigStatus == EZConstants.EZWifiConfigStatus.DEVICE_WIFI_CONNECTING) {
                    EzvizUtils.getOpenSDK().stopConfigWiFi();
                    sendEvent("onConfigureNetworkCallback", onConfigureNetworkCallback(1, s));
                } else if (ezWifiConfigStatus == EZConstants.EZWifiConfigStatus.DEVICE_WIFI_CONNECTED) {
                    EzvizUtils.getOpenSDK().stopConfigWiFi();
                    sendEvent("onConfigureNetworkCallback", onConfigureNetworkCallback(2, s));
                } else if (ezWifiConfigStatus == EZConstants.EZWifiConfigStatus.DEVICE_PLATFORM_REGISTED) {
                    EzvizUtils.getOpenSDK().stopConfigWiFi();
                    sendEvent("onConfigureNetworkCallback", onConfigureNetworkCallback(3, s));
                }
            }
        };
    }

    public WritableMap onConfigureNetworkCallback(Integer statusCode, String deviceSerial){
        WritableMap writableMap = Arguments.createMap();
        writableMap.putInt("status", statusCode);
        writableMap.putString("serial", deviceSerial);
        if(statusCode == 1){
            writableMap.putString("msg", "设备正在连接中");
        }else if(statusCode == 2){
            writableMap.putString("msg", "设备wifi连接成功");
        }else{
            writableMap.putString("msg", "设备注册平台成功可以添加设备");
        }
        return writableMap;
    }

    public EzvizPlayModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
        staticContext = reactContext;

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                Configuration newConfig = intent.getParcelableExtra("newConfig");
                WritableMap params = Arguments.createMap();
                if(newConfig.orientation == 1){
                    params.putString("orientation", "PORTRAIT");
                    context.getCurrentActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }else{
                    params.putString("orientation", "LANDSCAPE");
                    context.getCurrentActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                if(context.hasActiveCatalystInstance()){
                    sendEvent("orientationDidChange", params);
                }
            }
        };
        context.addLifecycleEventListener(this);
    }

    public static void setSurfaceView(final EzvizView mEzvizView){
        mSurfaceView = mEzvizView;
    };

    public static void setRealplayPlayFail(Message msg){
        staticSendEvent("onRealplayCallback", onRealplayPlayFail(msg));
    }

    public static WritableMap onRealplayPlayFail(Message msg){
        WritableMap writableMap = Arguments.createMap();
        writableMap.putInt("code", msg.what);
        ErrorInfo errorInfo = (ErrorInfo) msg.obj;
        Log.i("ezviz_yi", errorInfo+"//errorCode");
        if(errorInfo != null){
            writableMap.putInt("errorCode", errorInfo.errorCode);
            writableMap.putString("description", errorInfo.description);
            writableMap.putString("moduleCode", errorInfo.moduleCode);
            writableMap.putString("sulution", errorInfo.sulution);
        }
        return writableMap;
    }

    public static void setRealplayPlaySuccess(Message msg){
        staticSendEvent("onRealplayCallback", onRealplayPlaySuccess(msg, "开启播放成功"));
    }

    public static void setRealplayTalkSuccess(Message msg){
        staticSendEvent("onRealplayCallback", onRealplayPlaySuccess(msg, "开启对讲成功"));
    }

    public static WritableMap onRealplayPlaySuccess(Message msg, String description){
        WritableMap writableMap = Arguments.createMap();
        writableMap.putInt("code", msg.what);
        writableMap.putString("description", description);
        writableMap.putString("status", "success");
        return writableMap;
    }

    private void onCapturePic(){
        WritableMap writableMap = Arguments.createMap();
        if (!SDCardUtil.isSDCardUseable()) {
            writableMap.putString("description", "截图失败：存储卡不可用");
            writableMap.putString("status", "error");
            sendEvent("onCaptureCallback", writableMap);
            return;
        }
        if (SDCardUtil.getSDCardRemainSize() < SDCardUtil.PIC_MIN_MEM_SPACE) {
            writableMap.putString("description", "截图失败：存储空间已满");
            writableMap.putString("status", "error");
            sendEvent("onCaptureCallback", writableMap);
            return;
        }
        if(mSurfaceView != null && mSurfaceView.player != null && mSurfaceView.mStatus == RealPlayStatus.STATUS_PLAY){
            Thread thr = new Thread() {
                @Override
                public void run() {
                    Bitmap bmp = mSurfaceView.player.capturePicture();
                    if (bmp != null) {
                        try {
                            mAudioPlayUtil.playAudioFile(AudioPlayUtil.CAPTURE_SOUND);
                            java.util.Date date = new java.util.Date();
                            final String path = Environment.getExternalStorageDirectory().getPath() + "/EZOpenSDK/CapturePicture/ez_"+ mSurfaceView.deviceSerial + "/" + String.format("%tY", date)
                                    + String.format("%tm", date) + String.format("%td", date) + "/"
                                    + String.format("%tH", date) + String.format("%tM", date) + String.format("%tS", date) + String.format("%tL", date) +".jpg";

                            if (TextUtils.isEmpty(path)) {
                                bmp.recycle();
                                bmp = null;
                                return;
                            }
                            EzvizUtils.saveCapturePictrue(path, bmp);

                            MediaScanner mMediaScanner = new MediaScanner(context.getApplicationContext());
                            mMediaScanner.scanFile(path, "jpg");
                            WritableMap _writableMap = Arguments.createMap();
                            _writableMap.putString("description", "已保存至相册"+ path);
                            _writableMap.putString("status", "success");
                            sendEvent("onCaptureCallback", _writableMap);
                        } catch (InnerException e) {
                            e.printStackTrace();
                        } finally {
                            if (bmp != null) {
                                bmp.recycle();
                                bmp = null;
                                return;
                            }
                        }
                    }
                    super.run();
                }
            };
            thr.start();
        }else if(mSurfaceView.mStatus == RealPlayStatus.STATUS_STOP || mSurfaceView.mStatus == RealPlayStatus.STATUS_INIT){
            writableMap.putString("description", "截图失败：请先开启直播功能");
            writableMap.putString("status", "error");
            sendEvent("onCaptureCallback", writableMap);
            return;
        }else{
            writableMap.putString("description", "截图失败");
            writableMap.putString("status", "error");
            sendEvent("onCaptureCallback", writableMap);
            return;
        }
    }

    private void setQrientation(Integer orientation){
        final Activity activity = getCurrentActivity();
        if(activity != null){
            activity.setRequestedOrientation(orientation);
        }
    }

    @ReactMethod
    public void ezStartConfigureNetwork(String deviceSerial, String ssid, String password){
        EzvizUtils.getOpenSDK().startConfigWifi(context.getApplicationContext(), deviceSerial, ssid, password, mode, mEZStartConfigWifiCallback);
    }

    @ReactMethod
    public void ezStopConfigureNetwork(){
        EzvizUtils.getOpenSDK().stopConfigWiFi();
    }

    @ReactMethod
    public void ezPlay(Boolean bool, String verifyCode){
        if(mSurfaceView != null){
            if(!bool){
                mSurfaceView.stopPlay();
            }else {
                if(verifyCode.length() <= 0){
                    mSurfaceView.startPlay();
                }else{
                    if(mSurfaceView.player != null){
                        mSurfaceView.stopPlay();
                        mSurfaceView.player.setPlayVerifyCode(verifyCode);
                        mSurfaceView.startPlay();
                    }
                }
            }
        }
    }

    @ReactMethod
    public void ezSound(Boolean bool){        
        if(mSurfaceView != null && mSurfaceView.player != null){
            if(!bool){
                mSurfaceView.player.closeSound();
            }else{
                mSurfaceView.player.openSound();
            }
        }
    }

    @ReactMethod
    public void ezCapture(){
        onCapturePic();
    }

    @ReactMethod
    public void ezDefinition(Integer currentQulityMode){
        if(currentQulityMode == null){
            return;
        }
        if(mSurfaceView.player != null && mSurfaceView.mStatus == RealPlayStatus.STATUS_PLAY){
            try {
                mSurfaceView.stopPlay();
                EzvizUtils.getOpenSDK().setVideoLevel(mSurfaceView.deviceSerial, mSurfaceView.cameraNo, currentQulityMode);
                mSurfaceView.startPlay();
            } catch (BaseException e) {
                e.printStackTrace();
            }
        }else{
            try {
                EzvizUtils.getOpenSDK().setVideoLevel(mSurfaceView.deviceSerial, mSurfaceView.cameraNo, currentQulityMode);
            } catch (BaseException e) {
                e.printStackTrace();
            }
        }
    }

    @ReactMethod
    public void ezTalk(Boolean bool, String verifyCode){
        if(mSurfaceView != null && mSurfaceView.player != null && mSurfaceView.talk != null){
            if(bool){
                mSurfaceView.player.closeSound();
                mSurfaceView.talk.setVoiceTalkStatus(false);
                mSurfaceView.startTalk();
            }else{
                mSurfaceView.stopTalk();
            }
        }
    }

    @ReactMethod
    public void ezTalking(Boolean bool){
        if(mSurfaceView != null && mSurfaceView.talk != null){
            if(bool){
                mSurfaceView.talk.setVoiceTalkStatus(true);
            }else{
                mSurfaceView.talk.setVoiceTalkStatus(false);
            }
        }
    }

    @ReactMethod
    public void ezFullscreen(Boolean bool){
        if(mSurfaceView != null){
            Integer flag = mSurfaceView.mStatus;
            mSurfaceView.stopPlay();
            if(bool){
                setQrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }else{
                setQrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            if(flag == RealPlayStatus.STATUS_PLAY){
                mSurfaceView.startPlay();
            }
        }
    }

    @ReactMethod
    private void getOrientation(Promise promise){
        WritableMap writableMap = Arguments.createMap();
        final int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;
        String orientationValue = orientationInt == 1 ? "PORTRAIT" : "LANDSCAPE";

        writableMap.putString("orientation", orientationValue);
        writableMap.putInt("value", orientationInt);
        promise.resolve(writableMap);
    }

    private static void staticSendEvent(String eventName,@Nullable WritableMap params) {
        staticContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public void onHostResume() {
        final Activity activity = getCurrentActivity();
        if (activity != null){
            activity.registerReceiver(receiver, new IntentFilter("onConfigurationChanged"));
        }else{
            FLog.e(ReactConstants.TAG, "no activity to register receiver");
        }
    }

    @Override
    public void onHostPause() {
        final Activity activity = getCurrentActivity();
        if(activity != null){
            try{
                activity.unregisterReceiver(receiver);
            }catch (java.lang.IllegalArgumentException e){
                FLog.e(ReactConstants.TAG, "receiver already unregistered", e);
            }
        }
    }

    @Override
    public void onHostDestroy() {

    }
}
