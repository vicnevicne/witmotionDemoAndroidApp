package com.wtzn.wtfile.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Kevin on 2020/4/10.
 */

public class MyFile {
    FileOutputStream fout;
    public File file;

    public MyFile(String fileName) throws FileNotFoundException {
        file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }
        fout = new FileOutputStream(fileName, false);
    }

    public void write(String str) throws IOException {
        byte[] bytes = str.getBytes();
        fout.write(bytes);
    }

    public void close() throws IOException {
        fout.close();
        fout.flush();
    }

    public void openFile(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uriForFile;
        if (Build.VERSION.SDK_INT > 23) {
            uriForFile = FileProvider.getUriForFile(context, "com.wtzn.fileProvider", file);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);//Temporarily authorize the target file
        }
        else {
            uriForFile = Uri.fromFile(file);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.setDataAndType(uriForFile,getMimeTypeFromFile(file));
        intent.setDataAndType(uriForFile, "text/plain");
        context.startActivity(intent);
    }

    private static String getMimeTypeFromFile(File file) {
        String type = "*/*";
        String fName = file.getName();
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex > 0) {
            String end = fName.substring(dotIndex).toLowerCase(Locale.getDefault());
            HashMap<String, String> map = MyMimeMap.getMimeMap();
            if (!TextUtils.isEmpty(end) && map.containsKey(end)) {
                type = map.get(end);
            }
        }
        return type;
    }
}
