package com.jd.ecc.autoconf.entity;

/**
 * 配置存储对象
 * Created by wangwenhao on 2018/1/23.
 */
public class ConfData {

    private String name;
    private byte[] data;
    private String content;
    private int version;

    public ConfData(){}

    public ConfData(String name, String content, int version) {
        this.name = name;
        this.content = content;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
