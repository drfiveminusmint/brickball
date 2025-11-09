package com.github.drfiveminusmint.brickball.util;

public class Counter {
    private int value = 0;
    public void reset() {value = 0;}
    public int value() {return value;}
    public void increment() {value += 1;}
}
