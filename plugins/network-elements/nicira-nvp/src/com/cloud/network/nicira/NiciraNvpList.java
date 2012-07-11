package com.cloud.network.nicira;

import java.util.List;

public class NiciraNvpList<T> {
    private List<T> results;
    private int result_count;

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    public int getResult_count() {
        return result_count;
    }

    public void setResult_count(int result_count) {
        this.result_count = result_count;
    }        

}
