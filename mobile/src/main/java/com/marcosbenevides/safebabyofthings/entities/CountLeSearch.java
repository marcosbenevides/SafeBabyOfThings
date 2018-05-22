package com.marcosbenevides.safebabyofthings.entities;

import com.marcosbenevides.safebabyofthings.callbacks.CountCallback;

public class CountLeSearch {

    private int count;
    private int max;
    private CountCallback countCallback;

    public CountLeSearch(CountCallback countCallback, int max) {
        this.countCallback = countCallback;
        this.max = max;
        zerar();
    }

    public void zerar() {
        count = 0;
    }

    public Integer getCount() {
        return count;
    }

    public void increment() {
        if (isMax()) {
            countCallback.callAlert();
        } else {
            count++;
        }
    }

    public boolean isMax() {
        return count >= max;
    }


}
