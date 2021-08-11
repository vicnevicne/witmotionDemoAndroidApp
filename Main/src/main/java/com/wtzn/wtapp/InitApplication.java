package com.wtzn.wtapp;

import android.app.Application;
import com.wtzn.wtfile.util.SharedUtil;

public class InitApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        SharedUtil.init(getApplicationContext());
    }


}
