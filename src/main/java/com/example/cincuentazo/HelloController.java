package org.example.cincuentazo.Controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.example.cincuentazo.Model.*;

import java.util.ArrayList;

/**
 * Controlador principal del juego.
 *
 * NOTA PARA EL COMPAÑERO DE VISTA (FXML/JavaFX):
 * - Todos los campos marcados con @FXML deben existir en el archivo .fxml
 *   con el mismo fx:id (lblTableSum, lblCurrentCard, lblPlayerTurn,
 *   playerHandContainer, imgActiveCard). Si cambian el nombre en el FXML,
 *   hay que actualizarlo aquí también, o la app truena al cargar la vista.
 * - Este controller NO crea las cartas de la máquina en pantalla, solo
 *   pinta la mano del humano (ver renderPlayerHand()).
 *
 * NOTA PARA EL COMPAÑERO DE LÓGICA (Model: Board, Deck, Card, MachinePlayer):
 * - Este controller asume que Board, Deck y MachinePlayer ya existen con
 *   los métodos usados aquí (PlaceCard, CanPlayCard, GetTotalSum,
 *   GetActiveCard, RecycleCards, canPlay, findCardToPlay, etc).
 * - Falta implementar el flujo de "startNewGame" desde otra pantalla
 *   (selección de rivales), este método solo se llama externamente.
 */
public class GameController {

    // ===================== Configuración del juego =====================
    // Ajustar estos valores para balancear la partida.
    private static final int INITIAL_CARDS = 4;     // cartas iniciales por jugador
    private static final int MIN_MACHINE_DELAY = 2;  // seg. mínimo "pensando" la máquina
    private static final int MAX_MACHINE_DELAY = 4;  // seg. máximo "pensando" la máquina

    // ===================== Componentes FXML =====================
    // IMPORTANTE (Vista): estos fx:id deben coincidir EXACTO con el .fxml.
    @FXML private Label lblTableSum;        // muestra la suma actual de la mesa
    @FXML private Label lblCurrentCard;     // texto de la carta activa (ej. "7 de Oros")
    @FXML private Label lblPlayerTurn;      // indica de quién es el turno / mensajes de estado
    @FXML private HBox playerHandContainer; // contenedor donde se pintan las cartas del humano
    @FXML private ImageView imgActiveCard;  // imagen de la carta que está en la mesa

    // ===================== Modelo del juego =====================
    private Board board;                    // estado de la mesa (suma, carta activa, etc.)
    private Deck deck;                      // mazo para robar/reciclar cartas
    private ArrayList<MachinePlayer> players; // jugadores: índice 0 = humano, resto = máquinas

    private int currentPlayerIndex; // índice del jugador en turno dentro de "players"
    private boolean gameIsOver;     // bandera para saber si ya terminó la partida
    private boolean waitingForMachine; // evita clics del humano mientras "piensa" la máquina

    // Número de rivales elegidos por el usuario (se recibe desde la pantalla anterior)
    private int numberOfMachines;


    /**
     * Se ejecuta automáticamente al cargar el FXML.
     * OJO (Vista): aquí NO se arma la partida todavía, solo se muestra un
     * mensaje inicial. La partida real arranca cuando otra pantalla llama
     * a startNewGame(int).
     */
    @FXML
    public void initialize() {

        // No iniciar partida aquí.
        // La partida comienza cuando el usuario elige rivales.
        lblPlayerTurn.setText("Seleccione cantidad de rivales");
    }


