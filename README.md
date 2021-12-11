
# DSTS_Assignment1 - Orri Nehamkin & Yonathan Einan
* Permisssion by Meni Adler to hand in our assignment via Git Repository
* Orri Nehamkin, Yonathan Einan
* DSTS Assignment 1  - https://github.com/nehamkin/DSTS_Assignment1.git

![DSPS DIagram drawio](https://user-images.githubusercontent.com/73988005/145674805-57e8f92d-a2fb-4965-9569-488ac3fdb59a.png)

We have 3 static queues that initialized through the aws website:
* 1. LocalAppToManagerQueue
* 2. ManagerToWorkerQueue
* 3. WorkerToManagerQueue

* We have a preinitialized bucket that we put our (Manager.jar) and (Worker.jar)

LocalApp:
1. create a unique id for the localapp
2. Create a manager if one is not yet created
3. create input bucket and output bucket and name them: localappIdinput/output
4. create a unique queue for comunication between the localapp and the manager name it : ManagerToLocalappIdQueue
5. enter the file into the input bucket
6. send a message to the manager through the LocalAppToManagerQueue containing the localappId and number of messages per worker 
7. wait to recieve a message of completion from the ManagerToLocalappIdQueue

Manager:
1. Listen to the LocalAppToManagerQueue and get a message that should contain the localAppId
2. get the input file from the localAppIdInputBucket
3. parse the file line by line
4. create workers as needed in relation to the number of lines and currently running workers. (Make sure you do not start more than 19)
    * the number of lines per worker ratio is 20 as default.
    * the time it took to run the short input is 120 seconds.
5. send to the ManagerToWorkerQueue a message per each line as the following: msgId + "\t" + localAppId +"\t" + line
    * each message has a unique id, enter into a hashmap the msgId with the value false
6. listen to workerToManagerQueue - for each recieved message
    a. change the value of msgid in the hashmap to true
    b. enter into the summary file the relevant information
7. Wait for all the values in the hashmap to be true (indicating that each message was returned) then:
8. upload the summaryfile to the LocalAppIdOutput bucket
9. send completion message to the ManagerToLocalAppIdQueue
10. delete the message from the LocalAppToManagerQueue
11. Return to step 1

Worker:
1. Listen to ManagerToWorkerQueue and get message
2. parse message and get url
3. convert url
4. upload converted file to localappidoutput bucket
5. send message of completion to manager
6. return to step 1
