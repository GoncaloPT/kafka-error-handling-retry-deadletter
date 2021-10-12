# Kafka Error Handling: Retry and Deadletter

## Retry
The simple action of trying to consume the message again.
This can be performed multiple times ( not infinite ) and the retry interval should grow with number of retries.

## Selective Retry
A different topic that is used to store messages that have special retry scenarios.
For example you can have a specific field on a message that when present/true will trigger an API call, which the other messages do not call. In this scenario it would make sense to keep processing the other messages since they will succeed when the problem of the processing is the API being down.


## Pre requisites / Setup

1. Setup Confluent cloud account 
1. setup cli 
`curl -L --http1.1 https://cnfl.io/ccloud-cli | sh -s -- -b /usr/local/bin`  
create and retrieve a api-key for latter use  
`ccloud api-key create --resource <resource>`  
`source set-credencials.sh`  

## Projects in this repo

1. kafka-deadletter-spring-boot
done using spring-boot
1. kafka-deadletter-consumer
done using kafka consumer


## Deadletter 
It's just another topic; it is not a queue!

### When does a message go to deadletter?
1. When the type of error that occurred cannot/should not be retried.
For example, a business rule was not fulfilled. Retrying is of no use since it will fail again.
1. Max number of retries was reached without successfully processing the message

### Must have when using...
1. Some kind of alerting when a message arrives in deadletter. Just dropping messages in a deadletter and ignoring them is not an option
1. Information about the error that lead to the message being fwd to DL; 
Like the original topic name, partition and exception message

## DEMO
The project inside both repos can be used to showcase the usage of retry + deadletter strategies.  
Flow is: message consumed > if(db_connection_exception) retryLater() else sendToDL()  

### run the demo
1. Make sure environment variables are set:  
2. CONFLUENT_CLOUD_API_KEY
2. CONFLUENT_CLOUD_KEY
2. POSTGRES_URL
2. POSTGRES_PASSWORD

1. Make sure docker container with postgres is running ( database folder )
1. run the project
1. ( to test retry ) kill postgres docker container 
2. see the retry taking place
1. start docker container with postgress
2. message should be processed and values saved to the database
1. To test DL 
2. send a message without a mandatory field ( like id ) 
2. the message should be sent to DL after failed insert




## Bibliography

. kafka ( deadletter and retry )
https://www.youtube.com/watch?v=t8_y5Ac_flo&ab_channel=DXLab

