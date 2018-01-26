package com.jd.ecc.autoconf;

import com.jd.ecc.autoconf.annotation.AutoConfFile;
import com.jd.ecc.autoconf.annotation.AutoConfItem;

/**
 * Redis配置类，模拟spring单例
 * Created by wangwenhao on 2018/1/10.
 */
@AutoConfFile(filename = "redis-pid-*.properties")
public class RedisConfPid {

    private RedisConfPid(){}

    private static RedisConfPid redisConf = new RedisConfPid();

    /**
     * 测试中约定必须有次方法
     * @return
     */
    public static RedisConfPid getInstance(){
        return redisConf;
    }

    @AutoConfItem(key="host")
    private String host;

    @AutoConfItem(key="port")
    private int port;

    @AutoConfItem(key="password")
    private String password;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
