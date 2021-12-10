import adapters.S3Adapter;
import adapters.SQS;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Workers {
    private static PDF_Handler pdf_handler = new PDF_Handler();
    private String name;
    private static final String managerToWorkerQueue = "https://sqs.us-east-1.amazonaws.com/812638325025/FromManagerToWorkerQueue";
    private static final String workerToManagerQueue = "https://sqs.us-east-1.amazonaws.com/812638325025/FromWorkerToManagerQueue";
    private static boolean terminate = false;
    private static SQS sqs = new SQS();
    private static S3Adapter s3 = new S3Adapter();

    /**TODO
     * Gets message from adapters.SQS
      */
    private static List<Message> getMessage(){
        return sqs.retrieveMessages(managerToWorkerQueue);
    }

    private static String getMessage1() throws IOException {
        Path path = Paths.get("C:\\Users\\orrin\\Desktop\\DSTS ORRI\\src\\main\\resources\\crazyinput.txt");

        BufferedReader reader = Files.newBufferedReader(path);
        return reader.readLine();
    }

    /**TODO
     * uploads the resulting output file to S3
     * @return the location on S3 where the result is
     */
    public String uploadResult(){
        return "not yet implemented";
    }

    private static void handleMessage (Message message) throws Exception {
        String msg = message.body();
        if(msg == "terminate"){
            terminate = true;
            return;
        }
        String[] parsedMsg = msg.split("\t");
        String op = parsedMsg[0];
        String url = parsedMsg[1];
        String localAppId = parsedMsg[2];
        String msgId = parsedMsg[3];
        String path = pdf_handler.handleInput(op, url);
        if (path != "bad url"){
            s3.putFileInBucketFromFile(localAppId+"output", msgId, new File(path) );
        }
        else
            op = "";
        sqs.sendMessage(localAppId+"\t"+url+"\t"+path+"\t"+op+"\t"+msgId+"\t", workerToManagerQueue);

    }

    public static void main (String[] args) throws Exception {
        while(!terminate){
            List<Message> messages = getMessage();
            for(Message msg : messages){
                try {
                    handleMessage(msg);
                }catch (Exception e){
                    System.out.println("an error occurred handling the file  "+ e.getMessage());
                }
                finally {
                    sqs.deleteMessage(msg,managerToWorkerQueue);
                }
            }

        }
        //terminate
    }






}
