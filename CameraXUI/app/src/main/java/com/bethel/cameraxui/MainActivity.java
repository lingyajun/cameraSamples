package com.bethel.cameraxui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

/**
 'com.github.xuexiangjys:CameraView:1.0.3'
 */
public class MainActivity extends AppCompatActivity {
    // 申请相机权限的requestCode
    private static final int PERMISSION_CAMERA_REQUEST_CODE = 10;

    public static final int REQUEST_CODE_OPEN_CAMERA = 1245;

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //  check permission -->  openCamera
                checkPermissionAndCamera();
            }
        });
    }

    private void openCamera() {
        CameraActivity.open(MainActivity.this, REQUEST_CODE_OPEN_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_OPEN_CAMERA && resultCode == RESULT_OK) {
            if (data != null) {
                String imgPath = data.getStringExtra(CameraActivity.KEY_PICTURE_PATH);
                Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    /**
     * 检查权限并拍照。
     * 调用相机前先检查权限。
     */
    private void checkPermissionAndCamera() {
        int hasCameraPermission = ContextCompat.checkSelfPermission(getApplication(),
                Manifest.permission.CAMERA);
        if (hasCameraPermission == PackageManager.PERMISSION_GRANTED) {
            //有调起相机拍照。
            openCamera();
        } else {
            //没有权限，申请权限。
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA_REQUEST_CODE);
        }
    }

    /**
     * 处理权限申请的回调。
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，有调起相机拍照。
                openCamera();
            } else {
                //拒绝权限，弹出提示框。
                Toast.makeText(this," 拍照权限被拒绝 ", Toast.LENGTH_LONG).show();
            }
        }
    }
}
