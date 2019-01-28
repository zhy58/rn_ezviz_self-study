package com.geer2.dakuai.lib.ezviz;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.geer2.dakuai.MainApplication;
import com.videogo.constant.Constant;
import com.videogo.exception.BaseException;
import com.videogo.openapi.EZConstants;
import com.videogo.openapi.EZOpenSDK;
import com.videogo.realplay.RealPlayStatus;
import com.videogo.widget.CustomRect;
import com.videogo.widget.CustomTouchListener;

/**
 * Created by zhy on 2018/11/2.
 */

public class EzvizViewGroupManager extends SimpleViewManager<EzvizView> implements SurfaceHolder.Callback {
    private static final String REACT_CLASS = "RCTEzvizView";

    public static String AppKey = "";
    private MainApplication application;
    private ThemedReactContext mContext;

    private String verifyCode = "";
    private String accessToken = "";

    private EzvizView mSurfaceView;
    private SurfaceHolder mSurfaceHolder = null;

    private CustomTouchListener mCustomTouchListener = null;
    private int count = 1; 
    private Boolean sound = false;

    private float mPlayScale = 1;

    private Integer mCurrentQulityMode = EZConstants.EZVideoLevel.VIDEO_LEVEL_HD.getVideoLevel();

    /**
     *  初始化SDK
     * @param context
     */
    public void initSDK(Context context) {
       application = (MainApplication) context.getApplicationContext();
       // 设置是否支持P2P取流
       EZOpenSDK.enableP2P(true);
       EZOpenSDK.initLib(application, AppKey);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected EzvizView createViewInstance(ThemedReactContext reactContext) {
        mContext = reactContext;
        EzvizView mSurfaceView = new EzvizView(reactContext);
        EzvizPlayModule.setSurfaceView(mSurfaceView);
        setListeners(mSurfaceView);
        return mSurfaceView;
    }

    private void setListeners(final EzvizView mEzvizView) {
        mSurfaceView = mEzvizView;
        mContext.getCurrentActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setSurfaceHolder();
        createPlayer();
        mCustomTouchListener = new CustomTouchListener() {
            @Override
            public boolean canZoom(float scale) {
                if (mSurfaceView != null && mSurfaceView.mStatus == RealPlayStatus.STATUS_PLAY) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean canDrag(int direction) {
                if (mSurfaceView != null && mSurfaceView.mStatus != RealPlayStatus.STATUS_PLAY) {
                    return false;
                }
                if (mSurfaceView.player != null) {
                    if (DRAG_LEFT == direction || DRAG_RIGHT == direction) {
                        return true;
                    } else if (DRAG_UP == direction || DRAG_DOWN == direction) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onSingleClick() {
                WritableMap writableMap = Arguments.createMap();
                writableMap.putString("type", "onSingleClick");
                writableMap.putInt("number", 1);
                sendEvent(mSurfaceView, "onSingleClick", writableMap);
            }

            @Override
            public void onDoubleClick(MotionEvent motionEvent) {
                WritableMap writableMap = Arguments.createMap();
                writableMap.putString("type", "onDoubleClick");
                writableMap.putInt("number", 2);
                sendEvent(mSurfaceView, "onDoubleClick", writableMap);
            }

            @Override
            public void onZoom(float scale) {}

            @Override
            public void onZoomChange(float v, CustomRect customRect, CustomRect customRect1) {
                if (mSurfaceView != null && mSurfaceView.mStatus == RealPlayStatus.STATUS_PLAY) {
                    if (v > 1.0f && v < 1.1f) {
                        v = 1.1f;
                    }
                    setPlayScaleUI(v, customRect, customRect1);
                }
            }

            @Override
            public void onDrag(int i, float v, float v1) {}

            @Override
            public void onEnd(int i) {}
        };
        mSurfaceView.setOnTouchListener(mCustomTouchListener);
    }

    public void setSurfaceHolder() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    public void createPlayer(){
        if(mSurfaceView != null && mSurfaceView.deviceSerial.length() > 0){
            if(mSurfaceView.player == null){
                mSurfaceView.player = EzvizUtils.getOpenSDK().createPlayer(mSurfaceView.deviceSerial, mSurfaceView.cameraNo);
                if(mHandler != null && mSurfaceHolder != null){
                    mSurfaceView.player.setHandler(mHandler);
                    mSurfaceView.player.setSurfaceHold(mSurfaceHolder);
                }
            }
            if(mSurfaceView.talk == null){
                mSurfaceView.talk = EzvizUtils.getOpenSDK().createPlayer(mSurfaceView.deviceSerial, mSurfaceView.cameraNo);
                if(mHandler != null && mSurfaceHolder != null){
                    mSurfaceView.talk.setHandler(mHandler);
                }
            }
        }
    }

    private void setPlayScaleUI(float scale, CustomRect oRect, CustomRect curRect) {
        if (scale == 1) {
            try {
                if (mSurfaceView.player != null) {
                    mSurfaceView.player.setDisplayRegion(false, null, null);
                }
            } catch (BaseException e) {
                e.printStackTrace();
            }
        } else {

            if (mPlayScale == scale) {
                try {
                    if (mSurfaceView.player != null) {
                        mSurfaceView.player.setDisplayRegion(true, oRect, curRect);
                    }
                } catch (BaseException e) {
                    e.printStackTrace();
                }
                return;
            }
            try {
                if (mSurfaceView.player != null) {
                    mSurfaceView.player.setDisplayRegion(true, oRect, curRect);
                }
            } catch (BaseException e) {
                e.printStackTrace();
            }
        }
        mPlayScale = scale;
    }

    private void realplayPlaySuccess(Message msg) {
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        mContext.getCurrentActivity().getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;
        mCustomTouchListener.setSacaleRect(Constant.MAX_SCALE, 0, 0, width, height);
        setPlayScaleUI(1, null, null);
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what){
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_SUCCESS: //视频播放成功执行
                    mSurfaceView.mStatus = RealPlayStatus.STATUS_PLAY;
                    EzvizPlayModule.setRealplayPlaySuccess(message);
                    realplayPlaySuccess(message);
                    if(mSurfaceView.soundStatus){
                        mSurfaceView.player.openSound();
                    }else{
                        mSurfaceView.player.closeSound();
                    }
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_STOP_SUCCESS: //视频暂停播放执行
                    mSurfaceView.mStatus = RealPlayStatus.STATUS_STOP;
                    EzvizPlayModule.setRealplayPlaySuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_FAIL: //视频播放失败执行
                    mSurfaceView.mStatus = RealPlayStatus.STATUS_STOP;
                    EzvizPlayModule.setRealplayPlayFail(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_SET_VEDIOMODE_SUCCESS: //视频清晰度设置成功执行
                    EzvizPlayModule.setRealplayPlaySuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_SET_VEDIOMODE_FAIL: //视频清晰度设置失败执行
                    EzvizPlayModule.setRealplayPlayFail(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_SUCCESS: //开启对讲成功执行
                    mSurfaceView.mTalkStatus = RealPlayStatus.STATUS_PLAY;
                    EzvizPlayModule.setRealplayPlaySuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_STOP: //暂停对讲成功执行
                    mSurfaceView.mTalkStatus = RealPlayStatus.STATUS_STOP;
                    EzvizPlayModule.setRealplayPlaySuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_FAIL: //开启对讲失败执行
                    mSurfaceView.mTalkStatus = RealPlayStatus.STATUS_STOP;
                    EzvizPlayModule.setRealplayPlayFail(message);
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if(mSurfaceView != null){
            if(mSurfaceView.player != null){
                mSurfaceView.player.setSurfaceHold(surfaceHolder);
            }else{
                if(mSurfaceView.deviceSerial.length() > 0){
                    mSurfaceView.player = EzvizUtils.getOpenSDK().createPlayer(mSurfaceView.deviceSerial, mSurfaceView.cameraNo);
                }
            }
        }
        mSurfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mSurfaceView != null && mSurfaceView.player != null) {
            mSurfaceView.player.setSurfaceHold(null);
        }else{
            mSurfaceView.player = null;
            mSurfaceHolder = null;
        }
    }

    @ReactProp(name = "deviceSerial")
    public void setDeviceSerial(EzvizView mSurfaceView, String serial){
        mSurfaceView.deviceSerial = serial;
        createPlayer();
    }

    @ReactProp(name = "accessToken")
    public void setAccessToken(EzvizView mSurfaceView, String token){
        accessToken = token;
        if(token.length() > 0){
            EzvizUtils.getOpenSDK().setAccessToken(accessToken);
        }
    }

    @ReactProp(name = "verifyCode")
    public void setVerifyCode(EzvizView mSurfaceView, String verCode){
        verifyCode = verCode;
        if(mSurfaceView != null && mSurfaceView.player != null && verCode.length() > 0){
            mSurfaceView.player.setPlayVerifyCode(verifyCode);
        }
    }

    @ReactProp(name = "cameraNo")
    public void setCameraNo(EzvizView mSurfaceView, Integer cameraNo){
        if(cameraNo == null){
            return;
        }
        mSurfaceView.cameraNo = cameraNo;
        createPlayer();
    }

    @ReactProp(name = "sound")
    public void setSound(EzvizView mSurfaceView, Boolean bool){
        if(mSurfaceView != null && mSurfaceView.player != null){
            mSurfaceView.soundStatus = bool;
            if(bool){
                mSurfaceView.player.openSound();
            }else{
                mSurfaceView.player.closeSound();
            }
        }
    }

    @ReactProp(name = "currentQulityMode")
    public void setCurrentQulityMode(EzvizView mSurfaceView, Integer currentQulityMode){
        mCurrentQulityMode = currentQulityMode;
        if(mSurfaceView != null && currentQulityMode != null && mSurfaceView.deviceSerial.length() > 0){
            try {
                if(mSurfaceView.player == null){
                    EzvizUtils.getOpenSDK().setVideoLevel(mSurfaceView.deviceSerial, mSurfaceView.cameraNo, mCurrentQulityMode);
                }else{
                    if(mSurfaceView.mStatus == RealPlayStatus.STATUS_PLAY){
                        mSurfaceView.stopPlay();
                        EzvizUtils.getOpenSDK().setVideoLevel(mSurfaceView.deviceSerial, mSurfaceView.cameraNo, mCurrentQulityMode);
                        mSurfaceView.startPlay();
                    }else{
                        EzvizUtils.getOpenSDK().setVideoLevel(mSurfaceView.deviceSerial, mSurfaceView.cameraNo, mCurrentQulityMode);
                    }
                }
            } catch (BaseException e) {
                e.printStackTrace();
            }
        }

    }

    public WritableMap ezOnSingleClick(String str){
        WritableMap writableMap = Arguments.createMap();
        writableMap.putString("type", str);
        return writableMap;
    }

    private void sendEvent(EzvizView mSurfaceView, String eventName, WritableMap params){
        WritableMap event = Arguments.createMap();
        event.putMap("params", params);
        event.putString("type", eventName);
        mContext
                .getJSModule(RCTEventEmitter.class)
                .receiveEvent(mSurfaceView.getId(),
                        "topChange",
                        event);
    }


}

