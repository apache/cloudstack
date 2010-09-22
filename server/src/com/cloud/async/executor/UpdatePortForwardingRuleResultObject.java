package com.cloud.async.executor;

import com.cloud.serializer.Param;

public class UpdatePortForwardingRuleResultObject {
    @Param(name="id")
    private long id;

    @Param(name="publicip")
    private String publicIp;

    @Param(name="privateip")
    private String privateIp;

    @Param(name="publicport")
    private String publicPort;

    @Param(name="privateport")
    private String privatePort;

    @Param(name="protocol")
    private String protocol;

    @Param(name="virtualmachineid")
    private long virtualMachineId;

    @Param(name="vmname")
    private String virtualMachineName;

    @Param(name="vmdisplayname")
    private String virtualMachineDisplayName;
    
    public String getVirtualMachineDisplayName(){
    	return this.virtualMachineDisplayName;
    }
    
    public void setVirtualMachineDisplayName(String name){
    	this.virtualMachineDisplayName = name;
    }
    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public void setPublicPort(String publicPort) {
        this.publicPort = publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public void setPrivatePort(String privatePort) {
        this.privatePort = privatePort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }
}
