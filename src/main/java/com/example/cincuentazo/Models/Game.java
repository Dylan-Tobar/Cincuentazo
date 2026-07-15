package com.example.cincuentazo.Models;

import java.util.ArrayList;

/**
 * Orquesta la lógica central de una partida de Cincuentazo: reparto
 * inicial, turnos, jugadas, eliminaciones y condición de victoria.
 * <p>
 * Esta clase pertenece a la capa de Modelo (MVC) y no depende de JavaFX;
 * toda la manipulación de hilos relacionada con la interfaz (turno de la
 * máquina, temporizador) vive en el Controlador, que es quien conoce el
 * hilo de UI y debe usar {@code Platform.runLater(...)} para actualizarla.
 */
public class Game {
    private final ArrayList<Player> players;
    private final Deck deck;
    private Table table;
    private int currentTurn;

    /**
     * Crea una partida nueva con la lista de jugadores dada. El primer
     * jugador de la lista (índice 0) es quien inicia el juego.
     *
     * @param players lista de jugadores participantes (humano + máquinas)
     */
    public Game(ArrayList<Player> players) {
        this.players = players;
        this.deck = new Deck();
        this.table = null;
        this.currentTurn = 0;
    }

    /**
     * Prepara la partida: mezcla el mazo, reparte 4 cartas a cada
     * jugador y coloca la primera carta boca arriba en la mesa (HU-2).
     */
    public void initialGame() {
        deck.mixCards();
        for (Player p : players) {
            for (int i = 0; i < 4; i++) {
                try {
                    p.receiveCard(deck.distributeC());
                } catch (EmptyDeck e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        try {
            Card initialCard = deck.distributeC();
            int initialValue = (initialCard.getNumber() == 14) ? 1 : initialCard.calculateValue(0);
            table = new Table(initialCard, initialValue);
        } catch (EmptyDeck e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Ejecuta la jugada de un jugador (HU-3): valida la regla principal
     * y mueve la carta a la mesa. A partir de este punto el jugador
     * queda con 3 cartas en mano; para completar su turno debe robar
     * una carta con {@link #drawCard(Player)} (HU-4).
     *
     * @param player   jugador que juega
     * @param position posición (0-3) de la carta dentro de su mano
     * @param aceValue si la carta es un As, valor a usar (1 o 10); se
     *                 ignora para las demás cartas
     * @throws InvalidPlay si no hay carta en esa posición o la jugada
     *                      superaría 50 en la mesa
     */
    public void playCard(Player player, int position, int aceValue) throws InvalidPlay {
        Card card = player.getHand()[position];
        if (card == null) {
            throw new InvalidPlay("No hay carta en esa posición.");
        }
        int value = (card.getNumber() == 14) ? card.calculateValue(aceValue) : card.calculateValue(0);

        if (!table.validPlay(value)) {
            throw new InvalidPlay("Esa jugada supera 50 en la mesa.");
        }

        player.useCard(position);
        table.plus(card, value);
    }

    /**
     * Roba una carta del mazo para el jugador y así completa su turno
     * (HU-4): si el mazo está vacío, recicla antes las cartas ya
     * jugadas en la mesa (excepto la última) antes de intentarlo de
     * nuevo.
     *
     * @param player jugador que roba la carta (debe tener 3 cartas en
     *               mano tras haber jugado con {@link #playCard})
     */
    public void drawCard(Player player) {
        try {
            player.receiveCard(deck.distributeC());
        } catch (EmptyDeck e) {
            ArrayList<Card> recycled = table.getUsedCards();
            deck.reuseCards(recycled);
            try {
                player.receiveCard(deck.distributeC());
            } catch (EmptyDeck ex) {
                System.out.println("No hay cartas suficientes ni para reciclar.");
            }
        }
    }

    /**
     * Evalúa si un jugador ya no tiene ninguna jugada posible dado el
     * estado actual de la mesa (HU-5); si es así, lo elimina y envía sus
     * cartas de vuelta al mazo.
     *
     * @param player jugador a evaluar
     */
    public void checkElimination(Player player) {
        if (!player.validMove(table.getPresentV())) {
            player.eliminate();
            ArrayList<Card> lostCards = new ArrayList<>();
            for (Card c : player.getHand()) {
                if (c != null) lostCards.add(c);
            }
            deck.reuseCards(lostCards);
        }
    }

    /** Avanza el turno de forma circular al siguiente jugador que siga activo. */
    public void nextTurn() {
        do {
            currentTurn = (currentTurn + 1) % players.size();
        } while (!players.get(currentTurn).isLife());
    }

    /** @return el jugador al que le corresponde jugar actualmente */
    public Player getCurrentPlayer() {
        return players.get(currentTurn);
    }

    /** @return la lista completa de jugadores de la partida */
    public ArrayList<Player> getPlayers() {
        return players;
    }

    /** @return la suma actual acumulada en la mesa */
    public int getTableSum() {
        return table.getPresentV();
    }

    /** @return la carta que está actualmente boca arriba en la mesa */
    public Card getActiveCard() {
        return table.getUltimateC();
    }

    /**
     * Evalúa la condición de fin de juego (HU-6): si solo queda un
     * jugador activo, lo devuelve como ganador.
     *
     * @return el único jugador activo, o null si aún hay más de uno en juego
     */
    public Player checkWinner() {
        Player onlyActive = null;
        int count = 0;
        for (Player p : players) {
            if (p.isLife()) {
                count++;
                onlyActive = p;
            }
        }
        return (count == 1) ? onlyActive : null;
    }
}