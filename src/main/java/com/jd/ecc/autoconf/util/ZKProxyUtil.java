package com.jd.ecc.autoconf.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.ecc.autoconf.entity.ConfData;
import com.jd.ecc.autoconf.entity.ZKProxyResult;
import com.jd.ecc.middleware.zk.client.ZkWebSocketClient;
import com.jd.ecc.middleware.zk.common.module.vo.common.MessageTypeEnum;
import com.jd.ecc.middleware.zk.common.module.vo.req.*;
import com.jd.ecc.middleware.zk.common.tool.JsonTool;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * zk代理通信方法的封装工具类
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

    public static void get(String path, ZkWebSocketClient client) {
        try{
            GetDataCommand command = new GetDataCommand();
            command.setPath(path);

            CommandReq req = new CommandReq();
            req.setMessageType(MessageTypeEnum.GET_DATA);
            req.setPayload(JsonTool.toJsonAsBytes(command));

            client.send(JsonTool.toJsonAsBytes(req));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void children(String path, ZkWebSocketClient client) {
        log.info("发送获取子节点请求path={}", path);
        try{
            GetChildrenCommand command = new GetChildrenCommand();
            command.setPath(path);

            CommandReq req = new CommandReq();
            req.setMessageType(MessageTypeEnum.GET_CHILDREN);
            req.setPayload(JsonTool.toJsonAsBytes(command));

            client.send(JsonTool.toJsonAsBytes(req));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save(String path, String data, ZkWebSocketClient client) {
        try{
            SaveDataCommand command = new SaveDataCommand();
            command.setPath(path);
            command.setData(data.getBytes());

            CommandReq req = new CommandReq();
            req.setMessageType(MessageTypeEnum.SAVE);
            req.setPayload(JsonTool.toJsonAsBytes(command));

            client.send(JsonTool.toJsonAsBytes(req));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(String path, ZkWebSocketClient client) {
        try{
            DeleteCommand command = new DeleteCommand();
            command.setPath(path);

            CommandReq req = new CommandReq();
            req.setMessageType(MessageTypeEnum.DELETE);
            req.setPayload(JsonTool.toJsonAsBytes(command));

            client.send(JsonTool.toJsonAsBytes(req));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
