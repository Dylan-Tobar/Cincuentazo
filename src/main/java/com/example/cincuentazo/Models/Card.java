package com.example.cincuentazo.Models;

/**
 * @autor Dylan Tobar, Ricardo Hallado, Alejandro Arias
 * Representa una carta de la baraja utilizada en Cincuentazo.
 */
public class Card {
    private final int number;
    private final String suit;

    /**
     * Crea una nueva carta
     * @param number número de la carta (2-14, donde 11=J, 12=Q, 13=K, 14=A)
     * @param suit   palo de la carta (Diamantes, Treboles, Corazones, Picas)
     */
    public Card(int number, String suit) {
        this.number = number;
        this.suit = suit;
    }

    /** @return el número de la carta (2-14) */
    public int getNumber() {
        return number;
    }

    /** @return el palo de la carta */
    public String getSuit() {
        return suit;
    }

    /**
     * Calcula el valor que esta carta aporta a la suma de la mesa.
     *
     * @param valueAs solo se usa cuando la carta es un As (14): indica si
     *                debe valer 1 o 10. Para las demás cartas se ignora.
     * @return el valor numérico que debe sumarse (puede ser negativo para J/Q/K)
     */
    public int calculateValue(int valueAs) {
        if (number >= 2 && number <= 8) {
            return number;
        } else if (number == 9) {
            return 0;
        } else if (number == 10) {
            return 10;
        } else if (number == 11 || number == 12 || number == 13) {
            return -10;
        } else if (number == 14) {
            return valueAs;
        }
        return 0;
    }

    /** @return representación legible de la carta, ej. "7 de Picas" */
    @Override
    public String toString() {
        String rank;
        switch (number) {
            case 11: rank = "J"; break;
            case 12: rank = "Q"; break;
            case 13: rank = "K"; break;
            case 14: rank = "A"; break;
            default: rank = String.valueOf(number);
        }
        return rank + " de " + suit;
    }
}
