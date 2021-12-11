import adapters.S3Adapter;
import adapters.SQS;
import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import javax.swing.text.html.HTML;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class Local_App {
    private static Region region = Region.US_EAST_1;
    private static Ec2Client ec2 = Ec2Client.builder().region(region).build();
    private static String arn;
    private static String keyName;
    private static S3Adapter s3 = new S3Adapter();
    private static String inputFile;
    private static String outputFile;
    private static final String localAppId = "localapp"+UUID.randomUUID();
    private static final String localAppIdInputBucketName = localAppId+"input";
    private static final String localAppIdOutputBucketName = localAppId+"output";
    private static final String managerToMeQName = "ManagerTo"+localAppId;

    private static boolean terminate = false;
    private static final String localAppToManagerQueueUrl = "https://sqs.us-east-1.amazonaws.com/812638325025/LocalAppsToManagerQueue";
    private static SQS sqs = new SQS();
    private static final String nameOfFileInBucket = "originalFile";
    private static boolean taskCompleted = false;

    //-----------------------------------------------------------------------
    public static void startInstance(String instanceId){
        StartInstancesRequest startRequest = StartInstancesRequest.builder()
                .instanceIds(instanceId).build();
        ec2.startInstances(startRequest);
    }

    public static String getOrCreateManager(String arn){ //TODO: Need to add data parameter

        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .build();

        String nextToken;

        do {
            DescribeInstancesResponse response = ec2.describeInstances(request);

            for(Reservation reservation : response.reservations()) {
                for(Instance instance : reservation.instances()) {
                    for (Tag tag: instance.tags()) {
                        if (tag.value().equals("manager")){
                            if(instance.state().name().toString().equals("running") || instance.state().name().toString().equals("pending")){
                                return instance.instanceId();
                            }
                            else if(instance.state().name().toString().equals("stopped")){
                                startInstance(instance.instanceId());
                                return instance.instanceId();
                            }
                        }
                    }
                }
            }

            nextToken = response.nextToken();

        } while(nextToken != null);

        return createManagerInstance("ami-00e95a9222311e8ed", arn);
    }

    private static String createManagerInstance(String amiId, String arn) {
        IamInstanceProfileSpecification role = IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build();

        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .imageId(amiId)
                .keyName(keyName)
                .maxCount(1)
                .minCount(1)
                .userData(getManagerScript(arn))
                .iamInstanceProfile(role)
                .build();

        RunInstancesResponse response = ec2.runInstances(runRequest);

        String instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("manager")
                .value("manager")
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 instance %s based on AMI %s",
                    instanceId, amiId);

        } catch (Ec2Exception e) {
            e.printStackTrace();
        }

        return instanceId;
    }

    private static String getManagerScript(String arn) {
        String script =  "#!/bin/bash\n" +
                "mkdir jars\n" +
                "mkdir summary_file_folder\n" +
                "aws s3 cp s3://jarsbucketorri/Manager.jar ./jars/Manager.jar\n" +
                "java -jar /jars/Manager.jar\n";

        return new String(java.util.Base64.getEncoder().encode(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
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

    private static void createHtmlSummeryHTML(InputStream summaryFile) throws IOException {
        File htmlSummary = new File("C:\\Users\\orrin\\Desktop\\DSTS ORRI\\htmlSummary.html");
        BufferedWriter bw = new BufferedWriter(new FileWriter(htmlSummary));
        String htmlPrefix =
                "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<body>";
        bw.write(htmlPrefix);
        try  {
            String summary = convertInputStreamToString(summaryFile);
            String[] lines = summary.split("\n");
            for(String line : lines) {
                System.out.println(line);
                bw.write(line);
                bw.newLine();
            }
        }
         catch (IOException e) {
            e.printStackTrace();
        }
        String htmlPostFix =
                "</body>\n" +
                        "</html>";
        bw.write(htmlPostFix);
        bw.close();
        Desktop.getDesktop().browse(htmlSummary.toURI());
    }
//-----------------------------------------------------------------------------

    /** TODO
     * @return if a manager is already active
     */
    private static void startManager(){
        try{
            String nextToken = null;
            do{
                DescribeInstancesRequest req = DescribeInstancesRequest.builder().build();
                DescribeInstancesResponse response = ec2.describeInstances(req);
                for(Reservation reservation : response.reservations()){
                    for(Instance instance : reservation.instances()){
                        List<Tag> tags = instance.tags();
                        if(tags.size() > 0){
                            if(tags.get(0).key().equals("Manager")){
                                if(!instance.state().name().toString().equals("running")) {
                                    String instanceId = instance.instanceId();
                                    StartInstancesRequest request2 = StartInstancesRequest.builder()
                                            .instanceIds(instanceId)
                                            .build();
                                    ec2.startInstances(request2);
                                    System.out.printf("Successfully started Manager");
                                }
                                else {
                                    System.out.printf("Manager is already running");
                                }
                                return;
                            }
                        }
                    }
                }
                nextToken = response.nextToken();
            }
            while(nextToken != null);
        } catch (Ec2Exception e) {
//            e.printStackTrace();
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    private static void uploadFiles(File f) throws IOException {
        s3.putFileInBucketFromFile(localAppIdInputBucketName,nameOfFileInBucket, f.getAbsoluteFile());
    }

    /** TODO
      * @param q
     * sends a message to q with the location of the file on s3
     */
    private static void fileLocationMessage(SQS q, String loc){
    }

    /**TODO
     * checks if there is a 'done' message in the adapters.SQS
     */
    private static boolean done(){
        return false;
    }

    /**
     *
     * @return a html representing the results
     */
    private static HTML createHTML(){
        return null;
    }

    private static void getSecurityDetails(){
        File file = new File("./AssKey.txt");
        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
            arn = bf.readLine();
            keyName = bf.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteMyBuckets(){
        s3.deleteBucket(localAppIdInputBucketName);
        s3.deleteBucket(localAppIdOutputBucketName);
    }

    private static void deleteQueue(){
        sqs.deleteQueue(sqs.getQueueURL(managerToMeQName));
    }

    /**
     *
     * @param args
     * args[0] = fl - text file containing with URLs of PDFs
     * args[1] = n - number of PDFs per worker
     * args[2] = outputFile - where to upload the output
     * args[3] = [terminate] - optional, to send to manager indicating to terminate
     */
    public static void main (String[] args) throws IOException {

        BasicConfigurator.configure();
//        getSecurityDetails();
        if(args.length == 3 || args.length == 4) {
            if (args.length == 4) {
                if (args[3].equals("terminate")){
                    terminate = true;
                    sqs.sendMessage("terminate", localAppToManagerQueueUrl);
                }
            }

            if(!terminate) {
                CreateBucketResponse inputBucket = s3.createBucket(localAppIdInputBucketName);
                CreateBucketResponse outputBucket =s3.createBucket(localAppIdOutputBucketName);
                sqs.initQueue(managerToMeQName, "10000");
                getOrCreateManager(arn);
                try {
                    uploadFiles(new File(("C:\\Users\\orrin\\Desktop\\DSTS ORRI\\src\\main\\resources\\crazyinput.txt")));
//                    uploadFiles(new File((args[0])));
                    String msgToManager = localAppId + "\t" + nameOfFileInBucket + "\t"+20;
                    sqs.sendMessage(msgToManager, localAppToManagerQueueUrl);
                    while(!taskCompleted){
                        List<Message> messages = sqs.retrieveMessages(sqs.getQueueURL(managerToMeQName));
                        for(Message msg : messages){
                            if(msg.body().equals("completed"))
                                System.out.println(msg.body()+"is the message that received");
                            taskCompleted = true;
                            InputStream is =  s3.getObject(localAppIdOutputBucketName, "summary_file");
                            createHtmlSummeryHTML(is);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    deleteQueue();
                    deleteMyBuckets();
                    System.out.println("DELETED BUCKETS!!!");
                }
            }
        }
    }
}
