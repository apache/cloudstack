package com.cloud.simulator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.vm.VirtualMachine.State;

@Entity
@Table(name="mockvm")

public class MockVMVO implements MockVm{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="name")
    private String name;
    
    @Column(name="host_id")
    private long hostId;
    
    @Column(name="type")
    private String vmType;
    
    @Column(name="state")
    private State state;
    
    @Column(name="vnc_port")
    private int vncPort;
    
    @Column(name="memory")
    private long memory;
    
    @Column(name="cpu")
    private int cpu;
    
    public MockVMVO() {
        
    }
    
    public long getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getHostId() {
        return this.hostId;
    }
    
    public void setHostId(long hostId) {
        this.hostId = hostId;
    }
    
    public String getVmType() {
        return this.vmType;
    }
    
    public void setVmType(String vmType) {
        this.vmType = vmType;
    }
    
    public State getState() {
        return this.state;
    }
    
    public String getType() {
    	return this.vmType;
    }
    
    public void setState(State state) {
        this.state = state;
    }
    
    public int getVncPort() {
        return this.vncPort;
    }
    
    public void setVncPort(int vncPort) {
        this.vncPort = vncPort;
    }
    
    public long getMemory() {
        return this.memory;
    }
    
    public void setMemory(long memory) {
        this.memory = memory;
    }
    
    public int getCpu() {
        return this.cpu;
    }
    
    public void setCpu(int cpu) {
        this.cpu = cpu;
    }
    
    public void setType(String type) {
    	this.vmType = type;
    }

}
