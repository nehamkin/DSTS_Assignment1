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

    private static void handleMessage (String msg) throws Exception {
        String[] parsedMsg = msg.split("\t");
        if(parsedMsg.length < 3){
            if(parsedMsg.length > 0 && parsedMsg[0] == "terminate"){
                terminate = true;
                return;
            }
            if(parsedMsg.length == 2){
                String path = pdf_handler.handleInput(parsedMsg[0], parsedMsg[1]);
                return;
            }
            sqs.sendMessage("bad line exception", workerToManagerQueue);
        }
        String op = parsedMsg[0];
        String url = parsedMsg[1];
        String localAppId = parsedMsg[2];
        String path = pdf_handler.handleInput(op, url);
        s3.putFileInBucketFromFile(localAppId+"pdfs", "Result", new File(path) );
        sqs.sendMessage(url+"\t"+path+"\t"+op, workerToManagerQueue);
    }

    public static void main (String[] args) throws Exception {
        while(!terminate){
            List<Message> messages = getMessage();
            for(Message msg : messages){
                try {
                    handleMessage(msg.body());
                }catch (Exception e){
                    System.out.println("an error occurred handling the file  "+ e.getMessage());
                }
            }

        }
//        String msg = getMessage1();
//        handleMessage(msg);

        //terminate
    }






}
