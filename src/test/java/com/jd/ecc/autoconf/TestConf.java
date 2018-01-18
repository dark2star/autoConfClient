package com.jd.ecc.autoconf;

import com.jd.ecc.autoconf.http.Load;
import com.jd.ecc.autoconf.zk.ZKConfClient;
import com.jd.ecc.autoconf.zk.ZnodeEvent;
import com.jd.ecc.autoconf.util.FileUtil;
import com.jd.ecc.autoconf.util.PropertiesUtil;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by wangwenhao on 2018/1/10.
 */
public class TestConf {

    private static final String watchUrl = "/disconf/disconf_demo_1_0_0_0_local/file";
    //private static final String zkHostList="127.0.0.1:2181,192.168.178.126:12181";//集群配置
    private static final String zkHostList="127.0.0.1:2181";//单机配置
    private static final String packageName = "org.haosapp.autoconf";//扫描包空间

    public static void main(String[] args) throws Exception {
        final String localPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        System.out.println("配置文件位置："+localPath);
        //初始化扫描
        Load.initScan(packageName);

        ZKConfClient.initZk(zkHostList,new ZnodeEvent(){
            /**
             * 当启动连接zk失败时触发
             *
             * @return
             * @throws Exception
             */
            @Override
            public boolean zkConnectFail() {
                System.out.println("============启动连接zk失败=================");
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
                System.out.println("============初始化操作=================");
                try {
                    Iterator<Map.Entry<String, Object>> entries = map.entrySet().iterator();
                    while (entries.hasNext()) {
                        Map.Entry<String, Object> entry = entries.next();
                        System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                        String path = entry.getKey();
                        Object content = entry.getValue();
                        String[] pathSplit = path.split("/");
                        String fileName = pathSplit[pathSplit.length-1];
                        String localfile = localPath + "/"+ fileName;
                        FileUtil.write( localfile,trans(String.valueOf(content)));;
                        Map<String, Object> returnMap = PropertiesUtil.GetAllProperties(localfile);
                        Load.setConf(fileName, returnMap);
                    }
                    return true;
                } catch (Exception e){
                    return false;
                }
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
                System.out.println("============创建操作=================");
                try{
                    String[] pathSplit = path.split("/");
                    String fileName = pathSplit[pathSplit.length-1];
                    String localfile = localPath + "/"+ fileName;
                    FileUtil.write( localfile,trans(String.valueOf(content)));;
                    Map<String, Object> returnMap = PropertiesUtil.GetAllProperties(localfile);
                    Load.setConf(fileName, returnMap);
                    return true;
                } catch (Exception e){
                    return false;
                }
            }

            /**
             * 删除znode事件
             *
             * @param path znode的url
             * @return 是否操作成功
             */
            @Override
            public boolean delNode(String path) {
                System.out.println("============删除操作=================");
                String[] pathSplit = path.split("/");
                FileUtil.deleteEveryThing(localPath + "/"+ pathSplit[pathSplit.length-1]);
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
                    System.out.println("============修改操作=================");
                    String[] pathSplit = path.split("/");
                    String fileName = pathSplit[pathSplit.length-1];
                    String localfile = localPath + "/"+ fileName;
                    FileUtil.write( localfile,trans(String.valueOf(content)));
                    Map<String, Object> returnMap = PropertiesUtil.GetAllProperties(localfile);
                    Load.setConf(fileName, returnMap);//此处需解决从spring中获取对象
                    return true;
                } catch (Exception e){
                    return false;
                }
            }
        });

        ZKConfClient.startWatch(watchUrl);

        ZKConfClient.startWatch("/disconf/disconf_demo_1_0_0_0_local/item");//测试多目录支持

        /**
         * 模拟其他的线程调用
         */
        new Thread(){
            @Override
            public void run(){
                while (true){
                    System.out.println("用户使用redis配置 Host = " + RedisConf.getInstance().getHost()
                            + ", Port = " + RedisConf.getInstance().getPort()
                            + ", Password = " + RedisConf.getInstance().getPassword());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        Thread.currentThread().sleep(1000000l);
    }

    /**
     * 兼容disconf服务端做的字符转换
     * @param text
     * @return
     */
    public static String trans(String text){
        if(text.startsWith("\"")){
            text = text.substring(1);
        }
        if(text.endsWith("\"")){
            text = text.substring(0,text.length() - 1);
        }
        return text;
    }

}