    /**
     * Punto de entrada real de la partida.
     * Debe llamarse desde el controlador de la pantalla de selección de
     * rivales, pasando cuántas máquinas van a jugar.
     *
     * (Lógica): aquí se resetea todo el estado del juego desde cero, por
     * si se quiere reiniciar una partida sin cerrar la app.
     */
    public void startNewGame(int numberOfMachines) {

        this.numberOfMachines = numberOfMachines;

        this.board = new Board();
        this.deck = new Deck();
        this.players = new ArrayList<>();

        this.currentPlayerIndex = 0; // el humano siempre empieza en el índice 0
        this.gameIsOver = false;
        this.waitingForMachine = false;


        setupPlayers();

        dealInitialCards();

        updateUI();

        lblPlayerTurn.setText(
                "Turno de: " + players.get(currentPlayerIndex).getName()
        );


        MachinePlayer human = players.get(0);

        // Caso borde: el humano no tiene ninguna carta jugable desde el inicio.
        // (Lógica): esto depende de que MachinePlayer.canPlay(board) esté bien
        // implementado, revisando toda la mano contra la suma de la mesa.
        if (!human.canPlay(board)) {

            System.out.println(
                    "El humano no puede jugar en el primer turno!"
            );

            eliminatePlayer(human);
            passTurn();
        }
    }


    /**
     * Crea la lista de jugadores: el humano siempre va primero (índice 0),
     * seguido de las máquinas numeradas.
     * (Vista): el nombre "Humano" y "Máquina N" es lo que se muestra en
     * lblPlayerTurn, si quieren cambiar el texto visible es aquí donde se
     * define el nombre de cada jugador.
     */
    private void setupPlayers() {

        players.add(new MachinePlayer("Humano"));


        for (int i = 1; i <= numberOfMachines; i++) {

            players.add(
                    new MachinePlayer("Máquina " + i)
            );
        }
    }


    /**
     * Reparte INITIAL_CARDS cartas a cada jugador (una por una, en ronda)
     * y coloca la primera carta de la mesa.
     * (Lógica): si el deck se queda sin cartas a mitad del reparto, esas
     * vueltas simplemente no reparten (no hay reciclaje aquí todavía).
     */
    private void dealInitialCards() {

        for (int i = 0; i < INITIAL_CARDS; i++) {

            for (MachinePlayer player : players) {

                if (!deck.isEmpty()) {
                    player.addCard(deck.getCard());
                }
            }
        }


        if (!deck.isEmpty()) {

            Card initialCard = deck.getCard();

            board.PlaceCard(initialCard);

            System.out.println("=== PREPARACIÓN ===");
            System.out.println("Carta inicial: " + initialCard);
            System.out.println("Suma inicial: " + board.GetTotalSum());
        }
    }


    /**
     * Se llama cuando el humano hace clic en una de sus cartas
     * (ver renderPlayerHand() -> setOnMouseClicked).
     * (Vista): este es el método que reacciona al clic del usuario;
     * si agregan animaciones o sonidos al jugar una carta, van aquí.
     * (Lógica): la validación real de si la carta se puede jugar vive en
     * board.CanPlayCard(cardSelected), no aquí.
     */
    private void handleHumanCardClick(Card cardSelected) {

        // Filtros de seguridad: no permitir jugar si no es el turno del
        // humano, si el juego ya terminó, o si una máquina está "pensando".
        if (currentPlayerIndex != 0 ||
                gameIsOver ||
                waitingForMachine) {
            return;
        }


        MachinePlayer humanPlayer = players.get(0);


        if (board.CanPlayCard(cardSelected)) {

            board.PlaceCard(cardSelected);

            humanPlayer.removeCard(cardSelected);

            drawCardForPlayer(humanPlayer);

            updateUI();

            passTurn();

        } else {

            // (Vista): aquí sería buen lugar para mostrar un mensaje de
            // error visible al usuario (ej. shake de la carta, label roja),
            // en vez de solo un println en consola.
            System.out.println(
                    "Jugada no válida. La suma superaría 50."
            );
        }
    }


