package at.ac.fhcampuswien.barcode_scanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600); // Adjust the width and height
        stage.setTitle("Barcode Scanner");
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        Path dllPath = Paths.get("libs", "native", "opencv_java4100.dll");
        System.load(dllPath.toAbsolutePath().toString());

        //System.load("C:/Users/Lil/Google Drive/FH/Programmierung_1/barcode_scanner/libs/native/opencv_java4100.dll");
        launch();
    }
}