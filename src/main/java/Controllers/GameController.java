package com.example.cincuentazo.Controllers;

import com.example.cincuentazo.Models.Card;
import com.example.cincuentazo.Models.Game;
import com.example.cincuentazo.Models.InvalidPlay;
import com.example.cincuentazo.Models.Player;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Controlador de la pantalla principal del juego. Conecta la vista
 * (game-view.fxml) con el modelo ({@link Game}), y es responsable de:
 * <ul>
 *     <li>Manejar el clic del jugador humano sobre sus cartas y sobre
 *     el mazo de robo.</li>
 *     <li>Disparar el turno de las máquinas en hilos separados, para no
 *     congelar la interfaz mientras "piensan" y "roban" (Hilo 1).</li>
 *     <li>Correr un temporizador de partida en un segundo hilo,
 *     independiente del anterior (Hilo 2).</li>
 *     <li>Refrescar la interfaz después de cada cambio en el modelo,
 *     incluyendo la microseñal de turno (efecto glow).</li>
 * </ul>
 */
public class GameController {

    private static final int MIN_MACHINE_DELAY_MS = 2000;
    // NOTA: se acorta el techo a 3000ms (en vez de 4000ms). Sigue
    // cumpliendo el "entre 2 y 4 segundos" de las HU-3/HU-4 (es un
    // subrango válido dentro de ese intervalo), pero evita que un turno
    // de máquina se sienta larguísimo al sumar las dos fases (pensar +
    // tomar carta), que en el peor caso ahora son 3+3=6s en vez de 4+4=8s.
    private static final int MAX_MACHINE_DELAY_MS = 3000;

    /** Duración de la microseñal de turno (glow), en segundos. */
    private static final double GLOW_DURATION_SECONDS = 1.5;

    /** Nombres de archivo de los avatares disponibles para hasta 3 máquinas. */
    private static final String[] AVATAR_FILES = {"carita1.png", "carita2.png", "carita3.png"};

    @FXML private Label lblPlayerTurn;
    @FXML private Label lblTableSum;
    @FXML private Label lblTimer;
    @FXML private Label lblMessage;
    @FXML private ImageView imgActiveCard;
    @FXML private ImageView imgDrawPile;
    @FXML private StackPane drawPileContainer;
    @FXML private HBox playerHandContainer;
    @FXML private VBox machine1Box;
    @FXML private VBox machine2Box;
    @FXML private VBox machine3Box;
    @FXML private VBox humanBox;

    private Game game;
    private final Random random = new Random();

    /** Contenedor visual (para el efecto glow) asociado a cada jugador. */
    private final Map<Player, VBox> playerContainers = new HashMap<>();

    private volatile boolean waitingForMachine;
    private volatile boolean gameOver;

    /** true cuando el jugador humano ya jugó su carta y debe tomar del mazo para cerrar el turno (HU-4). */
    private boolean awaitingHumanDraw;

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
        this.awaitingHumanDraw = false;
        this.elapsedSeconds = 0;

