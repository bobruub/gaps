package com.tcp;

import com.core.logger;

import org.apache.commons.lang3.StringUtils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.JsonObject;

import com.core.config;

public class tcpStub {

  private static config config;
  public static logger logger;
  static JsonObject configObject = null;

  public void mqStub() {

  }

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

      if (Name.equals("core")) {
        System.out.println("CONF: mqStub: setting config for: " + Name);
        JSONArray detailsArray = (JSONArray) configObject.get("details");
        JSONObject details = (JSONObject) detailsArray.get(0);
        String stubName = (String) details.get("stubName");
        config.setName(stubName);
        String threadPool = (String) details.get("threadPool");
        config.setThreadPool(Integer.parseInt(threadPool));
        config.setloglevel((String) details.get("logLevel"));
        System.out.println("CONF: mqStub: \tLog level: " + config.getLoglevel() + "");
        logger.info("mqStub: \tStub Name: " + config.getName(), config.getLoglevel());
        logger.info("mqStub: \tthread pool size: " + config.getThreadPool() + "", config.getLoglevel());
      } else if (Name.equals("tcp")) {
        System.out.println("CONF: mqStub: setting config for: " + Name);
        JSONArray detailsArray = (JSONArray) configObject.get("details");
        JSONObject details = (JSONObject) detailsArray.get(0);
        String tcpHostName = (String) details.get("tcpHostName");
        String tcpHostPort = (String) details.get("tcpHostPort");

        config.setTcpHostName(tcpHostName);
        config.setTcpHostPort(Integer.parseInt(tcpHostPort));

        logger.info("mqStub: \ttcpHostName: " + config.getTcpHostName(), config.getLoglevel());
        logger.info("mqStub: \ttcpHostPort: " + config.getTcpHostPort(), config.getLoglevel());

      } else if (Name.equals("redis")) {
        System.out.println("CONF: mqStub: setting config for: " + Name);
        JSONArray detailsArray = (JSONArray) configObject.get("details");
        JSONObject details = (JSONObject) detailsArray.get(0);
        String redisHostName = (String) details.get("redisHostName");
        String redisHostPort = (String) details.get("redisHostPort");
        config.setRedisHostName(redisHostName);
        config.setRedisHostPort(Integer.parseInt(redisHostPort));
        logger.info("mqStub: \tRedis Host: " + config.getRedisHostName(), config.getLoglevel());
        logger.info("mqStub: \tRedis Port: " + config.getRedisHostPort() + "", config.getLoglevel());

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
    tcpStub tcpStub = new tcpStub();
    tcpStub.RunIsolator();

  }

  ServerSocket getServerSocket() throws Exception {
    System.out
        .println("CONF: tcpStub: Preparing a regular TCP Server Socket on server:port " + config.getTcpHostPort());
    return new ServerSocket(config.getTcpHostPort());

  }

  public void RunIsolator() throws IOException {

    logger logger = new logger();
    // open redis
    if (!StringUtils.isEmpty(config.getRedisHostName())) {
      String redisServer = config.getRedisHostName();
      int redisPort = config.getRedisHostPort();
      try {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, redisServer, redisPort);
        Jedis jedis = jedisPool.getResource();
        logger.info("tcpStub: redis: " + redisServer + ":" + redisPort, config.getLoglevel());
        logger.info("tcpStub: Redis running. PING - " + jedis.ping(), config.getLoglevel());
        config.setJedisPool(jedisPool);
        jedis.close();
      } catch (Exception e) {
        logger.error("httpStub: error opening redis: " + e);
        return;
      }
    }

    ExecutorService executor = Executors.newFixedThreadPool(config.getThreadPool());

    // open connection
    try {
      ServerSocket serverSocket = getServerSocket();
      do {
        try {
          Socket clientConnection = serverSocket.accept();
          // Handle the connection with a separate thread
          
          if (clientConnection != null) {
            logger.info("tcpStub: received connection...",config.getLoglevel());
            Runnable tcpStubWorker = new tcpStubWorker(clientConnection,
                config);
            executor.execute(tcpStubWorker);
          }

        } catch (Exception e) {
          logger.error("httpStub: error opening connection: " + e);
          e.printStackTrace();
        }
      } while (true);
    } catch (Exception ex) {
      logger.error("tcpStub: Unable to listen on " + config.getTcpHostPort() + ".");
      ex.printStackTrace();
      System.exit(1);
    }

  }

}
