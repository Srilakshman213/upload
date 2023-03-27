package io.getarrays.fileuploadanddownload.resource;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;

public class FileResource {

    public static final String SRC = "C:\\Users\\legor\\Downloads\\uploads\\original.pdf";
    public static final String DEST = "C:\\Users\\legor\\Downloads\\uploads\\test.pdf";
    public static final float FACTOR = 0.5f;

    public static void main(String[] args) throws IOException {
        new FileResource().manipulatePdf(SRC, DEST);
    }

    public void manipulatePdf(String src, String dest) throws IOException {
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(src), new PdfWriter(dest));

        // Iterate over all pages to get all images.
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            PdfDictionary pageDict = pdfDoc.getPage(i).getPdfObject();
            PdfDictionary resources = pageDict.getAsDictionary(PdfName.Resources);

            // Get images
            PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);
            for (PdfName imgRef : xObjects.keySet()) {
                // Get image
                PdfStream stream = xObjects.getAsStream(imgRef);
                PdfImageXObject image = new PdfImageXObject(stream);
                BufferedImage bi = image.getBufferedImage();
                if (bi == null)
                    continue;

                // Create new image
                int width = (int) (bi.getWidth() * FACTOR);
                int height = (int) (bi.getHeight() * FACTOR);
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                AffineTransform at = AffineTransform.getScaleInstance(FACTOR, FACTOR);
                Graphics2D g = img.createGraphics();
                g.drawRenderedImage(bi, at);
                ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();

                // Write new image
                ImageIO.write(img, "JPG", imgBytes);
                PdfStream newStream = new PdfStream(imgBytes.toByteArray());
                newStream.put(PdfName.Type, PdfName.XObject);
                newStream.put(PdfName.Subtype, PdfName.Image);
                newStream.put(PdfName.Filter, PdfName.DCTDecode);
                newStream.put(PdfName.Width, new PdfNumber(width));
                newStream.put(PdfName.Height, new PdfNumber(height));
                newStream.put(PdfName.BitsPerComponent, new PdfNumber(8));
                newStream.put(PdfName.ColorSpace, PdfName.DeviceRGB);
                PdfImageXObject newImage = new PdfImageXObject(newStream);
                
                // Replace the original image with the resized image
                xObjects.put(imgRef, newImage.getPdfObject());
            }
        }

        pdfDoc.close();
    }

}
