package com.geer2.xiaokuai.lib.ezviz;

import android.support.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

/**
 * Created by zhy on Nov 2, 2018.
 */
abstract public class BaseModule extends ReactContextBaseJavaModule {

    protected ReactApplicationContext context;

    public BaseModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
    }

    /**
     *
     * @param eventName
     * @param params
     *
     *  通过RCTDeviceEventEmitter向JS传递事件
     */
    protected void sendEvent(String eventName,@Nullable WritableMap params) {
        context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
}
