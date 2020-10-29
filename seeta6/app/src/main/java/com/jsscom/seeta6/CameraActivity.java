package com.jsscom.seeta6;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jsscom.seetafacedemo.seeta.FaceDetector;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends Activity {

    private static final String TAG = CameraActivity.class.getSimpleName();
    public static  boolean isRuning=false;
    private SurfaceView surfaceView;
    private SurfaceView surfaceRect;
    private SurfaceView surfaceView_view;
    private SurfaceView surfaceRect_rect;
    private Camera camera;
    private Camera camera2;
    private int camereId = 0;
    private int camereId2 = 1;
    private int displayOrientation = 0;
    private int displayOrientation2 = 0;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;
    private SurfaceHolder surfaceholder;
    private Canvas canvas=null;
    private ExecutorService activeService = Executors.newSingleThreadExecutor();
    public static final String RECEIVER_ACTION_FINISH_D = "receiver_action_finish_cameradetect";
    private FinishActivityRecevier mRecevier;
    private FaceDetector faceDetector;
    private static String path= Environment.getExternalStorageDirectory()+"/face/face_detector.csta";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        boolean allGranted = true;
        for(String needPermission: NEEDED_PERMISSIONS) {
            allGranted &= ContextCompat.checkSelfPermission(this, needPermission) == PackageManager.PERMISSION_GRANTED;
        }
        if(!allGranted) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, 2);
        }
        isRuning=true;
        faceDetector=FaceDetector.getInstance();
        faceDetector.loadEngine();
        mRecevier = new FinishActivityRecevier();
        registerFinishReciver();
        surfaceView = findViewById(R.id.surfaceView);
        surfaceView_view = findViewById(R.id.surfaceView_view);
        surfaceView_view.setZOrderMediaOverlay(true);
        surfaceView_view.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        if(Camera.getNumberOfCameras()>0){
            surfaceholder = surfaceView.getHolder();
            surfaceholder.addCallback(callback);
        }else {
            Toast.makeText(this,"未检测到摄像头，请打开相机或客户端直接测试", Toast.LENGTH_SHORT).show();
            finish();
        }
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

