module at.ac.fhcampuswien.barcode_bot {
    requires javafx.controls;
    requires javafx.fxml;


    opens at.ac.fhcampuswien.barcode_bot to javafx.fxml;
    exports at.ac.fhcampuswien.barcode_bot;
}