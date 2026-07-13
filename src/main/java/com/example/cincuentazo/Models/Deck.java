package com.example.cincuentazo.Models;

import java.util.ArrayList;
import java.util.Collections;

public class Deck {
    private ArrayList<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        generateCards();
    }

    private void generateCards() {
        String[] suits = {"Diamantes", "Treboles", "Corazones", "Picas"};

        for (int i = 2; i <= 14; i++) {
            for(int j = 0; j<4;j++ ){
                cards.add(new Card(i, suits[j]));
            }
        }
    }

    public void mixCards() {
        Collections.shuffle(cards);
    }

    public Card distributeC() throws EmptyDeck {
        if (cards.isEmpty()) {
            throw new EmptyDeck("El mazo está vacío.");
        }
        return cards.remove(cards.size() - 1);
    }

    public void reuseCards(ArrayList<Card> discarC) {
        cards.addAll(discarC);
        mixCards();
    }
}
