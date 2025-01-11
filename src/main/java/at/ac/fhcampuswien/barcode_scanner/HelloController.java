package at.ac.fhcampuswien.barcode_scanner;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.BarcodeDetector;
import org.opencv.videoio.VideoCapture;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import org.json.JSONObject; // Add JSON dependency to parse API response

import java.nio.ByteBuffer;

public class HelloController {


    @FXML
    private ImageView videoFeedView; // For the video feed

    @FXML
    private ImageView productImageView; // For the product image

    @FXML
    private Label productInfoLabel; // New label for product details

    @FXML
    private Button resumeButton; // The UI button

    private VideoCapture camera;

    private BarcodeDetector barcodeDetector;
    private String lastDetectedBarcode = null;
    private volatile boolean cameraPaused = false;


    @FXML
    public void initialize() {
        // Load the OpenCV native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Initialize the camera and start the video feed
        camera = new VideoCapture(0);

        barcodeDetector = new BarcodeDetector();
        startCameraFeed();
    }

    private void startCameraFeed() {


        new Thread(() -> {
            Mat frame = new Mat(); // OpenCV Mat to hold frames
            while (camera.isOpened()) {
                if (camera.read(frame) && !cameraPaused) {

                    // Process the frame for barcode detection
                    String barcode = detectBarcode(frame);

                    if (barcode != null && !barcode.isEmpty()) {
                        if (!barcode.equals(lastDetectedBarcode)) {
                            cameraPaused = true;
                            lastDetectedBarcode = barcode;
                            fetchProductInfo(barcode);
                        }
                    }

                    // Convert Mat to JavaFX Image and update the video feed
                    Image image = matToImage(frame);
                    Platform.runLater(() -> videoFeedView.setImage(image));
                }
            }
        }).start();
    }


    private String detectBarcode(Mat frame) {
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY); // Convert to grayscale for better detection

        // Detect and decode the barcode
        Mat points = new Mat(); // Points for the barcode corners
        String decodedText = barcodeDetector.detectAndDecode(grayFrame, points);

        // Return the detected text or null if no barcode was found
        return decodedText.isEmpty() ? null : decodedText;
    }


    private void fetchProductInfo(String barcode) {
        // LOG #1: Right before we spawn the new thread
        System.out.println("fetchProductInfo called with barcode = " + barcode);

        new Thread(() -> {
            try {
                // LOG #2: Indicate we entered the background thread
                System.out.println("Entering background thread for fetchProductInfo with barcode = " + barcode);

                // Build the API URL with selected fields
                String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json?fields=product_name,brands,categories,nutriments,image_front_url,allergens_tags";
                System.out.println("fetchProductInfo URL = " + url);

                // Create HttpClient
                HttpClient client = HttpClient.newHttpClient();

                // Build HttpRequest
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                // LOG #3: About to send HTTP request
                System.out.println("Sending HTTP request for barcode = " + barcode);

                // Send request and get response (blocking call)
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // LOG #4: Response received
                System.out.println("HTTP response received. Status code = " + response.statusCode());

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    System.out.println("JSON parsing done. Checking 'product' field...");

                    if (jsonResponse.has("product") && !jsonResponse.isNull("product")) {
                        JSONObject product = jsonResponse.getJSONObject("product");

                        String productName = product.optString("product_name", "Unknown");
                        String brand = product.optString("brands", "Unknown");
                        String category = product.optString("categories", "Unknown");
                        String imageUrl = product.optString("image_front_url", null);
                        JSONObject nutriments = product.optJSONObject("nutriments");
                        String allergens = product.optString("allergens_tags", "None");

                        String energy = (nutriments != null) ? nutriments.optString("energy_100g", "N/A") : "N/A";
                        String fat = (nutriments != null) ? nutriments.optString("fat_100g", "N/A") : "N/A";

                        // LOG #5: Right before we update the UI
                        System.out.println("Parsed product data: " + productName + " (" + brand + ")  - Image URL: " + imageUrl);

                        if (imageUrl != null) {
                            System.out.println("Loading product image from URL: " + imageUrl);
                            Image productImage = new Image(imageUrl);

                        }
                        Platform.runLater(() -> {
                            // LOG #6: We are now on the UI thread
                            System.out.println("Updating UI on JavaFX thread for barcode = " + barcode);

                            String info = "Product: " + productName +
                                    "\nBrand: " + brand +
                                    "\nCategory: " + category +
                                    "\nEnergy: " + energy + " kcal" +
                                    "\nFat: " + fat + " g" +
                                    "\nAllergens: " + allergens;

                            productInfoLabel.setText(info);

                            // Display product image in the dedicated ImageView
                            if (imageUrl != null) {
                                System.out.println("Loading product image from URL: " + imageUrl);
                                Image productImage = new Image(imageUrl, true);
                                productImageView.setImage(productImage);
                                System.out.println("Finished loading and setting product image.");

                            }
                            resumeButton.setDisable(false);
                        });
                    } else {
                        System.out.println("No 'product' field in JSON. Updating UI with 'No product found...'");
                        Platform.runLater(() -> {
                            productInfoLabel.setText("No product found for this barcode.");
                            resumeButton.setDisable(false);
                        });

                    }
                } else {
                    System.out.println("HTTP error code: " + response.statusCode() + " - updating UI with error.");
                    Platform.runLater(() -> {
                        productInfoLabel.setText("Error fetching product details. HTTP Code: " + response.statusCode());
                        resumeButton.setDisable(false);
                    });
                    resumeButton.setDisable(false);
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception caught in fetchProductInfo: " + e.getMessage());
                Platform.runLater(() -> productInfoLabel.setText("Error fetching product details."));
                resumeButton.setDisable(false);
            } finally {
                // LOG #7: Mark the end of the background thread execution
                System.out.println("fetchProductInfo background thread exiting for barcode = " + barcode);

            }
        }).start();

        // LOG #8: Right after we spawn the new thread
        System.out.println("Thread spawned for fetchProductInfo with barcode = " + barcode);
    }


    private Image matToImage(Mat mat) {
        // Ensure the Mat is in BGRA format (4 channels)
        if (mat.channels() != 4) {
            Mat convertedMat = new Mat();
            Imgproc.cvtColor(mat, convertedMat, Imgproc.COLOR_BGR2BGRA);
            mat = convertedMat;
        }

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels(); // Should be 4 for BGRA
        byte[] buffer = new byte[width * height * channels];

        // Copy the data from the Mat object to the buffer
        mat.get(0, 0, buffer);

        // Create a WritableImage
        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(width, height);

        // Set the pixels using PixelWriter
        javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();
        pixelWriter.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getByteBgraPreInstance(), buffer, 0, width * channels);

        return writableImage;
    }

    @FXML
    private void onResumeButtonClicked() {
        cameraPaused = false;
        resumeButton.setDisable(true);
    }

}