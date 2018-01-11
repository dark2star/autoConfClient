package org.haosapp.autoconf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by wangwenhao on 2018/1/10.
 */
public class Test {
    public static void main(String[] a) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        Class<?> threadClazz = Class.forName("org.haosapp.autoconf.RedisConf");
        Method method1 = threadClazz.getMethod("getInstance");
        Object o = method1.invoke(null);

        Method method = threadClazz.getMethod("setHost", String.class);
        method.invoke(o, "126.0.0.1");
        System.out.println(RedisConf.getInstance().getHost());
    }
}
