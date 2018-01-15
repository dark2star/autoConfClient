package org.haosapp.autoconf.zk;

/**
 * 与sprig等bean容器交互接口
 * Created by wangwenhao on 2018/1/11.
 */
public interface ConfInstance {

    /**
     * 根据bean名称获取bean
     * @param beanName
     * @return
     */
    Object getConfInstance(String beanName);

    /**
     * 在设置新属性后的回调方法
     * @param fileName
     * @param obj
     * @param isDelete
     * @return
     */
    boolean afterDataChange(String fileName, Object obj, boolean isDelete);
}
