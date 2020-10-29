package com.jsscom.seeta6;

import android.app.Application;

public class MyApp extends Application {

    private static final String TAG = MyApp.class.getSimpleName();

    private static MyApp instance;
    public static MyApp getInstance(){
        return instance;
    }
    static {
        System.loadLibrary("FaceDetectorJni");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }


}
