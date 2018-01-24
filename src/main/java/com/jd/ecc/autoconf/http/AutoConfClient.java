package com.jd.ecc.autoconf.http;

import com.jd.ecc.autoconf.entity.ConfData;
import com.jd.ecc.autoconf.entity.DiffrentMapWraper;
import com.jd.ecc.autoconf.util.Common;
import com.jd.ecc.autoconf.util.ZKProxyUtil;
import com.jd.ecc.autoconf.zk.ZnodeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * 实现watch指定目录下的所有znode
 * Created by wangwenhao on 2018/1/9.
 */
public class AutoConfClient {

    protected static final Logger log = LoggerFactory.getLogger(AutoConfClient.class);

    private final Map<String, Integer> oldConfDataMap = new HashMap<String, Integer>();
    private static AutoConfClient autoConfClient = null;
    private static ZnodeEvent znodeEvent;

    private AutoConfClient(){}

    /**
     * 创建对象
     * @param znodeEvent
     * @throws Exception
     */
    public static synchronized AutoConfClient buildAutoConfClient(ZnodeEvent znodeEvent,String packageName) throws Exception {
        if(autoConfClient != null){//保证只初始化一次
            return autoConfClient;
        }
        //初始化
        autoConfClient = new AutoConfClient();
        AutoConfClient.znodeEvent = znodeEvent;
        //初始化扫描
        log.info("初始化扫描packageName={}", packageName);
        Load.initScan(packageName);
        return autoConfClient;
    }

    /**
     * 设置watch
     * @param host
     * @param path
     * @param key
     * @param getConfTime
     */
    public void setWatch(final String host, final String path, final String key,final int getConfTime){

        log.info("开始初始化， host={},path={},key={},getConfTime={}", host, path, key, getConfTime);
        //连接server获取配置
        if(!startJob(host, path, key)){
            //如果未成功连接服务端则应用本地配置
            log.info("未成功连接服务端则应用本地配置");
            znodeEvent.zkConnectFail();
        }
        //实现轮询并对配置的应用
        new Thread(){
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(getConfTime);
                        if(!startJob(host, path, key)){
                            log.error("更新配置失败， host={},path={},key={}", host, path, key);
                        }
                    } catch (Exception e) {
                        log.error("获取最新配置时异常", e);
                    }
                }
            }
        }.start();
        log.info("成功启动autoConf");
    }

    /**
     * 主方法，任务是更新下载配置
     * @param host
     * @param path
     * @param key
     * @return
     */
    private boolean startJob(String host, String path, String key){
        /**
         * 初始化
         */
        ZKProxyUtil zkProxyUtil = new ZKProxyUtil(host, Common.connectTimeout, Common.readTimeout);
        Map<String, Integer> newConfDataMap = zkProxyUtil.getChild(key,path);
        if(newConfDataMap.size() < 1){
            log.error("获取子节点失败，host={},path={}",host,path);
            return false;
        }
        //判断是否是第一次加载，如果是第一次则执行全量下载，如果不是则需判断每个znode的版本是否发生了变动
        DiffrentMapWraper diffrentMapWraper = getDiffrent(oldConfDataMap, newConfDataMap);
        Map<String, Integer> oldDiffConfDataMap = diffrentMapWraper.getOldDiffConfDataMap();
        Map<String, Integer> newDiffConfDataMap = diffrentMapWraper.getNewDiffConfDataMap();
        Map<String, Integer> changeConfDataMap = diffrentMapWraper.getChangeConfDataMap();
        if(newDiffConfDataMap.size() > 0){
            //执行增加
            log.info("执行增加新的配置");
            Iterator<Map.Entry<String, Integer>> entries = newDiffConfDataMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, Integer> entry = entries.next();
                String name = entry.getKey();
                ConfData confData = zkProxyUtil.getData(key, name);
                if(znodeEvent.addNode(name, confData.getContent())){
                    log.info("执行增加新的配置成功");
                } else {
                    log.error("执行增加新的配置失败");
                }
            }
        }
        if(changeConfDataMap.size() > 0){
            //执行修改
            log.info("执行更新配置");
            Iterator<Map.Entry<String, Integer>> entries = changeConfDataMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, Integer> entry = entries.next();
                String name = entry.getKey();
                ConfData confData = zkProxyUtil.getData(key, name);
                if(znodeEvent.updateNode(name, confData.getContent())){
                    log.info("执行更新配置成功");
                } else {
                    log.error("执行更新配置失败");
                }
            }
        }
        if(oldDiffConfDataMap.size() > 0){
            log.info("执行删除配置");
            //执行删除
            Iterator<Map.Entry<String, Integer>> entries = oldDiffConfDataMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, Integer> entry = entries.next();
                String name = entry.getKey();
                if(znodeEvent.delNode(name)){
                    log.info("执行更新配置成功");
                } else {
                    log.error("执行更新配置失败");
                }
            }
        }

        oldConfDataMap.clear();
        oldConfDataMap.putAll(newConfDataMap);
        return true;

    }

    /**
     * 获取zk操作对象，用于扩展
     * @return
     */
    public static AutoConfClient getAutoConfClient(){
        return autoConfClient;
    }

    private static List<String> apendPath(String path, List<String> znodeList) {
        List<String> returnList = new ArrayList<String>();
        for(String znode : znodeList){
            returnList.add(path + "/" + znode);
        }
        return returnList;
    }

    /**
     * 得到两个map的差集
     * @param oldConfDataMap
     * @param newConfDataMap
     * @return
     */
    public static DiffrentMapWraper getDiffrent(Map<String, Integer> oldConfDataMap, Map<String, Integer> newConfDataMap){

        DiffrentMapWraper diffrentMapWraper = new DiffrentMapWraper();
        Map<String,Integer> diffOnLeft = new HashMap<String, Integer>();
        Map<String,Integer> diffOnRight = new HashMap<String, Integer>();
        Map<String,Integer> diffOnValue = new HashMap<String, Integer>();
        Iterator<Map.Entry<String, Integer>> entries = oldConfDataMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Integer> entry = entries.next();
            String key = entry.getKey();
            if(newConfDataMap.containsKey(key)){
                Integer val = newConfDataMap.get(key);
                if(!entry.getValue().equals(val)){
                    diffOnValue.put(key, val);
                }
            } else {
                diffOnLeft.put(key, entry.getValue());
            }
        }

        entries = newConfDataMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Integer> entry = entries.next();
            String key = entry.getKey();
            if(!oldConfDataMap.containsKey(key)){
                diffOnRight.put(key, entry.getValue());
            }
        }
        diffrentMapWraper.setChangeConfDataMap(diffOnValue);
        diffrentMapWraper.setOldDiffConfDataMap(diffOnLeft);
        diffrentMapWraper.setNewDiffConfDataMap(diffOnRight);
        return diffrentMapWraper;
    }

}