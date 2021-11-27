import adapters.S3Adapter;
import adapters.SQS;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;

import javax.swing.text.html.HTML;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Local_App {

    private static String arn;
    private static String keyName;
    private static S3Client s3;
    private static final String amiId = "ami-04902260ca3d33422";
    private static String inputFile;
    private static String outputFile;
    private static String localAppBucket;




    /** TODO
     * @return if a manager is already active
     */
    private static boolean managerActive(){
        return false;
    }

    /** TODO
     * @return an existing manager, or a new one if one is not found
     */
    private static Ec2Client getManager(){
        return null;
    }

    /**TODO
     * @param s3
     * @param f
     * @return the location of the file in s3
     */
    private static String uploadFiles(S3Adapter s3, File f){
        return "";
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

    /**TODO
     * send a termination message to the manager
     */
    private static void terminate(){

    }

    /**
     *
     * @param args
     * args[0] = fl - text file containing with URLs of PDFs
     * args[1] = n - number of PDFs per worker
     * args[2] = outputFile - where to upload the output
     * args[3] = [terminate] - optional, to send to manager indicating to terminate
     */

    private static void getSecurityDetails(){
        File file = new File("./AssKey.txt");
        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
            arn = bf.readLine();
            keyName = bf.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main (String[] args){}
}
