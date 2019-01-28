package com.geer2.dakuai.lib.ezviz;

import android.content.Context;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.Arrays;
import java.util.List;


/**
 * Created by zhy on Nov 2, 2018.
 */
public class EzvizPackage implements ReactPackage {

    private Context mContext;
    private EzvizViewGroupManager ezvizViewGroupManager;

    public EzvizPackage(Context context) {
        this.mContext = context;
        ezvizViewGroupManager = new EzvizViewGroupManager();
        ezvizViewGroupManager.initSDK(context);
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        return Arrays.<NativeModule>asList(
                new EzvizModule(reactContext),
                new EzvizPlayModule(reactContext)
        );
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
                //new EzvizViewGroupManager()
                ezvizViewGroupManager
        );
    }
}
