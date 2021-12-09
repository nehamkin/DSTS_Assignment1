import adapters.EC2;
import adapters.S3Adapter;
import adapters.SQS;
import software.amazon.awssdk.services.sqs.model.Message;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Manager {

    private String name;
    private static final String nameOfFileInBucket = "originalFile";
    private static final String managerToWorkerQueue = "https://sqs.us-east-1.amazonaws.com/812638325025/FromManagerToWorkerQueue";
    private static final String workerToManagerQueue = "https://sqs.us-east-1.amazonaws.com/812638325025/FromWorkerToManagerQueue";
    private static boolean terminate = false;
    private static SQS sqs = new SQS();
    private static S3Adapter s3 = new S3Adapter();
    private static EC2 ec2 = new EC2();
    private static final String workerUserData =  "#!/bin/bash\n" +
            "sudo yum install -y java-1.8.0-openjdk\n" +
            "sudo yum update -y\n" +
            "mkdir jars\n" +
            "aws s3 cp s3://jarsbucketorri/Worker.jar ./jars/Worker.jar\n" +
            "java -jar /jars/Worker.jar\n";
    private static final String localAppToManagerQueueUrl = "https://sqs.us-east-1.amazonaws.com/812638325025/LocalAppsToManagerQueue";
    private static HashMap<String,Boolean> urlMap = new HashMap<>();
    private static Boolean notAllTrue = true;

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        final char[] buffer = new char[8192];
        final StringBuilder result = new StringBuilder();
        // InputStream -> Reader
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int charsRead;
            while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0) {
                result.append(buffer, 0, charsRead);
            }
        }
        return result.toString();
    }

    private static List<Message> getMessageFromLocalApp(){
        return sqs.retrieveMessages(localAppToManagerQueueUrl);
    }

    private static void handleMessage(String msg) {
        String[] messages = msg.split("\t");
        if(messages.length == 0)
            return;
        if(messages[0] == "terminate"){
            terminate =true;
            return;
        }
        if(messages.length >= 2){
            String localAppId = messages[0];
            String keyInBucket = messages[1];
            InputStream is = s3.getObject(localAppId+"input",keyInBucket);
            try{
            String message = convertInputStreamToString(is);
            String[] lines = message.split("\n");
            for(String line : lines){
                String url = line.split("\t")[1];
                String msgId = UUID.randomUUID().toString();
                urlMap.putIfAbsent(msgId, false);
                String msgToWorker = line + "\t" + localAppId+"\t"+ msgId;
                sqs.sendMessage(msgToWorker, managerToWorkerQueue);
            }
            } catch (Exception e){return;}
        }
    }


    public static void main (String[] args){
//        BasicConfigurator.configure();


        //Start workers
//        for (int i = 0; i <5 ; i++) {
//            ec2.createEC2Instance(Ec2Client.builder().region(Region.US_EAST_1).build(),"worker"+i,  workerUserData, 1, "worker");
//        }

        while(true){
            List<Message> messages = getMessageFromLocalApp();
            for(Message msg : messages){
                if (msg.body() == "terminate"){
                    terminate=true;
                    break;
                }
                try {
                    handleMessage(msg.body());
                    while(notAllTrue){
                        List<Message> returnMsg = sqs.retrieveMessages(workerToManagerQueue);
                        for(Message m : returnMsg){
                           urlMap.put(m.body().split("\t")[3], true);
                           sqs.deleteMessage(m, workerToManagerQueue);
                        }
                        notAllTrue = false;
                        for(Boolean b : urlMap.values())
                            if(!b)
                                notAllTrue = true;
                    }
                    sqs.sendMessage("completed", sqs.getQueueURL("ManagerTo"+msg.body().split("\t")[0]));
                }catch (Exception e){
                    System.out.println("an error occurred handling the file  "+ e.getMessage());
                }
                finally {
                    sqs.deleteMessage(msg,localAppToManagerQueueUrl);
                }
            }
        }
//        ec2.
        //Terminate workers
        //Terminate/stop manager
    }


}
