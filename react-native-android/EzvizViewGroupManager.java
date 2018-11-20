package com.geer2.xiaokuai.lib.ezviz;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.geer2.xiaokuai.MainApplication;
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

    // init SDK
    public static String AppKey = "2b79fac39a4c4689b1d2bdc9f7cf2710";
    private MainApplication application;
    private ThemedReactContext mContext;
    // init data
    private String verifyCode = ""; //设备验证码
    private String accessToken = ""; //播放token
    // view
    private EzvizView mSurfaceView;
    private SurfaceHolder mSurfaceHolder = null;
    // 消息处理(播放，对讲)
    //private Handler mHandler;
    // 播放
    //private EZPlayer player = null;
    private CustomTouchListener mCustomTouchListener = null;
    private int count = 1; // 控制播放开关
    private Boolean sound = false;
    // 缩放
    private float mPlayScale = 1;
    // 清晰度
    private Integer mCurrentQulityMode = EZConstants.EZVideoLevel.VIDEO_LEVEL_HD.getVideoLevel();

    /**
     *  初始化SDK
     * @param context
     */
    public void initSDK(Context context) {
        application = (MainApplication) context.getApplicationContext();
        // 设置是否支持P2P取流
        //EZOpenSDK.enableP2P(true);
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
        // 保持屏幕常亮
        mContext.getCurrentActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 设置mSurfaceHolder
        setSurfaceHolder();
        createPlayer();
        mCustomTouchListener = new CustomTouchListener() {
            @Override
            public boolean canZoom(float scale) {
                // 实现
                Log.i("ezviz_yi",scale + "//canZoom");
                if (mSurfaceView != null && mSurfaceView.mStatus == RealPlayStatus.STATUS_PLAY) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean canDrag(int direction) {
                Log.i("ezviz_yi",direction + "//canDrag");
                if (mSurfaceView != null && mSurfaceView.mStatus != RealPlayStatus.STATUS_PLAY) {
                    return false;
                }
                if (mSurfaceView.player != null) {
                    // 出界判断
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
                Log.i("ezviz_yi", "//onSingleClick");
                //sendEvent(mSurfaceView, "onSingleClick", play());
            }

            @Override
            public void onDoubleClick(MotionEvent motionEvent) {}

            @Override
            public void onZoom(float scale) {}

            @Override
            public void onZoomChange(float v, CustomRect customRect, CustomRect customRect1) {
                Log.i("ezviz_yi",v+"//onZoomChange");
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

    /**
     * 创建播放和创建对讲
     */
    public void createPlayer(){
        if(mSurfaceView != null && mSurfaceView.deviceSerial.length() > 0){
            // 创建直播
            if(mSurfaceView.player == null){
                mSurfaceView.player = EzvizUtils.getOpenSDK().createPlayer(mSurfaceView.deviceSerial, mSurfaceView.cameraNo);
                //设置Handler, 该handler将被用于从播放器向handler传递消息
                if(mHandler != null && mSurfaceHolder != null){
                    mSurfaceView.player.setHandler(mHandler);
                    mSurfaceView.player.setSurfaceHold(mSurfaceHolder);
                }
            }
            // 创建对讲
            if(mSurfaceView.talk == null){
                mSurfaceView.talk = EzvizUtils.getOpenSDK().createPlayer(mSurfaceView.deviceSerial, mSurfaceView.cameraNo);
                //设置Handler, 该handler将被用于从播放器向handler传递消息
                if(mHandler != null && mSurfaceHolder != null){
                    mSurfaceView.talk.setHandler(mHandler);
                    //mSurfaceView.talk.setSurfaceHold(mSurfaceHolder);
                }
            }
        }
    }

    /**
     * 播放成功后回调方法 实现缩放功能
     * @param scale
     * @param oRect
     * @param curRect
     */
    private void setPlayScaleUI(float scale, CustomRect oRect, CustomRect curRect) {
        if (scale == 1) {
            try {
                if (mSurfaceView.player != null) {
                    mSurfaceView.player.setDisplayRegion(false, null, null);
                }
            } catch (BaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {

            if (mPlayScale == scale) {
                try {
                    if (mSurfaceView.player != null) {
                        mSurfaceView.player.setDisplayRegion(true, oRect, curRect);
                    }
                } catch (BaseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return;
            }
            try {
                if (mSurfaceView.player != null) {
                    mSurfaceView.player.setDisplayRegion(true, oRect, curRect);
                }
            } catch (BaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        mPlayScale = scale;
    }

    /**
     * 播放成功回调并实现缩放功能
     * @param msg
     */
    private void realplayPlaySuccess(Message msg) {
        //注：构造函数DisplayMetrics 不需要传递任何参数；getDefaultDisplay() 方法将取得的宽高维度存放于DisplayMetrics 对象中，而取得的宽高维度是以像素为单位
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        // 将当前窗口的一些信息放在DisplayMetrics类中
        mContext.getCurrentActivity().getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;
        mCustomTouchListener.setSacaleRect(Constant.MAX_SCALE, 0, 0, width, height);
        setPlayScaleUI(1, null, null);
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            Log.i("ezviz_yi", message+"//message");
            switch (message.what){
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_SUCCESS: //视频播放成功执行
                    //Log.i("ezviz_yi", EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_SUCCESS+"//s");
                    mSurfaceView.mStatus = RealPlayStatus.STATUS_PLAY;
                    EzvizPlayModule.setRealplayPlaySuccess(message);
                    realplayPlaySuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_STOP_SUCCESS: //视频暂停播放执行
                    //Log.i("ezviz_yi", EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_SUCCESS+"//s");
                    mSurfaceView.mStatus = RealPlayStatus.STATUS_STOP;
                    EzvizPlayModule.setRealplayPlaySuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_FAIL: //视频播放失败执行
                    //Log.i("ezviz_yi", EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_FAIL+"//f");
                    mSurfaceView.mStatus = RealPlayStatus.STATUS_STOP;
                    EzvizPlayModule.setRealplayPlayFail(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_SET_VEDIOMODE_SUCCESS: //视频清晰度设置成功执行
                    Log.i("ezviz_yi", EZConstants.EZRealPlayConstants.MSG_SET_VEDIOMODE_SUCCESS+"//q_s");
                    EzvizPlayModule.setRealplayPlaySuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_SET_VEDIOMODE_FAIL: //视频清晰度设置失败执行
                    Log.i("ezviz_yi", EZConstants.EZRealPlayConstants.MSG_SET_VEDIOMODE_FAIL+"//q_e");
                    EzvizPlayModule.setRealplayPlayFail(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_SUCCESS: //开启对讲成功执行
                    Log.i("ezviz_yi", EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_SUCCESS+"//t_s");
                    mSurfaceView.mTalkStatus = RealPlayStatus.STATUS_PLAY;
                    EzvizPlayModule.setRealplayTalkSuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_STOP: //暂停对讲成功执行
                    Log.i("ezviz_yi", EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_SUCCESS+"//t_s");
                    mSurfaceView.mTalkStatus = RealPlayStatus.STATUS_STOP;
                    EzvizPlayModule.setRealplayTalkSuccess(message);
                    break;
                case EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_FAIL: //开启对讲失败执行
                    Log.i("ezviz_yi", EZConstants.EZRealPlayConstants.MSG_REALPLAY_VOICETALK_FAIL+"//t_e");
                    mSurfaceView.mTalkStatus = RealPlayStatus.STATUS_STOP;
                    EzvizPlayModule.setRealplayPlayFail(message);
                    break;
                default:
                    //133 stop
                    Log.i("ezviz_yi", message.what+"//d");
                    break;
            }
            return false;
        }
    });

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.i("ezviz_yi", "surfaceCreated");
        if(mSurfaceView != null){
            if(mSurfaceView.player != null){
                mSurfaceView.player.setSurfaceHold(surfaceHolder);
            }else{
                if(mSurfaceView.deviceSerial.length() > 0){
                    mSurfaceView.player = EzvizUtils.getOpenSDK().createPlayer(mSurfaceView.deviceSerial, mSurfaceView.cameraNo);
                    //mSurfaceView.player.setSurfaceHold(mSurfaceHolder);
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
        Log.i("ezviz_yi", "surfaceDestroyed");
        if (mSurfaceView != null && mSurfaceView.player != null) {
            mSurfaceView.player.setSurfaceHold(null);
        }else{
            mSurfaceView.player = null;
        }
    }

    @ReactProp(name = "deviceSerial")
    public void setDeviceSerial(EzvizView mSurfaceView, String serial){
        //Log.i("ezviz_yi", mSurfaceView+"//serial");
        mSurfaceView.deviceSerial = serial;
        createPlayer();
    }

    @ReactProp(name = "accessToken")
    public void setAccessToken(EzvizView mSurfaceView, String token){
        //Log.i("ezviz_yi", token+"//token");
        accessToken = token;
        if(token.length() > 0){
            EzvizUtils.getOpenSDK().setAccessToken(accessToken);
        }
    }

    @ReactProp(name = "verifyCode")
    public void setVerifyCode(EzvizView mSurfaceView, String verCode){
        //Log.i("ezviz_yi", verCode+"//verCode");
        verifyCode = verCode;
        if(mSurfaceView != null && mSurfaceView.player != null && verCode.length() > 0){
            mSurfaceView.player.setPlayVerifyCode(verifyCode);
        }
    }

    @ReactProp(name = "cameraNo")
    public void setCameraNo(EzvizView mSurfaceView, Integer cameraNo){
        //Log.i("ezviz_yi", no+"//cameraNo");
        if(cameraNo == null){
            return;
        }
        mSurfaceView.cameraNo = cameraNo;
        createPlayer();
    }

    @ReactProp(name = "sound")
    public void setSound(EzvizView mSurfaceView, Boolean bool){
        //Log.i("ezviz_yi", bool+"//sound");
        sound = bool;
        if(mSurfaceView != null && mSurfaceView.player != null){
            if(bool){
                mSurfaceView.player.openSound();
            }else{
                mSurfaceView.player.closeSound();
            }
        }
    }

    @ReactProp(name = "currentQulityMode")
    public void setCurrentQulityMode(EzvizView mSurfaceView, Integer currentQulityMode){
        //Log.i("ezviz_yi", currentQulityMode+"//qxd");
        mCurrentQulityMode = currentQulityMode;
        if(mSurfaceView != null && currentQulityMode != null && mSurfaceView.deviceSerial.length() > 0){
            try {
                if(mSurfaceView.player == null){
                    EzvizUtils.getOpenSDK().setVideoLevel(mSurfaceView.deviceSerial, mSurfaceView.cameraNo, mCurrentQulityMode);
                }else{
                    if(mSurfaceView.mStatus == RealPlayStatus.STATUS_PLAY){
                        //先停止播放,然后重新开启播放,才能生效
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

