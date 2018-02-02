package com.jd.ecc.autoconf.core;

import com.jd.ecc.autoconf.util.FileUtil;
import com.jd.ecc.autoconf.util.PropertiesUtil;
import com.jd.ecc.autoconf.util.StringUtil;
import com.jd.ecc.autoconf.zk.ConfInstance;
import com.jd.ecc.autoconf.zk.ZnodeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 适配spring的配置中心事件处理类
 * Created by wangwenhao on 2018/1/16.
 */
public class SpringZnodeEvent implements ZnodeEvent {

    private final static String subProperties = ".properties";
    protected static final Logger log = LoggerFactory.getLogger(SpringZnodeEvent.class);
    private final static String resourcePath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    private ConfInstance confInstance;

    public SpringZnodeEvent(ConfInstance confInstance){
        this.confInstance = confInstance;
    }
    /**
     * 当启动连接zk失败时触发
     *
     * @return
     * @throws Exception
     */
    @Override
    public boolean zkConnectFail() {
        List<String> resourcePathList = FileUtil.getAllFileNameInFold(resourcePath);
        for(String resourcePath : resourcePathList){
            try {
                if(resourcePath.contains(subProperties)){
                    String resourceName = new File(resourcePath).getName();//获取资源名称
                    Map<String, Object> returnMap = PropertiesUtil.GetAllProperties(resourcePath);
                    Load.setConf(resourceName, returnMap, confInstance, false);
                }
            } catch (Exception e) {
                log.error("加载本地配置" + resourcePath + "失败",e);
                return false;
            }
        }
        return true;
    }

    /**
     * 初始化znode事件
     *
     * @param map key：znode的url, value：znode的内容
     * @return 是否操作成功
     * @throws Exception
     */
    @Override
    public boolean init(Map<String, Object> map) {
        return true;
    }

    /**
     * 添加znode事件
     *
     * @param path    znode的url
     * @param content znode的内容
     * @return 是否操作成功
     */
    @Override
    public boolean addNode(String path, Object content) {
        try{
            String[] pathSplit = path.split("/");
            String fileName = pathSplit[pathSplit.length-1];
            String localfile = resourcePath + "/"+ fileName;
            FileUtil.write( localfile,StringUtil.trans(String.valueOf(content)));;
            Map<String, Object> returnMap = PropertiesUtil.GetAllProperties(localfile);
            Load.setConf(fileName, returnMap, confInstance, false);
        } catch (Exception e){
            log.error("添加配置失败，path={},content={}", path, content, e);
            return false;
        }
        return true;
    }

    /**
     * 删除znode事件
     *
     * @param path znode的url
     * @return 是否操作成功
     */
    @Override
    public boolean delNode(String path) {
        try{
            String[] pathSplit = path.split("/");
            String fileName = pathSplit[pathSplit.length-1];
            FileUtil.deleteEveryThing(resourcePath + "/"+ pathSplit[pathSplit.length-1]);
            Load.setConf(fileName, null, confInstance, true);
        } catch (Exception e){
            log.error("删除配置失败，path={}", path, e);
            return false;
        }
        return true;
    }

    /**
     * 修改znode事件
     *
     * @param path    znode的url
     * @param content znode的内容
     * @return 是否操作成功
     */
    @Override
    public boolean updateNode(String path, Object content) {
        try {
            String[] pathSplit = path.split("/");
            String fileName = pathSplit[pathSplit.length-1];
            String localfile = resourcePath + "/"+ fileName;
            FileUtil.write( localfile, StringUtil.trans(String.valueOf(content)));
            Map<String, Object> returnMap = PropertiesUtil.GetAllProperties(localfile);
            Load.setConf(fileName, returnMap, confInstance,false);//此处需解决从spring中获取对象
        } catch (Exception e){
            log.error("更新配置失败，path={},content={}", path, content, e);
            return false;
        }
        return true;
    }
}
