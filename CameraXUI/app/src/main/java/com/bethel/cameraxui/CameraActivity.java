package com.bethel.cameraxui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bethel.cameraxui.utils.PictureUtils;
import com.bethel.cameraxui.utils.RotateSensorHelper;
import com.google.android.cameraview.CameraView;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String KEY_PICTURE_PATH = "key_picture_path";

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };
    private int mCurrentFlash;

    private ImageView ivFlashLight;
    private  ImageView ivCameraClose;
    private  ImageView ivTakePhoto;
    private CameraView mCameraView;

    private RotateSensorHelper mSensorHelper;

    /**
        @Permission({CAMERA, STORAGE})
    */
    public static void open(@NonNull Activity activity, int requestCode) {
        activity.startActivityForResult(new Intent(activity, CameraActivity.class), requestCode);
    }

    /**
        @Permission({CAMERA, STORAGE})
    */
    public static void open(@NonNull Fragment fragment, int requestCode) {
        fragment.startActivityForResult(new Intent(fragment.getContext(), CameraActivity.class), requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_layout);
        mCameraView = findViewById(R.id.camera_view);
        ivFlashLight = findViewById(R.id.iv_flash_light);
        ivCameraClose = findViewById(R.id.iv_camera_close);
        ivTakePhoto = findViewById(R.id.iv_take_photo);
        ivFlashLight.setOnClickListener(this);
        ivCameraClose.setOnClickListener(this);
        ivTakePhoto.setOnClickListener(this);

        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }

        List<View> views = new ArrayList<>();
        views.add(ivFlashLight);
        views.add(ivCameraClose);
        views.add(ivTakePhoto);
        mSensorHelper = new RotateSensorHelper(this, views);
    }


    /**
     * 拍照的回调
     */
    private CameraView.Callback mCallback = new CameraView.Callback() {
        @Override
        public void onCameraOpened(CameraView cameraView) {
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
        }

        @Override
        public void onPictureTaken(final CameraView cameraView, final byte[] data) {
            handlePictureTaken(data);
        }
    };

    // @IOThread
    private void handlePictureTaken(final byte[] data) {
        new Thread(){
            @Override
            public void run() {
                super.run();
                final String picPath = PictureUtils.handleOnPictureTaken(getApplication(), data);
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!TextUtils.isEmpty(picPath)) {
                                handlePictureResult(picPath);
                            } else {
                                Toast.makeText(getApplication(), " 图片保存失败！ ", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        }.start();
    }

    private void handlePictureResult(String imgPath) {
        setResult(RESULT_OK, new Intent().putExtra(KEY_PICTURE_PATH, imgPath));
        finish();
    }


    @Override
    public void onClick(View view) {
        if (!checkClickAble()) return;

        switch (view.getId()) {
            case R.id.iv_camera_close:
                finish();
                break;
            case R.id.iv_flash_light:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    ivFlashLight.setImageResource(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                break;
            case R.id.iv_take_photo:
                if (mCameraView != null) {
                    mCameraView.takePicture();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView != null) {
            mCameraView.start();
        }
    }

    @Override
    protected void onPause() {
        if (mCameraView != null) {
            mCameraView.stop();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (isFinishing()) {
            onRelease();
        }
        super.onStop();
    }

    /**
     * 资源释放
     */
    protected void onRelease() {
        mSensorHelper.recycle();
    }

    private static long lastClickTime=0;
    private static boolean checkClickAble() {
        final long time = System.currentTimeMillis();
        if (time - lastClickTime > 500) {
            lastClickTime = time;
            return true;
        }
        return false;
    }
}
