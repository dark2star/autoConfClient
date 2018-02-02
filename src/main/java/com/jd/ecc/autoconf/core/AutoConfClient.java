package com.jd.ecc.autoconf.core;

import com.jd.ecc.autoconf.util.Common;
import com.jd.ecc.autoconf.util.ZKProxyUtil;
import com.jd.ecc.autoconf.zk.ZnodeEvent;
import com.jd.ecc.middleware.zk.client.AbstractZkClientListner;
import com.jd.ecc.middleware.zk.client.ZkWebSocketClient;
import com.jd.ecc.middleware.zk.common.module.vo.common.ZkEventEnum;
import com.jd.ecc.middleware.zk.common.module.vo.resp.CommandResp;
import com.jd.ecc.middleware.zk.common.module.vo.resp.ZkDataVO;
import com.jd.ecc.middleware.zk.common.module.vo.resp.ZkEventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

/**
 * 配置中心客户端 主类
 * Created by wangwenhao on 2018/1/29.
 */
public class AutoConfClient extends AbstractZkClientListner {

    protected static final Logger log = LoggerFactory.getLogger(AutoConfClient.class);

    public Manager manager;
    private static AutoConfClient autoConfClient = null;
    private ZkWebSocketClient zkWebSocketClient = null;
    private static boolean isLogin = false;//是否登录成功
    private static boolean isNeedConnnet = true;//是否需要尝试连接zk proxy，当前只有当登录返回失败时为false
    private static int connnetCloseCount = 0; // = OnClose方法被调用的次数
    private String host;
    private String path;
    private String key;
    private int getConfTime;

    private AutoConfClient(){}
    /**
     * 创建对象
     * @throws Exception
     */
    public static synchronized AutoConfClient buildAutoConfClient(String packageName) throws Exception {
        if(autoConfClient != null){//保证只初始化一次
            return autoConfClient;
        }
        //初始化
        autoConfClient = new AutoConfClient();
        //初始化扫描
        log.info("初始化扫描packageName={}", packageName);
        Load.initScan(packageName);
        return autoConfClient;
    }

    /**
     * 创建新连接，并开始连接
     * @return
     */
    private ZkWebSocketClient getZkWebSocketClientConnect(){
        try {
            log.info("尝试连接zk proxy");
            zkWebSocketClient = new ZkWebSocketClient(host, key, this, path);
            zkWebSocketClient.connect();
        } catch (URISyntaxException e) {
            log.info("连接zk proxy异常", e);
        }
        return zkWebSocketClient;
    }

    /**
     * 初始化连接，并使用单线程实现定时更新配置（定时更新属于补偿机制）
     * @param host
     * @param path
     * @param key
     * @param getConfTime
     * @param znodeEvent
     */
    public void doJob(final String host, final String path, final String key, final int getConfTime, ZnodeEvent znodeEvent){
        this.host = host;
        this.path = path;
        this.key = key;
        this.getConfTime = getConfTime;
        zkWebSocketClient = getZkWebSocketClientConnect();
        this.manager = new Manager(znodeEvent);

        new Thread(){
            @Override
            public void run(){
                while (true){
                    try{
                        if(isLogin) {
                            ZKProxyUtil.children(path, zkWebSocketClient);
                        }
                        Thread.sleep(getConfTime);
                    } catch (Exception e) {
                        log.error("获取最新配置时异常", e);
                    }
                }
            }
        }.start();
    }

    /**
     * 实现获取子节点事件
     * @param commandResp
     * @param list
     */
    @Override
    public void onGetChildren(CommandResp commandResp, List<ZkDataVO> list) {
        log.info("获取子节点");
        String currentPath = "";
        if(list == null || list.size() < 1){
            return;
        }
        Iterator<ZkDataVO> it = list.iterator();
        while (it.hasNext()) {
            ZkDataVO zkDataVO = it.next();
            String name = zkDataVO.getName();//name中包含了path信息
            currentPath = manager.getPathByFullName(name);
            log.info("name={},version={},content={}", zkDataVO.getName(), zkDataVO.getVersion(), new String(zkDataVO.getData()));
            if(manager.getNameByFullName(name).startsWith(Common.PLATFORMFLOWERFLAG)){//继续跟踪租户目录
                //发送新的获取子节点请求
                ZKProxyUtil.children(name, zkWebSocketClient);
                it.remove();
            }
        }
        if(!manager.updateAllConf(currentPath, list)){
            log.error("定时全量更新配置失败currentPath={}", currentPath);
        }
    }

    /**
     * 实现监听增删改事件
     * @param commandResp
     * @param zkEventVO
     */
    @Override
    public void onEvent(CommandResp commandResp, ZkEventVO zkEventVO) {
        log.info("监听到配置文件变动");
        if(commandResp.isSuccess() && zkEventVO != null){
            String name = zkEventVO.getName();
            int version = zkEventVO.getVersion();
            byte[] bytes = zkEventVO.getData();
            String content = null;
            if(bytes != null && bytes.length > 0){
                content = new String(bytes);
            }
            log.info("name={},version={},content={}", zkEventVO.getName(), zkEventVO.getVersion(), new String(zkEventVO.getData()));
            if(zkEventVO.getEvent() == ZkEventEnum.NODE_ADDED){//新增
                manager.addConf(name, content, version);
            } else if(zkEventVO.getEvent() == ZkEventEnum.NODE_UPDATED){//更新
                manager.updateConf(name, content, version);
            } else if(zkEventVO.getEvent() == ZkEventEnum.NODE_REMOVED){//删除
                manager.delConf(name, version);
            }
        } else {
            log.error("onEvent异常", commandResp.getFailMsg());
        }
    }

    /**
     * 连接成功并使用key登录后触发
     * @param resp
     */
    @Override
    public void onLogin(CommandResp resp) {
        isLogin = resp.isSuccess();
        isNeedConnnet = isLogin;
        log.info("登录回调isSuccess={},failMsg={}", resp.isSuccess(), resp.getFailMsg());
        if(isLogin){
            //登录成功后开始获取所有配置
            ZKProxyUtil.children(path, zkWebSocketClient);
        }
    }

    /**
     * 当连接被关闭后触发
     * @param code
     * @param reason
     * @param remote
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.error("连接关闭code={},reason={},remote={}",code, reason, remote);
        if(!zkWebSocketClient.isClosed()){
            zkWebSocketClient.close();
        }
        if(isNeedConnnet){
            zkWebSocketClient = getZkWebSocketClientConnect();
        }
        //加载本地配置(当连接首次被关闭时触发加载本地配置操作，只加载一次即可),判断条件为：从未成功连接zk且第一次连接被关闭
        if(++connnetCloseCount == 1 && !isLogin){
            log.info("连接zk失败，尝试加载本地配置");
            manager.loadFromLocal();
        }
        isLogin = false;//重置登录状态为未登录
    }

    /**
     * 有异常时触发
     * @param e
     */
    @Override
    public void onError(Exception e) {
        log.error("连接异常", e);
    }

    /**
     * 实现自己发起的创建或保存事件
     * @param commandResp
     * @param zkDataVO
     */
    @Override
    public void onSave(CommandResp commandResp, ZkDataVO zkDataVO) {

    }

    /**
     * 实现自己发起的删除事件
     * @param commandResp
     * @param zkDataVO
     */
    @Override
    public void onDelete(CommandResp commandResp, ZkDataVO zkDataVO) {

    }

    /**
     * 实现获取节点内容事件
     * @param commandResp
     * @param zkDataVO
     */
    @Override
    public void onGetData(CommandResp commandResp, ZkDataVO zkDataVO) {

    }

}
