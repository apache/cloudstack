package org.apache.cloudstack.network.tungsten.service;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.PropertiesUtil;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorFactory;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.MacAddressesType;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.SecurityGroup;
import net.juniper.contrail.api.types.SubnetType;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnectorFactory;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
public class TungstenServiceImpl implements TungstenService {

    private static final Logger s_logger = Logger.getLogger(TungstenServiceImpl.class);

    private ApiConnector _api;
    private final String configuration = "plugins/network-elements/tungsten/conf/tungsten.properties";
    private VRouterApiConnector _vrouterApi;

    public ApiConnector get_api() {
        return _api;
    }

    public VRouterApiConnector get_vrouterApi() {
        return _vrouterApi;
    }

    @PostConstruct
    public void init() throws ConfigurationException {
        File configFile = PropertiesUtil.findConfigFile(configuration);
        FileInputStream fileStream = null;
        try {
            String hostname = null;
            int port = 0;
            String vrouterHost = null;
            String vrouterPort = null;
            if (configFile == null) {
                throw new FileNotFoundException("Tungsten config file not found!");
            } else {
                final Properties configProps = new Properties();
                fileStream = new FileInputStream(configFile);
                configProps.load(fileStream);

                hostname = configProps.getProperty("tungsten.api.hostname");
                String portStr = configProps.getProperty("tungsten.api.port");
                if (portStr != null && portStr.length() > 0) {
                    port = Integer.parseInt(portStr);
                }

                vrouterHost = configProps.getProperty("tungsten.vrouter.hostname");
                vrouterPort = configProps.getProperty("tungsten.vrouter.port");
            }
            _api =  ApiConnectorFactory.build(hostname, port);
            _vrouterApi = VRouterApiConnectorFactory.getInstance(vrouterHost, vrouterPort);
        } catch (IOException ex) {
            s_logger.warn("Unable to read " + configuration, ex);
            throw new ConfigurationException();
        } catch (Exception ex) {
            s_logger.debug("Exception in configure: " + ex);
            ex.printStackTrace();
            throw new ConfigurationException();
        } finally {
            IOUtils.closeQuietly(fileStream);
        }
    }

    @Override
    public VirtualNetwork createNetworkInTungsten(String networkUuid, String networkName, String networkIpamUuid, String ipAllocPoolStart,
                                                  String ipAllocPoolEnd, String subnetIpPrefix, int subnetIpPrefixLength, String defaultGateway,
                                                  boolean isDhcpEnabled, List<String> dnsNameservers, boolean isIpAddrFromStart){
        VirtualNetwork network = new VirtualNetwork();
        try {
            network.setName(networkName);
            network.setNetworkIpam(getNetworkIpam(networkName, networkIpamUuid), getVnSubnetsType(ipAllocPoolStart, ipAllocPoolEnd, subnetIpPrefix,
                    subnetIpPrefixLength, defaultGateway, isDhcpEnabled, dnsNameservers, isIpAddrFromStart));
            _api.create(network);
            return (VirtualNetwork) _api.findByFQN(VirtualNetwork.class, getFqnName(network));
        } catch (IOException e) {
            s_logger.error("Unable to read " + configuration, e);
            return null;
        }
    }

    @Override
    public VirtualNetwork deleteNetworkFromTungsten(String tungstenNetworkUuid) throws IOException {
        VirtualNetwork network = (VirtualNetwork)_api.findById(VirtualNetwork.class, tungstenNetworkUuid);
        if(network != null) {
            if(network.getVirtualMachineInterfaceBackRefs() != null)
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete network");
            _api.delete(network);
            return network;
        }
        else
            throw new InvalidParameterValueException("Unable to find tungsten network with UUID: " + tungstenNetworkUuid);
    }

    @Override
    public VirtualMachine createVmInTungsten(String vmUuid, String vmName){
        VirtualMachine virtualMachine = new VirtualMachine();
        try {
            virtualMachine.setName(vmName);
            virtualMachine.setUuid(vmUuid);
            _api.create(virtualMachine);
            return (VirtualMachine) _api.findByFQN(VirtualMachine.class, getFqnName(virtualMachine));
        } catch (IOException e) {
            s_logger.error("Unable to read " + configuration, e);
            return null;
        }
    }

