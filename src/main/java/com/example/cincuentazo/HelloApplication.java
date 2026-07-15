package com.example.cincuentazo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Punto de entrada de la aplicación JavaFX. Carga primero la pantalla de
 * selección de rivales (HU-1); desde ahí se navega a la pantalla del juego.
 */
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        registerFonts();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("selection-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 620);
        stage.setTitle("Cincuentazo");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Registra la tipografía Montserrat en tiempo de ejecución ANTES de
     * cargar cualquier FXML/CSS, para que -fx-font-family: 'Montserrat'
     * la encuentre disponible como si estuviera instalada en el sistema.
     * <p>
     * Se hace por código (y no con @font-face en el CSS) a propósito:
     * el @font-face de JavaFX es conocido por fallar en silencio incluso
     * con archivos .ttf perfectamente válidos, dejando solo un warning
     * genérico en consola ("Could not load @font-face font") sin causa
     * clara. Font.loadFont() es el camino confiable.
     */
    private void registerFonts() {
        loadFont("fonts/Montserrat-Regular.ttf");
        loadFont("fonts/Montserrat-Bold.ttf");
        loadFont("fonts/Montserrat-SemiBold.ttf");
        // Fuente "de casino" para números/labels de impacto (Suma, Turno).
        loadFont("fonts/CinzelDecorative-Bold.ttf");
        // Fuente script elegante para el título "Cincuentazo" (ver .title-label).
        loadFont("fonts/MonteCarlo-Regular.ttf");
    }

    private void loadFont(String resourcePath) {
        // Ruta absoluta (con "/" inicial): se resuelve desde la raíz del classpath
        // (src/main/resources), no desde el paquete de esta clase. Así el archivo
        // puede seguir estando en src/main/resources/fonts sin moverlo a
        // src/main/resources/com/example/cincuentazo/fonts.
        String absolutePath = "/" + resourcePath;
        try (InputStream in = HelloApplication.class.getResourceAsStream(absolutePath)) {
            if (in == null) {
                System.err.println("No se encontró el recurso de fuente: " + resourcePath
                        + " (revisa que el archivo esté en src/main/resources/" + resourcePath + ")");
                return;
            }
            Font loaded = Font.loadFont(in, 12);
            if (loaded == null) {
                System.err.println("Font.loadFont devolvió null para: " + resourcePath + " (el archivo pudo no cargarse correctamente)");
            } else {
                System.out.println("Fuente cargada: " + loaded.getName() + " (" + resourcePath + ")");
            }
        } catch (IOException e) {
            System.err.println("Error leyendo la fuente " + resourcePath + ": " + e.getMessage());
        }
    }
}