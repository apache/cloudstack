package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Command;

public class prepareCreateTemplateCommand extends Command {
    String ip;
    String mac;
    String netMask;
    String gateway;
    String dns;
    String template;
    
    @Override
    public boolean executeInSequence() {
        return true;
    }
    
    public prepareCreateTemplateCommand(String ip, String mac, String netMask, String gateway, String dns, String template) {
        this.ip = ip;
        this.mac = mac;
        this.netMask = netMask;
        this.gateway = gateway;
        this.dns = dns;
        this.template = template;
    }

    public String getIp() {
        return ip;
    }
    
    public String getMac() {
        return mac;
    }
    
    public String getNetMask() {
        return netMask;
    }
    
    public String getGateWay() {
        return gateway;
    }
    
    public String getDns() {
        return dns;
    }
    
    public String getTemplate() {
        return template;
    }
}
