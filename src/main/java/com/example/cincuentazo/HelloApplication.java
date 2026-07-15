package com.example.cincuentazo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punto de entrada de la aplicación JavaFX. Carga primero la pantalla de
 * selección de rivales (HU-1); desde ahí se navega a la pantalla del juego.
 */
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("selection-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 520, 380);
        stage.setTitle("Cincuentazo");
        stage.setScene(scene);
        stage.show();
    }
}
