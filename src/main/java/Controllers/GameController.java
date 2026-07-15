package com.example.cincuentazo.Controllers;

import com.example.cincuentazo.Models.Card;
import com.example.cincuentazo.Models.Game;
import com.example.cincuentazo.Models.InvalidPlay;
import com.example.cincuentazo.Models.Player;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Random;

/**
 * Controlador de la pantalla principal del juego. Conecta la vista
 * (game-view.fxml) con el modelo ({@link Game}), y es responsable de:
 * <ul>
 *     <li>Manejar el clic del jugador humano sobre sus cartas.</li>
 *     <li>Disparar el turno de las máquinas en un hilo separado, para no
 *     congelar la interfaz mientras "piensan" (Hilo 1).</li>
 *     <li>Correr un temporizador de partida en un segundo hilo,
 *     independiente del anterior (Hilo 2).</li>
 *     <li>Refrescar la interfaz después de cada cambio en el modelo.</li>
 *     <li>Construir por código los elementos gráficos que no vienen del
 *     FXML: el escudo de la suma y los avatares de las máquinas.</li>
 * </ul>
 */
public class GameController {

    private static final int MIN_MACHINE_DELAY_MS = 2000;
    private static final int MAX_MACHINE_DELAY_MS = 4000;

    /** Nombres de archivo de los avatares disponibles para hasta 3 máquinas. */
    private static final String[] AVATAR_FILES = {"carita1.png", "carita2.png", "carita3.png"};

    @FXML private Label lblPlayerTurn;
    @FXML private Label lblTableSum;
    @FXML private Label lblTimer;
    @FXML private Label lblMessage;
    @FXML private ImageView imgActiveCard;
    @FXML private HBox playerHandContainer;
    @FXML private HBox opponentsContainer;

    private Game game;
    private final Random random = new Random();

    private volatile boolean waitingForMachine;
    private volatile boolean gameOver;

    // Hilo 2: temporizador de la partida, independiente del hilo de la máquina.
    private Thread timerThread;
    private int elapsedSeconds;

    /**
     * Arranca una partida nueva.
     *
     * @param numberOfMachines cantidad de rivales máquina (1 a 3), elegida en la pantalla anterior
     */
    public void startGame(int numberOfMachines) {
        ArrayList<Player> players = new ArrayList<>();
        players.add(new Player("Tú", false));
        for (int i = 1; i <= numberOfMachines; i++) {
            players.add(new Player("Máquina " + i, true));
        }

        this.game = new Game(players);
        game.initialGame();

        this.gameOver = false;
        this.waitingForMachine = false;
        this.elapsedSeconds = 0;

        startTimerThread();
        updateUI();

        Player current = game.getCurrentPlayer();
        if (!current.validMove(game.getTableSum())) {
            lblMessage.setText("No tienes jugadas posibles desde el inicio. Eliminado.");
            game.checkElimination(current);
            updateUI();
            passTurn();
        } else {
            lblPlayerTurn.setText("Turno de: " + current.getName());
        }
    }

    /**
     * Maneja el clic del jugador humano sobre una carta de su mano.
     *
     * @param position posición (0-3) de la carta dentro de la mano
     */
    private void handleHumanCardClick(int position) {
        if (gameOver || waitingForMachine) return;

        Player human = game.getCurrentPlayer();
        if (human.isMachineP()) return; // no es el turno del humano

        Card card = human.getHand()[position];
        if (card == null) return;

        int aceValue = chooseAceValue(card);

        try {
            game.playCard(human, position, aceValue);
            lblMessage.setText("");
            updateUI();
            passTurn();
        } catch (InvalidPlay e) {
            lblMessage.setText(e.getMessage());
        }
    }

    /**
     * Avanza el turno, manejando eliminaciones en cadena y la condición
     * de fin de juego.
     */
    private void passTurn() {
        if (checkAndHandleWinner()) return;

        game.nextTurn();
        Player next = game.getCurrentPlayer();

        if (!next.validMove(game.getTableSum())) {
            game.checkElimination(next);
            lblMessage.setText(next.getName() + " fue eliminado: no tenía jugadas posibles.");
            updateUI();
            passTurn();
            return;
        }

        updateUI();
        lblPlayerTurn.setText("Turno de: " + next.getName());

        if (next.isMachineP()) {
            startMachineThread(next);
        }
    }

