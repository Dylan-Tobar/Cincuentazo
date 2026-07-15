package com.example.cincuentazo.Models;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Representa el mazo de cartas del juego (52 cartas, sin comodines: del 2
 * al 14 -donde 11=J, 12=Q, 13=K, 14=A- en los cuatro palos).
 */
public class Deck {
    private final ArrayList<Card> cards;

    /** Crea un mazo completo de 52 cartas, sin mezclar todavía. */
    public Deck() {
        cards = new ArrayList<>();
        generateCards();
    }

    private void generateCards() {
        String[] suits = {"Diamantes", "Treboles", "Corazones", "Picas"};
        for (int i = 2; i <= 14; i++) {
            for (int j = 0; j < 4; j++) {
                cards.add(new Card(i, suits[j]));
            }
        }
    }

    /** Mezcla aleatoriamente las cartas restantes del mazo. */
    public void mixCards() {
        Collections.shuffle(cards);
    }

    /** @return true si no quedan cartas por repartir en el mazo */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Retira y entrega la carta superior del mazo.
     *
     * @return la carta repartida
     * @throws EmptyDeck si el mazo no tiene cartas disponibles
     */
    public Card distributeC() throws EmptyDeck {
        if (cards.isEmpty()) {
            throw new EmptyDeck("El mazo está vacío.");
        }
        return cards.remove(cards.size() - 1);
    }

    /**
     * Devuelve cartas usadas (jugadas o de un jugador eliminado) al mazo
     * y lo vuelve a mezclar, para que sigan estando disponibles.
     *
     * @param discarC cartas a reincorporar al mazo
     */
    public void reuseCards(ArrayList<Card> discarC) {
        cards.addAll(discarC);
        mixCards();
    }
}
