package org.haosapp.autoconf;

import org.haosapp.autoconf.annotation.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangwenhao on 2018/1/10.
 */
public class LoadTest {

    private LoadTest(){}

    private static final Map<String, List<AutoConfField>> confClassMap = new HashMap<String, List<AutoConfField>>();
    private static final Map<String, AutoConfClass> confClazzMap = new HashMap<String, AutoConfClass>();

    public static void initScan(String sapceName) throws ClassNotFoundException, IOException {
        ClasspathPackageScanner scan = new ClasspathPackageScanner(sapceName);
        List<String> list = scan.getFullyQualifiedClassNameList();
        for (String str : list) {
            Class cs = Class.forName(str);
            AutoConfFile acf = (AutoConfFile) cs.getAnnotation(AutoConfFile.class);
            if (acf == null) {
                continue;
            }
            Parent c = new Parent(cs);
            List<AutoConfField> autoConfFieldList = c.init();//获取泛型中类里面的注解
            confClassMap.put(acf.filename(), autoConfFieldList);
        }
    }


    public static void initScanTest(String sapceName) throws ClassNotFoundException, IOException {
        ClasspathPackageScanner scan = new ClasspathPackageScanner(sapceName);
        List<String> list = scan.getFullyQualifiedClassNameList();
        for (String str : list) {
            AutoConfClass acc = new AutoConfClass();
            acc.setClassName(str);
            Class cs = Class.forName(str);
            AutoConfFile acf = (AutoConfFile) cs.getAnnotation(AutoConfFile.class);
            if (acf == null) {
                continue;
            }
            Parent c = new Parent(cs);
            List<AutoConfField> autoConfFieldList = c.init();//获取泛型中类里面的注解
            acc.setConfClassList(autoConfFieldList);
            confClazzMap.put(acf.filename(), acc);
        }
    }

    public static void setConf(String fileName, Map<String, Object> confMap, Object obj) throws IllegalAccessException {
        List<AutoConfField> autoConfFieldList = confClassMap.get(fileName);
        for(AutoConfField acf : autoConfFieldList){
            Object value = confMap.get(acf.getMeta().key());
            acf.getField().set(obj, value);
        }
    }

    public static void setConfTest(String fileName, Map<String, Object> confMap) throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        AutoConfClass acc = confClazzMap.get(fileName);
        if(acc == null){
            return;
        }
        List<AutoConfField> autoConfFieldList = acc.getConfClassList();
        Class<?> threadClazz = Class.forName(acc.getClassName());
        Method method1 = threadClazz.getMethod("getInstance");//必须使用此方法名初始化，这是约定
        Object obj = method1.invoke(null);
        for(AutoConfField acf : autoConfFieldList){
            Object value = confMap.get(acf.getMeta().key());
            Class type = acf.getType();
            Field field = acf.getField();
            if(value != null){
                if(type == int.class || type == Integer.class){
                    field.set(obj, Integer.valueOf(String.valueOf(value)));
                } else if(type == long.class || type == Long.class){
                    field.set(obj, Long.valueOf(String.valueOf(value)));
                } else if(type == float.class || type == Float.class){
                    field.set(obj, Float.valueOf(String.valueOf(value)));
                } else if(type == double.class || type == Double.class){
                    field.set(obj, Double.valueOf(String.valueOf(value)));
                } else {
                    field.set(obj, value);
                }
            } else {//此处是为了防止用户真的传null，目的是为了替换旧的参数值
                field.set(obj, value);
            }
        }
    }


}
