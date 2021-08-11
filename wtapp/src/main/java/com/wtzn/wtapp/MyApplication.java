package com.wtzn.wtapp;

import android.app.Application;
import com.wtzn.wtfile.util.SharedUtil;

public class MyApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        SharedUtil.init(getApplicationContext());
    }


}
