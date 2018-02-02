package com.jd.ecc.autoconf.core;

import com.jd.ecc.autoconf.annotation.*;
import com.jd.ecc.autoconf.util.Common;
import com.jd.ecc.autoconf.zk.ConfInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注解与配置文件解析类
 * Created by wangwenhao on 2018/1/10.
 */
public class Load {

    protected static final Logger log = LoggerFactory.getLogger(Load.class);

    private Load(){}

    private static final Map<String, AutoConfClass> confClazzMap = new HashMap<String, AutoConfClass>();

    /**
     * 初始化方法
     * @param sapceName 扫描的包
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static void initScan(String sapceName) throws ClassNotFoundException, IOException {
        ClasspathPackageScanner scan = new ClasspathPackageScanner(sapceName);
        List<String> list = scan.getFullyQualifiedClassNameList();
        for (String str : list) {
            AutoConfClass acc = new AutoConfClass();
            acc.setClassPath(str);
            Class cs = Class.forName(str);
            acc.setClassName(cs.getSimpleName());
            AutoConfFile acf = (AutoConfFile) cs.getAnnotation(AutoConfFile.class);
            if (acf == null) {
                continue;
            }
            AutoConf c = new AutoConf(cs);
            List<AutoConfField> autoConfFieldList = c.init();//获取泛型中类里面的注解
            acc.setConfClassList(autoConfFieldList);
            confClazzMap.put(acf.filename(), acc);
        }
    }

    /**
     * 设置注解变量
     * @param fileName
     * @param confMap
     * @param obj
     * @throws IllegalAccessException
     */
    public static void setConf(String fileName, Map<String, Object> confMap, Object obj) throws IllegalAccessException {
        AutoConfClass acc = confClazzMap.get(fileName);
        if(acc == null){
            return;
        }
        List<AutoConfField> autoConfFieldList = acc.getConfClassList();
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
                if(!(type == int.class || type == Integer.class || type == long.class || type == Long.class || type == float.class || type == Float.class || type == double.class || type == Double.class)){
                    field.set(obj, value);
                }
            }
        }
    }

    /**
     * 设置注解变量
     * @param fileName
     * @param confMap
     * @param ci
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static void setConf(String fileName, Map<String, Object> confMap, ConfInstance ci, boolean isDelete) throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        String tagName = Manager.getTagName(fileName);
        AutoConfClass acc = confClazzMap.get(tagName);
        if(acc == null){
            return;
        }
        List<AutoConfField> autoConfFieldList = acc.getConfClassList();
        Object obj = ci.getConfInstance(acc.getClassName());//为spring特有
        if(isDelete){//如果是删除事件
            if(ci.afterDataChange(fileName, obj, isDelete)){
                log.info("开发者删除{}事件处理成功", fileName);
            } else {
                log.info("开发者删除{}事件处理失败", fileName);
            }
            return;
        }
        for(AutoConfField acf : autoConfFieldList){
            Object value = confMap.get(acf.getMeta().key());
            Class type = acf.getType();
            Field field = acf.getField();

            String filedName = field.getName();
            filedName = filedName.substring(0, 1).toUpperCase() + filedName.substring(1);
            Class<?> threadClazz = Class.forName(acc.getClassPath());
            Method method1 = threadClazz.getMethod("set" + filedName, type);

            if(value != null){
                if(type == int.class || type == Integer.class){
                    method1.invoke(obj, Integer.valueOf(String.valueOf(value)));
                } else if(type == long.class || type == Long.class){
                    method1.invoke(obj, Long.valueOf(String.valueOf(value)));
                } else if(type == float.class || type == Float.class){
                    method1.invoke(obj, Float.valueOf(String.valueOf(value)));
                } else if(type == double.class || type == Double.class){
                    method1.invoke(obj, Double.valueOf(String.valueOf(value)));
                } else {
                    method1.invoke(obj, String.valueOf(value));
                }
            } else {//此处是为了防止用户真的传null，目的是为了替换旧的参数值
                if(!(type == int.class || type == Integer.class || type == long.class || type == Long.class || type == float.class || type == Float.class || type == double.class || type == Double.class)){
                    method1.invoke(obj, String.valueOf(value));
                }
            }
        }

        if(ci.afterDataChange(fileName, obj, isDelete)){
            log.info("开发者修改{}事件处理成功", fileName);
        } else {
            log.info("开发者修改{}事件处理失败", fileName);
        }
    }

    /**
     * 设置注解变量
     * @param fileName
     * @param confMap
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static void setConf(String fileName, Map<String, Object> confMap) throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        String tagName = Manager.getTagName(fileName);
        AutoConfClass acc = confClazzMap.get(tagName);
        if(acc == null){
            return;
        }
        List<AutoConfField> autoConfFieldList = acc.getConfClassList();
        Class<?> threadClazz = Class.forName(acc.getClassPath());
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
                if(!(type == int.class || type == Integer.class || type == long.class || type == Long.class || type == float.class || type == Float.class || type == double.class || type == Double.class)){
                    field.set(obj, value);
                }
            }
        }
    }

}
