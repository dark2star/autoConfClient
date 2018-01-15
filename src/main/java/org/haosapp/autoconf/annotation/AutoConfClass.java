package org.haosapp.autoconf.annotation;

import java.util.List;

/**
 * 配置包装类
 * Created by wangwenhao on 2018/1/10.
 */
public class AutoConfClass {

    /**
     * 类名
     */
    private String className;

    /**
     * 类路径
     */
    private String classPath;

    /**
     * 注解属性列表
     */
    private List<AutoConfField> confClassList;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public List<AutoConfField> getConfClassList() {
        return confClassList;
    }

    public void setConfClassList(List<AutoConfField> confClassList) {
        this.confClassList = confClassList;
    }
}
