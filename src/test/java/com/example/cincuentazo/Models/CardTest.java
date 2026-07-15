package com.example.cincuentazo.Models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas unitarias para {@link Card}: valida que el cálculo de valor
 * de cada tipo de carta respete las reglas de Cincuentazo.
 */
class CardTest {

    @Test
    void numberCardsShouldSumTheirOwnValue() {
        Card card = new Card(7, "Picas");
        assertEquals(7, card.calculateValue(0));
    }

    @Test
    void tenShouldSumTen() {
        Card card = new Card(10, "Diamantes");
        assertEquals(10, card.calculateValue(0));
    }

    @Test
    void nineShouldNotAddOrSubtract() {
        Card card = new Card(9, "Corazones");
        assertEquals(0, card.calculateValue(0));
    }

    @Test
    void faceCardsShouldSubtractTen() {
        assertEquals(-10, new Card(11, "Treboles").calculateValue(0)); // J
        assertEquals(-10, new Card(12, "Picas").calculateValue(0));    // Q
        assertEquals(-10, new Card(13, "Corazones").calculateValue(0)); // K
    }

    @Test
    void aceShouldUseTheGivenValue() {
        Card ace = new Card(14, "Diamantes");
        assertEquals(1, ace.calculateValue(1));
        assertEquals(10, ace.calculateValue(10));
    }
}
