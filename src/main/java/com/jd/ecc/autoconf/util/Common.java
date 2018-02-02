package com.jd.ecc.autoconf.util;

/**
 * Created by wangwenhao on 2018/1/22.
 */
public class Common {

    public static final int CONNECTTIMEOUT = 30;
    public static final int READTIMEOUT = 30;
    public static final int GETCONFTIME = 10000;//每隔多长毫秒获取zk上配置，定时轮询用
    public static final String PLATFORMFLOWERFLAG = "pid_";//zk上的租户标志
    public static final String PLATFORMFLAG = "-pid-";//本地存储文件的租户标志
    public static final String PLATFORMSTR = "*";//指定的模糊对象标志
    public static final String ZKCONFNAMESPLIT = "_ins_";//zk上的实例标志
    public static final String lOCALCONFNAMESPLIT = "-ins-";//本地存储文件的租户标志
}
