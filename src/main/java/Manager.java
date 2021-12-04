import adapters.EC2;
import adapters.S3Adapter;
import adapters.SQS;
import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Manager {

    private String name;
    private static final String nameOfFileInBucket = "originalFile";
    private static final String managerToWorkerQueue = "https://sqs.us-east-1.amazonaws.com/812638325025/FromManagerToWorkerQueue";
    private static final String workerToManagerQueue = "https://sqs.us-east-1.amazonaws.com/812638325025/FromWorkerToManagerQueue";
    private static boolean terminate = false;
    private static SQS sqs = new SQS();
    private static S3Adapter s3 = new S3Adapter();
    private static EC2 ec2 = new EC2();
    private static final String workerUserData = "#! /bin/bash\n" +
            "sudo yum install -y java-1.8.0-openjdk\n" +
            "sudo yum update -y\n" +
            "mkdir worker\n" +
            "aws s3 cp s3:<PATH TO JAR FILE IN S3>" +
            "java -jar /worker/Worker.jar\n"
            ;
    private static final String localAppToManagerQueueUrl = "https://sqs.us-east-1.amazonaws.com/812638325025/LocalAppsToManagerQueue";


    /**TODO
     * Constructor for manager
     */
    public Manager(){
    }

    /**TODO
     * downloads the input file
     */
    public void downloadInputFile(){
    }

    /**TODO
     * create a worker per n messages if there are no running workers
     */
    public void createWorkers(){
    }

    /**TODO
     * adds workers if needed according to message received by local app
     */
    public void updateNumberOfWorkers(){
    }

    /**TODO
     * creates a response message
     */
    private void createResponseMessage(){

    }
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

    /**TODO
     * creates a response message
     */
    private static List<Message> getMessageFromLocalApp(){
        return sqs.retrieveMessages(localAppToManagerQueueUrl);
    }

    private static void handleMessage(String msg) throws IOException {
        String[] messages = msg.split("\t");
        if(messages.length == 0)
            return;
        if(messages[0] == "terminate"){
            terminate =false;
            return;
        }
        if(messages.length >= 2){
            String localAppId = messages[0];
            String keyInBucket = messages[1];
            InputStream is = s3.getObject(localAppId,keyInBucket);
            try{
            String message = convertInputStreamToString(is);
            String[] lines = message.split("\n");
            for(String line : lines){
                String msgToWorker = line + "\t" + localAppId;
                sqs.sendMessage(msgToWorker, managerToWorkerQueue);
            }
            } catch (Exception e){return;}
        }


    }

    /**TODO
     * stops excepting input files
     * waits for workers to finish job
     * terminates workers
     * creates response message
     * terminates
     */
    public void terminate(){
    }

    public static void main (String[] args){
        BasicConfigurator.configure();

        //Start workers
        for (int i = 0; i <5 ; i++) {
            ec2.createEC2Instance(Ec2Client.builder().region(Region.US_EAST_1).build(),"worker"+i,  workerUserData, 1, "worker");
        }
        while(!terminate){
            List<Message> messages = getMessageFromLocalApp();
            for(Message msg : messages){
                try {
                    handleMessage(msg.body());
                }catch (Exception e){
                    System.out.println("an error occurred handling the file  "+ e.getMessage());
                }
            }
        }
//        ec2.
        //Terminate workers
        //Terminate/stop manager
    }


}
