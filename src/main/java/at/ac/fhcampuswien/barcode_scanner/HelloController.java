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


    private VideoCapture camera;

    private BarcodeDetector barcodeDetector;


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
                if (camera.read(frame)) {

                    // Process the frame for barcode detection
                    String barcode = detectBarcode(frame);

                    if (barcode != null && !barcode.isEmpty()) {

                        // Fetch product info
                        fetchProductInfo(barcode);
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
        new Thread(() -> {
            try {
                // Build the API URL with selected fields
                String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json?fields=product_name,brands,categories,nutriments,image_front_url,allergens_tags";

                // Create HttpClient
                HttpClient client = HttpClient.newHttpClient();

                // Build HttpRequest
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                // Send request and get response
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(response.body());

                    if (jsonResponse.has("product") && !jsonResponse.isNull("product")) {
                        JSONObject product = jsonResponse.getJSONObject("product");

                        String productName = product.optString("product_name", "Unknown");
                        String brand = product.optString("brands", "Unknown");
                        String category = product.optString("categories", "Unknown");
                        String imageUrl = product.optString("image_front_url", null);
                        JSONObject nutriments = product.optJSONObject("nutriments");
                        String allergens = product.optString("allergens_tags", "None");

                        // Get specific nutrition details
                        String energy = nutriments != null ? nutriments.optString("energy_100g", "N/A") : "N/A";
                        String fat = nutriments != null ? nutriments.optString("fat_100g", "N/A") : "N/A";

                        // Update the UI with product details
                        Platform.runLater(() -> {
                            String info = "Product: " + productName +
                                    "\nBrand: " + brand +
                                    "\nCategory: " + category +
                                    "\nEnergy: " + energy + " kcal" +
                                    "\nFat: " + fat + " g" +
                                    "\nAllergens: " + allergens;

                            productInfoLabel.setText(info);

                            // Display product image in the dedicated ImageView
                            if (imageUrl != null) {
                                Image productImage = new Image(imageUrl);
                                productImageView.setImage(productImage);
                            }
                        });
                    } else {
                        Platform.runLater(() -> productInfoLabel.setText("No product found for this barcode."));
                    }
                } else {
                    Platform.runLater(() -> productInfoLabel.setText("Error fetching product details. HTTP Code: " + response.statusCode()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> productInfoLabel.setText("Error fetching product details."));
            }
        }).start();
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

    public void stopCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
    }

}