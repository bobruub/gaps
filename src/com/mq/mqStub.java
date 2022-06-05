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

import com.core.logger;
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

    public mqStub() {

    }

    private static config config;
    public static logger logger;
    static JsonObject configObject = null;

    public static void main(String[] args) throws Exception {
        logger logger = new logger();
        config = new config();
        //
        // load the configuration file
        //
        String fileName = "./config/config.json";
        logger.info("mqStub: opening file: " + fileName);
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
            System.out.println("mqStub: error processing file: " + fileName + "..." + e);
            System.exit(1);
        }
        //
        // set the config elements
        //
        JSONParser parser = null;
        JSONObject configVariables = null;
        JSONArray configArray = null;
        parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        configVariables = (JSONObject) parser.parse(configString);
        configArray = (JSONArray) configVariables.get("config");
        for (int x = 0; x < configArray.size(); x++) {
          JSONObject configObject = (JSONObject) configArray.get(x);
          String Name = (String) configObject.get("name");
          System.out.println("CONF: mqStub: setting config for: " + Name);
          if (Name.equals("core")) {
            JSONArray detailsArray = (JSONArray) configObject.get("details");
            JSONObject details = (JSONObject) detailsArray.get(0);
            String stubName = (String) details.get("stubName");
            config.setName(stubName);
            String threadPool = (String) details.get("threadPool");
            config.setThreadPool(Integer.parseInt(threadPool));
            config.setloglevel((String) details.get("logLevel"));
            System.out.println("CONF: mqStub: \tLog level: " + config.getLoglevel() + "");
            logger.info("mqStub: \tStub Name: " + config.getName(),config.getLoglevel());
            logger.info("mqStub: \tthread pool size: " + config.getThreadPool() + "",config.getLoglevel());
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
    
            logger.info("mqStub: \tmqHostName: " + config.getMQHostName(),config.getLoglevel());
            logger.info("mqStub: \tmqHostPort: " + config.getMQHostPort(),config.getLoglevel());
            logger.info("mqStub: \tmq In Queue: " + config.getMQInQueue(),config.getLoglevel());
            logger.info("mqStub: \tmq Out Queue: " + config.getMQOutQueue(),config.getLoglevel());
    
            // optional https requirements
            if (details.containsKey("sslKeyName")) {
              config.setSslKeyName((String) details.get("sslKeyName"));
              config.setSslKeyPassword((String) details.get("sslKeyPassword"));
              logger.info("mqStub: \tsslKeyName: " + config.getSslKeyName(),config.getLoglevel());
              logger.info("mqStub: \tsslKeyPassword: " + config.getSslKeyPassword() + "",config.getLoglevel());
            }
    
          } else if (Name.equals("redis")) {
            JSONArray detailsArray = (JSONArray) configObject.get("details");
            JSONObject details = (JSONObject) detailsArray.get(0);
            String redisHostName = (String) details.get("redisHostName");
            String redisHostPort = (String) details.get("redisHostPort");
            config.setRedisHostName(redisHostName);
            config.setRedisHostPort(Integer.parseInt(redisHostPort));
            logger.info("mqStub: \tRedis Host: " + config.getRedisHostName(),config.getLoglevel());
            logger.info("mqStub: \tRedis Port: " + config.getRedisHostPort() + "",config.getLoglevel());
    
          }
        }
            //
        // display the config name
        //
        logger.info("mqStub: starting: " + config.getName());
        //
        // get the requestresponse pairs
        //
        fileName = "./config/requestresponse.json";
        System.out.println("CONF: mqStub: opening file: " + fileName);
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
            logger.info("mqStub: error processing file: " + fileName + "..." + e);
            System.exit(1);
        }
        //
        // get the data variables
        //
        fileName = "./config/datavariables.json";
        System.out.println("CONF: mqStub: opening file: " + fileName);
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
            logger.error("mqStub: error processing file: " + fileName + "..." + e);
            System.exit(1);
        }
        //
        // kick of the listener and responder
        //
        mqStub mqStub = new mqStub();
        mqStub.RunIsolator();
    }

    public void RunIsolator() throws IOException {

        logger logger = new logger();
        
        ExecutorService executor = Executors.newFixedThreadPool(config.getThreadPool());

        // open redis
        if (!StringUtils.isEmpty(config.getRedisHostName())) {
            String redisServer = config.getRedisHostName();
            int redisPort = config.getRedisHostPort();
            try {
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                JedisPool jedisPool = new JedisPool(poolConfig,redisServer,redisPort);
                Jedis jedis = jedisPool.getResource();
                logger.info("mqStub: Redis: " + redisServer + ":" + redisPort,config.getLoglevel());
                logger.info("mqStub: Redis running. PING - " + jedis.ping(),config.getLoglevel());
                config.setJedisPool(jedisPool);
                jedis.close();
            } catch (Exception e) {
                logger.error("mqStub: error opening redis: " + e);
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
            logger.info("mqStub: connecting to mq server : " + config.getMQHostName() + ":" + config.getMQHostPort(),config.getLoglevel());
            factory = new ConnectionFactory();
            factory.setHost(config.getMQHostName());
            factory.setPort(config.getMQHostPort());
            connection = factory.newConnection();
            channel = connection.createChannel();
            config.setMQChannel(channel);
        } catch (Exception e) {
            logger.info("mqStub: error connecting mq server : " + config.getMQHostName() + "..." + e);
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