    @Override
    public InstanceIp createInstanceIpInTungsten(String instanceIpName, String tungstenVmInterfaceUuid, String tungstenNetworkUuid,
                                                 String tungstenInstanceIpAddress){
        InstanceIp instanceIp = new InstanceIp();
        try{
            instanceIp.setName(instanceIpName);
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) _api.findById(VirtualMachineInterface.class, tungstenVmInterfaceUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) _api.findById(VirtualNetwork.class, tungstenNetworkUuid);
            if(virtualNetwork != null)
                instanceIp.setVirtualNetwork(virtualNetwork);
            if(virtualMachineInterface != null)
                instanceIp.setVirtualMachineInterface(virtualMachineInterface);
            instanceIp.setAddress(tungstenInstanceIpAddress);
            _api.create(instanceIp);
            return (InstanceIp) _api.findByFQN(InstanceIp.class, getFqnName(instanceIp));
        } catch (IOException e) {
            s_logger.error("Unable to read " + configuration, e);
            return null;
        }
    }

    @Override
    public VirtualMachineInterface createVmInterfaceInTungsten(String vmInterfaceName, String tungstenProjectUuid, String tungstenNetworkUuid,
                                                               String tungstenVmUuid, String tungstenSecurityGroupUuid, List<String> tungstenVmInterfaceMacAddresses){
        VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
        try {
            virtualMachineInterface.setName(vmInterfaceName);
            Project project = (Project) _api.findById(Project.class, tungstenProjectUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) _api.findById(VirtualNetwork.class, tungstenNetworkUuid);
            VirtualMachine virtualMachine = (VirtualMachine) _api.findById(VirtualMachine.class, tungstenVmUuid);
            SecurityGroup securityGroup = (SecurityGroup) _api.findById(SecurityGroup.class, tungstenSecurityGroupUuid);
            if(virtualNetwork != null)
                virtualMachineInterface.setVirtualNetwork(virtualNetwork);
            if(virtualMachine != null)
                virtualMachineInterface.setVirtualMachine(virtualMachine);
            if(securityGroup != null)
                virtualMachineInterface.setSecurityGroup(securityGroup);
            if(project != null)
                virtualMachineInterface.setParent(project);
            if(tungstenVmInterfaceMacAddresses != null && !tungstenVmInterfaceMacAddresses.isEmpty())
                virtualMachineInterface.setMacAddresses(new MacAddressesType(tungstenVmInterfaceMacAddresses));
            _api.create(virtualMachineInterface);
            return (VirtualMachineInterface) _api.findByFQN(VirtualMachineInterface.class, getFqnName(virtualMachineInterface));
        } catch (IOException e) {
//            s_logger.error("Unable to read " + configuration, e);
            return null;
        }
    }

    @Override
    public VirtualNetwork getVirtualNetworkFromTungsten(String virtualNetworkUuid) throws IOException {
        return (VirtualNetwork)_api.findById(VirtualNetwork.class, virtualNetworkUuid);
    }

    @Override
    public void expugeVmFromTungsten(String vmUuid) throws IOException {
        VirtualMachine virtualMachine = (VirtualMachine) _api.findById(VirtualMachine.class, vmUuid);
        if(virtualMachine == null)
            return;
        if(virtualMachine.getVirtualMachineInterfaceBackRefs() != null && virtualMachine.getVirtualMachineInterfaceBackRefs().size() > 0) {
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) _api.findById(VirtualMachineInterface.class, virtualMachine.getVirtualMachineInterfaceBackRefs().get(0).getUuid());
            _api.delete(VirtualMachineInterface.class, virtualMachineInterface.getUuid());
        }
        _api.delete(VirtualMachine.class, virtualMachine.getUuid());
    }

    public VnSubnetsType getVnSubnetsType(String ipAllocPoolStart,
                                          String ipAllocPoolEnd, String subnetIpPrefix, int subnetIpPrefixLength, String defaultGateway,
                                          boolean isDhcpEnabled, List<String> dnsNameservers, boolean isIpAddrFromStart){
        List<VnSubnetsType.IpamSubnetType.AllocationPoolType> allocationPoolTypes = new ArrayList<>();
        if(ipAllocPoolStart != null && ipAllocPoolEnd != null) {
            allocationPoolTypes.add(new VnSubnetsType.IpamSubnetType.AllocationPoolType(ipAllocPoolStart, ipAllocPoolEnd));
        }
        VnSubnetsType.IpamSubnetType ipamSubnetType = new VnSubnetsType.IpamSubnetType(
                new SubnetType(subnetIpPrefix, subnetIpPrefixLength), defaultGateway,
                null, isDhcpEnabled, dnsNameservers, allocationPoolTypes, isIpAddrFromStart, null, null, null);
        return new VnSubnetsType(Arrays.asList(ipamSubnetType), null);
    }

    public NetworkIpam getNetworkIpam(String networkName, String networkIpamUuid) throws IOException {
        if(networkIpamUuid != null){
            NetworkIpam networkIpam = (NetworkIpam) _api.findById(NetworkIpam.class, networkIpamUuid);
            if(networkIpam != null)
                return networkIpam;
        }
        NetworkIpam networkIpam = new NetworkIpam();
        networkIpam.setName(networkName + "-ipam");
        _api.create(networkIpam);
        return (NetworkIpam) _api.findByFQN(NetworkIpam.class, getFqnName(networkIpam));
    }

    public String getFqnName(ApiObjectBase obj){
        StringBuilder sb = new StringBuilder();
        for(String item : obj.getQualifiedName()){
            sb.append(item);
            sb.append(":");
        }
        sb.deleteCharAt(sb.toString().length()-1);
        return sb.toString();
    }
}
