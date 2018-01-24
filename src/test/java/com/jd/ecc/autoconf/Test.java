package com.jd.ecc.autoconf;

import com.jd.ecc.autoconf.http.AutoConfClient;
import com.jd.ecc.autoconf.http.DefaultZnodeEvent;
import com.jd.ecc.autoconf.util.Common;

/**
 * Created by wangwenhao on 2018/1/10.
 */
public class Test {
    private static final String platformFlag = "-pid-";
    public static void main(String[] a) throws Exception {
        /*Class<?> threadClazz = Class.forName("org.haosapp.autoconf.RedisConf");
        Method method1 = threadClazz.getMethod("getInstance");
        Object o = method1.invoke(null);

        Method method = threadClazz.getMethod("setHost", String.class);
        method.invoke(o, "126.0.0.1");
        System.out.println(RedisConf.getInstance().getHost());*/

/*        String fileName = "redis-pid-12222.properties";
        if(fileName.contains(platformFlag)){
            System.out.println("ok");
            String prefix = fileName.substring(0, fileName.indexOf(platformFlag) + platformFlag.length());
            String lastfix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
            System.out.println(prefix + "*"+lastfix);
            System.out.println(fileName.substring(fileName.indexOf(platformFlag) + platformFlag.length(),fileName.lastIndexOf(".")));
        }*/

/*        List<String> list = FileUtil.getAllFileNameInFold("D:\\SwitchHosts");
        for(String str : list){
            str = new File(str).getName();
            System.out.println(str);
        }*/

        String host = "http://192.168.171.124:18902";
        //String host = "http://192.168.171.125:18902";
        String path = "/";
        String key = "test";
        AutoConfClient autoConfClient = AutoConfClient.buildAutoConfClient(new DefaultZnodeEvent(), "com.jd.ecc.autoconf");
        autoConfClient.setWatch(host, path, key, Common.getConfTime);

        /**
         * 模拟其他的线程调用
         */
        new Thread(){
            @Override
            public void run(){
                while (true){
                    System.out.println("用户使用redis配置 Host = " + RedisConf.getInstance().getHost()
                            + ", Port = " + RedisConf.getInstance().getPort()
                            + ", Password = " + RedisConf.getInstance().getPassword());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        Thread.currentThread().sleep(1000000l);
    }
}
