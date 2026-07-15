package com.example.cincuentazo.Models;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link Deck}: reparto de cartas, detección de
 * mazo vacío y reciclaje de cartas usadas.
 */
class DeckTest {

    @Test
    void newDeckShouldNotBeEmpty() {
        Deck deck = new Deck();
        assertFalse(deck.isEmpty());
    }

    @Test
    void distributeCShouldReduceDeckSize() throws EmptyDeck {
        Deck deck = new Deck();
        deck.distributeC();
        assertFalse(deck.isEmpty()); // el mazo tiene 52 cartas, sigue sin estar vacío
    }

    @Test
    void distributeCOnEmptyDeckShouldThrowEmptyDeck() {
        Deck deck = new Deck();
        // Vaciamos el mazo completo (52 cartas).
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 52; i++) {
                deck.distributeC();
            }
        });
        assertTrue(deck.isEmpty());
        assertThrows(EmptyDeck.class, deck::distributeC);
    }

    @Test
    void reuseCardsShouldMakeDeckAvailableAgain() {
        Deck deck = new Deck();
        ArrayList<Card> discarded = new ArrayList<>();
        discarded.add(new Card(5, "Picas"));
        discarded.add(new Card(10, "Corazones"));

        deck.reuseCards(discarded);

        assertFalse(deck.isEmpty());
    }
}
