package com.cloud.agent;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.manager.AgentAttache;
import com.cloud.agent.manager.Commands;
import com.cloud.api.commands.UpdateHostPasswordCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.PodCluster;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ServerResource;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.User;
import com.cloud.utils.Pair;

@Local(value = { AgentManager.class })
public class MockAgentManagerImpl implements AgentManager {

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer easySend(Long hostId, Command cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer send(Long hostId, Command cmd) throws AgentUnavailableException, OperationTimedoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer[] send(Long hostId, Commands cmds) throws AgentUnavailableException, OperationTimedoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer[] send(Long hostId, Commands cmds, int timeout) throws AgentUnavailableException, OperationTimedoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long gatherStats(Long hostId, Command cmd, Listener listener) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long send(Long hostId, Commands cmds, Listener listener) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int registerForHostEvents(Listener listener, boolean connections, boolean commands, boolean priority) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int registerForInitialConnects(StartupCommandProcessor creator, boolean priority) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void unregisterForHostEvents(int id) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Set<Long> getConnectedHosts() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void disconnect(long hostId, Event event, boolean investigate) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public HostStats getHostStatistics(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getGuestOSCategoryId(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getHostTags(long hostId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PodCluster> listByDataCenter(long dcId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PodCluster> listByPod(long podId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Host addHost(long zoneId, ServerResource resource, Type hostType, Map<String, String> hostDetails) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteHost(long hostId, boolean isForced, boolean forceDestroy, User caller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Pair<HostPodVO, Long> findPod(VirtualMachineTemplate template, ServiceOfferingVO offering, DataCenterVO dc, long userId, Set<Long> avoids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean maintain(long hostId) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean maintenanceFailed(long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean cancelMaintenance(long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean executeUserRequest(long hostId, Event event) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean reconnect(long hostId) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isHostNativeHAEnabled(long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Answer sendTo(Long dcId, HypervisorType type, Command cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void notifyAnswersToMonitors(long agentId, long seq, Answer[] answers) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public AgentAttache simulateStart(Long id, ServerResource resource, Map<String, String> details, boolean old, List<String> hostTags, String allocationState, boolean forRebalance)
            throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean updateHostPassword(UpdateHostPasswordCmd upasscmd) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long sendToSecStorage(HostVO ssHost, Command cmd, Listener listener) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Answer sendToSecStorage(HostVO ssHost, Command cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO getSSAgent(HostVO ssHost) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateStatus(HostVO host, Event event) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean disconnect(long hostId) {
        // TODO Auto-generated method stub
        return false;
    }

}
