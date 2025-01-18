package at.ac.fhcampuswien.barcode_scanner;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;


public class UIController {


    @FXML
    private ImageView videoFeedView; // For the video feed

    @FXML
    private ImageView productImageView; // For the product image

    @FXML
    private TextArea productInfoArea; // New label for product details

    @FXML
    private Button resumeButton; // The UI button

    @FXML
    private TextArea logArea;

    @FXML
    private TextArea manualEntryArea;

    @FXML
    private Button lookupButton;

    private VideoCapture camera;
    private CameraHandler cameraHandler;
    private BarcodeScanner barcodeScanner;
    private ProductInfoFetcher productInfoFetcher;
    private String lastDetectedBarcode = null;



    @FXML
    public void initialize() {
        // Load the OpenCV native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        cameraHandler = new CameraHandler(videoFeedView, this);
        barcodeScanner = new BarcodeScanner();

        productInfoFetcher = new ProductInfoFetcher(productInfoArea, productImageView, this);
    }

    public void onFrameCaptured(Mat frame) {
        String barcode = barcodeScanner.detectBarcode(frame);

        if (barcode != null && !barcode.isEmpty() && !barcode.equals(lastDetectedBarcode)) {
            cameraHandler.pauseCamera();
            lastDetectedBarcode = barcode;
            productInfoFetcher.fetchProductInfo(barcode, () -> resumeButton.setDisable(false));
        }
    }


    @FXML
    private void onResumeButtonClicked() {
        logArea.setText("");
        productImageView.setImage(null);
        productInfoArea.setText("");
        cameraHandler.resumeCamera();
        resumeButton.setDisable(true);
    }

    @FXML void onLookupButtonClicked() {
        String barcode = manualEntryArea.getText().trim();

        if (barcode != null && !barcode.isEmpty() && !barcode.equals(lastDetectedBarcode)) {
            logArea.setText("");
            productImageView.setImage(null);
            productInfoArea.setText("");
            cameraHandler.pauseCamera();
            lastDetectedBarcode = barcode;
            productInfoFetcher.fetchProductInfo(barcode, () -> resumeButton.setDisable(false));
        }
    }

    @FXML
    public void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.setText(logArea.getText() + "\n" + message);
        });
    }

}