package com.jd.ecc.autoconf.entity;

import java.util.Map;

public class DiffrentMapWraper{

    private Map<String, Integer> oldDiffConfDataMap;
    private Map<String, Integer> newDiffConfDataMap;
    private Map<String, Integer> changeConfDataMap;

    public Map<String, Integer> getOldDiffConfDataMap() {
        return oldDiffConfDataMap;
    }

    public void setOldDiffConfDataMap(Map<String, Integer> oldDiffConfDataMap) {
        this.oldDiffConfDataMap = oldDiffConfDataMap;
    }

    public Map<String, Integer> getNewDiffConfDataMap() {
        return newDiffConfDataMap;
    }

    public void setNewDiffConfDataMap(Map<String, Integer> newDiffConfDataMap) {
        this.newDiffConfDataMap = newDiffConfDataMap;
    }

    public Map<String, Integer> getChangeConfDataMap() {
        return changeConfDataMap;
    }

    public void setChangeConfDataMap(Map<String, Integer> changeConfDataMap) {
        this.changeConfDataMap = changeConfDataMap;
    }
}