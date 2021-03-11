package com.bethel.cameraxui.utils;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @ProjectName: CameraXUI
 * @Package: com.bethel.cameraxui.utils
 * @ClassName: PictureUtils
 * @Description: java类作用描述
 * @Author: Bethel
 * @CreateDate: 2021-03-11 23:07
 * @UpdateUser: 更新者：
 * @UpdateDate: 2021-03-11 23:07
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class PictureUtils {
    //==========拍照===========//

    public static final String JPEG = ".jpeg";

    /**
     * 处理拍照的回调
     *
     * @param data
     * @return
     */
    public static String handleOnPictureTaken(Context context, byte[] data) {
        return handleOnPictureTaken(context, data, JPEG);
    }

    /**
     * 处理拍照的回调
     *
     * @param data
     * @return
     */
    public static String handleOnPictureTaken(Context context, byte[] data, String fileSuffix) {
        String picPath = getDiskCacheDir(context) + "/images/" + System.currentTimeMillis() + fileSuffix;
        boolean result = writeFileFromBytesByStream(picPath, data);
        return result ? picPath : "";
    }

    /**
     * 将字节数组写入文件
     *
     * @param filePath 文件路径
     * @param bytes    字节数组
     * @return {@code true}: 写入成功<br>{@code false}: 写入失败
     */
    public static boolean writeFileFromBytesByStream(final String filePath, final byte[] bytes) {
        return writeFileFromBytesByStream(getFileByPath(filePath), bytes, false);
    }

    private static File getFileByPath(final String filePath) {
        return isSpace(filePath) ? null : new File(filePath);
    }

    /**
     * 将字节数组写入文件
     *
     * @param file   文件
     * @param bytes  字节数组
     * @param append 是否追加在文件末
     * @return {@code true}: 写入成功<br>{@code false}: 写入失败
     */
    public static boolean writeFileFromBytesByStream(final File file,
                                                     final byte[] bytes,
                                                     final boolean append) {
        if (bytes == null || !createOrExistsFile(file)) {
            return false;
        }
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file, append));
            bos.write(bytes);
            bos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeIO(bos);
        }
    }

    /**
     * 关闭 IO
     *
     * @param closeables closeables
     */
    public static void closeIO(final Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean createOrExistsFile(final File file) {
        if (file == null) {
            return false;
        }
        if (file.exists()) {
            return file.isFile();
        }
        if (!createOrExistsDir(file.getParentFile())) {
            return false;
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }


    private static boolean isSpace(final String s) {
        if (s == null) {
            return true;
        }
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    /**
     * 获取磁盘的缓存目录
     *
     * @return SD卡不存在: /data/data/com.xxx.xxx/cache;<br>
     * 存在: /storage/emulated/0/Android/data/com.xxx.xxx/cache;
     */
    public static String getDiskCacheDir(Context context) {
        return isSDCardExist() && context.getExternalCacheDir() != null ? context.getExternalCacheDir().getPath() : context.getCacheDir().getPath();
    }

    /**
     * SD卡是否存在
     *
     * @return
     */
    public static boolean isSDCardExist() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable();
    }

}
