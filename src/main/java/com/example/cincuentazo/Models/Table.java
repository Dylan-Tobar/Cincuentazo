package com.example.cincuentazo.Models;
import java.util.ArrayList;

/**
 * @autor Dylan Tobar, Ricardo Hallado, Alejandro Arias
 * Representa la mesa de juego en Cincuentazo, donde se acumulan las cartas
 * jugadas y se lleva el control del valor acumulado de la ronda actual.
 */
public class Table {
    private int presentV;
    private ArrayList<Card> history;

    /**
     * Crea una nueva mesa de juego con una carta inicial y su valor asociado.
     * @param initialCard  la primera carta colocada en la mesa
     * @param initialValue el valor numérico inicial que aporta dicha carta
     */
    public Table(Card initialCard, int initialValue) {
        this.history = new ArrayList<>();
        this.history.add(initialCard);
        this.presentV = initialValue;
    }

    /**
     * Verifica si jugar una carta con el valor indicado es una jugada válida,
     * es decir, si al sumarla al valor actual de la mesa no se supera 50.
     * @param valueOfTheCar valor numérico de la carta que se desea jugar
     * @return {@code true} si la suma no excede 50, {@code false} en caso contrario
     */
    public boolean validPlay(int valueOfTheCar) {
        return presentV + valueOfTheCar <= 50;
    }

    /**
     * Registra la jugada de una carta en la mesa: suma su valor al total
     * acumulado y la añade al historial de cartas jugadas.
     * @aram c             la carta que se juega
     * @param valueOfTheCar el valor numérico que aporta la carta jugada
     */
    public void plus(Card c, int valueOfTheCar) {
        presentV += valueOfTheCar;
        history.add(c);
    }

    /**
     * Obtiene las cartas "usadas" de la mesa, es decir, todas las cartas del
     * historial excepto la última jugada.
     * @return una lista con las cartas usadas (todas menos la última jugada)
     */
    public ArrayList<Card> getUsedCards() {
        Card ultimateCard = getUltimateC();
        ArrayList<Card> reusablCards = new ArrayList<>(history);
        reusablCards.remove(ultimateCard);

        history.clear();
        history.add(ultimateCard);

        return reusablCards;
    }

    /**
     * Obtiene el valor numérico actual acumulado en la mesa.
     * @return el alor presente de la mesa
     */
    public int getPresentV() {
        return presentV;
    }

    /**
     * Obtiene la última carta jugada en la mesa.
     * @return la carta más reciente del historial
     */
    public Card getUltimateC() {
        return history.get(history.size() - 1);
    }

}