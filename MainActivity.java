package com.geer2.xiaokuai;

// ezviz 全屏操作
import android.content.Intent;
import android.content.res.Configuration;

import com.facebook.react.ReactActivity;
import android.os.Bundle;
import com.cboy.rn.splashscreen.SplashScreen;

public class MainActivity extends ReactActivity {

  /**
   * Returns the name of the main component registered from JavaScript.
   * This is used to schedule rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "iot2";
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SplashScreen.show(this);  // here
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    Intent intent = new Intent("onConfigurationChanged");
    intent.putExtra("newConfig", newConfig);
    this.sendBroadcast(intent);
  }

}
