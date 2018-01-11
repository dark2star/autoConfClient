package org.haosapp.autoconf.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Parent {

	private Class entity;


	public Parent(Class entity) {
		this.entity = entity;
        //init();
	}

	@SuppressWarnings("unchecked")
	public List<AutoConfField> init(){
		List<AutoConfField> list = new ArrayList<AutoConfField>();
		if(this.entity!=null){
			
			/**返回类中所有字段，包括公共、保护、默认（包）访问和私有字段，但不包括继承的字段
			 * entity.getFields();只返回对象所表示的类或接口的所有可访问公共字段
			 * 在class中getDeclared**()方法返回的都是所有访问权限的字段、方法等；
			 * 可看API
			 * */
			Field[] fields = entity.getDeclaredFields();
//			
			for(Field f : fields){
				f.setAccessible(true);
				//获取字段中包含fieldMeta的注解
				AutoConfItem meta = f.getAnnotation(AutoConfItem.class);
				if(meta!=null){
					AutoConfField af = new AutoConfField(meta, f);
					list.add(af);
				}
			}
			
			//返回对象所表示的类或接口的所有可访问公共方法
			Method[] methods = entity.getMethods();
			
			for(Method m:methods){
				AutoConfItem meta = m.getAnnotation(AutoConfItem.class);
				if(meta!=null){
					AutoConfField af = new AutoConfField(meta,m.getName(),m.getReturnType());
					list.add(af);
				}
			}
		}
		return list;
		
	}
}