        setupPlayerContainers(numberOfMachines);
        setDrawPileEnabled(false);
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
            applyTurnGlow(current);
        }
    }

    /**
     * Asocia cada jugador con su contenedor fijo en la distribución en
     * cruz (Norte = Máquina 2, Oeste = Máquina 1, Este = Máquina 3, Sur
     * = jugador humano), y oculta los contenedores de máquinas que no
     * participan en esta partida.
     */
    private void setupPlayerContainers(int numberOfMachines) {
        playerContainers.clear();
        ArrayList<Player> players = game.getPlayers();

        playerContainers.put(players.get(0), humanBox);

        VBox[] machineSlots = {machine1Box, machine2Box, machine3Box};
        for (int i = 0; i < 3; i++) {
            boolean active = i < numberOfMachines;
            machineSlots[i].setVisible(active);
            machineSlots[i].setManaged(active);
            if (active) {
                playerContainers.put(players.get(i + 1), machineSlots[i]);
            }
        }
    }

    /**
     * Maneja el clic del jugador humano sobre una carta de su mano
     * (HU-3). Tras jugar, la mano queda con 3 cartas y el turno no
     * finaliza hasta que el jugador toque el mazo (HU-4).
     *
     * @param position posición (0-3) de la carta dentro de la mano
     */
    private void handleHumanCardClick(int position) {
        if (gameOver || waitingForMachine || awaitingHumanDraw) return;

        Player human = game.getCurrentPlayer();
        if (human.isMachineP()) return; // no es el turno del humano

        Card card = human.getHand()[position];
        if (card == null) return;

        int aceValue = chooseAceValue(card);

        try {
            game.playCard(human, position, aceValue);
            lblMessage.setText("Toca el mazo para robar tu carta y terminar el turno.");
            awaitingHumanDraw = true;
            setDrawPileEnabled(true);
            updateUI();
        } catch (InvalidPlay e) {
            lblMessage.setText(e.getMessage());
        }
    }

    /**
     * Maneja el clic del jugador humano sobre el mazo de robo (HU-4):
     * solo tiene efecto si ya jugó su carta en este turno. Al robar, se
     * completa su mano de 4 cartas y el turno pasa oficialmente al
     * siguiente jugador.
     */
    @FXML
    private void handleDrawPileClick(MouseEvent event) {
        if (gameOver || waitingForMachine || !awaitingHumanDraw) return;

        Player human = game.getCurrentPlayer();
        game.drawCard(human);
        awaitingHumanDraw = false;
        setDrawPileEnabled(false);
        lblMessage.setText("");
        updateUI();
        passTurn();
    }

    /** Habilita/deshabilita visualmente el mazo como destino de clic. */
    private void setDrawPileEnabled(boolean enabled) {
        drawPileContainer.getStyleClass().remove("draw-pile-active");
        if (enabled) {
            drawPileContainer.getStyleClass().add("draw-pile-active");
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
        applyTurnGlow(next);

        if (next.isMachineP()) {
            startMachineThread(next);
        }
    }

    /**
     * Hilo 1: simula el tiempo de "pensar" de la máquina (2-4 segundos)
     * sin bloquear la interfaz, y luego juega su carta en el hilo de
     * JavaFX mediante {@code Platform.runLater}.
     *
     * @param machine jugador máquina que debe jugar
     */
    private void startMachineThread(Player machine) {
        waitingForMachine = true;
        lblPlayerTurn.setText("Turno de: " + machine.getName() + " (pensando...)");

        Thread machineThread = new Thread(() -> {
            try {
                Thread.sleep(randomDelay());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            Platform.runLater(() -> performMachinePlay(machine));
        });
        machineThread.setDaemon(true);
        machineThread.setName("machine-play-thread");
        machineThread.start();
    }

    /** Elige y juega la primera carta válida disponible en la mano de la máquina (HU-3). */
    private void performMachinePlay(Player machine) {
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
            waitingForMachine = false;
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
        lblPlayerTurn.setText("Turno de: " + machine.getName() + " (tomando carta...)");
        startMachineDrawThread(machine);
    }

    /**
     * Hilo 1 (segunda fase): simula el tiempo que tarda la máquina en
     * tomar una carta del mazo tras jugar (HU-4), también entre 2 y 4
     * segundos, y finaliza el turno.
     *
     * @param machine jugador máquina que debe robar carta
     */
    private void startMachineDrawThread(Player machine) {
        Thread drawThread = new Thread(() -> {
            try {
                Thread.sleep(randomDelay());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            Platform.runLater(() -> {
                if (gameOver) return;
                game.drawCard(machine);
                waitingForMachine = false;
                updateUI();
                passTurn();
            });
        });
        drawThread.setDaemon(true);
        drawThread.setName("machine-draw-thread");
        drawThread.start();
    }

    /** @return un retardo aleatorio entre 2000 y 4000 ms. */
    private long randomDelay() {
        return MIN_MACHINE_DELAY_MS + random.nextInt(MAX_MACHINE_DELAY_MS - MIN_MACHINE_DELAY_MS + 1);
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
        setDrawPileEnabled(false);
        for (VBox box : playerContainers.values()) {
            if (box != humanBox) {
                box.getChildren().clear();
            }
        }
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

    /**
     * Aplica la microseñal visual de turno (HU de feedback de UX): un
     * resplandor dorado que crece y se desvanece sobre el contenedor
     * del jugador activo, durante aproximadamente {@link #GLOW_DURATION_SECONDS}
     * segundos, para guiar la atención del usuario hacia quién juega.
     *
     * @param player jugador cuyo turno acaba de comenzar
     */
    private void applyTurnGlow(Player player) {
        Node container = playerContainers.get(player);
        if (container == null) return;

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#ffd166"));
        glow.setSpread(0.35);
        glow.setRadius(0);
        container.setEffect(glow);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 0)),
                new KeyFrame(Duration.seconds(GLOW_DURATION_SECONDS * 0.5), new KeyValue(glow.radiusProperty(), 40)),
                new KeyFrame(Duration.seconds(GLOW_DURATION_SECONDS), new KeyValue(glow.radiusProperty(), 0))
        );
        timeline.setOnFinished(e -> container.setEffect(null));
        timeline.play();
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
     * Dibuja el panel de cada jugador máquina (avatar circular, nombre
     * y sus cartas boca abajo) dentro de su contenedor fijo en la
     * distribución en cruz (Norte/Oeste/Este).
     */
    private void renderOpponents() {
        ArrayList<Player> players = game.getPlayers();

        for (int i = 1; i < players.size(); i++) {
            Player machine = players.get(i);
            VBox box = playerContainers.get(machine);
            if (box == null) continue;

            box.getChildren().clear();
            box.getStyleClass().removeAll("opponent-box", "opponent-box-eliminated");
            box.getStyleClass().add(machine.isLife() ? "opponent-box" : "opponent-box-eliminated");

            StackPane avatar = buildAvatar(i - 1);

            Label nameLabel = new Label(machine.getName() + (machine.isLife() ? "" : " (eliminado)"));
            nameLabel.getStyleClass().add("opponent-name");

            HBox cardsRow = new HBox(4);
            cardsRow.setAlignment(javafx.geometry.Pos.CENTER);
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

            box.getChildren().addAll(avatar, nameLabel, cardsRow);
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