    /**
     * Hilo 1: simula el tiempo de "pensar" de la máquina (2-4 segundos)
     * sin bloquear la interfaz, y luego ejecuta la jugada en el hilo de
     * JavaFX mediante {@code Platform.runLater}.
     *
     * @param machine jugador máquina que debe jugar
     */
    private void startMachineThread(Player machine) {
        waitingForMachine = true;
        lblPlayerTurn.setText("Turno de: " + machine.getName() + " (pensando...)");

        Thread machineThread = new Thread(() -> {
            try {
                int delay = MIN_MACHINE_DELAY_MS + random.nextInt(MAX_MACHINE_DELAY_MS - MIN_MACHINE_DELAY_MS + 1);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            Platform.runLater(() -> performMachineMove(machine));
        });
        machineThread.setDaemon(true);
        machineThread.setName("machine-turn-thread");
        machineThread.start();
    }

    /** Elige y juega la primera carta válida disponible en la mano de la máquina. */
    private void performMachineMove(Player machine) {
        waitingForMachine = false;
        if (gameOver) return;

        Card[] hand = machine.getHand();
        int chosenPosition = -1;
        int chosenAceValue = 0;

        for (int i = 0; i < hand.length; i++) {
            Card card = hand[i];
            if (card == null) continue;

            if (card.getNumber() == 14) {
                int asValue10 = card.calculateValue(10);
                int asValue1 = card.calculateValue(1);
                if (game.getTableSum() + asValue10 <= 50) {
                    chosenPosition = i;
                    chosenAceValue = 10;
                    break;
                } else if (game.getTableSum() + asValue1 <= 50) {
                    chosenPosition = i;
                    chosenAceValue = 1;
                    break;
                }
            } else {
                int value = card.calculateValue(0);
                if (game.getTableSum() + value <= 50) {
                    chosenPosition = i;
                    break;
                }
            }
        }

        if (chosenPosition == -1) {
            // No debería pasar (ya se validó antes de iniciar el turno), pero por seguridad:
            game.checkElimination(machine);
            lblMessage.setText(machine.getName() + " fue eliminado.");
            updateUI();
            passTurn();
            return;
        }

        try {
            game.playCard(machine, chosenPosition, chosenAceValue);
        } catch (InvalidPlay e) {
            lblMessage.setText(e.getMessage());
        }

        updateUI();
        passTurn();
    }

    /**
     * Decide con qué valor conviene jugar un As desde la interfaz (1 o
     * 10), priorizando el que mantenga la jugada válida.
     */
    private int chooseAceValue(Card card) {
        if (card.getNumber() != 14) return 0;
        int value10 = card.calculateValue(10);
        if (game.getTableSum() + value10 <= 50) return 10;
        return 1;
    }

    /** @return true si ya hay un ganador y se manejó el fin de juego */
    private boolean checkAndHandleWinner() {
        Player winner = game.checkWinner();
        if (winner != null) {
            endGame(winner);
            return true;
        }
        return false;
    }

    /** Finaliza la partida y detiene el hilo del temporizador. */
    private void endGame(Player winner) {
        gameOver = true;
        lblPlayerTurn.setText("¡" + winner.getName() + " ha ganado la partida!");
        lblMessage.setText("Juego terminado.");
        playerHandContainer.getChildren().clear();
        opponentsContainer.getChildren().clear();
    }

    /**
     * Hilo 2: cuenta los segundos transcurridos de la partida, en
     * paralelo e independiente del hilo de turno de la máquina, y
     * actualiza la etiqueta del temporizador cada segundo.
     */
    private void startTimerThread() {
        timerThread = new Thread(() -> {
            while (!gameOver) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                elapsedSeconds++;
                final int seconds = elapsedSeconds;
                Platform.runLater(() -> lblTimer.setText("Tiempo: " + seconds + "s"));
            }
        });
        timerThread.setDaemon(true);
        timerThread.setName("game-timer-thread");
        timerThread.start();
    }

    /** Refresca todos los elementos visuales según el estado actual del juego. */
    private void updateUI() {
        if (gameOver) return;

        lblTableSum.setText("Suma: " + game.getTableSum());

        Card activeCard = game.getActiveCard();
        Image image = new Image(getClass().getResourceAsStream(getCardImagePath(activeCard)));
        imgActiveCard.setImage(image);

        renderPlayerHand();
        renderOpponents();
    }

    /** Dibuja la mano del humano como cartas clicables. */
    private void renderPlayerHand() {
        playerHandContainer.getChildren().clear();
        Player human = game.getPlayers().get(0);
        Card[] hand = human.getHand();

        for (int i = 0; i < hand.length; i++) {
            Card card = hand[i];
            if (card == null) continue;

            final int position = i;
            ImageView cardView = new ImageView(new Image(getClass().getResourceAsStream(getCardImagePath(card))));
            cardView.setFitWidth(75);
            cardView.setFitHeight(110);
            cardView.setPreserveRatio(true);

            StackPane container = new StackPane(cardView);
            container.getStyleClass().add("hand-card");
            container.setOnMouseClicked(event -> handleHumanCardClick(position));

            playerHandContainer.getChildren().add(container);
        }
    }

