package com.shyam.camel.utils;

import org.springframework.stereotype.Component;

@Component
public class Counter {

    private int count = 1;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount() {
        count++;
    }
}
