public class Workers {

    /**TODO
     * Gets message from adapters.SQS
      */
    public void getMessage(){
    }

    /**TODO
     * downloads the PDF file indicated in the message
     */
    public void downloadPDF(){
    }

    /**TODO
     * performs the operation requested on the file
      */
    public void performOp(){
    }

    /**TODO
     * uploads the resulting output file to S3
     * @return the location on S3 where the result is
     */
    public String uploadResult(){
        return "not yet implemented";
    }

    /**
     * put a message in a adapters.SQS queue indicating:
     * the original URL of the PDF
     * the result URL in S3
     * the operation that was performed
     */
    public void messageSQS(){
    }






}
