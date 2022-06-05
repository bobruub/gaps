package com.core;

import net.minidev.json.JSONArray;
import redis.clients.jedis.JedisPool;

import com.rabbitmq.client.Channel;


public class config {

    private String Name;
    private String mqHostName;
    private int mqHostPort;
    private String mqInQueue;
    private String mqOutQueue;
    private JSONArray requestResponseArray;
    private JSONArray dataVariableArray;
    private Channel mqChannel;
    private int threadPool;
    private JedisPool jedisPool;
    private String redisHostName;
    private int redisHostPort;
    private String httpHostName;
    private int httpHostPort;
    private int httpSocketTimeout;
    private int httpClientTimeout;
    private String httpSslKeyName;
    private String httpSslKeyPassword;
    private String loglevel;
    private int tcpHostPort;
    private String tcpHostName;

//    
// setter and getter for name of the stub
//
    public String getName() {
        return this.Name;
    }
  
    public void setName(String name) {
        this.Name = name;
    }
    //    
// setter and getter for name of the MQ Details
//
public String getMQHostName() {
    return this.mqHostName;
}

public void setMQHostName(String mqHostName) {
    this.mqHostName = mqHostName;
}

public int getMQHostPort() {
    return this.mqHostPort;
}

public void setMQHostPort(int mqHostPort) {
    this.mqHostPort = mqHostPort;
}

public String getMQInQueue() {
    return this.mqInQueue;
}

public void setMQInQueue(String mqInQueue) {
    this.mqInQueue = mqInQueue;
}

public String getMQOutQueue() {
    return this.mqOutQueue;
}

public void setMQOutQueue(String mqOutQueue) {
    this.mqOutQueue = mqOutQueue;
}

public JSONArray getDataVariableArray() {
    return this.dataVariableArray;
}

public void setDataVariableArray(JSONArray dataVariableArray) {
    this.dataVariableArray = dataVariableArray;
}

public JSONArray getRequestResponseArray() {
    return this.requestResponseArray;
}

public void setRequestResponseArray(JSONArray requestResponseArray) {
    this.requestResponseArray = requestResponseArray;
}

public Channel getMQChannel() {
    return this.mqChannel;
}

public void setMQChannel(Channel mqChannel) {
    this.mqChannel = mqChannel;
}

public int getThreadPool() {
    return this.threadPool;
}

public void setThreadPool(int threadPool) {
    this.threadPool = threadPool;
}

public JedisPool getJedisPool() {
    return this.jedisPool;
}

public void setJedisPool(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
}

public String getRedisHostName() {
    return this.redisHostName;
}

public void setRedisHostName(String redisHostName) {
    this.redisHostName = redisHostName;
}

public int getRedisHostPort() {
    return this.redisHostPort;
}

public void setRedisHostPort(int redisHostPort) {
    this.redisHostPort = redisHostPort;
}

//

public String getHttpHostName() {
    return this.httpHostName;
}

public void setHttpHostName(String httpHostName) {
    this.httpHostName = httpHostName;
}

public int getHttpHostPort() {
    return this.httpHostPort;
}

public void setHttpHostPort(int httpHostPort) {
    this.httpHostPort = httpHostPort;
}


public int getHttpSocketTimeout() {
    return this.httpSocketTimeout;
}

public void setHttpSocketTimeout(int httpSocketTimeout) {
    this.httpSocketTimeout = httpSocketTimeout;
}

public int getHttpClientTimeout() {
    return this.httpClientTimeout;
}

public void setHttpClientTimeout(int httpClientTimeout) {
    this.httpClientTimeout = httpClientTimeout;
}

public String getSslKeyName() {
    return this.httpSslKeyName;
}

public void setSslKeyName(String httpSslKeyName) {
    this.httpSslKeyName = httpSslKeyName;
}

public String getSslKeyPassword() {
    return this.httpSslKeyPassword;
}

public void setSslKeyPassword(String httpSslKeyPassword) {
    this.httpSslKeyPassword = httpSslKeyPassword;
}

public String getLoglevel() {
    return this.loglevel;
}

public void setloglevel(String loglevel) {
    this.loglevel = loglevel;
}
//

public String getTcpHostName() {
    return this.tcpHostName;
}

public void setTcpHostName(String tcpHostName) {
    this.tcpHostName = tcpHostName;
}

public int getTcpHostPort() {
    return this.tcpHostPort;
}

public void setTcpHostPort(int tcpHostPort) {
    this.tcpHostPort = tcpHostPort;
}


}