    /**
     * Dibuja un panel por cada jugador máquina: avatar circular, nombre
     * y sus cartas boca abajo.
     */
    private void renderOpponents() {
        opponentsContainer.getChildren().clear();
        ArrayList<Player> players = game.getPlayers();

        for (int i = 1; i < players.size(); i++) {
            Player machine = players.get(i);

            StackPane avatar = buildAvatar(i - 1);

            Label nameLabel = new Label(machine.getName() + (machine.isLife() ? "" : " (eliminado)"));
            nameLabel.getStyleClass().add("opponent-name");

            HBox cardsRow = new HBox(4);
            int cardCount = 0;
            for (Card c : machine.getHand()) {
                if (c != null) cardCount++;
            }
            for (int c = 0; c < cardCount; c++) {
                ImageView back = new ImageView(new Image(getClass().getResourceAsStream(
                        "/com/example/cincuentazo/imagenes/back.jpg")));
                back.setFitWidth(35);
                back.setFitHeight(50);
                back.setPreserveRatio(true);
                cardsRow.getChildren().add(back);
            }

            VBox box = new VBox(6, avatar, nameLabel, cardsRow);
            box.getStyleClass().add(machine.isLife() ? "opponent-box" : "opponent-box-eliminated");
            box.setAlignment(javafx.geometry.Pos.CENTER);

            opponentsContainer.getChildren().add(box);
        }
    }

    /** Diámetro en píxeles del avatar circular de cada máquina. */
    private static final double AVATAR_DIAMETER = 60;

    /**
     * Construye el avatar circular de una máquina: la imagen recortada
     * en círculo (sin importar si el archivo original es cuadrado) más
     * un anillo dorado alrededor, para que combine con el escudo de la
     * suma y el borde de la mesa en vez de verse como un ícono suelto.
     *
     * @param avatarIndex índice 0-based dentro de {@link #AVATAR_FILES}
     */
    private StackPane buildAvatar(int avatarIndex) {
        String file = AVATAR_FILES[avatarIndex % AVATAR_FILES.length];
        ImageView avatar = new ImageView(new Image(getClass().getResourceAsStream(
                "/com/example/cincuentazo/imagenes/" + file)));
        avatar.setFitWidth(AVATAR_DIAMETER);
        avatar.setFitHeight(AVATAR_DIAMETER);
        avatar.setPreserveRatio(true);

        // Recorte circular: sin esto, cualquier avatar cuadrado o con
        // fondo rectangular se ve como una "estampilla" pegada sobre la mesa.
        Circle clip = new Circle(AVATAR_DIAMETER / 2, AVATAR_DIAMETER / 2, AVATAR_DIAMETER / 2);
        avatar.setClip(clip);

        // Anillo dorado por encima de la imagen recortada, mismo tono
        // que el borde de la mesa y el escudo (ajustable en style.css
        // con -fx-stroke si el dorado no combina exactamente).
        Circle ring = new Circle(AVATAR_DIAMETER / 2);
        ring.setFill(null);
        ring.getStyleClass().add("avatar-ring");

        StackPane container = new StackPane(avatar, ring);
        container.getStyleClass().add("avatar-circle");
        container.setMaxSize(AVATAR_DIAMETER, AVATAR_DIAMETER);
        return container;
    }

    /**
     * Construye la ruta del recurso de imagen para una carta.
     * Nota: los assets tienen un typo histórico ("K_pikas.jpg" en vez de
     * "K_picas.jpg" para el Rey de Picas), que se corrige aquí.
     */
    private String getCardImagePath(Card card) {
        String rank;
        switch (card.getNumber()) {
            case 11: rank = "J"; break;
            case 12: rank = "Q"; break;
            case 13: rank = "K"; break;
            case 14: rank = "A"; break;
            default: rank = String.valueOf(card.getNumber());
        }

        String suitFile;
        switch (card.getSuit()) {
            case "Diamantes": suitFile = "diamante"; break;
            case "Treboles": suitFile = "trebol"; break;
            case "Corazones": suitFile = "corazon"; break;
            case "Picas": suitFile = rank.equals("K") ? "pikas" : "picas"; break;
            default: suitFile = card.getSuit().toLowerCase();
        }

        return "/com/example/cincuentazo/imagenes/" + rank + "_" + suitFile + ".jpg";
    }
}