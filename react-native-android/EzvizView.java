package com.geer2.xiaokuai.lib.ezviz;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.videogo.openapi.EZPlayer;
import com.videogo.realplay.RealPlayStatus;

import java.io.IOException;

/**
 * Created by zhy on 2018/11/3.
 */

public class EzvizView extends SurfaceView implements SurfaceHolder.Callback, Handler.Callback {

    //private MediaPlayer mPlayer = new MediaPlayer();
    //String uri="https://media.w3.org/2010/05/sintel/trailer.mp4";
    private SurfaceHolder mSurfaceHolder = null;

    protected EZPlayer player;
    protected String deviceSerial = "";
    protected int cameraNo = 1; 
    protected int mStatus = RealPlayStatus.STATUS_INIT; 

    protected EZPlayer talk;
    protected int mTalkStatus = RealPlayStatus.STATUS_INIT; 

    public EzvizView(Context context) {
        super(context);
        //initView(context);
    }
//--------------------------------------------------
    private void initView(Context context) {
        try {
            mPlayer.setDataSource(context, Uri.parse(uri));
            mSurfaceHolder=this.getHolder();
            mSurfaceHolder.addCallback(this);
            mPlayer.prepare();
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mPlayer.start();
                    mPlayer.setLooping(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void vRestart(){
        mPlayer.start();
    }

    public void vPause(){
        mPlayer.pause();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {}
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPlayer.setDisplay(holder);
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    public boolean handleMessage(Message message) {
        return false;
    }
//--------------------------------------------------
    
    public void stopPlay(){
        if(player != null){
            player.stopRealPlay();
            mStatus = RealPlayStatus.STATUS_STOP;
        }
    }
    
    public void startPlay(){
        if(player != null){
            player.startRealPlay();
            mStatus = RealPlayStatus.STATUS_PLAY;
        }
    }

    public void stopTalk(){
        if(talk != null){
            talk.stopVoiceTalk();
            mTalkStatus = RealPlayStatus.STATUS_STOP;
        }
    }

    public void startTalk(){
        if(talk != null){
            talk.startVoiceTalk();
            mTalkStatus = RealPlayStatus.STATUS_PLAY;
        }
    }
}
