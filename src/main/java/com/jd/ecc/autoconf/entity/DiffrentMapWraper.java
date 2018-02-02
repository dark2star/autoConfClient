package com.jd.ecc.autoconf.entity;

import java.util.Map;

/**
 * 包含两个map比较结果的封装类
 */
public class DiffrentMapWraper{

    private Map<String, ConfData> oldDiffConfDataMap;//旧map中特有的元素
    private Map<String, ConfData> newDiffConfDataMap;//新map中特有的元素
    private Map<String, ConfData> changeConfDataMap;//两个map中存在不同的元素

    public Map<String, ConfData> getOldDiffConfDataMap() {
        return oldDiffConfDataMap;
    }

    public void setOldDiffConfDataMap(Map<String, ConfData> oldDiffConfDataMap) {
        this.oldDiffConfDataMap = oldDiffConfDataMap;
    }

    public Map<String, ConfData> getNewDiffConfDataMap() {
        return newDiffConfDataMap;
    }

    public void setNewDiffConfDataMap(Map<String, ConfData> newDiffConfDataMap) {
        this.newDiffConfDataMap = newDiffConfDataMap;
    }

    public Map<String, ConfData> getChangeConfDataMap() {
        return changeConfDataMap;
    }

    public void setChangeConfDataMap(Map<String, ConfData> changeConfDataMap) {
        this.changeConfDataMap = changeConfDataMap;
    }
}