//        }
    }

    @Override
    protected void onDestroy() {
        if (camera != null) {
            camera.release();
        }
        if (camera2 != null) {
            camera2.release();
        }
//        FaceDetector.getInstance().releaseEngine();
        if (mRecevier != null) {
            unregisterReceiver(mRecevier);
        }
        isRuning=false;
        faceDetector.releaseEngine();
        super.onDestroy();
    }

    private void registerFinishReciver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVER_ACTION_FINISH_D);
        registerReceiver(mRecevier, intentFilter);
    }


    private class FinishActivityRecevier extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //根据需求添加自己需要关闭页面的action
            if (RECEIVER_ACTION_FINISH_D.equals(intent.getAction()))
            {
                CameraActivity.this.finish();
            }
        }
    }

    private SurfaceHolder.Callback callback=new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            camera = Camera.open(camereId);
            try {
                int cameraWidth=640,cameraHeight=480;
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                Camera.Parameters parameters = camera.getParameters();
//                Camera.Size previewSize = getBestSupportedSize(parameters.getSupportedPreviewSizes(), metrics);
                parameters.setPreviewSize(cameraWidth, cameraHeight);
                camera.setParameters(parameters);
                displayOrientation = getCameraOri(getWindowManager().getDefaultDisplay().getRotation());
//                displayOrientation=90;
//                Log.w(TAG, "surfaceCreated: displayOrientation="+displayOrientation );
                final int mWidth = cameraWidth;
                final int mHeight = cameraHeight;
                Log.w(TAG, "surfaceCreated: w="+mWidth+",h="+mHeight );
                camera.setDisplayOrientation(displayOrientation);
                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        long time=System.currentTimeMillis();
                        try {
                            Log.w(TAG, "onPreviewFrame: time="+time +",len="+bytes.length);
                            if (yuvType == null) {
                                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(bytes.length);
                                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(mWidth).setY(mHeight);
                                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
                            }
                            in.copyFrom(bytes);
                            yuvToRgbIntrinsic.setInput(in);
                            yuvToRgbIntrinsic.forEach(out);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                        out.copyTo(bitmap);
                        Log.w(TAG, "onPreviewFrame: time2="+time +",len="+bytes.length);
                        int ret= FaceDetector.getInstance().detect(bytes,bytes.length,mWidth,mHeight);
                        if(ret>0){
                            Log.w(TAG, "onPreviewFrame: time="+(System.currentTimeMillis()-time) );
                            Rect maxRect=new Rect(faceDetector.getX(), faceDetector.getY(), faceDetector.getWidth()+faceDetector.getX(), faceDetector.getHeight()+faceDetector.getY());
                            if((maxRect.left-10)>=0){
                                maxRect.left-=10;
                            }
                            if(maxRect.right+20<=mWidth){
                                maxRect.right+=20;
                            }
                            if((maxRect.top-10)>=0){
                                maxRect.top-=10;
                            }
                            if(maxRect.bottom<=mHeight){
                                maxRect.bottom+=20;
                            }
                            Log.w(TAG, "onPreviewFrame: time2="+(System.currentTimeMillis()-time) );
                            if(surfaceView_view!=null){
                                canvas=null;
                                surfaceView_view.setVisibility(View.VISIBLE);
                                try {
                                    canvas = surfaceView_view.getHolder().lockCanvas();
                                    Log.w(TAG, "onPreviewFrame: canvas="+canvas );
                                    if(canvas!=null){
                                        synchronized (surfaceholder){
                                            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                                            Rect rect = new Rect(maxRect.left,maxRect.top,maxRect.right,maxRect.bottom);
                                            int width=rect.right-rect.left;
                                            int height=rect.bottom-rect.top;
                                            bitmap=Bitmap.createBitmap(bitmap,rect.left,rect.top,width,height);
//                                            final String imgData=OpenCVTools.bitmapToBase64(bitmap);
//                                            baiduFace.getInstance().showFace(imgData);
                                            Log.w(TAG, "onPreviewFrame: time3="+(System.currentTimeMillis()-time) );
                                            if (rect != null) {
                                                android.graphics.Rect adjustedRect = DrawUtils.adjustRect(rect, mWidth, mHeight,
                                                        canvas.getWidth(), canvas.getHeight(), displayOrientation, camereId);
                                                //画人脸框
                                                DrawUtils.drawFaceRect(canvas, adjustedRect, Color.YELLOW, 5);
                                                Log.w(TAG, "onPreviewFrame: time4="+(System.currentTimeMillis()-time) );
//                                                int width=adjustedRect.right-adjustedRect.left;
//                                                int height=adjustedRect.bottom-adjustedRect.top;
//                                                bitmap=Bitmap.createBitmap(bitmap,adjustedRect.left,adjustedRect.top,width,height,null,false);
//                                                outBitmap(bitmap,"/sdcard/test.jpg");
//                                                Log.w(TAG, "onPreviewFrame: time4="+(System.currentTimeMillis()-time) );
                                            }
                                        }
                                    }
                                } catch(Exception ex) {}
                                finally {
                                    if(canvas!=null){
                                        surfaceView_view.getHolder().unlockCanvasAndPost(canvas);
                                    }
                                }
                            }
//                            drawBorder(mat,maxRect,FACE_RECT_COLOR,2);
                        }else {
                            if(surfaceView_view!=null){
                                surfaceView_view.setVisibility(View.INVISIBLE);
                            }
                        }

                    }
                });
                camera.startPreview();
            } catch (Exception e) {
                camera = null;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    };

    public void outBitmap(Bitmap bitmap,String path){
        try {
            synchronized (bitmap){
                FileOutputStream out =new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Byte转Bitmap
    public Bitmap ByteArray2Bitmap(byte[] data, int width, int height) {
        int Size = width * height;
        int[] rgba = new int[Size];

        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                rgba[i * width + j] = 0xff000000;
            }

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0 , width, 0, 0, width, height);
        return bmp;
    }

    private Camera.Size getBestSupportedSize(List<Camera.Size> sizes, DisplayMetrics metrics) {
        Camera.Size bestSize = sizes.get(0);
        float screenRatio = (float) metrics.widthPixels / (float) metrics.heightPixels;
        if (screenRatio > 1) {
            screenRatio = 1 / screenRatio;
        }

        for (Camera.Size s : sizes) {
            if (Math.abs((s.height / (float) s.width) - screenRatio) < Math.abs(bestSize.height /
                    (float) bestSize.width - screenRatio)) {
                bestSize = s;
            }
        }
        return bestSize;
    }

    private int getCameraOri(int rotation) {
        int degrees = rotation * 90;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        int result;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(camereId, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public void exit(View view){
        finish();
    }
}
