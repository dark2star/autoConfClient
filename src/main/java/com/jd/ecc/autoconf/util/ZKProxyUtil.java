package com.jd.ecc.autoconf.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.ecc.autoconf.entity.ConfData;
import com.jd.ecc.autoconf.entity.ZKProxyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangwenhao on 2018/1/22.
 */
public class ZKProxyUtil {

    protected static final Logger log = LoggerFactory.getLogger(ZKProxyUtil.class);

    private String host;
    private int connectTimeout = 30;
    private int readTimeout = 30;

    public ZKProxyUtil(String host, int connectTimeout, int readTimeout){
        this.host = host;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public ConfData getData(String secretKey, String pathValue){
        ConfData confData = null;
        String url = host + "/client/zk/data/" + secretKey + "?path=" + pathValue;
        log.info("获取内容url={}",url);
        try {
            String res = HttpUtil.getInstance(this.connectTimeout,this.readTimeout).get(url);
            log.debug("zkproxy返回的内容:{}",res);
            if(res != null && res.length() > 0){
                ObjectMapper mapper = new ObjectMapper();
                ZKProxyResult zkProxyResult = mapper.readValue(res, ZKProxyResult.class);
                JsonNode node = mapper.readTree(res);
                String payloadJson = node.get("payload").toString();
                if(payloadJson != null && payloadJson.length() > 0 && zkProxyResult.isSuccess()){
                    confData = mapper.readValue(payloadJson, ConfData.class);
                    String content = new String(confData.getData());
                    log.info("zkproxy返回的data内容:{}",content);
                    confData.setContent(content);
                }
            }
        } catch (Exception e){
            log.error("获取zkproxy pathValue={}时异常",pathValue,e);
        }
        return confData;
    }

    public Map<String, Integer> getChild(String secretKey, String pathValue){
        Map<String, Integer> confDataMap = new HashMap<String, Integer>();
        String url = host + "/client/zk/children/" + secretKey + "?path=" + pathValue;
        log.info("获取子节点url={}",url);
        try {
            String res = HttpUtil.getInstance(this.connectTimeout,this.readTimeout).get(url);
            log.debug("zkproxy返回的内容:{}",res);
            if(res != null && res.length() > 0){
                ObjectMapper mapper = new ObjectMapper();
                ZKProxyResult zkProxyResult = mapper.readValue(res, ZKProxyResult.class);
                JsonNode node = mapper.readTree(res);
                String payloadJson = node.get("payload").toString();
                if(payloadJson != null && payloadJson.length() > 0 && zkProxyResult.isSuccess()){
                    List<ConfData> confDataList = mapper.readValue(payloadJson, getCollectionType(mapper, List.class, ConfData.class));
                    for(ConfData confData : confDataList){
                        confDataMap.put(confData.getName(), confData.getVersion());
                    }
                }
            }
        } catch (Exception e){
            log.error("获取zkproxy pathValue={}时异常",pathValue,e);
        }
        return confDataMap;
    }

    /**
     * 获取泛型的Collection Type
     * @param mapper
     * @param collectionClass
     * @param elementClasses
     * @return
     */
    private static JavaType getCollectionType(ObjectMapper mapper, Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }
}
