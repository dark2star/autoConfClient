package com.jd.ecc.autoconf;

import com.jd.ecc.autoconf.annotation.AutoConf;
import com.jd.ecc.autoconf.annotation.AutoConfField;
import com.jd.ecc.autoconf.annotation.AutoConfFile;
import com.jd.ecc.autoconf.annotation.ClasspathPackageScanner;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestAnnotation {

    public static void main(String[] args) throws IllegalAccessException, IOException, ClassNotFoundException {

		Anno anno = new Anno();
		Map<String, List<AutoConfField>> confClassMap = new HashMap<String, List<AutoConfField>>();

		ClasspathPackageScanner scan = new ClasspathPackageScanner("org.haosapp.autoconf");
		List<String> list = scan.getFullyQualifiedClassNameList();
		for (String str : list){
			Class cs = Class.forName(str);
			AutoConfFile acf = (AutoConfFile)cs.getAnnotation(AutoConfFile.class);
			if(acf == null){
				continue;
			}
			System.out.println(acf.filename());
			AutoConf c = new AutoConf(cs);
			List<AutoConfField> autoConfFieldList = c.init();//获取泛型中类里面的注解
			confClassMap.put(acf.filename(), autoConfFieldList);
			//输出结果
			for(AutoConfField l : autoConfFieldList){
				System.out.println("字段名称："+l.getName()+"\t字段类型："+l.getType()+
						"\t注解的key："+l.getMeta().key());
				if("id".equals(l.getName())){
					System.out.println("值1="+l.getField().get(anno));
					l.getField().set(anno, 2);
					System.out.println("值2="+l.getField().get(anno));
				}
			}
		}
		/*Parent c = new Parent(Anno.class);
		List<AutoConfField> list = c.init();//获取泛型中类里面的注解
		//输出结果
		for(AutoConfField l : list){
			System.out.println("字段名称："+l.getName()+"\t字段类型："+l.getType()+
					"\t注解的key："+l.getMeta().key());
			if("id".equals(l.getName())){
				System.out.println("值1="+l.getField().get(anno));
				l.getField().set(anno, 2);
				System.out.println("值2="+l.getField().get(anno));
			}
		}*/
	}
}