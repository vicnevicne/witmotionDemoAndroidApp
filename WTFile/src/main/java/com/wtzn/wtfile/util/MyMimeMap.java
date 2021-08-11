package com.wtzn.wtfile.util;

import java.util.HashMap;

/**
 * Created by Kevin on 2020/6/23.
 */

public class MyMimeMap {
    private static final HashMap<String, String> mapSimple = new HashMap<>();
    /**
     *  常用"文件扩展名—MIME类型"匹配表。
     *  注意，此表并不全，也并不是唯一的，就像有人喜欢用浏览器打开TXT一样，你可以根据自己的爱好自定义。
     */
    public static HashMap<String, String> getMimeMap() {
        if (mapSimple.size() == 0) {
            mapSimple.put(".3gp", "video/3gpp");
            mapSimple.put(".apk", "application/vnd.android.package-archive");
            mapSimple.put(".asf", "video/x-ms-asf");
            mapSimple.put(".avi", "video/x-msvideo");
            mapSimple.put(".bin", "application/octet-stream");
            mapSimple.put(".bmp", "image/bmp");
            mapSimple.put(".c", "text/plain");
            mapSimple.put(".chm", "application/x-chm");
            mapSimple.put(".class", "application/octet-stream");
            mapSimple.put(".conf", "text/plain");
            mapSimple.put(".cpp", "text/plain");
            mapSimple.put(".doc", "application/msword");
            mapSimple.put(".docx", "application/msword");
            mapSimple.put(".exe", "application/octet-stream");
            mapSimple.put(".gif", "image/gif");
            mapSimple.put(".gtar", "application/x-gtar");
            mapSimple.put(".gz", "application/x-gzip");
            mapSimple.put(".h", "text/plain");
            mapSimple.put(".htm", "text/html");
            mapSimple.put(".html", "text/html");
            mapSimple.put(".jar", "application/java-archive");
            mapSimple.put(".java", "text/plain");
            mapSimple.put(".jpeg", "image/jpeg");
            mapSimple.put(".jpg", "image/jpeg");
            mapSimple.put(".js", "application/x-javascript");
            mapSimple.put(".log", "text/plain");
            mapSimple.put(".m3u", "audio/x-mpegurl");
            mapSimple.put(".m4a", "audio/mp4a-latm");
            mapSimple.put(".m4b", "audio/mp4a-latm");
            mapSimple.put(".m4p", "audio/mp4a-latm");
            mapSimple.put(".m4u", "video/vnd.mpegurl");
            mapSimple.put(".m4v", "video/x-m4v");
            mapSimple.put(".mov", "video/quicktime");
            mapSimple.put(".mp2", "audio/x-mpeg");
            mapSimple.put(".mp3", "audio/x-mpeg");
            mapSimple.put(".mp4", "video/mp4");
            mapSimple.put(".mpc", "application/vnd.mpohun.certificate");
            mapSimple.put(".mpe", "video/mpeg");
            mapSimple.put(".mpeg", "video/mpeg");
            mapSimple.put(".mpg", "video/mpeg");
            mapSimple.put(".mpg4", "video/mp4");
            mapSimple.put(".mpga", "audio/mpeg");
            mapSimple.put(".msg", "application/vnd.ms-outlook");
            mapSimple.put(".ogg", "audio/ogg");
            mapSimple.put(".pdf", "application/pdf");
            mapSimple.put(".png", "image/png");
            mapSimple.put(".pps", "application/vnd.ms-powerpoint");
            mapSimple.put(".ppt", "application/vnd.ms-powerpoint");
            mapSimple.put(".pptx", "application/vnd.ms-powerpoint");
            mapSimple.put(".prop", "text/plain");
            mapSimple.put(".rar", "application/x-rar-compressed");
            mapSimple.put(".rc", "text/plain");
            mapSimple.put(".rmvb", "audio/x-pn-realaudio");
            mapSimple.put(".rtf", "application/rtf");
            mapSimple.put(".sh", "text/plain");
            mapSimple.put(".tar", "application/x-tar");
            mapSimple.put(".tgz", "application/x-compressed");
            mapSimple.put(".txt", "text/plain");
            mapSimple.put(".wav", "audio/x-wav");
            mapSimple.put(".wma", "audio/x-ms-wma");
            mapSimple.put(".wmv", "audio/x-ms-wmv");
            mapSimple.put(".wps", "application/vnd.ms-works");
            mapSimple.put(".xml", "text/plain");
            mapSimple.put(".xls", "application/vnd.ms-excel");
            mapSimple.put(".xlsx", "application/vnd.ms-excel");
            mapSimple.put(".z", "application/x-compress");
            mapSimple.put(".zip", "application/zip");
            mapSimple.put("", "*/*");
        }
        return mapSimple;
    }
}
