package at.ac.fhcampuswien.barcode_scanner;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProductInfoFetcher {

    private TextArea productInfoArea;
    private ImageView productImageView;
    private UIController uiController;

    public ProductInfoFetcher(TextArea productInfoArea, ImageView productImageView, UIController uiController) {
        this.productInfoArea = productInfoArea;
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
                String jsonResponse = sendHttpRequest(barcode);
                if (jsonResponse != null) {
                    Product product = parseProductInfo(jsonResponse);
                    displayProductInfo(product, barcode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                uiController.appendLog("Exception caught in fetchProductInfo: " + e.getMessage());
                Platform.runLater(() -> productInfoArea.setText("Error fetching product details."));
            } finally {
                // LOG #7: Mark the end of the background thread execution
                // Notify that product info fetching is complete
                if (onProductInfoFetched != null) {
                    Platform.runLater(onProductInfoFetched);
                }
                uiController.appendLog("fetchProductInfo background thread exiting for barcode = " + barcode);

            }
        }).

                start();

        // LOG #8: Right after we spawn the new thread
        uiController.appendLog("Thread spawned for fetchProductInfo with barcode = " + barcode);
    }

    private String sendHttpRequest(String barcode) throws Exception {
        // Build the API URL with selected fields
        String url = "https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json?fields=product_name,brands,categories,nutriments,image_front_url,allergens";
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
            return response.body();
        } else {
            uiController.appendLog("No 'product' field in JSON. Updating UI with 'No product found...'");
            Platform.runLater(() -> {
                productInfoArea.setText("No product found for this barcode.");
            });
            return null;
        }
    }

    private Product parseProductInfo(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        uiController.appendLog("JSON parsing done. Checking 'product' field...");

        if (jsonObject.has("product") && !jsonObject.isNull("product")) {
            JSONObject product = jsonObject.getJSONObject("product");

            String name = product.optString("product_name", "Unknown");
            String brand = product.optString("brands", "Unknown");
            String category = product.optString("categories", "Unknown");
            String imageUrl = product.optString("image_front_url", null);
            String allergens = product.optString("allergens", "Unknown");
            String quantity = product.optString("quantity", "Unknown");
            int nova_group = product.optInt("nova_group", 0);
            String countries = product.optString("countries", "Unknown");
            String manufacturing_places = product.optString("manufacturing_places");
            String ingredients_text = product.optString("ingredients_text", "Unknown");
            String nutriscore_grade = product.optString("nutriscore_grade", "Unknown");

            JSONObject nutriments = product.optJSONObject("nutriments");
            String energy = (nutriments != null) ? nutriments.optString("energy_100g", "N/A") : "N/A";
            String fat = (nutriments != null) ? nutriments.optString("fat_100g", "N/A") : "N/A";

            // LOG #5: Right before we update the UI
            uiController.appendLog("Parsed product data: " + name + " (" + brand + ")  - Image URL: " + imageUrl);

            return new Product(name, brand, category, energy, fat, allergens, imageUrl, quantity, nova_group, countries,
                    manufacturing_places, ingredients_text, nutriscore_grade);

        }
        return null;
    }

    private void displayProductInfo(Product product, String barcode) {
        Platform.runLater(() -> {
            // LOG #6: We are now on the UI thread
            uiController.appendLog("Updating UI on JavaFX thread for barcode = " + barcode);

            String info = "Product: " + product.getName() +
                    "\nQuantity: " + product.getQuantity() +
                    "\nBrand: " + product.getBrand() +
                    "\nCategory: " + product.getCategory() +
                    "\nIngredients: " + product.getIngredients_text() +
                    "\nEnergy: " + product.getEnergy() + " kcal" +
                    "\nFat: " + product.getFat() + " g" +
                    "\nAllergens: " + product.getAllergens() +
                    "\nSelling countries: " + product.getCountries() +
                    "\nManufacturing places: " + product.getManufacturing_places() +
                    "\nNutriscore: " + product.getNutriscore_grade() +
                    "\nNova Group: " + product.getNova_group();

            productInfoArea.setText(info);

            // Display product image in the dedicated ImageView
            if (product.getImageUrl() != null) {
                uiController.appendLog("Loading product image from URL: " + product.getImageUrl());
                Image productImage = new Image(product.getImageUrl(), true);
                productImageView.setImage(productImage);
                uiController.appendLog("Finished loading and setting product image.");

            }
        });
    }
}
