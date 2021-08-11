package com.bt901;

import android.app.Application;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class MyApp extends Application {

    // The CH34x driver class needs to be under the APP class
    // so that the life cycle of the helper class is the same as the life cycle of the entire application
    public static CH34xUARTDriver driver;

}