    /**
     * Simula el turno de una máquina con un pequeño retraso (para que se
     * vea como que "piensa"), y luego juega una carta si puede.
     * (Vista): mientras waitingForMachine = true, la UI debería mostrarse
     * "bloqueada" para el humano (por eso handleHumanCardClick la revisa).
     * (Lógica): findCardToPlay(board) es donde vive la estrategia/IA de la
     * máquina para elegir qué carta jugar.
     */
    private void playMachineTurn(MachinePlayer machine) {

        waitingForMachine = true;

        lblPlayerTurn.setText(
                "Turno de: " +
                machine.getName() +
                " (Pensando...)"
        );


        int delaySeconds =
                MIN_MACHINE_DELAY +
                (int)(Math.random() *
                (MAX_MACHINE_DELAY - MIN_MACHINE_DELAY + 1));


        PauseTransition pause =
                new PauseTransition(
                        Duration.seconds(delaySeconds)
                );


        pause.setOnFinished(event -> {

            // Si el juego terminó mientras esperábamos, no seguir jugando.
            if (gameIsOver)
                return;


            Card cardToPlay =
                    machine.findCardToPlay(board);


            if (cardToPlay != null) {

                board.PlaceCard(cardToPlay);

                machine.removeCard(cardToPlay);

                System.out.println(
                        machine.getName() +
                        " jugó " +
                        cardToPlay
                );

                drawCardForPlayer(machine);
            }
            // Si cardToPlay es null, la máquina no tiene jugada válida;
            // se resuelve en passTurn() -> eliminatePlayer().


            waitingForMachine = false;

            updateUI();

            passTurn();
        });


        pause.play();
    }


    /**
     * Avanza el turno al siguiente jugador activo (no eliminado).
     * También detecta fin de partida cuando queda 1 o 0 jugadores activos.
     * (Lógica): este es el "corazón" del flujo del juego, cualquier regla
     * nueva de turnos (ej. saltar turno, cartas especiales) se agrega aquí.
     */
    private void passTurn() {

        if (gameIsOver)
            return;


        int activePlayers = 0;
        MachinePlayer winner = null;


        for (MachinePlayer p : players) {

            if (!p.isEliminated()) {

                activePlayers++;
                winner = p;
            }
        }


        if (activePlayers <= 1) {

            endGame(winner);
            return;
        }


        MachinePlayer nextPlayer;

        // Recorre la lista de forma circular hasta encontrar el próximo
        // jugador que NO esté eliminado.
        do {

            currentPlayerIndex =
                    (currentPlayerIndex + 1)
                    % players.size();


            nextPlayer =
                    players.get(currentPlayerIndex);


        } while (nextPlayer.isEliminated());



        lblPlayerTurn.setText(
                "Turno de: " +
                nextPlayer.getName()
        );


        // Si el próximo jugador no tiene jugada posible, se elimina y se
        // vuelve a llamar passTurn() de forma recursiva.
        if (!nextPlayer.canPlay(board)) {

            eliminatePlayer(nextPlayer);

            passTurn();

            return;
        }


        // Índice 0 siempre es el humano: si no es su turno, se dispara
        // automáticamente el turno de la máquina.
        if (currentPlayerIndex != 0) {

            playMachineTurn(nextPlayer);
        }
    }


    /**
     * Elimina a un jugador de la partida y regresa sus cartas al mazo.
     * (Lógica): ojo que aquí se baraja el mazo cada vez que alguien es
     * eliminado; si el rendimiento es un problema con mazos grandes, se
     * podría optimizar para no barajar tan seguido.
     */
    private void eliminatePlayer(MachinePlayer player) {

        player.eliminate();


        ArrayList<Card> cards =
                player.returnAllCards();


        deck.AddCards(cards);

        deck.ShuffleDeck();


        System.out.println(
                "Jugador eliminado: "
                + player.getName()
        );
    }


    /**
     * Marca el juego como terminado y actualiza la UI con el ganador.
     * (Vista): aquí es donde se debería mostrar una pantalla/dialog de fin
     * de juego en vez de solo cambiar el texto de dos labels; de momento
     * solo limpia la mano del jugador y pone el nombre del ganador.
     */
    private void endGame(MachinePlayer winner) {

        gameIsOver = true;


        lblPlayerTurn.setText(
                "¡Juego terminado!"
        );


        if (winner != null) {

            lblTableSum.setText(
                    "GANADOR: "
                    + winner.getName()
            );

        } else {

            lblTableSum.setText(
                    "Sin ganador"
            );
        }


        playerHandContainer.getChildren().clear();
    }


