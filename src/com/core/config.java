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


}
