package org.haosapp.autoconf.zk;

import java.util.Map;

/**
 * znode事件回调接口
 * Created by wangwenhao on 2018/1/9.
 */
public interface ZnodeEvent {

    /**
     * 当启动连接zk失败时触发
     * @return
     * @throws Exception
     */
    boolean zkConnectFail() throws Exception;

    /**
     * 初始化znode事件
     * @param map key：znode的url, value：znode的内容
     * @return 是否操作成功
     * @throws Exception
     */
    boolean init(Map<String, Object> map) throws Exception;

    /**
     * 添加znode事件
     * @param path znode的url
     * @param content znode的内容
     * @return 是否操作成功
     */
    boolean addNode(String path, Object content) throws Exception;

    /**
     * 删除znode事件
     * @param path znode的url
     * @return 是否操作成功
     */
    boolean delNode(String path) throws Exception;

    /**
     * 修改znode事件
     * @param path znode的url
     * @param content znode的内容
     * @return 是否操作成功
     */
    boolean updateNode(String path, Object content) throws Exception;
}
