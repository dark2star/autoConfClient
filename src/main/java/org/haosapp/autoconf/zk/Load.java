package org.haosapp.autoconf.zk;

import org.haosapp.autoconf.annotation.AutoConfField;
import org.haosapp.autoconf.annotation.AutoConfFile;
import org.haosapp.autoconf.annotation.ClasspathPackageScanner;
import org.haosapp.autoconf.annotation.Parent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangwenhao on 2018/1/10.
 */
public class Load {

    private Load(){}

    private static final Map<String, List<AutoConfField>> confClassMap = new HashMap<String, List<AutoConfField>>();

    public static void initScan(String sapceName) throws ClassNotFoundException, IOException {
        ClasspathPackageScanner scan = new ClasspathPackageScanner(sapceName);
        List<String> list = scan.getFullyQualifiedClassNameList();
        for (String str : list) {
            Class cs = Class.forName(str);
            AutoConfFile acf = (AutoConfFile) cs.getAnnotation(AutoConfFile.class);
            if (acf == null) {
                continue;
            }
            Parent c = new Parent(cs);
            List<AutoConfField> autoConfFieldList = c.init();//获取泛型中类里面的注解
            confClassMap.put(acf.filename(), autoConfFieldList);
        }
    }

    public static void setConf(String fileName, Map<String, Object> confMap, Object obj) throws IllegalAccessException {
        List<AutoConfField> autoConfFieldList = confClassMap.get(fileName);
        for(AutoConfField acf : autoConfFieldList){
            Object value = confMap.get(acf.getMeta().key());
            acf.getField().set(obj, value);
        }
    }

}
