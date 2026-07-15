module com.example.cincuentazo {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.cincuentazo to javafx.fxml;
    opens com.example.cincuentazo.Controllers to javafx.fxml;

    exports com.example.cincuentazo;
    exports com.example.cincuentazo.Controllers;
    exports com.example.cincuentazo.Models;
}
