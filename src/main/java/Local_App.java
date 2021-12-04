import adapters.S3Adapter;
import adapters.SQS;
import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import javax.swing.text.html.HTML;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        Filter filter = Filter.builder()
                .name("manager")
                .values("running", "stopped")
                .build();

        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .filters(filter)
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

        return createManagerInstance("ami-04902260ca3d33422", arn);
    }

    private static String createManagerInstance(String amiId, String arn) {
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .imageId(amiId)
                .keyName(keyName)
                .maxCount(1)
                .minCount(1)
//                .securityGroups("launch-wizard-5")
                .userData(geManagerScript(arn))
//                .iamInstanceProfile(IamInstanceProfileSpecification.builder().arn(arn).build())
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

    private static String geManagerScript(String arn) {
        String script = "#!/bin/bash\n"+
                "sudo yum install -y java-1.8.0-openjdk\n" +
                "sudo yum update -y\n" ;
        script += "sudo mkdir jars\n";
        script += "cd jars\n";
        script += "sudo aws s3 cp s3://bucketqoghawn0ehuw2njlvyexsmxt5dczxfwc/Manager.jar ./\n";
        script += "sudo java -Xmx30g -jar ./Manager.jar ami-0878fb723a9a1c5db " + keyName + " " +  arn;

        return new String(java.util.Base64.getEncoder().encode(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
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
    public static void main (String[] args){
        BasicConfigurator.configure();
        getSecurityDetails();
        if(args.length == 3 || args.length == 4) {
            if (args.length == 4) {
                if (args[3].equals("terminate")){
                    terminate = true;
                    sqs.sendMessage("terminate", localAppToManagerQueueUrl);
                }
            }

            if(!terminate) {
//                CreateBucketResponse inputBucket = s3.createBucket(localAppIdInputBucketName);
//                CreateBucketResponse outputBucket =s3.createBucket(localAppIdOutputBucketName);
//                sqs.initQueue(managerToMeQName, "10000");
//                startManager();
                getOrCreateManager(arn);
                try {
//                    uploadFiles(new File(("C:\\Users\\orrin\\Desktop\\DSTS ORRI\\src\\main\\resources\\crazyinput.txt")));
//                    String msgToManager = localAppId + "\t" + nameOfFileInBucket;
//                    sqs.sendMessage(msgToManager, localAppToManagerQueueUrl);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
//                    while(!taskCompleted){
//                        List<Message> messages = sqs.retrieveMessages(sqs.getQueueURL(managerToMeQName));
//                        for(Message msg : messages){
//                            if(msg.body().equals("completed"))
//                                taskCompleted = true;
//                        }
//                    }
//                    System.out.println("DELETED BUCKETS!!!");
//                    deleteMyBuckets();
//                    deleteQueue();
                }
            }
        }
    }
}
