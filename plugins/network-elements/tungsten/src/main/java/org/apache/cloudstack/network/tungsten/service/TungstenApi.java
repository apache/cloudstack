package org.apache.cloudstack.network.tungsten.service;

import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.vm.NicProfile;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.log4j.Logger;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.NetworkIpam;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import net.juniper.tungsten.api.types.VnSubnetsType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class TungstenApi {

    private static final Logger S_LOGGER = Logger.getLogger(TungstenApi.class);

    private String hostname;
    private String port;
    private String vrouter;
    private String vrouterPort;
    private String zoneId;
    private ApiConnector apiConnector;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(final String zoneId) {
        this.zoneId = zoneId;
    }

    public ApiConnector getApiConnector() {
        return apiConnector;
    }

    public void setApiConnector(ApiConnector apiConnector) {
        this.apiConnector = apiConnector;
    }

    public String getVrouter() {
        return vrouter;
    }

    public void setVrouter(String vrouter) {
        this.vrouter = vrouter;
    }

    public String getVrouterPort() {
        return vrouterPort;
    }

    public void setVrouterPort(String vrouterPort) {
        this.vrouterPort = vrouterPort;
    }

    public void checkTungstenProviderConnection() {
        try {
            URL url = new URL("http://" + hostname + ":" + port);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();

            if (huc.getResponseCode() != 200) {
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                        "There is not a tungsten provider using hostname: " + hostname + " and port: " + port);
            }
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                    "There is not a tungsten provider using hostname: " + hostname + " and port: " + port);
        }
    }

    public void checkTungstenVrouterConnection() {
        try {
            URL url = new URL("http://" + vrouter + ":" + vrouterPort);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();

            if (huc.getResponseCode() != 200) {
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                        "There is not a tungsten vrouter using hostname: " + vrouter + " and port: " + vrouterPort);
            }
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                    "There is not a tungsten vrouter using hostname: " + vrouter + " and port: " + vrouterPort);
        }
    }

    public VirtualNetwork createTungstenNetwork(long networkId, TungstenService tungstenService,
                                                NetworkDao networkDao, DataCenterDao dcDao) {
        NetworkVO network = networkDao.findById(networkId);
        try {
            Project project = tungstenService.getTungstenNetworkProject(network.getAccountId(), network.getDomainId());

            // we use default network ipam
            NetworkIpam networkIpam = tungstenService.getDefaultProjectNetworkIpam(project);

            // create tungsten subnet
            DataCenter dataCenter = dcDao.findById(network.getDataCenterId());

            String cidr = network.getCidr();
            if (cidr == null) {
                cidr = dataCenter.getGuestNetworkCidr();
            }
            String[] addr_pair = cidr.split("\\/");
            String gateway = network.getGateway();
            boolean isDhcpEnable = network.getMode().equals(Networks.Mode.Dhcp);

            VnSubnetsType subnet = tungstenService.getVnSubnetsType(null, null, addr_pair[0],
                    Integer.parseInt(addr_pair[1]), gateway, isDhcpEnable, null, true);
            return tungstenService.createTungstenVirtualNetwork(network, project, networkIpam, subnet);
        } catch (IOException e) {
            return null;
        }
    }

    public VirtualMachine createTungstenVirtualMachine(String vmUuid, String vmName, TungstenService tungstenService) {
        VirtualMachine virtualMachine = tungstenService.createVmInTungsten(vmUuid, vmName);
        if (virtualMachine == null)
            S_LOGGER.error("Failed creating virtual machine in tungsten");
        return virtualMachine;
    }

    public VirtualMachineInterface createTungstenVmInterface(NicProfile nic, String virtualNetworkUuid, String virtualMachineUuid,
                                                             String projectUuid, TungstenService tungstenService) {
        VirtualNetwork virtualNetwork = null;
        VirtualMachine virtualMachine = null;
        Project project = null;

        try {
            virtualNetwork = (VirtualNetwork) tungstenService.getObject(VirtualNetwork.class, virtualNetworkUuid);
            virtualMachine = (VirtualMachine) tungstenService.getObject(VirtualMachine.class, virtualMachineUuid);
            project = (Project) tungstenService.getObject(Project.class, projectUuid);
        } catch (IOException e) {
            S_LOGGER.error("Failed getting the resources needed for virtual machine interface creation from tungsten");
        }

        VirtualMachineInterface vmi = tungstenService.createVmInterfaceInTungsten(nic, virtualNetwork, virtualMachine, project);

        if (vmi == null)
            S_LOGGER.error("Failed creating virtual machine interface in tungsten");
        return vmi;
    }

    public InstanceIp createTungstenInstanceIp(NicProfile nic, String virtualNetworkUuid, String vmInterfaceUuid,
                                               TungstenService tungstenService) {
        VirtualNetwork virtualNetwork = null;
        VirtualMachineInterface virtualMachineInterface = null;

        try {
            virtualNetwork = (VirtualNetwork) tungstenService.getObject(VirtualNetwork.class, virtualNetworkUuid);
            virtualMachineInterface = (VirtualMachineInterface) tungstenService.getObject(VirtualMachineInterface.class, vmInterfaceUuid);
        } catch (IOException e) {
            S_LOGGER.error("Failed getting the resources needed for virtual machine interface creation from tungsten");
        }

        InstanceIp instanceIp = tungstenService.createInstanceIpInTungsten(virtualNetwork, virtualMachineInterface, nic);

        if (instanceIp == null) {
            S_LOGGER.error("Failed creating instance ip in tungsten");
        }
        return instanceIp;
    }

    public boolean deleteTungstenNetwork(String networkUuid, TungstenService tungstenService) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenService.getObject(VirtualNetwork.class, networkUuid);
            if (virtualNetwork != null) {
                tungstenService.deleteNetworkFromTungsten(networkUuid);
            }
            return true;
        } catch (IOException e) {
            S_LOGGER.error("Failed deleting the network from tungsten");
            return false;
        }
    }

    public boolean deleteTungstenVmInterface(String vmInterfaceUuid, TungstenService tungstenService) {
        try {
            VirtualMachineInterface vmi = (VirtualMachineInterface) tungstenService.getObject(
                    VirtualMachineInterface.class, vmInterfaceUuid);
            if (vmi != null) {
                List<ObjectReference<ApiPropertyBase>> instanceIpORs = vmi.getInstanceIpBackRefs();
                for (ObjectReference<ApiPropertyBase> instanceIpOR : instanceIpORs) {
                    tungstenService.deleteObject(InstanceIp.class, instanceIpOR.getUuid());
                }
                tungstenService.deleteObject(VirtualMachineInterface.class, vmi.getUuid());
            }
            return true;
        } catch (IOException e) {
            S_LOGGER.error("Failed deleting the network from tungsten");
            return false;
        }
    }

    public boolean deleteTungstenVm(String virtualMachineUuid, TungstenService tungstenService) {
        try {
            VirtualMachine virtualMachine = (VirtualMachine) tungstenService.getObject(VirtualMachine.class, virtualMachineUuid);
            if (virtualMachine != null) {
                tungstenService.deleteObject(VirtualMachine.class, virtualMachineUuid);
            }
            return true;
        } catch (IOException e) {
            S_LOGGER.error("Failed deleting the virtual machine from tungsten");
            return false;
        }
    }

}
