## General All Purpose Stub (GAPS)

In performance testing the requirement for interfacing systems can sometimes cause delays in execution or inconsistent results due to Interfacing Systems:
1.	environment size, if not the same capacity as the System under test (SUT)
2.	environment availability, if it is being used by another testing project
3.	data alignment with SUT
4.	data size may not be production like in size or 

The General All Purpose Stub (GAPS) is a generic stubbing platform used to emulate downstream systems for performance testing.

Consider an environment that looks as follows…


## Designing your test stub

The aim of the Stub is to allow you to isolate the system you want to test, so that it is the only system in play. This will prevent external systems from affecting your results. It also means you are not dependent on the presence of that system to complete your test. With this in mind, the design centres on illustrating the flows in and out of the system of concern to complete a transaction.

## Determine the transactions

Transactions can generally be identified from detailed test specifications or use case documents. They represent a flow through your system that serves a particular purpose. The first step in completing your design is to identify the list of transactions that you want to use in your test. With each transaction you have identified, you need to do the following. Some transactions will be simple in that involve only a test driver, others will be complex and will require not only a driver, but one or more request response pairs with external systems represented in order to complete the transaction.

## Determine the Driver

The first important aspect of designing your test is to determine what drives the transaction. By this, we mean what triggers off the transaction in your source system. This can be an interface from an external system or driven from a user interface.

## Request and Response Pairs

Once the driving source is identified, you trace the transaction through your system and seek to identify the series of request and response pairs involving your system and one or more external systems to complete the transaction. 

## Interface Configuration

Once you have represented the flows for each transaction, you will need to capture the configuration information relevant to the protocol you are using. The required information for each protocol is addressed in the relevant sections of this document.

## GAPS Structure

We are reliant on developing valid json files. The easiest way to do this is to use an json editor that will validate as you prepare the configuration file. This schema file contains the basic validations that need to be met in order to prepare a valid configuration for running the stub.

The following json files are required.
•	“config.json” details of interface type, ports, redis servers, sockets threads, etc.
•	“requestresponse.json” contain the base template content you want to use in your response messages.
•	“datavariables.json” the variables you want to generate for replacements in the response message.

## Configuration

Core properties include the following attributes to describe the content of the configuration file:

### MQ Sample
```
{
	"config": [
		{
			"name" : "core",
			"details" : [{
                "stubName": "MQ Stub",
                "threadPool": "100"
    		}]
		},
    {
			"name" : "rabbitmq",
			"details" : [{
                "mqHostName" : "localhost",
                "mqHostPort" : "5672",
                "mqInQueue" : "testQueue",
                "mqOutQueue" : "outQueue"    		
        }]
		},
    {
			"name" : "redis",
			"details" : [{
                "redisHostName" : "localhost",
                "redisHostPort" : "6379"
        }]
		}
  ]
} 
```

| Attribute | Attribute | Required | Valid Values | Description |
| :---: | :---: | :---: | :---: | :---: |
| config|  | Yes | string | controlling json element |
|       | name  | Yes | string | name of the current config element |
|       | details  | Yes | string | details of the current config element |
| :---: | :---: | :---: | :---: | :---: |
|       | mqHostName  | Yes | string | The hostname of the mq server |
|       | mqHostPort  | Yes | string | The hostport of the mq server |
|       | mqInQueue  | Yes | string | The name of the INBOUND (request) MQ queue |
|       | mqOutQueue  | Yes | string | The name of the OUTBOUND (response) MQ queue |
| :---: | :---: | :---: | :---: | :---: |
|       | httpHostName  | Yes | string | The hostname of the http/s server |
|       | httpHostPort  | Yes | string | The hostport of the http/s server |
|       | sslKeyName  | Yes (for https) | string | The location of the secure key |
|       | sslKeyPassword  | Yes (for https) | string | The password of the secure key |
| :---: | :---: | :---: | :---: | :---: |
|       | redisHostName  | No | string | The hostname of the redis server |
|       | redisHostPort  | No | string | The hostport of the redis server |




## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).
