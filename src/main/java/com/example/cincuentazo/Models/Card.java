package com.example.cincuentazo.Models;

public class Card {
    private int number;
    private String suit;

    public Card(int number, String suit) {
        this.number = number;
        this.suit = suit;
    }

    public int getNumber() {
        return number;
    }

    public String getSuit() {
        return suit;
    }

    public int calculateValue(int valueAs) {
        if (number >= 2 && number <= 8) {
            return number;
        }
        else if (number == 9) {
            return 0;
        }
        else if (number == 10) {
            return 10;
        }
        else if (number == 11 || number == 12 || number == 13) {
            return -10;
        }
        else if (number == 14) {
            return valueAs;
        }
        return 0;
    }
}
