package com.example.cincuentazo.Models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link Game}: reparto inicial, jugadas válidas
 * e inválidas, eliminación de jugadores y condición de victoria.
 */
class GameTest {

    private Game game;
    private Player human;
    private Player machine;

    @BeforeEach
    void setUp() {
        human = new Player("Humano", false);
        machine = new Player("Máquina 1", true);

        ArrayList<Player> players = new ArrayList<>();
        players.add(human);
        players.add(machine);

        game = new Game(players);
        game.initialGame();
    }

    @Test
    void eachPlayerShouldReceiveFourCards() {
        int humanCards = 0;
        for (Card c : human.getHand()) {
            if (c != null) humanCards++;
        }
        assertEquals(4, humanCards);
    }

    @Test
    void currentPlayerShouldBeFirstInList() {
        assertEquals(human, game.getCurrentPlayer());
    }

    @Test
    void playingAnEmptyPositionShouldThrowInvalidPlay() {
        human.useCard(0); // dejamos la posición 0 vacía a propósito
        assertThrows(InvalidPlay.class, () -> game.playCard(human, 0, 10));
    }

    @Test
    void tableShouldRejectAPlayThatWouldExceedFifty() {
        // Game.playCard delega la validación de la regla principal en
        // Table.validPlay; probamos aquí ese límite de forma controlada
        // (mesa en 45 + una carta que suma 10 = 55, debe rechazarse).
        Table table = new Table(new Card(10, "Corazones"), 45);
        assertFalse(table.validPlay(10));
    }

    @Test
    void checkWinnerShouldReturnNullWhileTwoPlayersAreAlive() {
        assertNull(game.checkWinner());
    }

    @Test
    void checkWinnerShouldReturnTheOnlyRemainingPlayer() {
        machine.eliminate();
        assertEquals(human, game.checkWinner());
    }
}
