package org.haosapp.autoconf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by wangwenhao on 2018/1/10.
 */
public class Test {
    private static final String platformFlag = "-pid-";
    public static void main(String[] a) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        /*Class<?> threadClazz = Class.forName("org.haosapp.autoconf.RedisConf");
        Method method1 = threadClazz.getMethod("getInstance");
        Object o = method1.invoke(null);

        Method method = threadClazz.getMethod("setHost", String.class);
        method.invoke(o, "126.0.0.1");
        System.out.println(RedisConf.getInstance().getHost());*/

        String fileName = "redis-pid-12222.properties";
        if(fileName.contains(platformFlag)){
            System.out.println("ok");
            String prefix = fileName.substring(0, fileName.indexOf(platformFlag) + platformFlag.length());
            String lastfix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
            System.out.println(prefix + "*"+lastfix);
            System.out.println(fileName.substring(fileName.indexOf(platformFlag) + platformFlag.length(),fileName.lastIndexOf(".")));
        }
    }
}
