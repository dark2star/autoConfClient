package org.haosapp.autoconf.annotation;

import org.haosapp.autoconf.annotation.AutoConfField;

import java.util.List;

/**
 * Created by wangwenhao on 2018/1/10.
 */
public class AutoConfClass {

    private String className;

    List<AutoConfField> confClassList;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<AutoConfField> getConfClassList() {
        return confClassList;
    }

    public void setConfClassList(List<AutoConfField> confClassList) {
        this.confClassList = confClassList;
    }
}
