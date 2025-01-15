package at.ac.fhcampuswien.barcode_scanner;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.BarcodeDetector;

public class BarcodeScanner {

    private BarcodeDetector barcodeDetector;

    public BarcodeScanner() {
        barcodeDetector = new BarcodeDetector();
    }

    public String detectBarcode(Mat frame) {
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        Mat points = new Mat();
        String decodedText = barcodeDetector.detectAndDecode(grayFrame, points);

        return decodedText.isEmpty() ? null : decodedText;
    }
}