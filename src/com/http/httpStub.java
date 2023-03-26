package com.http;

import com.core.stubWorker;
import com.http.httpStubWorker;

import org.apache.commons.lang3.StringUtils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import com.core.logger;
import com.core.config;

/**
class: httpStub
Purpose: implements a http stup, sets up a socket and listens for incoming messages
Notes:
Author: Tim Lane
Date: 07/05/2022
**/
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Security;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class httpStub {

  private ServerSocket serverSocket;
  static JsonObject configObject = null;
  static JsonObject requestResponseObject = null;
  static String requestResponseString = null;
  static String dataVariablesString = null;
  static JsonObject dataVariableObject = null;
  static int threadCount = 0;
  static int port = 0;
  static int contentFirstPos = 0;
  static int contentLastPos = 0;
  static String redisServer = null;
  static int redisPort = 0;
  static Jedis jedis;
  static JedisPool jedisPool;
  static String httpStubVersion = "1.7";
  static boolean jedisRequired = false;
  static boolean securePort = false;

  private static config config;
  public static logger logger;

  // Create an HTTPStub for a particular TCP port
  public httpStub() {
    // this.httpProperties = httpProperties;
    // this.logFileProperties = logFileProperties;
  }

  public static void main(String[] args) throws ParseException {

    //
    // load the configuration file
    //
    logger logger = new logger();
    config = new config();
    //
    // load the configuration file
    //
    String fileName = "./config/config.json";

    System.out.println("CONF: httpStub: opening file: " + fileName);
    String configString = null;
    try {
      // open the config.json file and load into an json object
      byte[] bytes = Files.readAllBytes(Paths.get(fileName));
      configString = new String(bytes);

    } catch (Exception e) {
      System.out.println("ERRR: httpStub: error processing file: " + fileName + "..." + e);
      System.exit(1);
    }
    JSONParser parser = null;
    JSONObject configVariables = null;
    JSONArray configArray = null;
    parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    configVariables = (JSONObject) parser.parse(configString);
    configArray = (JSONArray) configVariables.get("config");
    for (int x = 0; x < configArray.size(); x++) {
      JSONObject configObject = (JSONObject) configArray.get(x);
      String Name = (String) configObject.get("name");
      System.out.println("CONF: httpStub: setting config for: " + Name);
      if (Name.equals("core")) {
        JSONArray detailsArray = (JSONArray) configObject.get("details");
        JSONObject details = (JSONObject) detailsArray.get(0);
        String stubName = (String) details.get("stubName");
        config.setName(stubName);
        String threadPool = (String) details.get("threadPool");
        config.setThreadPool(Integer.parseInt(threadPool));
        config.setloglevel((String) details.get("logLevel"));
        System.out.println("CONF: httpStub: \tLog level: " + config.getLoglevel() + "");
        logger.conf("httpStub: \tStub Name: " + config.getName());
        logger.conf("httpStub: \tthread pool size: " + config.getThreadPool() + "");
      } else if (Name.equals("http")) {
        JSONArray detailsArray = (JSONArray) configObject.get("details");
        JSONObject details = (JSONObject) detailsArray.get(0);
        String httpHostName = (String) details.get("httpHostName");
        String httpHostPort = (String) details.get("httpHostPort");
        String socketTimeout = (String) details.get("socketTimeout");
        String clientTimeout = (String) details.get("clientTimeout");

        config.setHttpHostName(httpHostName);
        config.setHttpHostPort(Integer.parseInt(httpHostPort));
        config.setHttpSocketTimeout(Integer.parseInt(socketTimeout));
        config.setHttpClientTimeout(Integer.parseInt(clientTimeout));

        logger.conf("httpStub: \thttpHostName: " + config.getHttpHostName());
        logger.conf("httpStub: \thttpHostPort: " + config.getHttpHostPort());
        logger.conf("httpStub: \thttpSocketTimeOut: " + config.getHttpSocketTimeout());
        logger.conf("httpStub: \thttpClientTimeout: " + config.getHttpClientTimeout());

        // optional https requirements
        if (details.containsKey("sslKeyName")) {
          config.setSslKeyName((String) details.get("sslKeyName"));
          config.setSslKeyPassword((String) details.get("sslKeyPassword"));
          logger.conf("httpStub: \tsslKeyName: " + config.getSslKeyName());
          logger.conf("httpStub: \tsslKeyPassword: " + config.getSslKeyPassword() + "");
        }

      } else if (Name.equals("redis")) {

        JSONArray detailsArray = (JSONArray) configObject.get("details");
        JSONObject details = (JSONObject) detailsArray.get(0);
        String redisHostName = (String) details.get("redisHostName");
        String redisHostPort = (String) details.get("redisHostPort");
        config.setRedisHostName(redisHostName);
        config.setRedisHostPort(Integer.parseInt(redisHostPort));
        logger.conf("httpStub: \tRedis Host: " + config.getRedisHostName());
        logger.conf("httpStub: \tRedis Port: " + config.getRedisHostPort() + "");
      }
    }

    //
    // get the requestresponse pairs
    //
    fileName = "./config/requestresponse.json";
    logger.conf("httpStub: opening file: " + fileName);
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
      logger.error("httpStub: error processing file: " + fileName + "..." + e);
      System.exit(1);
    }
    //
    // get the data variables
    //
    fileName = "./config/datavariables.json";
    logger.conf("httpStub: opening file: " + fileName);
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
      logger.error("httpStub: error processing file: " + fileName + "..." + e);
      System.exit(1);
    }

    httpStub httpStub = new httpStub();
    httpStub.RunIsolator();

  }

  ServerSocket getServerSocket(logger logger) throws Exception {
    logger.conf("httpStub: Preparing a regular HTTP Server Socket on server:port " + config.getHttpHostPort());
    return new ServerSocket(config.getHttpHostPort());

  }

  ServerSocket getSslServerSocket(logger logger) throws Exception {
    logger.conf("httpStub: Preparing a Secure HTTPS Server Socket on server:port " + config.getHttpHostPort());
    // A keystore is where keys and certificates are kept
    // Both the keystore and individual private keys should be password protected
    KeyStore keystore = KeyStore.getInstance("JKS");
    String sslKeyName = configObject.getString("sslKeyName");
    String sslKeyPassword = configObject.getString("sslKeyPassword");
    keystore.load(new FileInputStream(sslKeyName), sslKeyPassword.toCharArray());
    // A KeyManagerFactory is used to create key managers
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(keystore, sslKeyPassword.toCharArray());
    // An SSLContext is an environment for implementing JSSE
    // It is used to create a ServerSocketFactory
    SSLContext sslc = SSLContext.getInstance("SSLv3");
    // Initialize the SSLContext to work with our key managers
    sslc.init(kmf.getKeyManagers(), null, null);
    // Create a ServerSocketFactory from the SSLContext
    SSLServerSocketFactory ssf = sslc.getServerSocketFactory();
    // Create a ServerSocketFactory from the SSLContext
    SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
    // return the socket
    return serverSocket;
  }

  public void RunIsolator() {
    //
    // setup redis server
    //
    logger logger = new logger();
    // open redis
    if (!StringUtils.isEmpty(config.getRedisHostName())) {
      String redisServer = config.getRedisHostName();
      int redisPort = config.getRedisHostPort();
      try {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, redisServer, redisPort);
        Jedis jedis = jedisPool.getResource();
        logger.conf("httpStub: redis: " + redisServer + ":" + redisPort + " - PING - " + jedis.ping());
        logger.conf("httpStub: redis: creating redis pool...");
        config.setJedisPool(jedisPool);
        jedis.close();
      } catch (Exception e) {
        logger.error("httpStub: error opening redis: " + redisServer + ":" + redisPort + " - " + e);
        return;
      }
    }
    /*
     * setup thread pool
     */
    ExecutorService executor = Executors.newFixedThreadPool(config.getThreadPool());

    boolean socketLoop = true;
    boolean connectionLoop = true;
    //
    // wait for a socket call
    //
    while (socketLoop) {

      serverSocket = null;
      try {
        // open a socket
        if (securePort) {
          serverSocket = getSslServerSocket(logger);
        } else {
          serverSocket = getServerSocket(logger);
        }
        serverSocket.setSoTimeout(5 * 1000);
      } catch (Exception e) {
        logger.error("httpStub: Unable to listen on " + port + ":" + e);
        e.printStackTrace();
        System.exit(1);
      }
      // * listen for connections...
      Socket clientConnection = null;
      while (connectionLoop) {
        try {
          // accept connections a connection on a new socket
          clientConnection = serverSocket.accept();
          clientConnection.setSoTimeout(5 * config.getHttpClientTimeout());
          // Handle the connection with a separate thread
          if (clientConnection != null) {
            logger.debug("httpStub: received connection...",config.getLoglevel());
            Runnable httpStubWorker = new httpStubWorker(clientConnection,
                config);
            executor.execute(httpStubWorker);
          }
        } catch (SocketTimeoutException e) {
          // System.out.println("socket timeout " + connectionLoopCntr + ".");
          // DO NOTHING - The timeout just allows the checking of the restart
          // request and will only close the socket server if a restart request
          // has been issued
        } catch (Exception e) {
          logger.error("httpStub: socket exception. " + e);
          e.printStackTrace();
        } finally {
          //try {
            //if (clientConnection != null) {
              //System.out.println(config.getLoglevel());
              //logger.info("httpStub: closing connection...",config.getLoglevel());
              //clientConnection.close();
            //}
//        } catch (Exception e){
  //          logger.error("httpStub: socket close exception. " + e);
    //      }  
        }
      }
      /*
       * shutdown threads
       */
      executor.shutdown();
      while (!executor.isTerminated()) {
      }

    }

  }

}
