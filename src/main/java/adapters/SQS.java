package adapters;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.List;

public class SQS {
    private final int RETRIEVE_MESSAGES = 5;

    private static SqsClient sqs;
//    private String queueName;
//    private String queueUrl;

    public SQS(/**String queueName*/) {
        sqs = SqsClient.builder().region(Region.US_EAST_1).build();
//        this.queueName = queueName;
//        this.queueUrl = getQueueURL(queueName);
    }

    public String getQueueURL(String name) {
        GetQueueUrlResponse getQueueUrlResponse =
                sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build());
        return getQueueUrlResponse.queueUrl();
    }

    public void initQueue(String queueName, String timeout) {
        try {

            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();
            sqs.createQueue(request);

            HashMap<QueueAttributeName,String> managerAttributes = new HashMap<>();
            managerAttributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, timeout);

            SetQueueAttributesRequest setQueueAttributesRequest = SetQueueAttributesRequest.builder()
                    .queueUrl(getQueueURL(queueName))
                    .attributes(managerAttributes)
                    .build();

            sqs.setQueueAttributes(setQueueAttributesRequest);

        } catch (QueueNameExistsException e) {
            throw e;
        }
    }

    public void sendMessage(String message, String url){
        sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(message)
                .delaySeconds(10)
                .build());
    }

    public List<Message> retrieveMessages(String queueURL){
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueURL)
                .maxNumberOfMessages(RETRIEVE_MESSAGES)
                .build();

        return sqs.receiveMessage(receiveMessageRequest).messages();
    }

    public void deleteMessage(Message message, String queueUrl){

        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build();
        sqs.deleteMessage(deleteMessageRequest);
    }

    public void deleteQueue(String queueUrl){
        DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();

        sqs.deleteQueue(deleteQueueRequest);
    }

//    public String getQueueUrl() {
//        return queueUrl;
//    }
}
