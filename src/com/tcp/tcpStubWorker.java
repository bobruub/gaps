package com.tcp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.core.config;
import com.core.logger;
import com.core.stubWorker;

import org.apache.commons.lang3.StringUtils;

import net.minidev.json.JSONArray;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class tcpStubWorker extends stubWorker implements Runnable {


    private final Socket clientSocket;
    private config config;
    PrintWriter out = null;
    BufferedReader in = null;
    private JSONArray requestResponseArray;
    private JSONArray dataVariableArray;


    public tcpStubWorker(Socket clientSocket,
      config config) {
    this.clientSocket = clientSocket;
    this.config = config;
    this.requestResponseArray = this.config.getRequestResponseArray();
    this.dataVariableArray = this.config.getDataVariableArray();    

  }

  public void run() {
    logger logger = new logger();
    logger.info("tcpStubWorker: processing connection....");
    
    String destName = "client";
    String destAddr = clientSocket.getInetAddress().getHostAddress();
    int destPort = clientSocket.getPort();
    logger.info("Accepted connection to "+destName+" (" + destAddr + ")" + " on port "+destPort+".");

    Jedis jedis = null;
    if (!StringUtils.isEmpty(config.getRedisHostName())) {
        JedisPool jedisPool = config.getJedisPool() ;
        jedis = jedisPool.getResource();
    }   

       //
    // create an input and output socket
    //
    try {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      } catch (Exception e) {
        logger.error("httpStubWorker: error opening socket: " + e);
        return;
      }

    // get the input message
    tcpInputStream inStream;
    String inputMsgLines = null;
    try{
        inStream = new tcpInputStream(clientSocket.getInputStream());  
        // assume tcp is a single line
        inputMsgLines = inStream.readLine();
    } catch (Exception e) {
        logger.error("httpStubWorker: error reading socket: input " + e);
        return;
    }
    logger.info("httpStubWorker: socket input " + inputMsgLines.toString(), config.getLoglevel());

    //
    // got the message, lets determine with template to use
    //
    String responseMsg = null;
    int defaultPause = 0;
    boolean responseTemplateMessage = setResponseTemplate(inputMsgLines.toString(),requestResponseArray);
    if (responseTemplateMessage) {
      responseMsg = getTemplate("templateContents");
      defaultPause = Integer.parseInt(getTemplate("templatePause"));
      logger.info("tcpStubWorker: Processing: " + getTemplate("templateName"));
    }
    //
    // got the template, lets process the variables
    //
    logger.info("tcpStubWorker: Processing: " + responseMsg,config.getLoglevel());
    responseMsg = processVariables(inputMsgLines.toString(), dataVariableArray, responseMsg, jedis);
    logger.info("tcpStubWorker: Processed: " + responseMsg,config.getLoglevel());

    //
    // default pause
    //
    try {
        Thread.sleep(defaultPause);
    } catch (InterruptedException e) {
        logger.error("tcpStubWorker: Error in thread sleep : " + e);
        e.printStackTrace();
    }

    try{
        logger.info("tcpStubWorker: Writing: " + responseMsg,config.getLoglevel());
        BufferedOutputStream outStream = new BufferedOutputStream(clientSocket.getOutputStream());
        outStream.write(responseMsg.getBytes());

    } catch (IOException e) {
        logger.error("tcpStubWorker: Error in writing : " + e);
        e.printStackTrace();
    }


  }

    
}
