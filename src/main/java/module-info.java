module at.ac.fhcampuswien.barcode_scanner {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;
    requires java.net.http;
    requires org.json;


    opens at.ac.fhcampuswien.barcode_scanner to javafx.fxml;
    exports at.ac.fhcampuswien.barcode_scanner;
}