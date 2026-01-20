package com.example.myshiftapp;

public class ScheduleCell {
    public enum Type { HEADER, CELL }
    public enum State { EMPTY, YES, NO }

    public Type type;
    public String text; // מה שמציגים
    public String key; // למשל Sun_Morning (רק לתאי CELL)
    public State state;

    public ScheduleCell(Type type, String text, String key, State state) {
        this.type = type;
        this.text = text;
        this.key = key;
        this.state = state;
    }
}