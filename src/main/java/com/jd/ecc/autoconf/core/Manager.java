package com.jd.ecc.autoconf.core;

import com.jd.ecc.autoconf.entity.ConfData;
import com.jd.ecc.autoconf.entity.DiffrentMapWraper;
import com.jd.ecc.autoconf.util.Common;
import com.jd.ecc.autoconf.zk.ZnodeEvent;
import com.jd.ecc.middleware.zk.common.module.vo.resp.ZkDataVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配置中心逻辑处理类
 * Created by wangwenhao on 2018/1/29.
 */
public class Manager {
    protected static final Logger log = LoggerFactory.getLogger(Manager.class);

    //存有当前配置的对象map，name中包含了路径
    private static final Map<String, Map<String, ConfData>> oldConfDataMaps = new ConcurrentHashMap<String, Map<String, ConfData>>();
    private ZnodeEvent znodeEvent;

    public Manager(ZnodeEvent znodeEvent){
        this.znodeEvent = znodeEvent;
    }

    /**
     * 更新所有的配置
     * @param currentPath
     * @param list
     * @return
     */
    public synchronized boolean updateAllConf(String currentPath, List<ZkDataVO> list){
        /**
         * 判断当前的path是不是租户目录
         * 我们的zonde存储租户目录结构为和应用配置是一起存储的，
         * 例如：/demo_0.1_local/pid_2/redies.properties
         * 而我们本地文件需转换为 redies-pid-2.properties放到resource目录中，resource目录中不存在子目录
         */
        String lastName = getPlatformFlagByPath(currentPath);//租户文件名中包含的特有标志
        /**
         * 如果不是租户目录就直接返回，因为应用目录没有配置文件是不应该出现的，
         * 但租户目录则存在配置文件被删除而没有删除租户目录的情况
         */
        if(list.size() < 1 && lastName == null){
            log.info("当前目录下不存在配置，path={}", currentPath);
            return true;
        }
        Map<String, ConfData> newConfDataMap = new HashMap<>();
        for(ZkDataVO zkDataVO : list){
            //为了防止依赖zk proxy的接口实体存在变动，故使用自定义实体
            newConfDataMap.put(zkDataVO.getName(), new ConfData(zkDataVO.getName(), new String(zkDataVO.getData()), zkDataVO.getVersion()));
        }
        /**
         * 判断是否是第一次加载，如果是第一次则执行全量下载，如果不是则需判断每个znode的版本是否发生了变动
         * 第一次加载的的时候由于oldConfDataMap是空的，所以会导致所有配置重新加载，加载完毕后会把当前的配置全部put到oldConfDataMap中，
         * 下次再有配置更新的时候会通过新老配置的znode name和版本号znode vsersion的对比，得出哪些配置节点应该被更新，
         * oldConfDataMaps存有各个znode路径下的子节点的映射副本，相当于记账本，以下简称记账本
         */
        Map<String, ConfData> oldConfDataMap = getOldConfDataMap(currentPath);
        
        DiffrentMapWraper diffrentMapWraper = getDiffrent(oldConfDataMap, newConfDataMap);//获取新老账本的比较结果对象
        Map<String, ConfData> oldDiffConfDataMap = diffrentMapWraper.getOldDiffConfDataMap();//老账本特有的znode，因为新账本中移除该对象，所以该被删除
        Map<String, ConfData> newDiffConfDataMap = diffrentMapWraper.getNewDiffConfDataMap();//新账本特有的znode，因为老账本中没有该对象，所以该被新增
        Map<String, ConfData> changeConfDataMap = diffrentMapWraper.getChangeConfDataMap();//新老账本中存在版本差异的znode，所以应该用新账本中的对象去更新
        
        try {
            if(newDiffConfDataMap.size() > 0){
                log.info("执行增加新的配置");
                for(String name : newDiffConfDataMap.keySet()){
                    ConfData confData = newDiffConfDataMap.get(name);
                    addConf(name, confData.getContent(), confData.getVersion());
                }
            }
            if(changeConfDataMap.size() > 0){
                log.info("执行更新配置");
                for(String name : changeConfDataMap.keySet()){
                    ConfData confData = changeConfDataMap.get(name);
                    updateConf(name, confData.getContent(), confData.getVersion());
                }
            }
            if(oldDiffConfDataMap.size() > 0){
                log.info("执行删除配置");
                for(String name : oldDiffConfDataMap.keySet()){
                    ConfData confData = oldDiffConfDataMap.get(name);
                    delConf(name, confData.getVersion());
                }
            }
        } catch (Exception e){
            log.error("应用配置时发生异常", e);
            return false;
        }
        
        return true;
    }

