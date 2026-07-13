package com.example.cincuentazo.Models;


public class Player {
    private Card[] hand;
    private boolean life;
    private String name;
    private boolean machineP;

    public Player(String name, boolean machineP) {
        this.name = name;
        this.machineP = machineP;
        this.hand = new Card[4];
        this.life = true;

    }

    public void receiveCard(Card c) {
        for (int i = 0; i < hand.length; i++) {
            if (hand[i] == null) {
                hand[i] = c;
                return;
            }
        }
    }

    public Card useCard(int pos) {
        Card cardUse = hand[pos];
        hand[pos] = null;
        return cardUse;
    }

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

    public boolean isLife() {
        return life;
    }

    public void eliminate() {
        this.life = false;
    }

    public boolean isMachineP() {
        return machineP;
    }

    public Card[] getHand() {
        return hand;
    }


    public String getName() {
        return name;
    }
}

