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

    /**
     * 用来存储znode的结构
     * 结构形式为一个znode保存其名称不包括路径为map的key，版本号为map的value,
     * 如果一个znode存在子节点则将子节点另存为一个map结构
     */
    private final Map<String, Map<String, Integer>> oldConfDataMaps = new HashMap<String, Map<String, Integer>>();
    private static AutoConfClient autoConfClient = null;
    private static ZnodeEvent znodeEvent;
    private ZKProxyUtil zkProxyUtil;

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
        zkProxyUtil = new ZKProxyUtil(host, Common.CONNECTTIMEOUT, Common.READTIMEOUT);
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
                            log.error("应用配置时错误， host=" + host + ",path=" + path + ",key=" + key);
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
     * 此方法包含了对租户配置的支持，每一层znode只对其子znode负责，子的znode中可能包含了租户的配置，
     * 但逻辑上使用的递归同一个方法，不是很好的被理解，后期应考虑拆开
     * @param host
     * @param path 路径 必须以 “/” 结尾
     * @param key
     * @return
     */
    private boolean startJob(String host, String path, String key){
        log.info("开始扫描更新配置 host={},path={},key={}", host, path, key);
        //获取子节点列表，返回结果包含了子节点名称（不包括路径），和版本号
        Map<String, Integer> newConfDataMap = zkProxyUtil.getChild(key,path);
        /**
         * 判断当前的path是不是租户目录
         * 我们的zonde存储租户目录结构为和应用配置是一起存储的，
         * 例如：/demo_0.1_local/pid_2/redies.properties
         * 而我们本地文件需转换为 redies-pid-2.properties放到resource目录中，resource目录中不存在子目录
         */
        String lastName = getPlatformFlagByPath(path);//租户文件名中包含的特有标志
        /**
         * 如果不是租户目录就直接返回失败，因为应用目录没有配置文件是不应该出现的，
         * 但租户目录则存在配置文件被删除而没有删除租户目录的情况
         */
        if(newConfDataMap.size() < 1 && lastName == null){
            log.error("获取子节点失败，host=" + host + ",path=" + path);
            return false;
        }
        //判断当前path下是否有租户目录，在具有租户目录的情况下应该使用递归进行租户目录的扫描
        Iterator<Map.Entry<String, Integer>> allentries = newConfDataMap.entrySet().iterator();
        while (allentries.hasNext()) {
            Map.Entry<String, Integer> entry = allentries.next();
            String name = entry.getKey();
            if(name.startsWith(Common.PLATFORMFLOWERFLAG)){//继续跟踪租户目录
                String newPath = path + name + "/";
                allentries.remove();//去除租户目录
                if(!startJob(host, newPath, key)){
                    log.error("应用租户配置时错误， host=" + host + ",path=" + newPath + ",key=" + key);
                }
            }
        }


        /**
         * 判断是否是第一次加载，如果是第一次则执行全量下载，如果不是则需判断每个znode的版本是否发生了变动
         * 第一次加载的的时候由于oldConfDataMap是空的，所以会导致所有配置重新加载，加载完毕后会把当前的配置全部put到oldConfDataMap中，
         * 下次再有配置更新的时候会通过新老配置的znode name和版本号znode vsersion的对比，得出哪些配置节点应该被更新，
         * oldConfDataMaps存有各个znode路径下的子节点的映射副本，相当于记账本，以下简称记账本
         */
        Map<String, Integer> oldConfDataMap = oldConfDataMaps.get(path);
        //获取当前path下存储的znode 节点的name和version，如果没有的话，则生成一个长度为0的初始化节点，保证节点不为空
        if(oldConfDataMap == null){
            oldConfDataMap = new HashMap<String, Integer>();
            oldConfDataMaps.put(path, oldConfDataMap);
        }
        DiffrentMapWraper diffrentMapWraper = getDiffrent(oldConfDataMap, newConfDataMap);//获取新老账本的比较结果对象
        Map<String, Integer> oldDiffConfDataMap = diffrentMapWraper.getOldDiffConfDataMap();//老账本特有的znode，因为新账本中移除该对象，所以该被删除
        Map<String, Integer> newDiffConfDataMap = diffrentMapWraper.getNewDiffConfDataMap();//新账本特有的znode，因为老账本中没有该对象，所以该被新增
        Map<String, Integer> changeConfDataMap = diffrentMapWraper.getChangeConfDataMap();//新老账本中存在版本差异的znode，所以应该用新账本中的对象去更新
        String localFileName = null;//本地配置文件名称

        try {
            if(newDiffConfDataMap.size() > 0){
                log.info("执行增加新的配置");
                for(String name : newDiffConfDataMap.keySet()){
                    try{
                        ConfData confData = zkProxyUtil.getData(key, path + name);
                        localFileName = getPlatformFileName(lastName, name);
                        if(znodeEvent.addNode(localFileName, confData.getContent())){
                            oldConfDataMap.put(name, newDiffConfDataMap.get(name));//只有应用成功应用配置后才替换旧的账本
                            log.info("执行增加新的配置成功name={}", name);
                        } else {
                            log.error("执行增加新的配置失败name={}", name);
                        }
                    }catch (Exception e){
                        log.error("执行增加新的配置异常name=[}", name ,e);
                    }
                }
            }
            if(changeConfDataMap.size() > 0){
                log.info("执行更新配置");
                for(String name : changeConfDataMap.keySet()){
                    try{
                        ConfData confData = zkProxyUtil.getData(key, path + name);
                        localFileName = getPlatformFileName(lastName, name);
                        if(znodeEvent.updateNode(localFileName, confData.getContent())){
                            oldConfDataMap.put(name, newDiffConfDataMap.get(name));//只有应用成功应用配置后才替换旧的账本
                            log.info("执行更新配置成功name={}", name);
                        } else {
                            log.error("执行更新配置失败name={}", name);
                        }
                    }catch (Exception e){
                        log.error("执行增加新的配置异常name=[}", name ,e);
                    }
                }
            }
            if(oldDiffConfDataMap.size() > 0){
                log.info("执行删除配置");
                for(String name : oldDiffConfDataMap.keySet()){
                    try{
                        localFileName = getPlatformFileName(lastName, name);
                        if(znodeEvent.delNode(localFileName)){
                            oldConfDataMap.remove(name);//只有应用成功应用配置后才移除旧的账本
                            log.info("执行更新配置成功name={}", name);
                        } else {
                            log.error("执行更新配置失败name={}", name);
                        }
                    }catch (Exception e){
                        log.error("执行删除配置异常name=[}", name ,e);
                    }
                }
            }
        } catch (Exception e){
            log.error("应用配置时发生异常", e);
            return false;
        }

        return true;
    }

    /**
     * 获取zk操作对象，用于扩展
     * @return
     */
    public static AutoConfClient getAutoConfClient(){
        return autoConfClient;
    }

    /**
     * 转换用户配置文件名的方法
     * @param lastName
     * @param name
     * @return
     */
    private String getPlatformFileName(String lastName, String name){
        if(lastName != null){
            String postName = name.substring(name.lastIndexOf("."));
            String fileName = name.replace(postName, "");
            name = fileName + lastName + postName;
        }
        return name;
    }

    /**
     * 通过判断当前path是否是租户目录，返回租户文件中的标志或不是租户目录则返回null
     * @param path
     * @return
     */
    private String getPlatformFlagByPath(String path){
        if(path.contains(Common.PLATFORMFLOWERFLAG)) {//表示有租户文件
            String platformFlowerName = path.substring(path.indexOf(Common.PLATFORMFLOWERFLAG));
            String pid = platformFlowerName.replace(Common.PLATFORMFLOWERFLAG, "").replace("/", "");
            return Common.PLATFORMFLAG + pid;//最终结果应该为类似于："-pid-2"
        }
        return null;
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

        for(String key : oldConfDataMap.keySet()){
            Integer oldVal = oldConfDataMap.get(key);
            if(newConfDataMap.containsKey(key)){
                Integer newVal = newConfDataMap.get(key);
                if(!oldVal.equals(newVal)){
                    diffOnValue.put(key, newVal);
                }
            } else {
                diffOnLeft.put(key, oldVal);
            }
        }

        for(String key : newConfDataMap.keySet()){
            if(!oldConfDataMap.containsKey(key)){
                diffOnRight.put(key, newConfDataMap.get(key));
            }
        }
        diffrentMapWraper.setChangeConfDataMap(diffOnValue);
        diffrentMapWraper.setOldDiffConfDataMap(diffOnLeft);
        diffrentMapWraper.setNewDiffConfDataMap(diffOnRight);
        return diffrentMapWraper;
    }

}