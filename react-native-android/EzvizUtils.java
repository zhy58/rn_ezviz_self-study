package com.geer2.dakuai.lib.ezviz;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.videogo.exception.InnerException;
import com.videogo.openapi.EZOpenSDK;
import com.videogo.util.LogUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by zhy on Nov 2, 2018.
 */
public class EzvizUtils {

    public static EZOpenSDK getOpenSDK() { return EZOpenSDK.getInstance(); }

    public static void saveCapturePictrue(String filePath, Bitmap bitmap) throws InnerException {
        if (TextUtils.isEmpty(filePath)){
            LogUtil.d("EZUtils","saveCapturePictrue file is null");
            return;
        }
        File filepath = new File(filePath);
        File parent = filepath.getParentFile();
        if (parent == null || !parent.exists() || parent.isFile()) {
            parent.mkdirs();
        }
        FileOutputStream out = null;
        try {
            // 保存原图

            if (!TextUtils.isEmpty(filePath)) {
                out = new FileOutputStream(filepath);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                //out.write(tempBuf, 0, size);
                out.flush();
                out.close();
                out = null;
            }


        } catch (FileNotFoundException e) {
            //throw new InnerException(e.getLocalizedMessage());
            e.printStackTrace();
        } catch (IOException e) {
            //throw new InnerException(e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}
