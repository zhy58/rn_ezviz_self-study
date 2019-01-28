package com.geer2.dakuai.lib.ezviz;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.videogo.openapi.EZOpenSDK;

/**
 * Created by zhy on 2018/11/2.
 */

public class EzvizModule extends BaseModule {
    private static final String REACT_CLASS = "EzvizModule";

    // public static String AppKey = "";

    public EzvizModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

   // @Override
   // public void initialize() {
   //     super.initialize();
   //     Application application = (Application) getReactApplicationContext().getBaseContext();
   //     EZOpenSDK.initLib(application, AppKey);
   // }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        EZOpenSDK.finiLib();
    }

    @ReactMethod
    public void setAccessToken(String accessToken){
        Log.i("ezviz_yi",accessToken+"//onZoomChange");
        EzvizUtils.getOpenSDK().setAccessToken(accessToken);
    }

}
