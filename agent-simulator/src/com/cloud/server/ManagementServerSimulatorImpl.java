package com.cloud.server;


public class ManagementServerSimulatorImpl extends ManagementServerExtImpl {
    @Override
    public String[] getApiConfig() {
        String[] apis = super.getApiConfig();
        String[] newapis = new String[apis.length + 1];
        for (int i = 0; i < apis.length; i++) {
            newapis[i] = apis[i];
        }
        
        newapis[apis.length] = "commands-simulator.properties";
        return newapis;
    }
}
