package com.example.cincuentazo.Models;
import java.util.ArrayList;

public class Table {
    private int presentV;
    private ArrayList<Card> history;

    public Table(Card initialCard, int initialValue) {
        this.history = new ArrayList<>();
        this.history.add(initialCard);
        this.presentV = initialValue;
    }

    public boolean validPlay(int valueOfTheCar) {
        return presentV + valueOfTheCar <=50;
    }

    public void plus(Card c, int valueOfTheCar) {
        presentV += valueOfTheCar;
        history.add(c);
    }

    public ArrayList<Card> getUsedCards() {
        Card ultimateCard = getUltimateC();
        ArrayList<Card> reusablCards = new ArrayList<>(history);
        reusablCards.remove(ultimateCard);

        history.clear();
        history.add(ultimateCard);

        return reusablCards;
    }

    public int getPresentV() {
        return presentV;
    }

    public Card getUltimateC() {
        return history.get(history.size() - 1);
    }

}