    public synchronized boolean addConf(String name, String content, int version){
        Map<String, ConfData> oldConfDataMap = getOldConfDataMapByFullName(name);
        return addConf(name, content, version, oldConfDataMap);
    }

    public synchronized boolean addConf(String name, String content, int version, Map<String, ConfData> oldConfDataMap){
        log.info("执行增加新的配置name={}", name);
        try{
            String localFileName = getPlatformFileNameByPath(name);
            if(znodeEvent.addNode(localFileName,content)){
                oldConfDataMap.put(name, new ConfData(name, content, version));//只有应用成功应用配置后才替换旧的账本
                log.info("执行增加新的配置成功name={}", name);
                return true;
            }
        }catch (Exception e){
            log.error("执行增加新的配置异常name=[}", name ,e);
        }
        log.error("执行增加新的配置失败name={}", name);
        return false;
    }

    public synchronized boolean updateConf(String name, String content, int version){
        Map<String, ConfData> oldConfDataMap = getOldConfDataMapByFullName(name);
        return updateConf(name, content, version, oldConfDataMap);
    }

    public synchronized boolean updateConf(String name, String content, int version, Map<String, ConfData> oldConfDataMap){
        log.info("执行更新配置name={}", name);
        try{
            String localFileName = getPlatformFileNameByPath(name);
            if(znodeEvent.updateNode(localFileName, content)){
                oldConfDataMap.put(name, new ConfData(name, content, version));//只有应用成功应用配置后才替换旧的账本
                log.info("执行更新配置成功name={}", name);
                return true;
            }
        }catch (Exception e){
            log.error("执行增加新的配置异常name=[}", name ,e);
        }
        log.error("执行更新配置失败name={}", name);
        return false;
    }

    public synchronized boolean delConf(String name, int version){
        Map<String, ConfData> oldConfDataMap = getOldConfDataMapByFullName(name);
        return delConf(name, version, oldConfDataMap);
    }

    public synchronized boolean delConf(String name, int version, Map<String, ConfData> oldConfDataMap){
        log.info("执行删除配置name={}", name);
        try{
            String localFileName = getPlatformFileNameByPath(name);
            if(znodeEvent.delNode(localFileName)){
                oldConfDataMap.remove(name);//只有应用成功应用配置后才移除旧的账本
                log.info("执行更新配置成功name={}", name);
                return true;
            }
        }catch (Exception e){
            log.error("执行删除配置异常name={}", name ,e);
        }
        log.error("执行更新配置失败name={}", name);
        return false;
    }

    /**
     * 加载并使用本地配置
     * @return
     */
    public synchronized boolean loadFromLocal(){
        return znodeEvent.zkConnectFail();
    }

    /**
     * 根据全配置名称获取配置存储的map中的对象
     * @param name
     * @return
     */
    public synchronized Map<String, ConfData> getOldConfDataMapByFullName(String name){
        return getOldConfDataMap(getPathByFullName(name));
    }

    /**
     * 根据配置路径获取配置存储的map中的对象
     * @param path
     * @return
     */
    public synchronized Map<String, ConfData> getOldConfDataMap(String path){
        Map<String, ConfData> oldConfDataMap = oldConfDataMaps.get(path);
        //获取当前path下存储的znode 节点的name和version，如果没有的话，则生成一个长度为0的初始化节点，保证节点不为空
        if(oldConfDataMap == null){
            oldConfDataMap = new HashMap<String, ConfData>();
            oldConfDataMaps.put(path, oldConfDataMap);
        }
        return oldConfDataMap;
    }

    /**
     * 根据配置文件全名称获取存储在本地的配置文件的名称
     * 例如： demo_1_dev/redis_ins_test.properties 转换后  redis-ins-test.properties
     *  demo_1_dev/pid_2/redis_ins_test.properties 转换后 redis-pid-2-ins-test.properties
     * @param lastName
     * @param name
     * @return
     */
    private String getPlatformFileNameByLN(String lastName, String name){
        if(name != null){
            name = getNameByFullName(name);//如果是全名称则需要转换为不包含路径的名称
            if(lastName != null){
                String postName = name.substring(name.lastIndexOf("."));
                String fileName = name.replace(postName, "");
                name = fileName + lastName + postName;
            }
            name = getLocalName(name.replace(Common.ZKCONFNAMESPLIT, Common.lOCALCONFNAMESPLIT));
        }
        return name;
    }

    /**
     * 根据路径和配置名称获取租户格式的文件名称
     * @param path
     * @param name
     * @return
     */
    public String getPlatformFileNameByPath(String path, String name){
        String lastName = getPlatformFlagByPath(path);//租户文件名中包含的特有标志
        return getPlatformFileNameByLN(lastName, name);
    }

