package com.jd.ecc.autoconf.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件操作类
 */
public class FileUtil {

    protected static final Logger log = LoggerFactory.getLogger(FileUtil.class);
    String newline = "\r\n";//windows

    /**
     * 写入文件,末尾自动添加\r\n
     * utf-8  追加
     * @param path
     * @param str
     */
    public static void write(String path, String str)
    {
        try
        {
            str = str.replace("\\n", System.getProperty("line.separator"));
            File file = new File(path);
            if(!file.exists())
                file.createNewFile();
            FileOutputStream out = new FileOutputStream(file); //true表示追加 
            StringBuffer sb = new StringBuffer();
            sb.append(str + "\r\n");
            out.write(sb.toString().getBytes("utf-8"));//
            out.close();
        }
        catch(IOException ex)
        {
            log.error("写入文件发生异常", ex);
        }
    }
    
    /**
     * 写入文件,末尾自动添加\r\n
     * @param path
     * @param str
     */
    public static void write(String path, String str, boolean is_append, String encode)
    {
        try
        {
            File file = new File(path);
            if(!file.exists())
                file.createNewFile();
            FileOutputStream out = new FileOutputStream(file, is_append); //true表示追加 
            StringBuffer sb = new StringBuffer();
            sb.append(str + "\r\n");
            out.write(sb.toString().getBytes(encode));//
            out.close();
        }
        catch(IOException ex)
        {
            log.error("写入文件发生异常", ex);
        }
    }
    /**
     * 读取文件，添加\r\n换行
     * @param path
     * @return
     */
    public static String readLogByString(String path)
    {
        StringBuffer sb=new StringBuffer();
        String tempstr=null;
        try {
            File file=new File(path);
            if(!file.exists())
                throw new FileNotFoundException();
            FileInputStream fis=new FileInputStream(file);
            BufferedReader br=new BufferedReader(new InputStreamReader(fis, "utf-8"));
            while((tempstr=br.readLine())!=null) {
                sb.append(tempstr + "\r\n");
            }
        } catch(IOException ex) {
            log.error("读取文件发生异常", ex);
        }
        return sb.toString();
    }
    
    /**
     * 加入编码
     * 整个文件以string放回，添加\r\n换行
     * @param path
     * @return
     */
    public static String readLogByStringAndEncode(String path, String encode)
    {
        StringBuffer sb=new StringBuffer();
        String tempstr=null;
        try {
            File file=new File(path);
            if(!file.exists())
                throw new FileNotFoundException();
            FileInputStream fis=new FileInputStream(file);
            BufferedReader br=new BufferedReader(new InputStreamReader(fis, encode));
            while((tempstr=br.readLine())!=null) {
                sb.append(tempstr + "\r\n");
            }
        } catch(IOException ex) {
            log.error("加入编码读取文件发生异常", ex);
        }
        return sb.toString();
    }
    
    /**
     * 按行读取文件，以list<String>的形式返回
     * @param path
     * @return
     */
    public static List<String> readLogByList(String path) {
        List<String> lines = new ArrayList<String>();
        String tempstr = null;
        try {
            File file = new File(path);
            if(!file.exists()) {
                throw new FileNotFoundException();
            }
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "utf-8"));
            while((tempstr = br.readLine()) != null) {
                lines.add(tempstr.toString());
            }
        } catch(IOException ex) {
            log.error("按行读取文件发生异常", ex);
        }
        return lines;
    }
    
    /**
     * 创建目录
     * @param dir_path
     */
    public static void mkDir(String dir_path) {
        File myFolderPath = new File(dir_path);   
        try {   
            if (!myFolderPath.exists()) {   
               myFolderPath.mkdir();   
            }   
        } catch (Exception e) {
            log.error("新建目录发生异常", e);
        }  
    }
    
    /**
     * 创建文件
     * @param file_path
     */
    public static void createNewFile(String file_path) {  
        File myFilePath = new File(file_path);   
        try {   
            if (!myFilePath.exists()) {   
                myFilePath.createNewFile();   
            } 
        }   
        catch (Exception e) {
            log.error("新建目录发生异常", e);
        }  
    }
    
    /**
     * 递归删除文件或者目录
     * @param file_path
     */
    public static void deleteEveryThing(String file_path) {
    try{
        File file=new File(file_path);
        if(!file.exists()){
            return ;
        }
        if(file.isFile()){
            file.delete();
        }else{
            File[] files = file.listFiles();
            for(int i=0;i<files.length;i++){
                String root=files[i].getAbsolutePath();//得到子文件或文件夹的绝对路径
                deleteEveryThing(root);
            }
            file.delete();
        }
    } catch(Exception e) {
        log.error("删除文件发生异常", e);
    }
    }
    
    /*
     * 得到一个文件夹下所有文件
     */
    public static List<String> getAllFileNameInFold(String fold_path) {
        List<String> file_paths = new ArrayList<String>();
        
        LinkedList<String> folderList = new LinkedList<String>();   
        folderList.add(fold_path);   
        while (folderList.size() > 0) {   
            File file = new File(folderList.peekLast());   
            folderList.removeLast();   
            File[] files = file.listFiles();   
            ArrayList<File> fileList = new ArrayList<File>();   
            for (int i = 0; i < files.length; i++) {   
                if (files[i].isDirectory()) {   
                    folderList.add(files[i].getPath());   
                } else {   
                    fileList.add(files[i]);   
                }   
            }   
            for (File f : fileList) {   
                file_paths.add(f.getAbsoluteFile().getPath());   
            }   
        }   
        return file_paths;
    }

}