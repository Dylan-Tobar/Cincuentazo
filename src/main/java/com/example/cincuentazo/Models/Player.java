package com.example.cincuentazo.Models;

/**
 * @autor Dylan Tobar, Ricardo Hallado, Alejandro Arias
 * Representa a un jugador de Cincuentazo, ya sea humano o controlado por
 * la máquina. Cada jugador tiene una mano de cartas, un estado de vida
 * (activo/eliminado) y un nombre identificador.
 */
public class Player {
    private Card[] hand;
    private boolean life;
    private String name;
    private boolean machineP;

    /**
     * Crea un nuevo jugador con una mano vacía de 4 posiciones y activo desde el inicio.
     * @param name     nombre identificador del jugador
     * @param machineP {@code true} si el jugador es controlado por la máquina, {@code false} si es humano
     */
    public Player(String name, boolean machineP) {
        this.name = name;
        this.machineP = machineP;
        this.hand = new Card[4];
        this.life = true;

    }

    /**
     * Agrega una carta a la mano del jugador, colocándola en la primera
     * posición libre disponible. Si la mano ya está llena, la carta no se agrega.
     * @param c la carta a recibir
     */
    public void receiveCard(Card c) {
        for (int i = 0; i < hand.length; i++) {
            if (hand[i] == null) {
                hand[i] = c;
                return;
            }
        }
    }

    /**
     * Juega (retira) la carta ubicada en la posición indicada de la mano,
     * dejando esa posición vacía.
     * @param pos índice de la carta dentro de la mano
     * @return la carta que estaba en esa posición
     * @throws ArrayIndexOutOfBoundsException si {@code pos} está fuera del rango de la mano
     */
    public Card useCard(int pos) {
        Card cardUse = hand[pos];
        hand[pos] = null;
        return cardUse;
    }

    /**
     * Determina si el jugador tiene al menos una jugada válida disponible
     * dado el valor actual acumulado en la mesa.
     * @param plusTable valor actual acumulado en la mesa
     * @return {@code true} si existe al menos una carta jugable, {@code false} en caso contrario
     */
    public boolean validMove(int plusTable) {
        for(int i= 0;i<hand.length;i++){
            if (hand[i] != null) {
                if (hand[i].getNumber() == 14) {
                    int value1 = hand[i].calculateValue(1);
                    int value10 = hand[i].calculateValue(10);

                    if (plusTable + value1 <= 50 || plusTable + value10 <= 50) {
                        return true;
                    }
                } else {
                    int value = hand[i].calculateValue(0);
                    if (plusTable + value <= 50) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Indica si el jugador sigue activo en la partida.
     * @retun {@code true} si el jugador no ha sido eliminado, {@code false} en caso contrario
     */
    public boolean isLife() {
        return life;
    }

    /**
     * Elimina al jugador de la partida, marcándolo como inactivo.
     */
    public void eliminate() {
        this.life = false;
    }

    /**
     * Indica si el jugador es controlado por la máquina.
     * @return {@code true} si es un jugador máquina, {@code false} si es humano
     */
    public boolean isMachineP() {
        return machineP;
    }

    /**
     * Obtiene la mano actual del jugador.
     * @return arreglo de 4 posiciones con las cartas del jugador (puede contener {@code null})
     */
    public Card[] getHand() {
        return hand;
    }

    /**
     * Obtiene el nombre del jugador.
     * @return el nombre identificador del jugador
     */
    public String getName() {
        return name;
    }
}