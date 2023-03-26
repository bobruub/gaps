package com.core;

import com.core.logger;
import com.core.config;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
class: stubWorker
Purpose: processes for inbound messages
Notesƒ
Author: Tim Lane
Date: 07/05/2022
**/
import java.util.Calendar;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class stubWorker {

    HashMap<String, String> variableContent = new HashMap<String, String>();
    HashMap<String, String> templateContent = new HashMap<String, String>();
    String currLine = null;
    logger logger = new logger();
    private config config;
    //
    // set and get array for the template message, this is used in other processes
    // for extracting details about the template.
    //
    public void setTemplate(String templateName, String templateValue) {
        templateContent.put(templateName, templateValue);
        return;
    }

    public String getTemplate(String templateName) {
        return templateContent.get(templateName);
    }

    //
    // set and get array for the variables, when the are processed are stored in in a key/pair manner
    //     
    public void setVariable(String variableName, String variableValue) {
        // if element already exists do not insert again
        String checkVarExists = null;
        checkVarExists = variableContent.get(variableName);
        if (checkVarExists == null){
            this.variableContent.put(variableName,variableValue);
        }
        return;
    }

    //
    // get variable value by passing the variable name
    //
    public String getVariable(String variableName) {
        return variableContent.get(variableName);
    }

    //
    // check whether the variable already exists, returns true/false
    //
    public boolean variableExists(String variableName) {
        boolean variableExists = false;
        String checkVarExists = null;
        checkVarExists = variableContent.get(variableName);
        if (checkVarExists != null){
            variableExists = true;                
        }
        return variableExists;
    }

    //
    // get the count of elements in the variable array
    //
    public int getVariableLength() {
        return variableContent.size();
    }

    //
    // determine the response message based on the input crieteria
    //{
    //  "name": "01-Transactional-records",
	// 	"type": "path"
	// 	"lookupWith": "regex",
	// 	"lookupValue": "accounts/(.+?)/",
    //  "pause": "100"
	// 	"contents": "HTTP/1.1 200 OK\nTabcorpAuth: %TabcorpAuth%\nContent-Length: %Content-Length%\nConnection: close\nContent-Type: %Content-Type%\n\n{\"transactions\": [%transactions%],\"_links\": {\"self\": \"https://%selfUrl%\",\"next\": \"https://%nextUrl%?transactionRef=7761910\"},\"authentication\": {\"token\": \"%TabcorpAuth%\",\"inactivityExpiry\": \"2022-05-02T09:20:47.817Z\",\"absoluteExpiry\": \"2022-05-02T09:20:47.305Z\",\"scopes\": [\"*\"]}}"
	// }
    //
    public boolean setResponseTemplate(String inMessage, JSONArray requestResponseArray,config config) {

        // JSONParser parser = null;
        // JSONObject responseVariables = null;
        // JSONArray responseArray = null;
        // // copy all json response messages into an array
        // try {
        //     parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        //     responseVariables = (JSONObject) parser.parse(requestResponseString);
        //     responseArray = (JSONArray) responseVariables.get("response");
        // } catch (Exception e) {
        //     String processMessage = "error in json processing : " + e;
        //     System.out.println("stubWorker: " + processMessage + " - " + e);
        // }

        //responseArray = JSONArray requestResponseArray
        int loopCounter = 0;
        String currentLookupLine = null;
        String responseTemplate = null;
        String templatePause = null;
        String responseName = null;
        boolean responseTemplateMessage = false;
        String callBackResponse = null;
        // loop through response message array looking for a match
        while (loopCounter < requestResponseArray.size()) {
            JSONObject variable = (JSONObject) requestResponseArray.get(loopCounter);
            loopCounter++;
            String responseType = (String) variable.get("type");
            responseName = (String) variable.get("name");
            String callForwardUrl = (String) variable.get("callForwardUrl");
            // if the current response message check is of type path
            if (callForwardUrl != null && !callForwardUrl.isEmpty() ){
                String callForwardBody = (String) variable.get("callForwardBody");
                String callForwardType = (String) variable.get("callForwardType");
                String USER_AGENT = "Mozilla/5.0";
                String GET_URL = callForwardUrl;
                if (callForwardType.toUpperCase().equals("GET")){
                    logger.debug("stubWorker: setResponseTemplate: callForwardUrl: " + callForwardUrl,config.getLoglevel());
                    logger.debug("stubWorker: setResponseTemplate: callForwardBody: " + callForwardBody,config.getLoglevel());
                    logger.debug("stubWorker: setResponseTemplate: callForwardType: " + callForwardType.toUpperCase(),config.getLoglevel());
                    logger.debug("stubWorker: setResponseTemplate: inmessage: " + inMessage,config.getLoglevel());
                    // extract content type from inbound message
                    var myPattern = Pattern.compile("Content-Type: (.+?),");
                    Matcher matcher = myPattern.matcher(inMessage);
                    String contentType = null;
                    // if the regex matches then this is the correct message
                    if (matcher.find()) {
                        contentType = matcher.group(1);
                    } else {
                        // default to appplicat/json
                        contentType = "application/json";
                    }    
                    logger.debug("stubWorker: setResponseTemplate: contentType: " + contentType,config.getLoglevel());
                    try {
                        URL obj = new URL(GET_URL);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                        con.setRequestMethod("GET");
                        con.setRequestProperty("User-Agent", USER_AGENT);
                        con.setRequestProperty("Content-Type", contentType);
                        int responseCode = con.getResponseCode();
                        logger.debug("stubWorker: GET Response Code : " + responseCode,config.getLoglevel());
                        if (responseCode == HttpURLConnection.HTTP_OK) { // success
                            BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            String inputLine;
                            StringBuffer response = new StringBuffer();
                            while ((inputLine = input.readLine()) != null) {
                                response.append(inputLine);
                            }
                            input.close();
                            con.disconnect();
                            // print result
                            logger.debug("stubWorker: setResponseTemplate: response: " + response.toString(),config.getLoglevel());
                            callBackResponse = response.toString();
                        }

                    } catch(Exception e){
                        logger.error("stubWorker: call forward: " + callForwardUrl + " failed. exception: " + e.toString());
                    }
                }
            }    

            if (responseType.equals("path")) {
                String responseLookupWith = (String) variable.get("lookupWith");
                String responseLookupValue = (String) variable.get("lookupValue");
                // extract the first line from the input message to see if it matches
                // GET 'http://192.168.0.81:8090/v1/account-service/tab/accounts/99999936/
                int pathPos = inMessage.indexOf(",");
                currentLookupLine = inMessage.substring(0,pathPos);
                // if the current response message array is of lookup type string
                if (responseLookupWith.equals("string")) {
                    // if the string exists then this is the correct message
                    if (currentLookupLine.contains(responseLookupValue)) {
                        templatePause = (String) variable.get("pause");
                        responseTemplate = (String) variable.get("contents");
                        responseTemplateMessage = true;
                        break; // found a match so break
                    }
                } else if (responseLookupWith.equals("regex")) {
                    // if the current response message array is of lookup type regex
                    String regexSearch = responseLookupValue;
                    var myPattern = Pattern.compile(regexSearch);
                    Matcher matcher = myPattern.matcher(currentLookupLine);
                    // if the regex matches then this is the correct message
                    if (matcher.find()) {
                        responseTemplate = (String) variable.get("contents");
                        templatePause = (String) variable.get("pause");
                        responseTemplateMessage = true;
                        break; // found a match so break
                    }
                }
            } else if (responseType.equals("body")) {
            // if the current response message array is of type body
            // copy the entire input string for processing
                currentLookupLine = inMessage.toString();
                String responseLookupWith = (String) variable.get("lookupWith");
                String responseLookupValue = (String) variable.get("lookupValue");
                // if the current response message array is of lookup type string
                if (responseLookupWith.contains("string")) {
                    // if the string exists then this is the correct message
                    if (currentLookupLine.contains(responseLookupValue)) {
                        responseTemplate = (String) variable.get("contents");
                        templatePause = (String) variable.get("pause");
                        responseTemplateMessage = true;
                        break; // found a match so break
                    }
                } else if (responseLookupWith.equals("regex")) {
                    // if the current response message array is of lookup type regex
                    String regexSearch = responseLookupValue;
                    var myPattern = Pattern.compile(regexSearch);
                    Matcher matcher = myPattern.matcher(currentLookupLine);
                    // if the regex matches then this is the correct message
                    if (matcher.find()) {
                        responseTemplate = (String) variable.get("contents");
                        templatePause = (String) variable.get("pause");
                        responseTemplateMessage = true;
                        break;
                    }
                }
            }
        }
        // if a teplate response message is found store in in a template array
        if (responseTemplateMessage){
            setTemplate("templateContents", responseTemplate);
            setTemplate("templatePause",templatePause);
            setTemplate("templateName",responseName);
//            logger.debug("stubWorker: IN TEMPLATE: scallBackResponse: " + callBackResponse);

            if (callBackResponse != null && !callBackResponse.isEmpty()){
//                logger.debug("stubWorker: IN TEMPLATE: callBackResponse: " + callBackResponse.toString());
                setTemplate("templateCallBackResponse",callBackResponse);
                logger.debug("stubWorker: templateCallBackResponse: " + getTemplate("templateCallBackResponse"),config.getLoglevel());
            }
        }
        return responseTemplateMessage;
    }
//
// loop through all the variables looking for matches and then process them.
//
    //public String processVariables(Vector<String> inputMsgLines, String dataVariablesString, JedisPool jedisPool, String responseMsg) {
      
        public String processVariables(String inMessage, JSONArray dataVariableArray, String responseMsg, config config) {
            logger.debug("stubWorker: processVariables: inMessage: " + inMessage,config.getLoglevel());
        //
        // loop required for those variables which aren't dependent on indivuduals lines
        // or any other variables being set primarily fixed strings
        //
        int loopCounter = 0;
        String variableReplace = null;
        JSONArray formatArray = null;    
       // for (i = 0; i < inMessage.size(); i++) {
            currLine = inMessage;
            if (currLine.length() == 0) {
                return "input message was empty"; 
              }
            loopCounter = 0;
            variableReplace = null;
            while (loopCounter < dataVariableArray.size()) {
                JSONObject variable = (JSONObject) dataVariableArray.get(loopCounter);
                loopCounter++;
                String variableName = (String) variable.get("name");
                String allowBypass = (String) variable.get("allowBypass");
                // check the variable is required in the repsonse message before processng
                // unless bypass is set to false
                if (bypassVariable(variableName, allowBypass, responseMsg)) {
                    continue;
                }
                logger.debug("stubWorker: processVariables: name: " + variableName,config.getLoglevel());
                // if the variable has already been set dont process it again
                if (variableExists(variableName)){
                    continue;
                }
                String variableType = (String) variable.get("type");
                if (variable.containsKey("format")) {
                    formatArray = (JSONArray) variable.get("format");
                }
                logger.debug("stubWorker: processVariables: \ttype: " + variableType,config.getLoglevel());
                int oddsCounter = 0;
                variableReplace = null;
                JSONObject format = (JSONObject) formatArray.get(oddsCounter);
                logger.debug("stubWorker: processVariables: \tformat: " + format,config.getLoglevel());

                if (variableType.equals("substring")) {
                    variableReplace = processSubstring(format,currLine,config);

                } else if (variableType.equals("regex")) {
                    String formatValue = (String) format.get("value");
                    variableReplace = processRegex(format,currLine,config);

                } else if (variableType.equals("string")) {
                    variableReplace = processString(format);

                } else if (variableType.equals("date")) {
                    variableReplace = processDate(format);

                } else if (variableType.equals("guid")) {
                    variableReplace = processGuid().toString();

                } else if (variableType.equals("aplhanumeric")) {
                    variableReplace = processAlphaNumeric(format);
                    
                } else if (variableType.equals("number")) {
                    variableReplace = processNumber(format);
                    
                }
                // if a variable is found then replace it in the response message 
                if (variableReplace != null) {
                    String searchSequence = "%" + variableName + "%";
                    if (responseMsg.contains(searchSequence)) {
                        responseMsg = responseMsg.replace(searchSequence, variableReplace);
                    }
                    // save the variable into an array if it's needed later
                    setVariable(variableName, variableReplace);
                }
            }
       // }
        //
        // another loop required for those which depend on variables already being set
        // above, at this stage only redis lookups.
        // and redis updates and concatenations (which rely on all sorts of variables being set)
        //
        loopCounter = 0;
        variableReplace = null;
        while (loopCounter < dataVariableArray.size()) {
            JSONObject variable = (JSONObject) dataVariableArray.get(loopCounter);
            loopCounter++;
            String variableName = (String) variable.get("name");
            String allowBypass = (String) variable.get("allowBypass");
            // check the varibale is required in the repsonse message before processng
            // unless bypass is set to false 
            if (bypassVariable(variableName, allowBypass, responseMsg)) {
                continue;
            }
            // if the variable has already been set dont process it again
            if (variableExists(variableName)){
                continue;
            }
            String variableType = (String) variable.get("type");
            if (variable.containsKey("format")) {
                formatArray = (JSONArray) variable.get("format");
            }
            int oddsCounter = 0;
            variableReplace = null;
            JSONObject format = (JSONObject) formatArray.get(oddsCounter);
            if (variableType.equals("redisRead")) {
                //{
                //    "name" : "redisCardNumber",
                //    "type" : "redisRead",
                //    "action": "variable",
                //    "allowBypass": "true",
                //    "format" : [
                //        {
                //            "redisSetName" : "tns_session_details",
                //            "redisKeyName" : "tokenSessionId"
                //        }
                //    ]
                // }
                //
                // open a connection from the redis pool
                //
                logger.debug("httpStubWorker: opening redis connection for read ....",config.getLoglevel()); 
                JedisPool jedisPool = config.getJedisPool() ;
                Jedis jedis = jedisPool.getResource();
                String redisSetName = (String) format.get("redisSetName");
                String redisKeyName = (String) format.get("redisKeyName");
                // if the key isnt set then stop trying
                if (!variableExists(redisKeyName)) {
                    continue;
                }
                String variableValue = getVariable(redisKeyName);
                variableReplace = jedis.hget(redisSetName, variableValue);
                //
                // close the jedis connection
                jedis.close();
            }
            if (variableType.equals("concatenate")) {
            //  {
			//      "name": "redisCardNumber",
			//      "type": "concatenate",
			//      "allowBypass": "false",
			//      "format": [{
			//	            "variableName": "cardNumberPrefix"
			//      }, {
			//	            "variableName": "cardXes"
			//      }, {
			//	            "variableName": "cardNumberSuffix"
			//      }]
		    //  }
                int formatLoopCounter = 0;
                String tmpValue = "";
                while (formatLoopCounter < formatArray.size()) {
                    JSONObject variableNames = (JSONObject) formatArray.get(formatLoopCounter);
                    String formatVariableName = (String) variableNames.get("variableName");
                    formatLoopCounter++;
                    if (!variableExists(formatVariableName)){ // if any of the variables aren't set yet then stop trying
                        break;
                    }
                    tmpValue = tmpValue + getVariable(formatVariableName);
                } 
                if (tmpValue.length() > 0) {
                    variableReplace = tmpValue;
                }
            
            }
            if (variableType.equals("redisUpdate")) {
                //  {
                //    "name" : "sessionCardNumber",
                //    "type" : "redisUpdate",
                //    "action": "variable",
                //    "allowBypass": "false",
                //    "format" : [
                //        {
                //            "redisSetName" : "tns_session_details",
                //            "redisKeyName" : "inSessionId",
                //            "redisKeyValue": "redisCardNumber"
                //        }
                //    ]
                //  }
                logger.debug("httpStubWorker: opening redis connection for update ....",config.getLoglevel()); 
                String redisSetName = (String) format.get("redisSetName");
                String redisKeyName = (String) format.get("redisKeyName");
                String redisKeyValue = (String) format.get("redisKeyValue");
                logger.debug("httpStubWorker: redis for update redisSetName: " + redisSetName,config.getLoglevel()); 
                logger.debug("httpStubWorker: redis for update redisKeyName: " + redisKeyName,config.getLoglevel()); 
                logger.debug("httpStubWorker: redis for update redisKeyValue: " + redisKeyValue,config.getLoglevel()); 
                // if the key or its value aren't set then stop trying
                if (!variableExists(redisKeyName) || !variableExists(redisKeyValue)){
                    continue;
                }
                //
                // open a connection from the redis pool
                //
                JedisPool jedisPool = config.getJedisPool() ;
                Jedis jedis = jedisPool.getResource();
                String keyName = getVariable(redisKeyName);
                String keyValue = getVariable(redisKeyValue);
                logger.debug("httpStubWorker: redis for update keyName: " + keyName,config.getLoglevel()); 
                logger.debug("httpStubWorker: redis for update redisKeyValue: " + keyValue,config.getLoglevel()); 
                variableReplace = String.valueOf(jedis.hset(redisSetName, keyName, keyValue));
                //
                // close the jedis connection
                //
                jedis.close();

            }
            // if a variable is set and it exists in the response message then replace it here
            if (variableReplace != null) {
                String searchSequence = "%" + variableName + "%";
                    if (responseMsg.contains(searchSequence)) {
                        responseMsg = responseMsg.replace(searchSequence, variableReplace);
                    }
                setVariable(variableName, variableReplace);
            }
        }
        return responseMsg;
    }

    public String processContentLengthType(String responseMsg, config config) {
        logger.debug("stubWorker: processing content length: " + responseMsg,config.getLoglevel());
        String variableValue = null;
        // splitting on empty line, header in part 0, body is remainder
        String[] parts = responseMsg.split("(?:\r\n|[\r\n])[ \t]*(?:\r\n|[\r\n])");
        // if response message has multiple blank line breaks
        // then loop through all adding the length
        if (parts.length > 2) {
            int bodyLen = 0;
            // start on second part of string, avoids header
            for (int x = 1; x < parts.length; x++) {
                bodyLen += parts[x].length();
            }
            bodyLen += parts.length; // add blank lines counter
            variableValue = Integer.toString(bodyLen);
        } else {
            // just determine length of second part.
            String stringBody = parts[1];
            variableValue = Integer.toString(stringBody.length());
        }

        return variableValue;
    }

    public boolean bypassVariable(String varName, String byPassFlag, String responseMsg) {
//
// if the varibale name does not exists in the templated response message 
// AND the variable bypassFlag = true then send back a negative response
//
        boolean returnFlag = false;
        if (!responseMsg.contains("%" + varName + "%") && byPassFlag.equals("true")) {
            returnFlag = true;
        }
        return returnFlag;
    }

    public void displayMsg(String message){
        System.out.println("Stubworker: " + message);
    }

    public String processSubstring(JSONObject format, String currLine,config config){
        //	{
		//	    "name" : "TabcorpAuth",
		//	    "type" : "substring",
		//	    "action": "replace",
		//	    "allowBypass": "true",
		//	    "format" : [{
		//			"startPos" : 13,
		//			"endPos" : "EOL"
		//		}]
		//  }
        //
        long formatStartPosLong = (long) format.get("startPos");
        Integer formatStartPos = (int) (long) formatStartPosLong;
        String formatEndPosString = (String) format.get("endPos");
        int formatEndPos = 0;
        if (formatEndPosString.equals("EOL")) {
            formatEndPos = currLine.length();
        } else {
            formatEndPos = Integer.parseInt(formatEndPosString);
        }
        return currLine.substring(formatStartPos, formatEndPos);
    }

    public String processRegex(JSONObject format, String currLine,config config){
        //  {
		//	    "name" : "inSessionId",
		//	    "type" : "regex",
		//	    "allowBypass": "true",
		//	    "format" : [{
		//			"regex" : "session/(.+?) "
		//		}]
		//  }

        String regexSearch = (String) format.get("regex");
        logger.debug("stubWorker: processRegex:  regext key: " + regexSearch,config.getLoglevel());
        var myPattern = Pattern.compile(regexSearch);
        Matcher matcher = myPattern.matcher(currLine.toString());
        String variableReplace = null;
        if (matcher.find()) {
            variableReplace = matcher.group(1);
        }
        return variableReplace;
    }

    public String processString(JSONObject format){
        //  {
		//	    "name" : "cardXes",
		//	    "type" : "string",
		//	    "allowBypass": "false",
		//	    "format" : [{
		//	        "value" : "xxxxxx"					
		//	    }]
		//  }
        return (String) format.get("value");                    
    }

    public String processDate(JSONObject format){
        //  {
		//	    "name" : "todaysDate",
		//	    "type" : "date",
		//	    "allowBypass": "false",
		//	    "format" : [{
		//	        "value" : "yyyy-MM-dd HH:mm:ss"					
		//	    }]
		//  }
        Calendar cal = Calendar.getInstance();
        String dateFormat = (String) format.get("value");
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(cal.getTime());
    }

    public UUID processGuid(){
        //{
		//	    "name" : "GUID",
		//	    "type" : "guid",
		//	    "allowBypass": "false"			
		//  }
        UUID idOne = UUID.randomUUID();
        return idOne;
    }

    public String processAlphaNumeric(JSONObject format){
        //  {
		//	    "name" : "sessionId",
		//	    "type" : "aplhanumeric",
		//	    "allowBypass": "true",
		//	    "format" : [{
		//			"length" : 24,
		//			"case": "upper"
		//		}]
		//  }
        String stringCase = (String) format.get("case");
        long lengthVar = (long) format.get("length");
        Integer stringLength = (int) (long) lengthVar;
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(stringLength);
        for (int x = 0; x < stringLength; x++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        String variableReplace = null;
        if (stringCase.equals("upper")){
            variableReplace = sb.toString().toUpperCase();
        } else if (stringCase.equals("lower")){
            variableReplace = sb.toString().toLowerCase();
        } else if (stringCase.equals("mixed")){
            variableReplace = sb.toString();
        }
        return variableReplace;
    }

    public String processNumber(JSONObject format){
        //  {
		//      "name" : "tokenNumber",
		//	    "type" : "number",
		//	    "allowBypass": "true",
		//	"format" : [{
		//			"length" : 16					
		//		}]
		//  }

        long numberLength = (long) format.get("length");
        Integer lengthNumber = (int) (long) numberLength;
        String AlphaNumericString = "0123456789";
        StringBuilder sb = new StringBuilder(lengthNumber);
        for (int x = 0; x < lengthNumber; x++) {
        int index = (int)(AlphaNumericString.length() * Math.random());
        sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }


}
