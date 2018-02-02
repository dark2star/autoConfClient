package com.jd.ecc.autoconf;

import com.jd.ecc.autoconf.core.Manager;
import com.jd.ecc.autoconf.util.Common;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangwenhao on 2018/1/16.
 */
public class HttpTest {

    private static String url = "http://192.168.171.124:18902/client/zk/data/test?path=temptest";

    @Test
    public void run() {

/*        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            System.out.println(response.body().string());
        } else {
            throw new IOException("Unexpected code " + response);
        }*/
/*        String res = HttpUtil.getInstance(30,30).get(url);
        System.out.println(res);
        ObjectMapper mapper = new ObjectMapper();
        ZKProxyResult zkProxyResult = mapper.readValue(res, ZKProxyResult.class);
        String str = new String(zkProxyResult.getData());
        System.out.println("解码后=  " + str);*/

        //ZKProxyUtil zkProxyUtil = new ZKProxyUtil("http://192.168.171.124:18902", Common.CONNECTTIMEOUT, Common.READTIMEOUT);
        //System.out.println(zkProxyUtil.getData("test","temptest"));
        //System.out.println(zkProxyUtil.getChild("test","/"));

        Map<String, Integer> m1 = new HashMap<String, Integer>();
        m1.put("qq",1);
        m1.put("ee",2);
        m1.put("rr",2);

        Map<String, Integer> m2 = new HashMap<String, Integer>();
        m2.put("qq",2);
        m2.put("ww",1);
        m2.put("qq1",2);
        m2.put("ss",2);

        //String path ="/qw/dlv/pid_1/redis_11.pf";
        //String path ="demo_1_dev/pid_4/redis.properties";
        //String path ="/qw/dlv/pid/redis.pf";
        //String path ="r.pf";
        Manager manager = new Manager(null);
        //System.out.println(path.substring(0, path.lastIndexOf("/")+1));
        //System.out.println(new Manager(null).getNameByFullName(path));
        //System.out.println(manager.getPlatformFileNameByPath(manager.getPathByFullName(path), manager.getNameByFullName(path)));

/*        String fileName = "redis-pid-3-ins-2.properties";

        String tagName = null;
        if(fileName.contains(Common.PLATFORMFLAG)) {//租户文件变动
            String prefix = fileName.substring(0, fileName.indexOf(Common.PLATFORMFLAG) + Common.PLATFORMFLAG.length());
            String lastfix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
            tagName = prefix + Common.PLATFORMSTR + lastfix;
        }

        System.out.println(tagName);*/

        System.out.println("--------------------------------------------");
        String[] testStr = new String[]{"redis.properties",
                "redis-pid-3-ins-2.properties", "redis-pid-3.properties", "redis-ins-2.properties"," redis-dev.properties"};
        for(String str : testStr){
            Map<String, String> map = manager.analyzeFileName(str);
            System.out.println("fileName =" + str);
            for(String key : map.keySet()){
                System.out.println(key + " = " + map.get(key));
            }
            System.out.println("tagName = " + Manager.getTagName(str));
            System.out.println("++++++++++++++++++++++++++++++++++++");
        }

        System.out.println("00  " + manager.getPlatformFileNameByPath("demo_1_dev/redis_ins_test.properties"));
        System.out.println("11  " + manager.getPlatformFileNameByPath("demo_1_dev/pid_2/redis_ins_test.properties"));
        System.out.println("22  " + manager.getPlatformFileNameByPath("demo_1_dev/pid_2/redis.properties"));
        System.out.println("33  " + manager.getPlatformFileNameByPath("demo_1_dev/redis.properties"));

/*        for(String name : m1.keySet()){
            System.out.println(name + "  " + m1.get(name));
        }*/
/*        Map<String,Integer> diffOnRight=  getDifferenceSetByGuava(m2, m1);
        for(Map.Entry<String, Integer> entry:diffOnRight.entrySet()){
            System.out.println("共同Map中key:"+entry.getKey()+"  value:"+entry.getValue());
        }*/
        //找出2个Map的不同之处与相同之处，以Map形式返回
/*        ImmutableMap<String,Integer> oneMap= ImmutableMap.copyOf(m1);
        ImmutableMap<String,Integer> twoMap=ImmutableMap.copyOf(m2);
        MapDifference<String,Integer> diffHadle= Maps.difference(oneMap,twoMap);
        Map<String,Integer> commonMap = diffHadle.entriesInCommon();//{"key2",2},若无共同Entry，则返回长度为0的Map
        for(Map.Entry<String, Integer> entry:commonMap.entrySet()){
            System.out.println("共同Map中key:"+entry.getKey()+"  value:"+entry.getValue());
        }
        System.out.println("=============================");
        Map<String,Integer> diffOnLeft=diffHadle.entriesOnlyOnLeft();//返回左边的Map中不同或者特有的元素
        for(Map.Entry<String, Integer> entry:diffOnLeft.entrySet()){
            System.out.println("共同Map中key:"+entry.getKey()+"  value:"+entry.getValue());
        }
        System.out.println("=============================");
        Map<String,Integer> diffOnRight=diffHadle.entriesOnlyOnRight();//返回右边的Map中不同或者特有的元素
        for(Map.Entry<String, Integer> entry:diffOnRight.entrySet()){
            System.out.println("共同Map中key:"+entry.getKey()+"  value:"+entry.getValue());
        }
        System.out.println("bub");
        Iterator<Map.Entry<String, Integer>> entries = m1.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Integer> entry = entries.next();
            if(m2.containsKey(entry.getKey())){
                Integer val = m2.get(entry.getKey());
                if(entry.getValue() != val){
                    System.out.println("共同Map中key:"+entry.getKey()+"  value:"+m2.get(entry.getKey()));
                }
            }
        }*/

        //AutoConfClient.getDiffrent(m1,m2);
/*        String path = "/test/dis/pid_3";
        String name = "/redis.properties";
        if(path.contains(Common.PLATFORMFLOWERFLAG)) {//表示有租户文件
            String platformFlowerName = path.substring(path.indexOf(Common.PLATFORMFLOWERFLAG));
            String pid = platformFlowerName.replace(Common.PLATFORMFLOWERFLAG, "");
            System.out.println(pid);
            String lastName = name.substring(name.lastIndexOf("."));
            String fastName = name.replace(lastName, "");
            lastName = Common.PLATFORMFLAG + pid + lastName;
            System.out.println(fastName);
            System.out.println(lastName);
        }*/
    }

    /**
     * 自己实现取Map集合的差集--站在巨人的肩膀上造轮子
     *
     * @param bigMap   大集合
     * @param smallMap 小集合
     * @return 两个集合的差集
     */
/*    private static Map<String, Integer> getDifferenceSetByGuava(Map<String, Integer> bigMap, Map<String, Integer> smallMap) {
        Set<String> bigMapKey = bigMap.keySet();
        Set<String> smallMapKey = smallMap.keySet();
        Set<String> differenceSet = Sets.difference(bigMapKey, smallMapKey);
        Map<String, Integer> result = Maps.newHashMap();
        for (String key : differenceSet) {
            result.put(key, bigMap.get(key));
        }
        return result;
    }*/

}
