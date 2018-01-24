package com.jd.ecc.autoconf.entity;

/**
 * Created by wangwenhao on 2018/1/22.
 */
public class ZKProxyResult {
    private boolean success; //是否成功
    private Object payload;// 数据
    private String failMsg;//异常字符

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getFailMsg() {
        return failMsg;
    }

    public void setFailMsg(String failMsg) {
        this.failMsg = failMsg;
    }
}
