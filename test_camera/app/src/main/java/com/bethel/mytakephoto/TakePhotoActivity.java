package com.bethel.mytakephoto;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * https://www.jianshu.com/p/ee886fc43c35
 */
public class TakePhotoActivity extends AppCompatActivity {
    private static final String TAG = "TakePhotoActivity";

    private final static int CAMERA_REQUEST_CODE = 11;

    // 相机拍照，适配 Android 10
    private final boolean isAndroidQ = Build.VERSION.SDK_INT >= 29;
    // 用于保存拍照图片的uri
    private Uri mCameraUri;

    private File imageFile;

    private static final int REQUEST_CAMERA_PERMISSION = 1000;

    final static String[] permissionArray = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        findViewById(R.id.button_camera).setOnClickListener(v -> openCamera());
        imageView = findViewById(R.id.image_view);
    }

    private void openCamera() {
        if (!checkPermission(REQUEST_CAMERA_PERMISSION)) {
            takePhoto();
        }
    }

    /**
     * 调起相机拍照。
     * 兼容 Android Q
     */
    private void takePhoto() {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 判断是否有相机
        if (captureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            Uri photoUri = null;

            if (isAndroidQ) {
                // 适配android 10
                photoUri = createImageUri();
            } else {
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (photoFile != null) {
//                    mCameraImagePath = photoFile.getAbsolutePath();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        //适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri
                        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                    } else {
                        photoUri = Uri.fromFile(photoFile);
                    }
                    imageFile = photoFile;
                }
            }

            mCameraUri = photoUri;
            if (photoUri != null) {
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(captureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final StringBuilder builder = new StringBuilder("CAMERA # onActivityResult:  ");
        builder.append("requestCode:  ").append(requestCode).append(" ; resultCode: ").append(resultCode);
        Log.i(TAG, builder.toString());
        if (resultCode != Activity.RESULT_OK) return;

        if (CAMERA_REQUEST_CODE == requestCode) {
            Uri result = data == null ? null : data.getData();
            if (result == null) {
                if (isAndroidQ && null != mCameraUri) {
//                    handleImageOnKitKat(mCameraUri);
                    imageView.setImageURI(mCameraUri);
                    return;
                }
                if (null != imageFile) {
                    final long size = imageFile.length();

                    compressImage(imageFile.getAbsolutePath());
                }
                return;
            }

            // null != result
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                handleImageOnKitKat(result);
            } else {
                handleImageBeforeKitKat(result);
            }
        }
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Uri uri) {
        String imagePath = null;
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content: //downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        compressImage(imagePath);
    }

    /**
     * Android4.4以前的处理方式
     *
     * @param uri
     */
    private void handleImageBeforeKitKat(Uri uri) {
        String imagePath = getImagePath(uri, null);
        Log.i(TAG, "imagePath->" + imagePath);
        compressImage(imagePath);
    }

    /**
     * 图片压缩
     *
     * @param filePath
     */
    private void compressImage(final String filePath) {
        Log.i(TAG, "compressImage, filePath->" + filePath);
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);//从路径加载出图片bitmap
        bitmap = rotateBitmap(90, bitmap);//旋转图片 90°
        imageView.setImageBitmap(bitmap);//ImageView显示图片

//        imageView.setImageURI(Uri.parse(filePath));
    }
    /**
     * 旋转Bitmap图片
     *
     * @param degree 旋转的角度
     * @param srcBitmap 需要旋转的图片的Bitmap
     * @return
     */
    private static Bitmap rotateBitmap(float degree, Bitmap srcBitmap) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degree);
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()
                , matrix, true);
        return bitmap;
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
        }
        return path;
    }

    private boolean checkPermission(int requestCode) {
        final ArrayList<String> noPermissionList = new ArrayList<>();
        for (String permission : permissionArray) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                noPermissionList.add(permission);
            }
        }
        final int size = noPermissionList.size();
        if (size > 0) {
            final String[] ps = new String[size];
            noPermissionList.toArray(ps);
            ActivityCompat.requestPermissions(this, ps, requestCode);
            final StringBuilder builder = new StringBuilder("requestPermissions :  ");
            builder.append(ps.length).append(" ; first: ").append(ps[0]);
            Log.i(TAG, builder.toString());
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = true;
        for (int g : grantResults) {
            if (g != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
                break;
            }
        }

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (isGranted) {
                takePhoto();
            } else {
                finish();
            }
        }
    }


    /**
     * 创建图片地址uri,用于保存拍照后的照片 Android 10以后使用这种方法
     */
    private Uri createImageUri() {
        String status = Environment.getExternalStorageState();
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }

    /**
     * 创建保存图片的文件
     */
    private File createImageFile() throws IOException {
        String imageName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())+ ".png";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        File tempFile = new File(storageDir, imageName);
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }
        return tempFile;
    }

}