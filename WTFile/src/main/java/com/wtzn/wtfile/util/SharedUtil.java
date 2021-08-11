package com.wtzn.wtfile.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by 葛文博 on 2017/11/11.
 */
public class SharedUtil {

    //1、通过上下文对象获得共享参数的对象
    private static SharedPreferences sharedPreferences;
    //2、获得共享参数的编辑对象
    private static SharedPreferences.Editor editor;

    /**
     * 初始化操作 一般在自定义的application中执行
     */
    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static void putString(String key, String value) {
        editor.putString(key, value);
        editor.commit();
    }

    public static String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    public static void putInt(String key, int value) {
        editor.putInt(key, value);
        editor.commit();
    }

    public static int getInt(String key) {
        return sharedPreferences.getInt(key, -1);
    }

    public static void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public static void putFloat(String key, float value) {
        editor.putFloat(key, value);
        editor.commit();
    }

    public static float getFloat(String key) {
        return sharedPreferences.getFloat(key, -1.0f);
    }

    public static void putLong(String key, long value) {
        editor.putLong(key, value);
        editor.commit();
    }

    public static long getLong(String key) {
        return sharedPreferences.getLong(key, -1);
    }

}
