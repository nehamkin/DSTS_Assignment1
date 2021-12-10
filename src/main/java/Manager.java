import adapters.EC2;
import adapters.S3Adapter;
import adapters.SQS;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
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
    private static boolean workersAreRunning = false;

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
            int n = Integer.parseInt(messages[2]);
            InputStream is = s3.getObject(localAppId+"input",keyInBucket);
            try{
            String message = convertInputStreamToString(is);
            String[] lines = message.split("\n");
            int k = (lines.length / n);
            int numberOfNeededWorkers = k > 18 ? 18 : k;
//            if(!workersAreRunning){
//                for (int i = 0; i <numberOfNeededWorkers ; i++) {
//                    Ec2Client ec2Client = Ec2Client.builder().region(Region.US_EAST_1).build()
//                    if(ec2.describeEC2Instances(ec2Client) < 19)
//                    ec2.createEC2Instance(ec2Client,"worker"+i,  workerUserData, 1, "worker");
//                }
//                workersAreRunning = true;
//            }
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
                    String[] localAppMsg = msg.body().split("\t");
                    String pathToSummaryFile = "C:\\Users\\orrin\\Desktop\\DSTS ORRI\\src\\summary_file_folder\\"+localAppMsg[0]+".txt";
                    PrintWriter summary_file = new PrintWriter(pathToSummaryFile,"UTF-8");
                    handleMessage(msg.body());
                    while(notAllTrue){
                        List<Message> returnMsg = sqs.retrieveMessages(workerToManagerQueue);
                        for(Message m : returnMsg){ String[] mbody = m.body().split("\t");
                           urlMap.put(mbody[4], true);
                           System.out.println("https://"+mbody[0]+"output.s3.amazonaws.com/"+mbody[4]);
                           summary_file.println("<p>https://"+mbody[0]+"output.s3.amazonaws.com/"+mbody[4]+"</p>");
                           sqs.deleteMessage(m, workerToManagerQueue);
                        }
                        notAllTrue = false;
                        for(Boolean b : urlMap.values())
                            if(!b)
                                notAllTrue = true;
                    }
                    summary_file.close();
                    s3.putFileInBucketFromFile(localAppMsg[0]+"output","summary_file", new File(pathToSummaryFile));
                    System.out.println("left loop sending completed message\n a ba bye");
                    sqs.sendMessage("completed", sqs.getQueueURL("ManagerTo"+msg.body().split("\t")[0]));
                }catch (Exception e){
                    e.printStackTrace();
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
