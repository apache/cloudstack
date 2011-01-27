package com.cloud.network;


public class NetworkProfile{
    private Network network;
    private String dns1;
    private String dns2;

    public NetworkProfile(Network network, String dns1, String dns2) {
        this.network = network;
        this.dns1 = dns1;
        this.dns2 = dns2;
    }
    
    public NetworkProfile() {
        
    }

    public Network getNetwork() {
        return network;
    }
    
    public void setNetwork(Network network){
        this.network = network;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }
    
    
}