    /**
     * Le da una carta nueva a un jugador, reciclando el mazo si hace falta.
     * (Lógica): revisar que board.RecycleCards() no incluya la carta activa
     * actual, o se podría duplicar/perder la carta que está en la mesa.
     */
    private void drawCardForPlayer(MachinePlayer player) {

        if (deck.isEmpty()) {

            recycleDeck();
        }


        if (!deck.isEmpty()) {

            player.addCard(deck.getCard());
        }
    }


    /**
     * Recicla las cartas ya jugadas en la mesa de vuelta al mazo cuando
     * este se queda vacío.
     */
    private void recycleDeck() {

        ArrayList<Card> cards =
                board.RecycleCards();


        deck.AddCards(cards);

        deck.ShuffleDeck();
    }


    /**
     * Refresca todos los elementos visuales según el estado actual del
     * juego (suma, carta activa, imagen, mano del humano).
     * (Vista): este es EL método a llamar cada vez que algo cambia en el
     * modelo y se necesita reflejar en pantalla. Si agregan nuevos labels
     * o efectos, probablemente van aquí.
     */
    private void updateUI() {

        if (gameIsOver)
            return;


        lblTableSum.setText(
                "Suma: "
                + board.GetTotalSum()
        );


        Card activeCard =
                board.GetActiveCard();


        if (activeCard != null) {

            lblCurrentCard.setText(
                    activeCard.toString()
            );


            // (Vista): las imágenes de las cartas se cargan como recursos
            // del classpath (getResourceAsStream). Si una imagen no existe
            // en la ruta calculada por getCardImagePath(), esto atrapa la
            // excepción y solo oculta el ImageView, sin tronar la app.
            try {

                Image image =
                        new Image(
                        getClass()
                        .getResourceAsStream(
                                getCardImagePath(activeCard)
                        ));


                imgActiveCard.setImage(image);

            } catch(Exception e) {

                imgActiveCard.setVisible(false);
            }


        } else {

            lblCurrentCard.setText(
                    "Mesa vacía"
            );
        }


        renderPlayerHand();
    }


    /**
     * Dibuja la mano del jugador humano como una serie de StackPane
     * clicables dentro de playerHandContainer.
     * (Vista): AQUÍ ES DONDE FALTA EL CONTENIDO VISUAL de cada carta.
     * Actualmente cada StackPane se crea vacío (sin imagen ni texto),
     * solo con tamaño 80x120 y el evento de clic. Hay que agregar un
     * ImageView (o Label) dentro de cada StackPane usando
     * getCardImagePath(card) para que se vea la carta en pantalla.
     */
    private void renderPlayerHand() {

        playerHandContainer.getChildren().clear();


        MachinePlayer human =
                players.get(0);


        for(Card card : human.getHand()) {


            StackPane container =
                    new StackPane();


            container.setPrefSize(80,120);


            // Nota: "card" se captura en el lambda tal cual está en este
            // momento del for-each; en Java esto es seguro porque "card"
            // es efectivamente final en cada iteración.
            container.setOnMouseClicked(event ->
                    handleHumanCardClick(card)
            );


            playerHandContainer
                    .getChildren()
                    .add(container);
        }
    }


    /**
     * Construye la ruta del recurso de imagen para una carta dada.
     * (Vista): las imágenes deben estar en
     * src/.../View/imagenes/{rank}_{suit}.png (en minúsculas para el palo).
     * Ejemplo: "7_oros.png". Si el compañero de lógica cambia los nombres
     * que devuelve getCardRank()/getCardName(), hay que renombrar también
     * los archivos de imagen para que sigan coincidiendo.
     */
    private String getCardImagePath(Card card) {

        String rank = card.getCardRank();

        String suit = card.getCardName()
                .toLowerCase();


        return "/org/example/cincuentazo/View/imagenes/"
                + rank
                + "_"
                + suit
                + ".png";
    }
}
