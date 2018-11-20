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

/**
 * Created by zhy on 2018/11/2.
 */

public class EzvizPlayModule extends BaseModule implements LifecycleEventListener{
    private static final String REACT_CLASS = "EzvizPlayModule";

    // static data
    private static EzvizView mSurfaceView;
    private static ReactApplicationContext staticContext;

    // 屏幕捕捉（截图）
    private Application mApplication;
    private AudioPlayUtil mAudioPlayUtil;

    // 广播
    final BroadcastReceiver receiver;

    // 配网
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
        // 获取本地信息
        mAudioPlayUtil = AudioPlayUtil.getInstance(mApplication);

        mEZStartConfigWifiCallback = new EZOpenSDKListener.EZStartConfigWifiCallback(){
            @Override
            public void onStartConfigWifiCallback(String s, EZConstants.EZWifiConfigStatus ezWifiConfigStatus) {
                if (ezWifiConfigStatus == EZConstants.EZWifiConfigStatus.DEVICE_WIFI_CONNECTING) {
                    // TODO: 设备正在连接中
                    Log.i("ezviz_yi", s+"//设备正在连接中");
                    EzvizUtils.getOpenSDK().stopConfigWiFi();
                    //onConfigureNetworkCallback(1, s);
                    sendEvent("onConfigureNetworkCallback", onConfigureNetworkCallback(1, s));
                } else if (ezWifiConfigStatus == EZConstants.EZWifiConfigStatus.DEVICE_WIFI_CONNECTED) {
                    // TODO:  设备wifi连接成功
                    Log.i("ezviz_yi", s+"//设备wifi连接成功");
                    EzvizUtils.getOpenSDK().stopConfigWiFi();
                    sendEvent("onConfigureNetworkCallback", onConfigureNetworkCallback(2, s));
                } else if (ezWifiConfigStatus == EZConstants.EZWifiConfigStatus.DEVICE_PLATFORM_REGISTED) {
                    // TODO:  设备注册平台成功可以添加设备
                    Log.i("ezviz_yi", s+"//设备注册平台成功可以添加设备");
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

        // 接收MainActivity.java 发送的广播
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                Configuration newConfig = intent.getParcelableExtra("newConfig");
                WritableMap params = Arguments.createMap();
                if(newConfig.orientation == 1){
                    params.putString("orientation", "PORTRAIT");
                    //显示状态栏
                    context.getCurrentActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }else{
                    params.putString("orientation", "LANDSCAPE");
                    //隐藏状态栏
                    context.getCurrentActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                // params.putString("orientation", orientationValue);
                if(context.hasActiveCatalystInstance()){
                    sendEvent("orientationDidChange", params);
                }
            }
        };
        context.addLifecycleEventListener(this);
    }

    // 获取布局视图
    public static void setSurfaceView(final EzvizView mEzvizView){
        mSurfaceView = mEzvizView;
    };

    /**
     * 播放/对讲 失败回调
     * @param msg
     */
    public static void setRealplayPlayFail(Message msg){
        staticSendEvent("onRealplayCallback", onRealplayPlayFail(msg));
    }

    /**
     * 失败处理
     * @param msg
     * @return
     */
    public static WritableMap onRealplayPlayFail(Message msg){
        WritableMap writableMap = Arguments.createMap();
        writableMap.putInt("code", msg.what);//错误码
        ErrorInfo errorInfo = (ErrorInfo) msg.obj;// 获取失败信息
        Log.i("ezviz_yi", errorInfo+"//errorCode");
        if(errorInfo != null){
            writableMap.putInt("errorCode", errorInfo.errorCode);//错误码
            writableMap.putString("description", errorInfo.description);//错误描述
            writableMap.putString("moduleCode", errorInfo.moduleCode);//模块错误码
            writableMap.putString("sulution", errorInfo.sulution);//解决方案
        }
//        Log.i("ezviz_yi", errorInfo.errorCode+"//errorCode");//错误码
//        Log.i("ezviz_yi", errorInfo.moduleCode+"//moduleCode");//错误描述
//        Log.i("ezviz_yi", errorInfo.description+"//description");//模块错误码
//        Log.i("ezviz_yi", errorInfo.sulution+"//sulution");//解决方案
        return writableMap;
    }

    /**
     * 播放成功回调
     * @param msg
     */
    public static void setRealplayPlaySuccess(Message msg){
        staticSendEvent("onRealplayCallback", onRealplayPlaySuccess(msg, "开启播放成功"));
    }

    /**
     * 对讲成功回调
     * @param msg
     */
    public static void setRealplayTalkSuccess(Message msg){
        staticSendEvent("onRealplayCallback", onRealplayPlaySuccess(msg, "开启对讲成功"));
    }

    /**
     * 成功处理
     * @param msg
     * @return
     */
    public static WritableMap onRealplayPlaySuccess(Message msg, String description){
        WritableMap writableMap = Arguments.createMap();
        //Log.i("ezviz_yi", msg+"//success");
        writableMap.putInt("code", msg.what);//成功码
        writableMap.putString("description", description);//描述
        writableMap.putString("status", "success");//状态
        return writableMap;
    }

    /**
     * 屏幕捕捉（截图）
     */
    private void onCapturePic(){
        WritableMap writableMap = Arguments.createMap();
        if (!SDCardUtil.isSDCardUseable()) {
            Log.i("ezviz_yi","存储卡不可用");
            // 提示SD卡不可用
            writableMap.putInt("code", 2);//状态码
            writableMap.putString("description", "截图失败：存储卡不可用");//描述
            writableMap.putString("status", "error");//状态
            sendEvent("onCaptureCallback", writableMap);
            return;
        }
        if (SDCardUtil.getSDCardRemainSize() < SDCardUtil.PIC_MIN_MEM_SPACE) {
            Log.i("ezviz_yi","存储空间已满");
            // 提示内存不足
            writableMap.putInt("code", 3);//状态码
            writableMap.putString("description", "截图失败：存储空间已满");//描述
            writableMap.putString("status", "error");//状态
            sendEvent("onCaptureCallback", writableMap);
            return;
        }
        if(mSurfaceView != null && mSurfaceView.player != null && mSurfaceView.mStatus == RealPlayStatus.STATUS_PLAY){
            Thread thr = new Thread() {
                @Override
                public void run() {
                    Log.i("ezviz_yi","capture is start");
                    Bitmap bmp = mSurfaceView.player.capturePicture();
                    if (bmp != null) {
                        try {
                            mAudioPlayUtil.playAudioFile(AudioPlayUtil.CAPTURE_SOUND);
                            // 文件命名
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
                            Log.i("ezviz_yi",path+"//capture for path");
                            WritableMap _writableMap = Arguments.createMap();
                            _writableMap.putInt("code", 0);//状态码
                            _writableMap.putString("description", "已保存至相册"+ path);//描述
                            _writableMap.putString("status", "success");//状态
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
            Log.i("ezviz_yi","没有开启直播");
            // 没有开启直播
            writableMap.putInt("code", 1);//状态码
            writableMap.putString("description", "截图失败：请先开启直播功能");//描述
            writableMap.putString("status", "error");//状态
            sendEvent("onCaptureCallback", writableMap);
            return;
        }else{
            writableMap.putInt("code", -1);//状态码
            writableMap.putString("description", "截图失败");//描述
            writableMap.putString("status", "error");//状态
            sendEvent("onCaptureCallback", writableMap);
            return;
        }
    }

    /**
     * 设置当前屏幕方向
     * @param orientation
     */
    private void setQrientation(Integer orientation){
        final Activity activity = getCurrentActivity();
        if(activity != null){
            activity.setRequestedOrientation(orientation);
        }
    }

    @ReactMethod
    public void ezStartConfigureNetwork(String deviceSerial, String ssid, String password){
        //mSurfaceView.deviceSerial = deviceSerial;
        EzvizUtils.getOpenSDK().startConfigWifi(context.getApplicationContext(), deviceSerial, ssid, password, mode, mEZStartConfigWifiCallback);
    }

    @ReactMethod
    public void ezStopConfigureNetwork(){
        EzvizUtils.getOpenSDK().stopConfigWiFi();
    }

    @ReactMethod
    public void ezPlay(Boolean bool, String verifyCode){
        Log.i("ezviz_yi",mSurfaceView+"//"+bool+ "//ezPlay");
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
    public void ezSound(Boolean bool, Promise promise){
        Log.i("ezviz_yi",mSurfaceView+"//"+bool+ "//ezSound");
        try{
            if(mSurfaceView != null && mSurfaceView.player != null){
                //WritableMap writableMap = Arguments.createMap();
                if(!bool){
                    mSurfaceView.player.closeSound();
//                    writableMap.putInt("code", 0);//成功码
//                    writableMap.putString("description", "关闭声音成功");//描述
                }else{
                    mSurfaceView.player.openSound();
//                    writableMap.putInt("code", 1);//成功码
//                    writableMap.putString("description", "打开声音成功");//描述
                }
//                writableMap.putString("status", "success");//状态
                //promise.resolve("1");
            }
        }catch (Exception e){
            //promise.reject("0",e.getMessage());
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
        Log.i("ezviz_yi", "ezTalk");
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
        Log.i("ezviz_yi", "ezTalking");
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
        Log.i("ezviz_yi",bool+ "//fullscreen");
        //正在播放发生屏幕方向改变
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
            //mSurfaceView.startPlay();
        }
        //getOrientation();
    }

    /**
     * 获取当前屏幕方向
     */
    @ReactMethod
    private void getOrientation(Promise promise){
        WritableMap writableMap = Arguments.createMap();
        final int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;
        String orientationValue = orientationInt == 1 ? "PORTRAIT" : "LANDSCAPE";
        Log.i("ezviz_yi",orientationInt+ "//getOrientation");

        writableMap.putString("orientation", orientationValue);
        writableMap.putInt("value", orientationInt);
        promise.resolve(writableMap);
    }

    /**
     * 静态发送事件
     * @param eventName
     * @param params
     *
     *  通过RCTDeviceEventEmitter向JS传递事件
     */
    private static void staticSendEvent(String eventName,@Nullable WritableMap params) {
        staticContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public void onHostResume() {
        final Activity activity = getCurrentActivity();
        if (activity != null){
            Log.i("ezviz_yi", "activity register receive");
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
                Log.i("ezviz_yi", "activity unregister receive");
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
