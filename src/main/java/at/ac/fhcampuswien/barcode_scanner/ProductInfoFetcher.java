package at.ac.fhcampuswien.barcode_scanner;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProductInfoFetcher {

    private Label productInfoLabel;
    private ImageView productImageView;
    private UIController uiController;

    public ProductInfoFetcher(Label productInfoLabel, ImageView productImageView, UIController uiController) {
        this.productInfoLabel = productInfoLabel;
        this.productImageView = productImageView;
        this.uiController = uiController;
    }

    public void fetchProductInfo(String barcode, Runnable onProductInfoFetched) {
        // LOG #1: Right before we spawn the new thread
        uiController.appendLog("fetchProductInfo called with barcode = " + barcode);

        new Thread(() -> {
            try {
                // LOG #2: Indicate we entered the background thread
                uiController.appendLog("Entering background thread for fetchProductInfo with barcode = " + barcode);

                // Build the API URL with selected fields
                String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json?fields=product_name,brands,categories,nutriments,image_front_url,allergens_tags";
                uiController.appendLog("fetchProductInfo URL = " + url);

                // Create HttpClient
                HttpClient client = HttpClient.newHttpClient();

                // Build HttpRequest
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                // LOG #3: About to send HTTP request
                uiController.appendLog("Sending HTTP request for barcode = " + barcode);

                // Send request and get response (blocking call)
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // LOG #4: Response received
                uiController.appendLog("HTTP response received. Status code = " + response.statusCode());

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    uiController.appendLog("JSON parsing done. Checking 'product' field...");

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
                        uiController.appendLog("Parsed product data: " + productName + " (" + brand + ")  - Image URL: " + imageUrl);

                        Platform.runLater(() -> {
                            // LOG #6: We are now on the UI thread
                            uiController.appendLog("Updating UI on JavaFX thread for barcode = " + barcode);

                            String info = "Product: " + productName +
                                    "\nBrand: " + brand +
                                    "\nCategory: " + category +
                                    "\nEnergy: " + energy + " kcal" +
                                    "\nFat: " + fat + " g" +
                                    "\nAllergens: " + allergens;

                            productInfoLabel.setText(info);

                            // Display product image in the dedicated ImageView
                            if (imageUrl != null) {
                                uiController.appendLog("Loading product image from URL: " + imageUrl);
                                Image productImage = new Image(imageUrl, true);
                                productImageView.setImage(productImage);
                                uiController.appendLog("Finished loading and setting product image.");

                            }
                        });
                    } else {
                        uiController.appendLog("No 'product' field in JSON. Updating UI with 'No product found...'");
                        Platform.runLater(() -> {
                            productInfoLabel.setText("No product found for this barcode.");
                        });

                    }
                } else {
                    uiController.appendLog("HTTP error code: " + response.statusCode() + " - updating UI with error.");
                    Platform.runLater(() -> {
                        productInfoLabel.setText("Error fetching product details. HTTP Code: " + response.statusCode());
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                uiController.appendLog("Exception caught in fetchProductInfo: " + e.getMessage());
                Platform.runLater(() -> productInfoLabel.setText("Error fetching product details."));
            } finally {
                // LOG #7: Mark the end of the background thread execution
                // Notify that product info fetching is complete
                if (onProductInfoFetched != null) {
                    Platform.runLater(onProductInfoFetched);
                }
                uiController.appendLog("fetchProductInfo background thread exiting for barcode = " + barcode);

            }
        }).start();

        // LOG #8: Right after we spawn the new thread
        uiController.appendLog("Thread spawned for fetchProductInfo with barcode = " + barcode);
    }


}