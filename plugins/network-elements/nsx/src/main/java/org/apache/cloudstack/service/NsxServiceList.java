package org.apache.cloudstack.service;

public enum NsxServiceList {
    AD_Server(1024, "/infra/services/AD_Server", "TCP"),
    Active_Directory_Server(464, "/infra/services/Active_Directory_Server", "TCP"),
    SSH(22, "/infra/services/SSH", "TCP");

    int port;
    String service;
    String path;
    String protocol;
    NsxServiceList(int port, String path, String protocol) {
        this.port = port;
        this.path = path;
        this.protocol = protocol;
    }
}
