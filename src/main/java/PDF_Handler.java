import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.fit.pdfdom.PDFDomTree;
import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


public class PDF_Handler {

    public static String handleInput(String action, String fileUrl) {
        try {
            String path = DownloadFile(fileUrl);
            if(!path.equals("work")){
                switch (action) {
                    case "ToImage":
                        return convertToImage(path);
                    case "ToHTML":
                        return convertToHTML(path);
                    case "ToText":
                        return convertToText(path);
                    default:
                        //do nothing
                        break;
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "bad url";
    }

    private static String DownloadFile(String filePath) throws IOException{
        System.setProperty("http.agent", "Chrome");

            String pdfPath = "C://Users/orrin/Desktop/DSTS ORRI/src/downloads/download.pdf";
            URL url = new URL(filePath);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            BufferedInputStream in = new BufferedInputStream(http.getInputStream());
            FileOutputStream fos = new FileOutputStream(pdfPath);
            BufferedOutputStream bout = new BufferedOutputStream(fos);
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = in.read(buffer, 0, 1024)) >= 0){
                bout.write(buffer, 0, read);
            }
            bout.close();
            in.close();
            System.out.println("Download pdf complete");
            return pdfPath;
    }




    private static String convertToImage(String inputPath) throws IOException {
        File file = new File(inputPath);
        PDDocument document = PDDocument.load(file);
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage image = renderer.renderImage(0);
        String path = "C://Users/orrin/Desktop/DSTS ORRI/src/output/image.png";
        ImageIO.write(image, "png", new File("C://Users/orrin/Desktop/DSTS ORRI/src/output/image.png"));
        document.close();
        return path;
    }

    private static String convertToText(String inputPath) throws IOException {
        File file = new File(inputPath);
        PDDocument document = PDDocument.load(file);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        pdfStripper.setStartPage(1);
        pdfStripper.setEndPage(1);
        String path = "C://Users/orrin/Desktop/DSTS ORRI/src/output/text.txt";
        String text = pdfStripper.getText(document);
        try (PrintWriter destination = new PrintWriter(path)) {
            destination.println(text);
        }
        document.close();
        return path;
    }
    private static String convertToHTML(String inputPath) throws IOException, ParserConfigurationException {
        PDDocument pdf = PDDocument.load(new File(inputPath));
        PDDocument document = null;
        document = new PDDocument();
        document.addPage((PDPage) pdf.getDocumentCatalog().getPages().get(0));
        document.save("temp_file.pdf");
        document.close();
        String path = "C://Users/orrin/Desktop/DSTS ORRI/src/output/tohtml.html";
        Writer output = new PrintWriter(path, "utf-8");
        new PDFDomTree().writeText(document, output);
        pdf.close();
        output.close();
        return path;
    }
}