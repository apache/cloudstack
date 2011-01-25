package com.cloud.api.doc;

public class Alert {
    private String type;
    private int value;
    
    public Alert(String type, int value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
    
}
