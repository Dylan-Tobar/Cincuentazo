package com.example.cincuentazo.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controlador de la pantalla inicial (HU-1): permite al jugador humano
 * elegir contra cuántos jugadores máquina (1, 2 o 3) quiere jugar, y
 * arranca la partida.
 */
public class SelectionController {

    @FXML private ToggleButton rb1;
    @FXML private ToggleButton rb2;
    @FXML private ToggleButton rb3;
    @FXML private Label lblError;

    /**
     * Se dispara al presionar "Iniciar juego". Valida la selección y, si
     * es correcta, carga la vista del juego y arranca la partida.
     *
     * @param event evento de clic del botón
     */
    @FXML
    private void handleStartGame(ActionEvent event) {
        try {
            int machines = getSelectedMachines(); // puede lanzar IllegalArgumentException (unchecked, no marcada)

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/cincuentazo/game-view.fxml"));
            Parent root = loader.load(); // puede lanzar IOException (checked, marcada)

            GameController controller = loader.getController();
            controller.startGame(machines);

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 950, 680));
            stage.setTitle("Cincuentazo");
            stage.centerOnScreen();

        } catch (IllegalArgumentException e) {
            // Exception no marcada (built-in): selección inválida del usuario.
            lblError.setText(e.getMessage());
        } catch (IOException e) {
            // Exception marcada (built-in): fallo al cargar el FXML del juego.
            lblError.setText("No se pudo cargar el juego: " + e.getMessage());
        }
    }

    /**
     * Determina cuántas máquinas eligió el usuario a partir de los radio
     * buttons.
     *
     * @return 1, 2 o 3 según lo seleccionado
     * @throws IllegalArgumentException si no se seleccionó ninguna opción
     */
    private int getSelectedMachines() {
        if (rb1.isSelected()) return 1;
        if (rb2.isSelected()) return 2;
        if (rb3.isSelected()) return 3;
        throw new IllegalArgumentException("Selecciona con cuántas máquinas quieres jugar.");
    }
}