package org.apache.cloudstack.network.tungsten.service;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ManagerBase;
import com.google.common.collect.Lists;
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
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.network.tungsten.api.command.AddVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenInstanceIpCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenVirtualMachineCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenVmInterfaceCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenVirtualMachineCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenVmInterfaceCmd;
import org.apache.cloudstack.network.tungsten.api.response.TungstenNetworkResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualMachineResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVmInterfaceResponse;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnectorFactory;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;

@Component
public class TungstenManagerImpl extends ManagerBase implements TungstenManager, Configurable {

  private static final Logger s_logger = Logger.getLogger(TungstenManager.class);

  private ApiConnector _api;
  private VRouterApiConnector _vrouterApi;
  private final String configuration = "plugins/network-elements/tungsten/conf/tungsten.properties";

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
      _api = ApiConnectorFactory.build(hostname, port);
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
  public List<Class<?>> getCommands() {
    return Lists.<Class<?>>newArrayList(ListTungstenNetworkCmd.class,
        CreateTungstenNetworkCmd.class, DeleteTungstenNetworkCmd.class,
        CreateTungstenVmInterfaceCmd.class, CreateTungstenVirtualMachineCmd.class,
        CreateTungstenInstanceIpCmd.class, ListTungstenVirtualMachineCmd.class,
        ListTungstenVmInterfaceCmd.class, AddVRouterPortCmd.class, DeleteVRouterPortCmd.class);
  }

  @Override
  public ListResponse<TungstenNetworkResponse> getNetworks(ListTungstenNetworkCmd cmd)
      throws IOException {
    List<VirtualNetwork> networks;
    ListResponse<TungstenNetworkResponse> response = new ListResponse<>();
    List<TungstenNetworkResponse> tungstenNetworkResponses = new ArrayList<>();

    if (cmd.getNetworkUUID() != null)
      networks = Arrays.asList(
          (VirtualNetwork) _api.findById(VirtualNetwork.class, cmd.getNetworkUUID()));
    else
      networks = (List<VirtualNetwork>) _api.list(VirtualNetwork.class, null);

    if (networks != null && !networks.isEmpty()) {
      for (VirtualNetwork virtualNetwork : networks) {
        TungstenNetworkResponse tungstenNetworkResponse =
            TungstenResponseHelper.createTungstenNetworkResponse(
            virtualNetwork);
        tungstenNetworkResponses.add(tungstenNetworkResponse);
      }
    }
    response.setResponses(tungstenNetworkResponses);
    return response;
  }

  @Override
  public ListResponse<TungstenVmInterfaceResponse> getVmInterfaces(ListTungstenVmInterfaceCmd cmd)
      throws IOException {
    List<VirtualMachineInterface> vmInterfaces;
    ListResponse<TungstenVmInterfaceResponse> response = new ListResponse<>();
    List<TungstenVmInterfaceResponse> tungstenVmInterfaceResponses = new ArrayList<>();

    if (cmd.getVmInterfaceUUID() != null)
      vmInterfaces = Arrays.asList(
          (VirtualMachineInterface) getTungstenObjectByUUID(VirtualMachineInterface.class,
              cmd.getVmInterfaceUUID()));
    else
      vmInterfaces = (List<VirtualMachineInterface>) _api.list(VirtualMachineInterface.class, null);

    if (vmInterfaces != null && !vmInterfaces.isEmpty()) {
      for (VirtualMachineInterface vmInterface : vmInterfaces) {
        TungstenVmInterfaceResponse tungstenVmInterfaceResponse =
            TungstenResponseHelper.createTungstenVmInterfaceResponse(
            vmInterface);
        tungstenVmInterfaceResponses.add(tungstenVmInterfaceResponse);
      }
    }
    response.setResponses(tungstenVmInterfaceResponses);
    return response;
  }

  public ListResponse<TungstenVirtualMachineResponse> getVirtualMachines(
      ListTungstenVirtualMachineCmd cmd) throws IOException {
    List<VirtualMachine> virtualMachines;
    ListResponse<TungstenVirtualMachineResponse> response = new ListResponse<>();
    List<TungstenVirtualMachineResponse> tungstenVirtualMachineResponses = new ArrayList<>();

    if (cmd.getVirtualMachineUUID() != null)
      virtualMachines = Arrays.asList(
          (VirtualMachine) _api.findById(VirtualMachine.class, cmd.getVirtualMachineUUID()));
    else
      virtualMachines = (List<VirtualMachine>) _api.list(VirtualMachine.class, null);

    if (virtualMachines != null && !virtualMachines.isEmpty()) {
      for (VirtualMachine virtualMachine : virtualMachines) {
        TungstenVirtualMachineResponse tungstenVirtualMachineResponse =
            TungstenResponseHelper.createTungstenVirtualMachineResponse(
            virtualMachine);
        tungstenVirtualMachineResponses.add(tungstenVirtualMachineResponse);
      }
    }
    response.setResponses(tungstenVirtualMachineResponses);
    return response;
  }

