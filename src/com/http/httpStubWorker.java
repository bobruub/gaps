package com.http;

import javax.json.JsonObject;

import com.core.stubWorker;

import org.apache.commons.lang3.StringUtils;

import net.minidev.json.JSONArray;

import com.core.logger;
import com.core.config;

import javax.json.JsonObject;
import java.util.*;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.*;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class httpStubWorker extends stubWorker implements Runnable {
  
  private final Socket clientSocket;
  private JedisPool jedisPool;
  private String requestResponseString;
  private String dataVariablesString;
  private int defaultPause;
  private config config;
  private logger logger;
  private Object inMessage;
  private JSONArray requestResponseArray;
  private JSONArray dataVariableArray;


  public httpStubWorker(Socket clientSocket,
      config config) {
    this.clientSocket = clientSocket;
    this.config = config;
    this.requestResponseArray = this.config.getRequestResponseArray();
    this.dataVariableArray = this.config.getDataVariableArray();    
  }

  @Override

  public void run() {
    logger logger = new logger();
    logger.info("httpStubWorker: processing connection....");
    PrintWriter out = null;
    BufferedReader in = null;
    httpInputStream inStream = null;
    int postLength = 0;
    
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
    Vector<String> inputMsgLines = new Vector<String>();
    String firstLine = null;
    String httpRespCode = "200 OK";
    String responseMsg = null;
    String errorMessage = null;
    try {
      inStream = new httpInputStream(clientSocket.getInputStream());
      // get header details
      String searchLine = null;
      int lineCntr = 0;
      while ((searchLine = inStream.readLine()).length() > 0) {
        if (lineCntr == 0) {
          firstLine = searchLine;
          logger.info("httpStubWorker: Processing: firstLine: " + firstLine,config.getLoglevel());
          lineCntr++;
        }
        inputMsgLines.addElement(searchLine);
        /*
         * get the content length for put post messages if required.
         */
        if (searchLine.contains("Content-Length:")) {
          postLength = Integer.parseInt(searchLine.substring(16, (searchLine.length())));
        }

      }
    } catch (Exception e) {
      logger.error("httpStubWorker: error in getting header : " + e);
      httpRespCode = "400 Bad Request";
      errorMessage = "error in getting header : " + e;
    }
    // if a POST or PUT message you'll need to read the remainder of the message
    // based on the content length
    String postLine = null;
    try {
      if (!firstLine.contains("GET")) {
        postLine = inStream.readLine(postLength);
        inputMsgLines.addElement(postLine);
      }
    } catch (Exception e) {
      logger.error("httpStubWorker: error in reading body : " + e);
      httpRespCode = "400 Bad Request";
      errorMessage = "error in reading body : " + e;
    }
    //
    // now we have the message need to determine what response template to use and how long to pause
    //
    logger.info("httpStubWorker: inputMsgLines.toString(): " + inputMsgLines.toString(),config.getLoglevel());
    boolean responseTemplateMessage = setResponseTemplate(inputMsgLines.toString(),requestResponseArray);
    if (responseTemplateMessage) {
      responseMsg = getTemplate("templateContents");
      defaultPause = Integer.parseInt(getTemplate("templatePause"));
      logger.info("httpStubWorker: Processing: " + getTemplate("templateName"));
    } else {
      errorMessage = "no matching response template found";
      httpRespCode = "400 Bad Request";
    }        
    
    //
    // loop through input message for variable extraction
    //
    logger.info("httpStubWorker: responseMsg: " + responseMsg,config.getLoglevel());
    responseMsg = processVariables(inputMsgLines.toString(), dataVariableArray, responseMsg, jedis);

    // if theres a http response code other than 200 then replace it here
    if (!httpRespCode.contains("200")) {
      responseMsg = responseMsg.replace("200 OK", httpRespCode);
      responseMsg = errorMessage;
    }

    // need to process the content length AFTER all other replacements are done
    String contentLength = processContentLengthType(responseMsg);
    responseMsg = responseMsg.replace("%Content-Length%", contentLength);

    //
    // time to write the output
    //
    try {
      BufferedOutputStream outStream = new BufferedOutputStream(clientSocket.getOutputStream());
      // if not a close message
      if (!inputMsgLines.toString().contains("Connection: close")) {
        // pause the response simulating downstream delay
        try {
          Thread.sleep(defaultPause);
        } catch (InterruptedException e) {
          logger.error("httpStubWorker: Error in thread sleep : " + e);
          e.printStackTrace();
        }
        // write the response message to output stream
        outStream.write(responseMsg.getBytes());

      }
      // close all open file handles
      outStream.flush();
      outStream.close();
      inStream.close();
      clientSocket.close();
    } catch (java.io.IOException e) {
      logger.error("httpStubWorker: error in writing response : " + e);
    }

    // close all resources
    try {
      in.close();
      out.close();
      clientSocket.close();
    } catch (Exception e) {
      logger.error("httpStubWorker: error closing resources: " + e);
    }
  }

}
