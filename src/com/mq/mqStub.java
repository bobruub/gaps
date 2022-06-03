package com.mq;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.core.utils;
import com.core.config;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import org.apache.commons.lang3.StringUtils;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class mqStub {

    // Create an HTTPStub for a particular TCP port
    public mqStub() {

    }

    private static utils myUtils;
    private static config config;
    static JsonObject configObject = null;

    public static void main(String[] args) throws Exception {
        myUtils = new utils();
        config = new config();
        //
        // load the configuration file
        //
        String fileName = "./config/config.json";
        utils.displayMsg("mqStub: opening file: " + fileName);
        String configString = null;
        try {
            // open the config.json file and load into an json object
            byte[] bytes = Files.readAllBytes(Paths.get(fileName));
            configString = new String(bytes);

            // InputStream fis = new FileInputStream(fileName);
            // JsonReader reader = Json.createReader(fis);
            // configObject = reader.readObject();
            // reader.close();
            // fis.close();

        } catch (Exception e) {
            System.out.println("httpStub: error processing file: " + fileName + "..." + e);
            System.exit(1);
        }
        //
        // set the config elements
        //
        // config.setName(configObject.getString("stubName"));
        // config.setThreadPool(configObject.getInt("threadPool"));

        JSONParser parser = null;
        JSONObject configVariables = null;
        JSONArray configArray = null;
        parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        configVariables = (JSONObject) parser.parse(configString);
        configArray = (JSONArray) configVariables.get("config");
        for (int x = 0; x < configArray.size(); x++) {
            JSONObject configObject = (JSONObject) configArray.get(x);
            String Name = (String) configObject.get("name");
            utils.displayMsg("mqStub: setting config for: " + Name);
            if (Name.equals("core")) {
                JSONArray detailsArray = (JSONArray) configObject.get("details");
                JSONObject details = (JSONObject) detailsArray.get(0);
                String stubName = (String) details.get("stubName");
                config.setName(stubName);
                String threadPool = (String) details.get("threadPool");
                config.setThreadPool(Integer.parseInt(threadPool));
                utils.displayMsg("mqStub: \tMQ Stub Name: " + config.getName());
                utils.displayMsg("mqStub: \tthread pool size: " + config.getThreadPool() + "");

            } else if (Name.equals("rabbitmq")) {
                JSONArray detailsArray = (JSONArray) configObject.get("details");
                JSONObject details = (JSONObject) detailsArray.get(0);
                String mqHostName = (String) details.get("mqHostName");
                String mqHostPort = (String) details.get("mqHostPort");
                String mqInQueue = (String) details.get("mqInQueue");
                String mqOutQueue = (String) details.get("mqOutQueue");
                config.setMQHostName(mqHostName);
                config.setMQHostPort(Integer.parseInt(mqHostPort));
                config.setMQInQueue(mqInQueue);
                config.setMQOutQueue(mqOutQueue);
                utils.displayMsg("mqStub: \tMQ Host: " + config.getMQHostName());
                utilss.displayMsg("mqStub: \tMQ Port: " + config.getMQHostPort() + "");
                utils.displayMsg("mqStub: \tMQ Request Queue: " + config.getMQInQueue());
                utils.displayMsg("mqStub: \tMQ Response Queue: " + config.getMQOutQueue());

            } else if (Name.equals("redis")) {
                JSONArray detailsArray = (JSONArray) configObject.get("details");
                JSONObject details = (JSONObject) detailsArray.get(0);
                String redisHostName = (String) details.get("redisHostName");
                String redisHostPort = (String) details.get("redisHostPort");
                config.setRedisHostName(redisHostName);
                config.setRedisHostPort(Integer.parseInt(redisHostPort));
                utils.displayMsg("mqStub: \tRedis Host: " + config.getRedisHostName());
                utils.displayMsg("mqStub: \tRedis Port: " + config.getRedisHostPort() + "");

            }
        }
        //
        // display the config name
        //
        utils.displayMsg("mqStub: starting: " + config.getName());
        //
        // get the requestresponse pairs
        //
        fileName = "./config/requestresponse.json";
        utils.displayMsg("mqStub: opening file: " + fileName);
        String requestResponseString = null;
        parser = null;
        JSONObject dataRequestResponse = null;
        JSONArray requestResponseArray = null;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(fileName));
            requestResponseString = new String(bytes);
            parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
            dataRequestResponse = (JSONObject) parser.parse(requestResponseString);
            requestResponseArray = (JSONArray) dataRequestResponse.get("response");
            config.setRequestResponseArray(requestResponseArray);
        } catch (Exception e) {
            utils.displayError("mqStub: error processing file: " + fileName + "..." + e);
            System.exit(1);
        }
        //
        // get the data variables
        //
        fileName = "./config/datavariables.json";
        utils.displayMsg("mqStub: opening file: " + fileName);
        String dataVariablesString = null;
        JSONObject dataVariables = null;
        JSONArray variableArray = null;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(fileName));
            dataVariablesString = new String(bytes);
            parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
            dataVariables = (JSONObject) parser.parse(dataVariablesString);
            variableArray = (JSONArray) dataVariables.get("variable");
            config.setDataVariableArray(variableArray);
        } catch (Exception e) {
            utils.displayError("mqStub: error processing file: " + fileName + "..." + e);
            System.exit(1);
        }
        //
        // kick of the listener and responder
        //
        mqStub mqStub = new mqStub();
        mqStub.RunIsolator();
    }

    public void RunIsolator() throws IOException {

        ExecutorService executor = Executors.newFixedThreadPool(config.getThreadPool());

        // open redis
        if (!StringUtils.isEmpty(config.getRedisHostName())) {
            String redisServer = config.getRedisHostName();
            int redisPort = config.getRedisHostPort();
            try {
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                JedisPool jedisPool = new JedisPool(poolConfig,redisServer,redisPort);
                Jedis jedis = jedisPool.getResource();
                System.out.println("httpStub: redis: " + redisServer + ":" + redisPort);
                System.out.println("httpStub: Redis running. PING - " + jedis.ping());
                config.setJedisPool(jedisPool);
                jedis.close();
            } catch (Exception e) {
                System.out.println("httpStub: error opening redis: " + e);
                return;
            }
        }
        ConnectionFactory factory = null;
        Channel channel = null;
        Connection connection = null;
        String QUEUE_NAME = config.getMQInQueue();
        //
        // connect to the mq server
        //
        try {
            utils.displayMsg("connecting to mq server : " + config.getMQHostName() + ":" + config.getMQHostPort());
            factory = new ConnectionFactory();
            factory.setHost(config.getMQHostName());
            factory.setPort(config.getMQHostPort());
            connection = factory.newConnection();
            channel = connection.createChannel();
            config.setMQChannel(channel);
        } catch (Exception e) {
            utils.displayError("mqStub: error connecting mq server : " + config.getMQHostName() + "..." + e);
            System.exit(1);
        }
        //
        // listen for a message
        //
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            //
            // a message has been found now process it in a seprate thread
            //
            String InMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Runnable mqStubWorker = new mqStubWorker(config, InMessage);
            executor.execute(mqStubWorker);

        };
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
        });

    }

}
