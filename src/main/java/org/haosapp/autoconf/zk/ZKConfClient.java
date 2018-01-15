package org.haosapp.autoconf.zk;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.haosapp.autoconf.util.CodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 实现watch指定目录下的所有znode
 * Created by wangwenhao on 2018/1/9.
 */
public class ZKConfClient {

    protected static final Logger log = LoggerFactory.getLogger(ZKConfClient.class);

    private static final Map<String, IZkDataListener> watchMap = new HashMap<String, IZkDataListener>();
    private static final List<String> fileList = new ArrayList<String>();
    private static ZkClient zkClient = null;
    private static ZnodeEvent znodeEvent;

    private ZKConfClient(){}

    /**
     * 初始化配置
     * @param zkHost
     * @param znodeEvent
     * @throws Exception
     */
    public static synchronized void initZk(String zkHost,final ZnodeEvent znodeEvent) throws Exception {
        if(zkClient != null){//保证只初始化一次
            return;
        }

        ZKConfClient.znodeEvent = znodeEvent;
        boolean isStartError = true;
        boolean isLoadFromLocal = true;
        while (isStartError){
            try{
                zkClient = null;
                zkClient = new ZkClient(zkHost,3000);
                isStartError = false;
            } catch (Exception e){
                if(isLoadFromLocal){
                    isLoadFromLocal = false;
                    if(znodeEvent.zkConnectFail()){
                        log.info("开发者初始化操作调用成功");
                    } else {
                        log.info("开发者初始化操作调用失败");
                    }
                }
                Thread.sleep(5000);
                log.error("连接失败，重试。。。。。。", e);
            }
        }

        zkClient.setZkSerializer(new ZkSerializer(){//设置解析器
            @Override
            public byte[] serialize(Object o) throws ZkMarshallingError {
                return CodeUtils.unicodeToUtf8(String.valueOf(o)).getBytes();
            }
            @Override
            public Object deserialize(byte[] bytes) throws ZkMarshallingError {
                return CodeUtils.unicodeToUtf8(new String(bytes));
            }
        });
    }

    public static void startWatch(String url){
        zkClient.waitUntilExists(url, TimeUnit.DAYS,90);//等待znode创建的最长时间

        /**
         * 初始化
         */
        List<String> initFileList = zkClient.getChildren(url);
        fileList.addAll(initFileList);
        Map<String, Object> initMap = new HashMap<String, Object>();
        for(String fileName : initFileList){
            String path = url + "/" + fileName;
            IZkDataListener iZkDataListener = getIZkDataListener(znodeEvent);
            zkClient.subscribeDataChanges(path,iZkDataListener);
            watchMap.put(path, iZkDataListener);
            String content = zkClient.readData(path);
            initMap.put(path, content);
            //输出所有的配置
            log.info("path={},content={}",path ,content);
        }
        znodeEvent.init(initMap);

        //设置watch
        zkClient.subscribeChildChanges(url,new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> children) throws Exception {
                if(children == null){
                    log.info("{}已删除",parentPath);
                    return;
                }

                //改进后的算法
                List<String> diffList = getDiffrent(children, fileList);//取出差集
                for(String diff : diffList){
                    String path = parentPath + "/" + diff;
                    if(fileList.contains(diff)){ //减
                        zkClient.unsubscribeDataChanges(path,watchMap.get(path));
                        watchMap.remove(path);
                        Iterator<String> it = fileList.iterator();
                        while(it.hasNext()) {
                            String fileName = it.next();
                            if(diff.equals(fileName)){
                                it.remove();
                                /**
                                 * 可以进行客户端配置删除操作
                                 */
                                log.info("去除watch:{}", path);
                            }
                        }
                    } else { //加
                        IZkDataListener iZkDataListener = getIZkDataListener(znodeEvent);
                        zkClient.subscribeDataChanges(path,iZkDataListener);
                        watchMap.put(path, iZkDataListener);
                        fileList.add(path);
                        /**
                         * 可以进行设置黑白名单等操作
                         */
                        if(znodeEvent.addNode(path, zkClient.readData(path))){
                            log.info("开发者创建znode操作调用成功");
                        } else {
                            log.info("开发者创建znode操作调用失败");
                        }
                        log.info("添加watch:{}", path);
                        //输出配置
                        String content = zkClient.readData(path);
                        log.info("path={},content={}",path ,content);
                    }
                }
            }
        });
    }

    /**
     * 获取zk操作对象，用于扩展
     * @return
     */
    public static ZkClient getZkClient(){
        return zkClient;
    }

    /**
     * 设置znode的watch
     * @return
     */
    public static IZkDataListener getIZkDataListener(final ZnodeEvent znodeEvent){
        return new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {
                log.info("znode:{}发生改变，内容为：{}", s, o);
                if(znodeEvent.updateNode(s, o)){
                    log.info("开发者修改znode操作调用成功");
                } else {
                    log.info("开发者修改znode操作调用失败");
                }
            }

            @Override
            public void handleDataDeleted(String path) throws Exception {
                //这里也可以做删除配置的事件
                log.info("znode:{}已删除", path);
                if(znodeEvent.delNode(path)){
                    log.info("开发者删除znode操作调用成功");
                } else {
                    log.info("开发者删除znode操作调用失败");
                }
            }
        };
    }

    /**
     * 获取两个List的不同元素
     * @param list1
     * @param list2
     * @return
     */
    private static List<String> getDiffrent(List<String> list1, List<String> list2) {
        long st = System.nanoTime();
        Map<String,Integer> map = new HashMap<String,Integer>(list1.size()+list2.size());
        List<String> diff = new ArrayList<String>();
        List<String> maxList = list1;
        List<String> minList = list2;
        if(list2.size()>list1.size())
        {
            maxList = list2;
            minList = list1;
        }
        for (String string : maxList) {
            map.put(string, 1);
        }
        for (String string : minList) {
            Integer cc = map.get(string);
            if(cc!=null)
            {
                map.put(string, ++cc);
                continue;
            }
            map.put(string, 1);
        }
        for(Map.Entry<String, Integer> entry:map.entrySet())
        {
            if(entry.getValue()==1)
            {
                diff.add(entry.getKey());
            }
        }
        return diff;

    }

}