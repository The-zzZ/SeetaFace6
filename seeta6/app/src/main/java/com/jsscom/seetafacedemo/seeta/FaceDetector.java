package com.jsscom.seetafacedemo.seeta;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;


import com.jsscom.seeta6.MyApp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FaceDetector {

    private static final String TAG = FaceDetector.class.getSimpleName();
    private Context mContext;
    private static final String filepath2= Environment.getExternalStorageDirectory()+"/tl/face/";
    private long tempLong=0;

    private FaceDetector() {
        mContext = MyApp.getInstance();
    }
    private static class InstanceHolder{
        private final static FaceDetector INSTANCE = new FaceDetector();
    }
    public static FaceDetector getInstance(){
        return InstanceHolder.INSTANCE;
    }

    /**
     * 初始化引擎，加载模式文件
     */
    public void loadEngine(String detectModelFile) {
        if (null == detectModelFile || "".equals(detectModelFile)) {
            Log.w(TAG, "detectModelFile file path is invalid!");
            return;
        }
        int ret=initFaceDetection(detectModelFile);
        Log.w(TAG, "loadEngine: ret="+ret );
    }

    public void loadEngine() {
        if (null == mContext) {
            Log.w(TAG, "please call initial first!");
            return;
        }
        Log.w(TAG, "loadEngine >>>");
        loadEngine(getPath2("face_detector.csta"));
    }

//    /**
//     * 检测图片
//     * @param rgbaddr 图片数据内存地址
//     * @return 识别结果
//     */
//    public int detect(long rgbaddr) {
//        long start = System.currentTimeMillis();
//        int ret=applyFaceDetection(rgbaddr);
//        Log.d(TAG, "spend time>>>: " + (System.currentTimeMillis() - start));
//        return ret;
//    }

    public int detect(byte[] base64,int len,int width,int height) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "spend time>>>: " + (System.currentTimeMillis() - start));
        int ret=applyFaceDetection2(base64,len,width,height);
        Log.d(TAG, "spend2 time>>>: " + (System.currentTimeMillis() - start));
        return ret;
    }

//
//    public int eyeDetect(long rgbaddr) {
//        long start = System.currentTimeMillis();
//        int ret=eyeLengDetect(rgbaddr);
//        Log.d(TAG, "spend time: " + (System.currentTimeMillis() - start));
//        return ret;
//    }

    public static String getPath2(String file){
        String path=filepath2+file;
        File file1=new File(path);
        if(file1.exists()) {
            return path;
        }else return "";
    }

    //该函数主要用来完成载入外部模型文件时，获取文件的路径加文件名
    public static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i("FileUtil", "Failed to upload a file");
        }
        return "";
    }

    /**
     * 释放引擎
     */
    public void releaseEngine() {
        releaseFaceDetection();
    }

    public Rect getRect(){
        Rect rect=new Rect(getX(),getY(),getWidth()-getX(),getHeight()-getY());
        Log.w(TAG, "getRect: similarity="+getX()+","+getY()+","+getWidth()+","+getHeight());
        return rect;
    }

    public int getX(){
        int ret=getFaceDetectionX();
        if(ret<0){
            ret=0;
        }
        return ret;
    }

    public int getY(){
        int ret=getFaceDetectionY();
        if(ret<0){
            ret=0;
        }
        return ret;
    }

    public int getWidth(){
        int ret=getFaceDetectionWidth();
        if(ret<0){
            ret=0;
        }
        return ret;
    }

    public int getHeight(){
        int ret=getFaceDetectionHeight();
        if(ret<0){
            ret=0;
        }
        return ret;
    }

    //人脸检测的三个native函数
    private native int initFaceDetection(String detectModelFile);
//    private native int applyFaceDetection(long addr);
    private native int applyFaceDetection2(byte[] data,int len,int width,int height);
//    private native int eyeLengDetect(long addr);
    private native int releaseFaceDetection();
    private native int getFaceDetectionX();
    private native int getFaceDetectionY();
    private native int getFaceDetectionWidth();
    private native int getFaceDetectionHeight();
}
