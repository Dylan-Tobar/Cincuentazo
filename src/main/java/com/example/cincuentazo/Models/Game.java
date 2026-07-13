package com.example.cincuentazo.Models;

import java.util.ArrayList;


public class Game {
    private ArrayList<Player> players;
    private Deck deck;
    private Table table;
    private int currentTurn;

    public Game(ArrayList<Player> players) {
        this.players = players;
        this.deck = new Deck();
        this.table = null;
        this.currentTurn = 0;
    }

    public void initialGame() {
        deck.mixCards();
        for (Player p : players) {
            for (int i = 0; i < 4; i++) {
                try {
                    p.receiveCard(deck.distributeC());
                } catch (EmptyDeck e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        try {
            Card initialCard = deck.distributeC();
            int initialValue = (initialCard.getNumber() == 14) ? 1 : initialCard.calculateValue(0);
            table = new Table(initialCard, initialValue);
        } catch (EmptyDeck e) {
            System.out.println(e.getMessage());
        }
    }

    public void playCard(Player player, int position, int aceValue) throws InvalidPlay {
        Card card = player.getHand()[position];
        if (card == null) {
            throw new InvalidPlay("No hay carta en esa posición.");
        }
        int value = (card.getNumber() == 14) ? card.calculateValue(aceValue) : card.calculateValue(0);

        if (!table.validPlay(value)) {
            throw new InvalidPlay("Esa jugada supera 50 en la mesa.");
        }

        player.useCard(position);
        table.plus(card, value);
        takeCard(player);
    }

    private void takeCard(Player player) {
        try {
            player.receiveCard(deck.distributeC());
        } catch (EmptyDeck e) {
            ArrayList<Card> recycled = table.getUsedCards();
            deck.reuseCards(recycled);
            try {
                player.receiveCard(deck.distributeC());
            } catch (EmptyDeck ex) {
                System.out.println("No hay cartas suficientes ni para reciclar.");
            }
        }
    }


    public void checkElimination(Player player) {
        if (!player.validMove(table.getPresentV())) {
            player.eliminate();
            ArrayList<Card> lostCards = new ArrayList<>();
            for (Card c : player.getHand()) {
                if (c != null) lostCards.add(c);
            }
            deck.reuseCards(lostCards);
        }
    }


    public void nextTurn() {
        do {
            currentTurn = (currentTurn + 1) % players.size();
        } while (!players.get(currentTurn).isLife());
    }

    public Player getCurrentPlayer() {
        return players.get(currentTurn);
    }


    public Player checkWinner() {
        Player onlyActive = null;
        int count = 0;
        for (Player p : players) {
            if (p.isLife()) {
                count++;
                onlyActive = p;
            }
        }
        return (count == 1) ? onlyActive : null;
    }


    public void machineTurn(Player machine, Runnable onFinish) {
        Thread machineThread = new Thread(() -> {
            try {
                int seconds = 2 + (int) (Math.random() * 3);
                Thread.sleep(seconds * 1000L);
                onFinish.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        machineThread.start();
    }
}