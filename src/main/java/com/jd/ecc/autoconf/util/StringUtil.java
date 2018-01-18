package com.jd.ecc.autoconf.util;

import java.net.URL;

/**
 * 字符串工具类
 * Created by wangwenhao on 2018/1/9.
 */
public class StringUtil {
    private StringUtil() {

    }

    /**
     * "file:/home/whf/cn/fh" -> "/home/whf/cn/fh"
     * "jar:file:/home/whf/foo.jar!cn/fh" -> "/home/whf/foo.jar"
     */
    public static String getRootPath(URL url) {
        String fileUrl = url.getFile();
        int pos = fileUrl.indexOf('!');

        if (-1 == pos) {
            return fileUrl;
        }

        return fileUrl.substring(5, pos);
    }

    /**
     * "cn.fh.lightning" -> "cn/fh/lightning"
     * @param name
     * @return
     */
    public static String dotToSplash(String name) {
        return name.replaceAll("\\.", "/");
    }

    /**
     * "Apple.class" -> "Apple"
     */
    public static String trimExtension(String name) {
        int pos = name.indexOf('.');
        if (-1 != pos) {
            return name.substring(0, pos);
        }

        return name;
    }

    /**
     * /application/home -> /home
     * @param uri
     * @return
     */
    public static String trimURI(String uri) {
        String trimmed = uri.substring(1);
        int splashIndex = trimmed.indexOf('/');

        return trimmed.substring(splashIndex);
    }

    /**
     * 兼容disconf服务端做的字符转换
     * @param text
     * @return
     */
    public static String trans(String text){
        if(text.startsWith("\"")){
            text = text.substring(1);
        }
        if(text.endsWith("\"")){
            text = text.substring(0,text.length() - 1);
        }
        return text;
    }
}