    /**
     *根据配置全名称获取租户格式的文件名称
     * @param name
     * @return
     */
    public String getPlatformFileNameByPath(String name){
        String lastName = getPlatformFlagByPath(getPathByFullName(name));//租户文件名中包含的特有标志
        return getPlatformFileNameByLN(lastName, name);
    }

    /**
     * 根据全配置名称获取配置路径
     * @param name
     * @return
     */
    public String getPathByFullName(String name){
        if(name.contains("/")){//如果包含路径
            return name.substring(0, name.lastIndexOf("/"));
        }
        return "";
    }

    /**
     * 根据全配置名称获取不包含路径的配置名称
     * @param name
     * @return
     */
    public String getNameByFullName(String name){
        if(name.contains("/")){//如果包含路径
            return name.substring(name.lastIndexOf("/") + 1, name.length());
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
     * 根据文件名称获取 “文件名+租户id+实例名+文件后缀”（同时也是文件的标准顺序）
     * @param fileName
     * @return
     */
    public static Map<String, String> analyzeFileName(String fileName){
        /**
         * 截取文件名取第一个结果 .+?(?=-pid-|-ins-|\.)
         截取租户id (?<=-pid-).+?(?=-|\.)
         截取实例名 (?<=-ins-).+?(?=-|\.)
         截取文件后缀包含 "."   (\.).*
         */
        String firstNameRegEx = ".+?(?=-pid-|-ins-|\\.)";
        String pidRegEx = "(?<=-pid-).+?(?=-|\\.)";
        String insRegEx = "(?<=-ins-).+?(?=-|\\.)";
        String lastNameRegEx = "(\\.).*";
        Map<String, String> returnMap = new HashMap<>();
        Pattern r = Pattern.compile(firstNameRegEx);
        Matcher m = r.matcher(fileName);
        if (m.find()) {
            returnMap.put("firstName", m.group(0));
        }
        r = Pattern.compile(pidRegEx);
        m = r.matcher(fileName);
        if (m.find()) {
            returnMap.put("pid", m.group(0));
        }
        r = Pattern.compile(insRegEx);
        m = r.matcher(fileName);
        if (m.find()) {
            returnMap.put("ins", m.group(0));
        }
        r = Pattern.compile(lastNameRegEx);
        m = r.matcher(fileName);
        if (m.find()) {
            returnMap.put("lastName", m.group(0));
        }
        return returnMap;
    }

    /**
     * 根据配置文件名称获取注解名称
     * @param fileName
     * @return
     */
    public static String getTagOrLocalName(String fileName, boolean isLocalName){
        String tagName = null;
        Map<String, String> analyzeResMap = analyzeFileName(fileName);
        String firstName = analyzeResMap.get("firstName");
        String pid = analyzeResMap.get("pid");
        String ins = analyzeResMap.get("ins");
        String lastName = analyzeResMap.get("lastName");
        if(firstName != null){
            tagName = firstName;
        }
        if(pid != null){
            tagName += Common.PLATFORMFLAG + (isLocalName ? pid : Common.PLATFORMSTR);
        }
        if(ins != null){
            tagName += Common.lOCALCONFNAMESPLIT + (isLocalName ? ins : Common.PLATFORMSTR);
        }
        if(lastName != null){
            tagName += lastName;
        }
        return tagName;
    }

    public static String getTagName(String fileName){
        return getTagOrLocalName(fileName, false);
    }

    public static String getLocalName(String fileName){
        return getTagOrLocalName(fileName, true);
    }

    /**
     * 得到两个map的差集
     * @param oldConfDataMap
     * @param newConfDataMap
     * @return
     */
    public static DiffrentMapWraper getDiffrent(Map<String, ConfData> oldConfDataMap, Map<String, ConfData> newConfDataMap){

        DiffrentMapWraper diffrentMapWraper = new DiffrentMapWraper();
        Map<String, ConfData> diffOnLeft = new HashMap<String, ConfData>();
        Map<String, ConfData> diffOnRight = new HashMap<String, ConfData>();
        Map<String, ConfData> diffOnValue = new HashMap<String, ConfData>();

        for(String key : oldConfDataMap.keySet()){
            ConfData oldConfData = oldConfDataMap.get(key);
            Integer oldVal = oldConfData.getVersion();
            if(newConfDataMap.containsKey(key)){
                ConfData newConfData = newConfDataMap.get(key);
                Integer newVal = newConfData.getVersion();
                if(!oldVal.equals(newVal)){
                    diffOnValue.put(key, newConfData);
                }
            } else {
                diffOnLeft.put(key, oldConfData);
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