  @Override
  public VirtualNetwork createTungstenNetwork(CreateTungstenNetworkCmd cmd) {
    VirtualNetwork network = new VirtualNetwork();
    try {
      network.setName(cmd.getName());
      network.setNetworkIpam(getNetworkIpam(cmd), getVnSubnetsType(cmd));
      _api.create(network);
      return (VirtualNetwork) _api.findByFQN(VirtualNetwork.class, getFqnName(network));
    } catch (IOException e) {
      s_logger.error("Unable to read " + configuration, e);
      return null;
    }
  }

  @Override
  public VirtualMachine createTungstenVirtualMachine(CreateTungstenVirtualMachineCmd cmd) {
    VirtualMachine virtualMachine = new VirtualMachine();
    try {
      virtualMachine.setName(cmd.getName());
      _api.create(virtualMachine);
      return (VirtualMachine) _api.findByFQN(VirtualMachine.class, getFqnName(virtualMachine));
    } catch (IOException e) {
      s_logger.error("Unable to read " + configuration, e);
      return null;
    }
  }

  @Override
  public InstanceIp createInstanceIp(CreateTungstenInstanceIpCmd cmd) {
    InstanceIp instanceIp = new InstanceIp();
    try {
      instanceIp.setName(cmd.getName());
      VirtualMachineInterface virtualMachineInterface =
          (VirtualMachineInterface) getTungstenObjectByUUID(
          VirtualMachineInterface.class, cmd.getTungstenVmInterfaceUuid());
      VirtualNetwork virtualNetwork = (VirtualNetwork) getTungstenObjectByUUID(VirtualNetwork.class,
          cmd.getTungstenNetworkUuid());
      if (virtualNetwork != null)
        instanceIp.setVirtualNetwork(virtualNetwork);
      if (virtualMachineInterface != null)
        instanceIp.setVirtualMachineInterface(virtualMachineInterface);
      instanceIp.setAddress(cmd.getTungstenInstanceIpAddress());
      _api.create(instanceIp);
      return (InstanceIp) _api.findByFQN(InstanceIp.class, getFqnName(instanceIp));
    } catch (IOException e) {
      s_logger.error("Unable to read " + configuration, e);
      return null;
    }
  }

  @Override
  public VirtualMachineInterface createTungstenVirtualMachineInterface(
      CreateTungstenVmInterfaceCmd cmd) {
    VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
    try {
      virtualMachineInterface.setName(cmd.getName());
      Project project = (Project) getTungstenObjectByUUID(Project.class,
          cmd.getTungstenProjectUuid());
      VirtualNetwork virtualNetwork = (VirtualNetwork) getTungstenObjectByUUID(VirtualNetwork.class,
          cmd.getTungstenNetworkUuid());
      VirtualMachine virtualMachine = (VirtualMachine) getTungstenObjectByUUID(VirtualMachine.class,
          cmd.getTungstenVirtualMachineUuid());
      SecurityGroup securityGroup = (SecurityGroup) getTungstenObjectByUUID(SecurityGroup.class,
          cmd.getTungstenSecurityGroupUuid());
      if (virtualNetwork != null)
        virtualMachineInterface.setVirtualNetwork(virtualNetwork);
      if (virtualMachine != null)
        virtualMachineInterface.setVirtualMachine(virtualMachine);
      if (securityGroup != null)
        virtualMachineInterface.setSecurityGroup(securityGroup);
      if (project != null)
        virtualMachineInterface.setParent(project);
      if (cmd.getTungstenVmInterfaceMacAddresses() != null
          && !cmd.getTungstenVmInterfaceMacAddresses().isEmpty())
        virtualMachineInterface.setMacAddresses(
            new MacAddressesType(cmd.getTungstenVmInterfaceMacAddresses()));
      _api.create(virtualMachineInterface);
      return (VirtualMachineInterface) _api.findByFQN(VirtualMachineInterface.class,
          getFqnName(virtualMachineInterface));
    } catch (IOException e) {
      s_logger.error("Unable to read " + configuration, e);
      return null;
    }
  }

