package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

public class CheckS2SVpnConnectionsAnswer extends Answer {
    Map<String, Boolean> ipToConnected;
    Map<String, String> ipToDetail;
    String details;
    
    protected CheckS2SVpnConnectionsAnswer() {
        ipToConnected = new HashMap<String, Boolean>();
        ipToDetail = new HashMap<String, String>();
    }
    
    public CheckS2SVpnConnectionsAnswer(CheckS2SVpnConnectionsCommand cmd, boolean result, String details) {
        super(cmd, result, details);
        ipToConnected = new HashMap<String, Boolean>();
        ipToDetail = new HashMap<String, String>();
        this.details = details;
        if (result) {
            parseDetails(details);
        }
    }
    
    protected void parseDetails(String details) {
        String[] lines = details.split("&");
        for (String line : lines) {
            String[] words = line.split(":");
            if (words.length != 3) {
                //Not something we can parse
                return;
            }
            String ip = words[0];
            boolean connected = words[1].equals("0");
            String detail = words[2];
            ipToConnected.put(ip, connected);
            ipToDetail.put(ip, detail);
        }
    }
    
    public boolean isConnected(String ip) {
        if (this.getResult()) {
            return ipToConnected.get(ip);
        }
        return false;
    }
    
    public String getDetail(String ip) {
        if (this.getResult()) {
            return ipToDetail.get(ip);
        }
        return null;
    }
}
