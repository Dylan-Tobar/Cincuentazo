package com.example.cincuentazo.Models;

public class InvalidPlay extends RuntimeException {
    public InvalidPlay(String message) {
        super(message);
    }
}