package com.jd.ecc.autoconf.http;

import com.jd.ecc.autoconf.zk.ZKConfClient;
import com.jd.ecc.autoconf.zk.ZnodeEvent;
import org.I0Itec.zkclient.IZkDataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 实现watch指定目录下的所有znode
 * Created by wangwenhao on 2018/1/9.
 */
public class AutoConfClient {

    protected static final Logger log = LoggerFactory.getLogger(ZKConfClient.class);

    private final static String resourcePath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    private static final Map<String, IZkDataListener> watchMap = new HashMap<String, IZkDataListener>();
    private static final List<String> fileList = new ArrayList<String>();
    private static AutoConfClient autoConfClient = null;
    private static ZnodeEvent znodeEvent;

    private AutoConfClient(){}

    /**
     * 创建对象
     * @param zkHost
     * @param znodeEvent
     * @throws Exception
     */
    public static synchronized AutoConfClient buildAutoConfClient(String zkHost, ZnodeEvent znodeEvent,String packageName) throws Exception {
        if(autoConfClient != null){//保证只初始化一次
            return autoConfClient;
        }
        //初始化
        autoConfClient = new AutoConfClient();
        AutoConfClient.znodeEvent = znodeEvent;
        //初始化扫描
        Load.initScan(packageName);
        return autoConfClient;
    }

    /**
     * 设置watch
     * @param path
     */
    public void setWatch(String path){
        //连接server获取配置
        if(!znodeEvent.init(null)){
            //如果未成功连接服务端则应用本地配置
            znodeEvent.zkConnectFail();
        }
        //实现轮询并对配置的应用
        new Thread(){
            @Override
            public void run() {
                while(true){
                    znodeEvent.addNode(null, null);
                    znodeEvent.updateNode(null, null);
                    znodeEvent.delNode(null);
                }
            }
        }.start();
    }

    /**
     * 获取zk操作对象，用于扩展
     * @return
     */
    public static AutoConfClient getAutoConfClient(){
        return autoConfClient;
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

    private static List<String> apendPath(String path, List<String> znodeList) {
        List<String> returnList = new ArrayList<String>();
        for(String znode : znodeList){
            returnList.add(path + "/" + znode);
        }
        return returnList;
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