  @Override
  public ApiObjectBase getTungstenObjectByUUID(Class<? extends ApiObjectBase> cls, String uuid)
      throws IOException {
    if (uuid != null)
      return _api.findById(cls, uuid);
    else
      return null;
  }

  public NetworkIpam getNetworkIpam(CreateTungstenNetworkCmd cmd) throws IOException {
    if (cmd.getNetworkIpamUUID() != null) {
      NetworkIpam networkIpam = (NetworkIpam) _api.findById(NetworkIpam.class,
          cmd.getNetworkIpamUUID());
      if (networkIpam != null)
        return networkIpam;
    }
    NetworkIpam networkIpam = new NetworkIpam();
    networkIpam.setName(cmd.getName() + "-ipam");
    _api.create(networkIpam);
    return (NetworkIpam) _api.findByFQN(NetworkIpam.class, getFqnName(networkIpam));
  }

  public VnSubnetsType getVnSubnetsType(CreateTungstenNetworkCmd cmd) {
    List<VnSubnetsType.IpamSubnetType.AllocationPoolType> allocationPoolTypes = new ArrayList<>();
    allocationPoolTypes.add(
        new VnSubnetsType.IpamSubnetType.AllocationPoolType(cmd.getIpAllocPoolStart(),
            cmd.getIpAllocPoolEnd()));
    VnSubnetsType.IpamSubnetType ipamSubnetType = new VnSubnetsType.IpamSubnetType(
        new SubnetType(cmd.getSubnetIpPrefix(), cmd.getSubnetIpPrefixLength()),
        cmd.getDefaultGateway(), null, cmd.isEnableDHCP(), cmd.getDnsNameservers(),
        allocationPoolTypes, cmd.isAddrFromStart(), null, null, null);
    return new VnSubnetsType(Arrays.asList(ipamSubnetType), null);
  }

  @Override
  public VirtualNetwork deleteTungstenNetwork(DeleteTungstenNetworkCmd cmd) throws IOException {
    VirtualNetwork network = (VirtualNetwork) _api.findById(VirtualNetwork.class, cmd.getUuid());
    if (network != null) {
      _api.delete(network);
      return network;
    } else
      throw new InvalidParameterValueException(
          "Unable to find tungsten network with UUID: " + cmd.getUuid());
  }

  @Override
  public SuccessResponse addVRouterPort(final AddVRouterPortCmd cmd) throws IOException {
    Port port = new Port();
    port.setId(cmd.getTungstenVmInterfaceUuid());
    port.setInstanceId(cmd.getTungstenVirtualMachineUuid());
    port.setIpAddress(cmd.getTungstenInstanceIpAddress());
    port.setMacAddress(cmd.getTungstenVmInterfaceMacAddress());
    port.setTapInterfaceName(getTapName(cmd.getTungstenVmInterfaceMacAddress()));
    port.setVmProjectId(String.valueOf(cmd.getProjectId()));
    port.setDisplayName(cmd.getTungstenVmName());
    port.setVnId(cmd.getTungstenVnUuid());
    SuccessResponse successResponse = new SuccessResponse(cmd.getCommandName());
    if (_vrouterApi.addPort(port)) {
      successResponse.setSuccess(true);
      successResponse.setDisplayText("Success to add vrouter port");
    } else {
      successResponse.setSuccess(false);
      successResponse.setDisplayText("Fail to add vrouter port");
    }
    return successResponse;
  }

  @Override
  public SuccessResponse deleteVRouterPort(final DeleteVRouterPortCmd cmd) throws IOException {
    String portId = cmd.getTungstenVmInterfaceUuid();
    SuccessResponse successResponse = new SuccessResponse(cmd.getCommandName());
    if (_vrouterApi.deletePort(portId)) {
      successResponse.setSuccess(true);
      successResponse.setDisplayText("Success to delete vrouter port");
    } else {
      successResponse.setSuccess(false);
      successResponse.setDisplayText("Fail to delete vrouter port");
    }
    return successResponse;
  }

  @Override
  public String getConfigComponentName() {
    return TungstenManager.class.getSimpleName();
  }

  @Override
  public ConfigKey<?>[] getConfigKeys() {
    return new ConfigKey[0];
  }

  public String getFqnName(ApiObjectBase obj) {
    StringBuilder sb = new StringBuilder();
    for (String item : obj.getQualifiedName()) {
      sb.append(item);
      sb.append(":");
    }
    sb.deleteCharAt(sb.toString().length() - 1);
    return sb.toString();
  }

  private String getTapName(final String macAddress) {
    return "tap" + macAddress.replace(":", "");
  }
}
