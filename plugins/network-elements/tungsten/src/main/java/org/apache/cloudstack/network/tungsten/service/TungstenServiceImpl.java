package org.apache.cloudstack.network.tungsten.service;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.Network;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.TungstenUtils;
import com.cloud.vm.NicProfile;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorFactory;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.ApiPropertyBase;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.Domain;
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
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnectorFactory;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class TungstenServiceImpl implements TungstenService {

    private static final Logger s_logger = Logger.getLogger(TungstenServiceImpl.class);

    private ApiConnector _api;
    private VRouterApiConnector _vrouterApi;

    @Inject
    ProjectDao _projectDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    TungstenProviderService _tungstenProviderService;


    public ApiConnector get_api() {
        return _api;
    }

    public VRouterApiConnector get_vrouterApi() {
        return _vrouterApi;
    }

    @Override
    public void init() {
        TungstenProviderVO tungstenProvider = _tungstenProviderService.getTungstenProvider();
        if (tungstenProvider != null) {
            String hostname = tungstenProvider.getHostname();
            int port = Integer.parseInt(tungstenProvider.getPort());
            String vrouter = tungstenProvider.getVrouter();
            String vrouterPort = tungstenProvider.getVrouterPort();
            _api = ApiConnectorFactory.build(hostname, port);
            _vrouterApi = VRouterApiConnectorFactory.getInstance(vrouter, vrouterPort);
        } else {
            _tungstenProviderService.disableTungstenNsp();
        }
    }

    @Override
    public VirtualNetwork createNetworkInTungsten(String networkUuid, String networkName, String projectUuid,
                                                  String networkIpamUuid, String ipAllocPoolStart, String ipAllocPoolEnd, String subnetIpPrefix,
                                                  int subnetIpPrefixLength, String defaultGateway, boolean isDhcpEnabled, List<String> dnsNameservers,
                                                  boolean isIpAddrFromStart) {
        VirtualNetwork network = new VirtualNetwork();
        try {
            if (projectUuid != null) {
                Project project = (Project) _api.findById(Project.class, projectUuid);
                network.setParent(project);
            }
            network.setUuid(networkUuid);
            network.setName(networkName);
            network.setNetworkIpam(getNetworkIpam(networkName, networkIpamUuid),
                    getVnSubnetsType(ipAllocPoolStart, ipAllocPoolEnd, subnetIpPrefix, subnetIpPrefixLength, defaultGateway,
                            isDhcpEnabled, dnsNameservers, isIpAddrFromStart));
            _api.create(network);
            return (VirtualNetwork) _api.findByFQN(VirtualNetwork.class, getFqnName(network));
        } catch (IOException e) {
            s_logger.error("Unable to create tungsten network " + networkUuid, e);
            return null;
        }
    }

    @Override
    public VirtualNetwork deleteNetworkFromTungsten(String tungstenNetworkUuid) throws IOException {
        VirtualNetwork network = (VirtualNetwork) _api.findById(VirtualNetwork.class, tungstenNetworkUuid);
        if (network != null) {
            if (network.getVirtualMachineInterfaceBackRefs() != null)
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete network");
            _api.delete(network);
        }
        return network;
    }

    @Override
    public VirtualMachine createVmInTungsten(String vmUuid, String vmName) {
        VirtualMachine virtualMachine = new VirtualMachine();
        try {
            virtualMachine.setName(vmName);
            virtualMachine.setUuid(vmUuid);
            _api.create(virtualMachine);
            return (VirtualMachine) _api.findByFQN(VirtualMachine.class, getFqnName(virtualMachine));
        } catch (IOException e) {
            s_logger.error("Unable to create tungsten vm " + vmUuid, e);
            return null;
        }
    }

    @Override
    public InstanceIp createInstanceIpInTungsten(String instanceIpName, String tungstenVmInterfaceUuid,
                                                 String tungstenNetworkUuid, String tungstenInstanceIpAddress) {
        InstanceIp instanceIp = new InstanceIp();
        try {
            instanceIp.setName(instanceIpName);
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) _api.findById(
                    VirtualMachineInterface.class, tungstenVmInterfaceUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) _api.findById(VirtualNetwork.class, tungstenNetworkUuid);
            if (virtualNetwork != null)
                instanceIp.setVirtualNetwork(virtualNetwork);
            if (virtualMachineInterface != null)
                instanceIp.setVirtualMachineInterface(virtualMachineInterface);
            instanceIp.setAddress(tungstenInstanceIpAddress);
            _api.create(instanceIp);
            return (InstanceIp) _api.findByFQN(InstanceIp.class, getFqnName(instanceIp));
        } catch (IOException e) {
            s_logger.error("Unable to create tungsten instance ip ", e);
            return null;
        }
    }

    @Override
    public VirtualMachineInterface createVmInterfaceInTungsten(String vmiUuid, String vmInterfaceName,
                                                               String tungstenProjectUuid, String tungstenNetworkUuid, String tungstenVmUuid, String tungstenSecurityGroupUuid,
                                                               List<String> tungstenVmInterfaceMacAddresses) {
        VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
        try {
            virtualMachineInterface.setUuid(vmiUuid);
            virtualMachineInterface.setName(vmInterfaceName);
            Project project = (Project) _api.findById(Project.class, tungstenProjectUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) _api.findById(VirtualNetwork.class, tungstenNetworkUuid);
            VirtualMachine virtualMachine = (VirtualMachine) _api.findById(VirtualMachine.class, tungstenVmUuid);
            SecurityGroup securityGroup = (SecurityGroup) _api.findById(SecurityGroup.class, tungstenSecurityGroupUuid);
            if (virtualNetwork != null)
                virtualMachineInterface.setVirtualNetwork(virtualNetwork);
            if (virtualMachine != null)
                virtualMachineInterface.setVirtualMachine(virtualMachine);
            if (securityGroup != null)
                virtualMachineInterface.setSecurityGroup(securityGroup);
            if (project != null)
                virtualMachineInterface.setParent(project);
            if (tungstenVmInterfaceMacAddresses != null && !tungstenVmInterfaceMacAddresses.isEmpty())
                virtualMachineInterface.setMacAddresses(new MacAddressesType(tungstenVmInterfaceMacAddresses));
            _api.create(virtualMachineInterface);
            return (VirtualMachineInterface) _api.findByFQN(VirtualMachineInterface.class,
                    getFqnName(virtualMachineInterface));
        } catch (IOException e) {
            //            s_logger.error("Unable to read " + configuration, e);
            return null;
        }
    }

    @Override
    public VirtualNetwork getVirtualNetworkFromTungsten(String virtualNetworkUuid) throws IOException {
        return (VirtualNetwork) _api.findById(VirtualNetwork.class, virtualNetworkUuid);
    }

    @Override
    public void expugeVmFromTungsten(String vmUuid) throws IOException {
        VirtualMachine virtualMachine = (VirtualMachine) _api.findById(VirtualMachine.class, vmUuid);
        if (virtualMachine == null)
            return;
        if (virtualMachine.getVirtualMachineInterfaceBackRefs() != null
                && !virtualMachine.getVirtualMachineInterfaceBackRefs().isEmpty()) {
            removeVirtualMachineInterfaceBackRefs(virtualMachine.getVirtualMachineInterfaceBackRefs());
        }
        _api.delete(VirtualMachine.class, virtualMachine.getUuid());
    }

    public void removeVirtualMachineInterfaceBackRefs(
            List<ObjectReference<ApiPropertyBase>> virtualMachineInterfaceBackRefs) throws IOException {
        for (ObjectReference<ApiPropertyBase> item : virtualMachineInterfaceBackRefs) {
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) _api.findById(
                    VirtualMachineInterface.class, item.getUuid());
            if (virtualMachineInterface.getInstanceIpBackRefs() != null
                    && !virtualMachineInterface.getInstanceIpBackRefs().isEmpty()) {
                for (ObjectReference<ApiPropertyBase> instanceIp : virtualMachineInterface.getInstanceIpBackRefs()) {
                    _api.delete(InstanceIp.class, instanceIp.getUuid());
                }
            }
            _api.delete(VirtualMachineInterface.class, virtualMachineInterface.getUuid());
        }
    }

    @Override
    public VnSubnetsType getVnSubnetsType(String ipAllocPoolStart, String ipAllocPoolEnd, String subnetIpPrefix,
                                          int subnetIpPrefixLength, String defaultGateway, boolean isDhcpEnabled, List<String> dnsNameservers,
                                          boolean isIpAddrFromStart) {
        List<VnSubnetsType.IpamSubnetType.AllocationPoolType> allocationPoolTypes = new ArrayList<>();
        if (ipAllocPoolStart != null && ipAllocPoolEnd != null) {
            allocationPoolTypes.add(
                    new VnSubnetsType.IpamSubnetType.AllocationPoolType(ipAllocPoolStart, ipAllocPoolEnd));
        }
        VnSubnetsType.IpamSubnetType ipamSubnetType = new VnSubnetsType.IpamSubnetType(
                new SubnetType(subnetIpPrefix, subnetIpPrefixLength), defaultGateway, null, isDhcpEnabled, dnsNameservers,
                allocationPoolTypes, isIpAddrFromStart, null, null, null);
        return new VnSubnetsType(Arrays.asList(ipamSubnetType), null);
    }

    public NetworkIpam getNetworkIpam(String networkName, String networkIpamUuid) throws IOException {
        if (networkIpamUuid != null) {
            NetworkIpam networkIpam = (NetworkIpam) _api.findById(NetworkIpam.class, networkIpamUuid);
            if (networkIpam != null)
                return networkIpam;
        }
        NetworkIpam networkIpam = new NetworkIpam();
        networkIpam.setName(networkName + "-ipam");
        _api.create(networkIpam);
        return (NetworkIpam) _api.findByFQN(NetworkIpam.class, getFqnName(networkIpam));
    }

    @Override
    public void addTungstenVrouterPort(Port port) throws IOException {
        port.setTapInterfaceName(TungstenUtils.getTapName(port.getMacAddress()));
        _vrouterApi.addPort(port);
    }

    @Override
    public void deleteTungstenVrouterPort(String portUuid) throws IOException {
        _vrouterApi.deletePort(portUuid);
    }

    /**
     * Get the tungsten project that match the project from cloudstack
     * <p>
     * @param accountId
     * @param domainId
     * @return
     * @throws IOException
     */
    @Override
    public Project getTungstenNetworkProject(long accountId, long domainId) throws IOException {
        ProjectVO cloudstackProject = getProject(accountId);
        DomainVO cloudstackDomain = getDomain(domainId);
        Domain tungstenDomain;
        Project tungstenProject;
        //get the tungsten domain
        if (cloudstackDomain != null && cloudstackDomain.getId() != com.cloud.domain.Domain.ROOT_DOMAIN) {
            tungstenDomain = (Domain) _api.findById(Domain.class, cloudstackDomain.getUuid());
            if (tungstenDomain == null) {
                tungstenDomain = createDomainInTungsten(cloudstackDomain.getName(), cloudstackDomain.getUuid());
            }
        } else {
            tungstenDomain = getDefaultTungstenDomain();
        }
        //get the tungsten project
        if (cloudstackProject != null) {
            tungstenProject = (Project) _api.findById(Project.class, cloudstackProject.getUuid());
            if (tungstenProject == null) {
                tungstenProject = createProjectInTungsten(cloudstackProject.getName(), cloudstackProject.getUuid(),
                        tungstenDomain);
            }
        } else {
            tungstenProject = getDefaultTungstenProject();
        }
        return tungstenProject;
    }

    /**
     * Create a project in tungsten that match the project from cloudstack
     *
     * @param projectName
     * @param projectUuid
     * @param tungstenDomain
     * @return the project created in tungsten
     * @throws IOException
     */
    public Project createProjectInTungsten(String projectName, String projectUuid, Domain tungstenDomain)
            throws IOException {
        Project tungstenProject = new Project();
        tungstenProject.setDisplayName(projectName);
        tungstenProject.setName(projectName);
        tungstenProject.setUuid(projectUuid);
        tungstenProject.setParent(tungstenDomain);
        _api.create(tungstenProject);
        return tungstenProject;
    }

    /**
     * Create a domain in tungsten that match the domain from cloudstack
     *
     * @param domainName
     * @param domainUuid
     * @return the domain created in tungsten
     * @throws IOException
     */
    public Domain createDomainInTungsten(String domainName, String domainUuid) throws IOException {
        Domain tungstenDomain = new Domain();
        tungstenDomain.setDisplayName(domainName);
        tungstenDomain.setName(domainName);
        tungstenDomain.setUuid(domainUuid);
        _api.create(tungstenDomain);
        return tungstenDomain;
    }

    public ProjectVO getProject(long accountId) {
        Account account = _accountDao.findById(accountId);
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            return _projectDao.findByProjectAccountId(account.getId());
        }
        return null;
    }

    public DomainVO getDomain(long domainId) {
        return _domainDao.findById(domainId);
    }

    /**
     * Create a default project in tungsten for a specific domain
     *
     * @param tungstenDomain
     * @return
     * @throws IOException
     */
    public Project createDefaultProject(String tungstenDomain) throws IOException {
        Domain domain = (Domain) _api.findByFQN(Domain.class, tungstenDomain);
        Project project = new Project();
        project.setParent(domain);
        project.setName(tungstenDomain + "-default-project");
        _api.create(project);
        return (Project) _api.findByFQN(Project.class, tungstenDomain + ":" + project.getName());
    }

    /**
     * Get the tungsten domain default project
     *
     * @return
     * @throws IOException
     */
    public Project getDefaultTungstenProject() throws IOException {
        return (Project) _api.findByFQN(Project.class, TUNGSTEN_DEFAULT_DOMAIN + ":" + TUNGSTEN_DEFAULT_PROJECT);
    }

    /**
     * Get the default domain from tungsten
     *
     * @return
     * @throws IOException
     */
    public Domain getDefaultTungstenDomain() throws IOException {
        Domain domain = (Domain) _api.findByFQN(Domain.class, TUNGSTEN_DEFAULT_DOMAIN);
        return domain;
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

    @Override
    public ApiObjectBase getObject(Class<? extends ApiObjectBase> obj, String uuid) throws IOException {
        return _api.findById(obj, uuid);
    }

    @Override
    public void deleteObject(Class<? extends ApiObjectBase> obj, String uuid) throws IOException {
        _api.delete(obj, uuid);
    }

    @Override
    public NetworkIpam getDefaultProjectNetworkIpam(Project project) throws IOException {
        List<String> names = new ArrayList<>();
        Domain domain = (Domain) _api.findById(Domain.class, project.getParentUuid());
        names.add(domain.getName());
        names.add(project.getName());
        names.add(TUNGSTEN_DEFAULT_IPAM);
        String ipamUuid = _api.findByName(NetworkIpam.class, names);
        if (ipamUuid == null) {
            NetworkIpam defaultIpam = new NetworkIpam();
            defaultIpam.setName(TUNGSTEN_DEFAULT_IPAM);
            defaultIpam.setParent(project);
            _api.create(defaultIpam);
            ipamUuid = defaultIpam.getUuid();
        }
        return (NetworkIpam) _api.findById(NetworkIpam.class, ipamUuid);
    }

    @Override
    public VirtualNetwork createTungstenVirtualNetwork(Network network, Project project, NetworkIpam networkIpam,
                                                       VnSubnetsType subnet) {
        try {
            VirtualNetwork virtualNetwork = new VirtualNetwork();
            virtualNetwork.setUuid(network.getUuid());
            virtualNetwork.setName(network.getName());
            virtualNetwork.addNetworkIpam(networkIpam, subnet);
            virtualNetwork.setParent(project);
            _api.create(virtualNetwork);
            return (VirtualNetwork) _api.findById(VirtualNetwork.class, virtualNetwork.getUuid());
        } catch (IOException e) {
            s_logger.error("Unable to create tungsten network", e);
            return null;
        }
    }

    @Override
    public InstanceIp createInstanceIpInTungsten(VirtualNetwork vn, VirtualMachineInterface vmi, NicProfile nic) {
        try {
            InstanceIp instanceIp = new InstanceIp();
            instanceIp.setName(TungstenUtils.getInstanceIpName(nic.getId()));
            instanceIp.setVirtualNetwork(vn);
            instanceIp.setVirtualMachineInterface(vmi);
            instanceIp.setAddress(nic.getIPv4Address());
            _api.create(instanceIp);
            return (InstanceIp) _api.findById(InstanceIp.class, instanceIp.getUuid());
        } catch (IOException e) {
            s_logger.error("Unable to create tungsten instance ip", e);
            return null;
        }
    }

    @Override
    public VirtualMachineInterface createVmInterfaceInTungsten(NicProfile nicProfile, VirtualNetwork vn,
                                                               VirtualMachine vm, Project project) {
        VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
        try {
            virtualMachineInterface.setUuid(nicProfile.getUuid());
            virtualMachineInterface.setName(TungstenUtils.getVmiName(nicProfile.getId()));
            virtualMachineInterface.setVirtualNetwork(vn);
            virtualMachineInterface.setVirtualMachine(vm);
            virtualMachineInterface.setParent(project);
            MacAddressesType macAddressesType = new MacAddressesType();
            macAddressesType.addMacAddress(nicProfile.getMacAddress());
            virtualMachineInterface.setMacAddresses(macAddressesType);
            _api.create(virtualMachineInterface);
            return (VirtualMachineInterface) _api.findById(VirtualMachineInterface.class,
                    virtualMachineInterface.getUuid());
        } catch (IOException e) {
            s_logger.error("Unable to create tungsten virtual machine interface", e);
            return null;
        }
    }
}
