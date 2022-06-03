package com.mq;

import com.core.stubWorker;
import com.core.utils;
import com.core.config;

import java.io.IOException;

// import javax.json.Json;
// import javax.json.JsonObject;
// import javax.json.JsonReader;

// import com.rabbitmq.client.ConnectionFactory;
// import com.rabbitmq.client.DeliverCallback;
// import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import org.apache.commons.lang3.StringUtils;

import net.minidev.json.JSONArray;
import redis.clients.jedis.Jedis;
// import net.minidev.json.JSONObject;
// import net.minidev.json.parser.JSONParser;
import redis.clients.jedis.JedisPool;

public class mqStubWorker extends stubWorker implements Runnable {

    private JSONArray requestResponseArray;
    private JSONArray dataVariableArray;
    private String inMessage;
    private String outQueueName;
    private int defaultPause;
    private String responseMsg;
    private Channel channel;
    private config config;

    public mqStubWorker(config config,
            String inMessage) {
        this.config = config;
        this.requestResponseArray = this.config.getRequestResponseArray();
        this.dataVariableArray = this.config.getDataVariableArray();
        this.inMessage = inMessage;
        this.outQueueName = this.config.getMQOutQueue();
        this.channel = this.config.getMQChannel();
    }

    @Override
    public void run() {

        //
        // setup redis pool
        //
        Jedis jedis = null;
        if (!StringUtils.isEmpty(config.getRedisHostName())) {
            JedisPool jedisPool = config.getJedisPool() ;
            jedis = jedisPool.getResource();
        }        
        
        utils.displayMsg("mqStubWorker: inbound message: " + inMessage);
        //
        // determine which request response template to use
        //
        boolean responseTemplateMessage = setResponseTemplate(inMessage, requestResponseArray);
        if (responseTemplateMessage) {
            responseMsg = getTemplate("templateContents");
            defaultPause = Integer.parseInt(getTemplate("templatePause"));
            utils.displayMsg("mqStubWorker: template match: " + getTemplate("templateName"));
        }
        //
        // now we have identified the template to use process it replacing any varibales
        // %varname% it may contain
        //
        responseMsg = processVariables(inMessage, dataVariableArray, responseMsg,jedis);
        utils.displayMsg("mqStubWorker: response message: " + responseMsg);
        //
        // now delay for the pause time
        //
        try {
            Thread.sleep(defaultPause);
        } catch (InterruptedException e) {
            utils.displayError("mqStubWorker: Error in thread sleep : " + e);
        }
        //
        // now write the response
        //
        try {
            channel.queueDeclare(outQueueName, true, false, false, null);
            channel.basicPublish("", outQueueName, null, responseMsg.getBytes());
        } catch (IOException e) {
            utils.displayError("mqStubWorker: error writing to : " + outQueueName + " : " + e);
            e.printStackTrace();
        }

    }
}
