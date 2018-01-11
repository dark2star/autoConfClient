package org.haosapp.autoconf.util;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

//关于Properties类常用的操作
public class PropertiesUtil {
    //根据Key读取Value
    public static String GetValueByKey(String filePath, String key) {
        Properties pps = new Properties();
        try {
            FileInputStream fis = new FileInputStream(filePath);
            InputStream in = new BufferedInputStream(fis);
            pps.load(in);
            fis.close();
            String value = pps.getProperty(key);
            System.out.println(key + " = " + value);
            return value;
            
        }catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    //读取Properties的全部信息
    public static Map<String, Object> GetAllProperties(String filePath) throws IOException {
        Map<String, Object> returnMap = new HashMap<String, Object>();
        Properties pps = new Properties();
        FileInputStream fis = new FileInputStream(filePath);
        InputStream in = new BufferedInputStream(fis);
        pps.load(in);
        Enumeration en = pps.propertyNames(); //得到配置文件的名字
        fis.close();
        while(en.hasMoreElements()) {
            String strKey = (String) en.nextElement();
            Object strValue = pps.get(strKey);
            returnMap.put(strKey, strValue);
            System.out.println("key =" + strKey + ", value =" + strValue);
        }
        return returnMap;
    }
    
    //写入Properties信息
    public static void WriteProperties (String filePath, String pKey, String pValue) throws IOException {
        Properties pps = new Properties();
        
        InputStream in = new FileInputStream(filePath);
        //从输入流中读取属性列表（键和元素对） 
        pps.load(in);
        //调用 Hashtable 的方法 put。使用 getProperty 方法提供并行性。  
        //强制要求为属性的键和值使用字符串。返回值是 Hashtable 调用 put 的结果。
        OutputStream out = new FileOutputStream(filePath);
        pps.setProperty(pKey, pValue);
        //以适合使用 load 方法加载到 Properties 表中的格式，  
        //将此 Properties 表中的属性列表（键和元素对）写入输出流  
        pps.store(out, "Update " + pKey + " name");
        in.close();
    }
    
    public static void main(String [] args) throws IOException{
        //String value = GetValueByKey("Test.properties", "name");
        //System.out.println(value);
        //GetAllProperties("Test.properties");
        WriteProperties("Test.properties","long", "212");
    }
}