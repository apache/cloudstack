/**
 * 
 */
package com.cloud.agent.api;

import com.cloud.agent.api.to.VirtualMachineTO;

public class Start2Command extends Command {
    VirtualMachineTO vm;
    
    public VirtualMachineTO getVirtualMachine() {
        return vm;
    }
    
    /*
    long id;
    String guestIpAddress;
    String gateway;
    int ramSize;
    String imagePath;
    String guestNetworkId;
    String guestMacAddress;
    String vncPassword;
    String externalVlan;
    String externalMacAddress;
    int utilization;
    int cpuWeight;
    int cpu;
    int networkRateMbps;
    int networkRateMulticastMbps;
    String hostName;
    String arch;
    String isoPath;
    boolean bootFromISO;
    String guestOSDescription;
    
    ---->console proxy
    private ConsoleProxyVO proxy;
    private int proxyCmdPort;
    private String vncPort;
    private String urlPort;
    private String mgmt_host;
    private int mgmt_port;
    private boolean sslEnabled;

    ----->abstract
    protected String vmName;
    protected String storageHosts[] = new String[2];
    protected List<VolumeVO> volumes;
    protected boolean mirroredVols = false;
    protected BootloaderType bootloader = BootloaderType.PyGrub;
    
     */
    
    @Override
    public boolean executeInSequence() {
        return true;
    }
    
    public Start2Command() {
    }
}
