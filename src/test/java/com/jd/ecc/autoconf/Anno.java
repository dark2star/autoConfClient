package com.jd.ecc.autoconf;

import com.jd.ecc.autoconf.annotation.AutoConfItem;
import com.jd.ecc.autoconf.annotation.AutoConfFile;

@AutoConfFile(filename = "test.xml")
public class Anno {

	@AutoConfItem(key="序列号")
    private int id;
	@AutoConfItem(key="姓名")
	private String name;
	@AutoConfItem(key="年龄")
	private int age;
	
	public String desc(){
		return "java反射获取annotation的测试";
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	
}