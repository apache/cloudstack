// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.ovs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.persistence.EntityExistsException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.OvsCreateTunnelAnswer;
import com.cloud.agent.api.OvsCreateTunnelCommand;
import com.cloud.agent.api.OvsDestroyBridgeCommand;
import com.cloud.agent.api.OvsDestroyTunnelCommand;
import com.cloud.agent.api.OvsFetchInterfaceAnswer;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.agent.api.OvsSetupBridgeCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.ovs.dao.OvsTunnelInterfaceDao;
import com.cloud.network.ovs.dao.OvsTunnelInterfaceVO;
import com.cloud.network.ovs.dao.OvsTunnelNetworkDao;
import com.cloud.network.ovs.dao.OvsTunnelNetworkVO;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

@Component
@Local(value = {OvsTunnelManager.class})
public class OvsTunnelManagerImpl extends ManagerBase implements OvsTunnelManager {
    public static final Logger s_logger = Logger.getLogger(OvsTunnelManagerImpl.class.getName());

	// boolean _isEnabled;
	ScheduledExecutorService _executorPool;
	ScheduledExecutorService _cleanupExecutor;

	@Inject
	ConfigurationDao _configDao;
	@Inject
	NicDao _nicDao;
	@Inject
	HostDao _hostDao;
	@Inject
	PhysicalNetworkTrafficTypeDao _physNetTTDao;
	@Inject
	UserVmDao _userVmDao;
	@Inject
	DomainRouterDao _routerDao;
	@Inject
	OvsTunnelNetworkDao _tunnelNetworkDao;
	@Inject
	OvsTunnelInterfaceDao _tunnelInterfaceDao;
	@Inject
	AgentManager _agentMgr;

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
        _executorPool = Executors.newScheduledThreadPool(10, new NamedThreadFactory("OVS"));
        _cleanupExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("OVS-Cleanup"));

        return true;
    }

	@DB
	protected OvsTunnelInterfaceVO createInterfaceRecord(String ip,
			String netmask, String mac, long hostId, String label) {
		OvsTunnelInterfaceVO ti = null;
		try {
			ti = new OvsTunnelInterfaceVO(ip, netmask, mac, hostId, label);
			// TODO: Is locking really necessary here?
			OvsTunnelInterfaceVO lock = _tunnelInterfaceDao
					.acquireInLockTable(Long.valueOf(1));
			if (lock == null) {
				s_logger.warn("Cannot lock table ovs_tunnel_account");
				return null;
			}
			_tunnelInterfaceDao.persist(ti);
			_tunnelInterfaceDao.releaseFromLockTable(lock.getId());
		} catch (EntityExistsException e) {
			s_logger.debug("A record for the interface for network " + label
					+ " on host id " + hostId + " already exists");
		}
		return ti;
	}

	private String handleFetchInterfaceAnswer(Answer[] answers, Long hostId) {
		OvsFetchInterfaceAnswer ans = (OvsFetchInterfaceAnswer) answers[0];
		if (ans.getResult()) {
			if (ans.getIp() != null && !("".equals(ans.getIp()))) {
				OvsTunnelInterfaceVO ti = createInterfaceRecord(ans.getIp(),
						ans.getNetmask(), ans.getMac(), hostId, ans.getLabel());
				return ti.getIp();
			}
		}
		// Fetch interface failed!
		s_logger.warn("Unable to fetch the IP address for the GRE tunnel endpoint"
				+ ans.getDetails());
		return null;
	}

    @DB
    protected OvsTunnelNetworkVO createTunnelRecord(long from, long to, long networkId, int key) {
        OvsTunnelNetworkVO ta = null;
        try {
            ta = new OvsTunnelNetworkVO(from, to, key, networkId);
            OvsTunnelNetworkVO lock = _tunnelNetworkDao.acquireInLockTable(Long.valueOf(1));
            if (lock == null) {
                s_logger.warn("Cannot lock table ovs_tunnel_account");
                return null;
            }
            _tunnelNetworkDao.persist(ta);
            _tunnelNetworkDao.releaseFromLockTable(lock.getId());
        } catch (EntityExistsException e) {
            s_logger.debug("A record for the tunnel from " + from + " to " + to + " already exists");
        }
        return ta;
    }


    private void handleCreateTunnelAnswer(Answer[] answers) {
        OvsCreateTunnelAnswer r = (OvsCreateTunnelAnswer)answers[0];
        String s =
            String.format("(hostIP:%1$s, remoteIP:%2$s, bridge:%3$s," + "greKey:%4$s, portName:%5$s)", r.getFromIp(), r.getToIp(), r.getBridge(), r.getKey(),
                r.getInPortName());
        Long from = r.getFrom();
        Long to = r.getTo();
        long networkId = r.getNetworkId();
        OvsTunnelNetworkVO tunnel = _tunnelNetworkDao.getByFromToNetwork(from, to, networkId);
        if (tunnel == null) {
            throw new CloudRuntimeException(
            		String.format("Unable find tunnelNetwork record" +
            					  "(from=%1$s,to=%2$s, account=%3$s",
            					  from, to, networkId));
		}
		if (!r.getResult()) {
			tunnel.setState("FAILED");
			s_logger.warn("Create GRE tunnel failed due to " + r.getDetails()
					+ s);
		} else {
			tunnel.setState("SUCCESS");
			tunnel.setPortName(r.getInPortName());
			s_logger.warn("Create GRE tunnel " + r.getDetails() + s);
		}
		_tunnelNetworkDao.update(tunnel.getId(), tunnel);
	}

	private String getGreEndpointIP(Host host, Network nw)
			throws AgentUnavailableException, OperationTimedoutException {
		String endpointIp = null;
		// Fetch fefault name for network label from configuration
		String physNetLabel = _configDao.getValue(Config.OvsTunnelNetworkDefaultLabel.key());
        Long physNetId = nw.getPhysicalNetworkId();
        PhysicalNetworkTrafficType physNetTT =
        		_physNetTTDao.findBy(physNetId, TrafficType.Guest);
        HypervisorType hvType = host.getHypervisorType();

        String label = null;
        switch (hvType) {
        	case XenServer:
        		label = physNetTT.getXenNetworkLabel();
        		if ((label!=null) && (!label.equals(""))) {
        			physNetLabel = label;
        		}
        		break;
			case KVM:
				label = physNetTT.getKvmNetworkLabel();
				if ((label != null) && (!label.equals(""))) {
					physNetLabel = label;
				}
				break;
        	default:
        		throw new CloudRuntimeException("Hypervisor " +
        				hvType.toString() +
        				" unsupported by OVS Tunnel Manager");
        }

        // Try to fetch GRE endpoint IP address for cloud db
        // If not found, then find it on the hypervisor
        OvsTunnelInterfaceVO tunnelIface =
                _tunnelInterfaceDao.getByHostAndLabel(host.getId(),
                        physNetLabel);
        if (tunnelIface == null) {
            //Now find and fetch configuration for physical interface
        	//for network with label on target host
			Commands fetchIfaceCmds =
					new Commands(new OvsFetchInterfaceCommand(physNetLabel));
			s_logger.debug("Ask host " + host.getId() +
						   " to retrieve interface for phy net with label:" +
						   physNetLabel);
			Answer[] fetchIfaceAnswers = _agentMgr.send(host.getId(), fetchIfaceCmds);
            //And finally save it for future use
            endpointIp = handleFetchInterfaceAnswer(fetchIfaceAnswers, host.getId());
        } else {
            endpointIp = tunnelIface.getIp();
        }
        return endpointIp;
    }

	private int getGreKey(Network network) {
		int key = 0;
		try {
		//The GRE key is actually in the host part of the URI
            // this is not true for lswitch/NiciraNvp!
            String keyStr = BroadcastDomainType.getValue(network.getBroadcastUri());
            // The key is most certainly and int if network is a vlan.
            // !! not in the case of lswitch/pvlan/(possibly)vswitch
            // So we now feel quite safe in converting it into a string
            // by calling the appropriate BroadcastDomainType method
    		key = Integer.valueOf(keyStr);
    		return key;
		} catch (NumberFormatException e) {
			s_logger.debug("Well well, how did '" + key
					+ "' end up in the broadcast URI for the network?");
			throw new CloudRuntimeException(String.format(
					"Invalid GRE key parsed from"
							+ "network broadcast URI (%s)", network
							.getBroadcastUri().toString()));
		}
	}

	@DB
    protected void CheckAndCreateTunnel(VirtualMachine instance, Network nw, DeployDestination dest) {

		s_logger.debug("Creating tunnels with OVS tunnel manager");
		if (instance.getType() != VirtualMachine.Type.User
				&& instance.getType() != VirtualMachine.Type.DomainRouter) {
			s_logger.debug("Will not work if you're not"
					+ "an instance or a virtual router");
			return;
		}

		long hostId = dest.getHost().getId();
		int key = getGreKey(nw);
		// Find active VMs with a NIC on the target network
		List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(nw.getId(),
				State.Running, State.Starting, State.Stopping, State.Unknown,
				State.Migrating);
		// Find routers for the network
		List<DomainRouterVO> routers = _routerDao.findByNetwork(nw.getId());
		List<VMInstanceVO> ins = new ArrayList<VMInstanceVO>();
		if (vms != null) {
			ins.addAll(vms);
		}
		if (routers.size() != 0) {
			ins.addAll(routers);
		}
		List<Long> toHostIds = new ArrayList<Long>();
		List<Long> fromHostIds = new ArrayList<Long>();
        for (VMInstanceVO v : ins) {
            Long rh = v.getHostId();
            if (rh == null || rh.longValue() == hostId) {
                continue;
            }
            OvsTunnelNetworkVO ta = _tunnelNetworkDao.getByFromToNetwork(hostId, rh.longValue(), nw.getId());
            // Try and create the tunnel even if a previous attempt failed
            if (ta == null || ta.getState().equals("FAILED")) {
                s_logger.debug("Attempting to create tunnel from:" + hostId + " to:" + rh.longValue());
                if (ta == null) {
                    this.createTunnelRecord(hostId, rh.longValue(), nw.getId(), key);
                }
                if (!toHostIds.contains(rh)) {
                    toHostIds.add(rh);
                }
            }

            ta = _tunnelNetworkDao.getByFromToNetwork(rh.longValue(),
            		hostId, nw.getId());
            // Try and create the tunnel even if a previous attempt failed
            if (ta == null || ta.getState().equals("FAILED")) {
            	s_logger.debug("Attempting to create tunnel from:" +
            			rh.longValue() + " to:" + hostId);
            	if (ta == null) {
            		this.createTunnelRecord(rh.longValue(), hostId,
            				nw.getId(), key);
            	}
                if (!fromHostIds.contains(rh)) {
                    fromHostIds.add(rh);
                }
            }
        }
        //TODO: Should we propagate the exception here?
        try {
            String myIp = getGreEndpointIP(dest.getHost(), nw);
            if (myIp == null)
                throw new GreTunnelException("Unable to retrieve the source " + "endpoint for the GRE tunnel." + "Failure is on host:" + dest.getHost().getId());
            boolean noHost = true;
			for (Long i : toHostIds) {
				HostVO rHost = _hostDao.findById(i);
				String otherIp = getGreEndpointIP(rHost, nw);
				if (otherIp == null)
					throw new GreTunnelException(
							"Unable to retrieve the remote "
									+ "endpoint for the GRE tunnel."
									+ "Failure is on host:" + rHost.getId());
				Commands cmds = new Commands(
						new OvsCreateTunnelCommand(otherIp, key,
								Long.valueOf(hostId), i, nw.getId(), myIp));
				s_logger.debug("Ask host " + hostId
						+ " to create gre tunnel to " + i);
				Answer[] answers = _agentMgr.send(hostId, cmds);
				handleCreateTunnelAnswer(answers);
				noHost = false;
			}

			for (Long i : fromHostIds) {
				HostVO rHost = _hostDao.findById(i);
				String otherIp = getGreEndpointIP(rHost, nw);
				Commands cmds = new Commands(new OvsCreateTunnelCommand(myIp,
						key, i, Long.valueOf(hostId), nw.getId(), otherIp));
				s_logger.debug("Ask host " + i + " to create gre tunnel to "
						+ hostId);
				Answer[] answers = _agentMgr.send(i, cmds);
				handleCreateTunnelAnswer(answers);
				noHost = false;
			}
			// If no tunnels have been configured, perform the bridge setup
			// anyway
			// This will ensure VIF rules will be triggered
			if (noHost) {
				Commands cmds = new Commands(new OvsSetupBridgeCommand(key,
						hostId, nw.getId()));
				s_logger.debug("Ask host " + hostId
						+ " to configure bridge for network:" + nw.getId());
				Answer[] answers = _agentMgr.send(hostId, cmds);
				handleSetupBridgeAnswer(answers);
			}
		} catch (Exception e) {
			// I really thing we should do a better handling of these exceptions
			s_logger.warn("Ovs Tunnel network created tunnel failed", e);
		}
	}

	@Override
	public boolean isOvsTunnelEnabled() {
		return true;
	}

    @Override
    public void VmCheckAndCreateTunnel(VirtualMachineProfile vm,
    		Network nw, DeployDestination dest) {
        CheckAndCreateTunnel(vm.getVirtualMachine(), nw, dest);
    }

    @DB
    private void handleDestroyTunnelAnswer(Answer ans, long from, long to, long network_id) {
        if (ans.getResult()) {
            OvsTunnelNetworkVO lock = _tunnelNetworkDao.acquireInLockTable(Long.valueOf(1));
            if (lock == null) {
                s_logger.warn(String.format("failed to lock" +
                		"ovs_tunnel_account, remove record of " +
                         "tunnel(from=%1$s, to=%2$s account=%3$s) failed",
                         from, to, network_id));
                return;
            }

            _tunnelNetworkDao.removeByFromToNetwork(from, to, network_id);
            _tunnelNetworkDao.releaseFromLockTable(lock.getId());

            s_logger.debug(String.format("Destroy tunnel(account:%1$s," +
            		"from:%2$s, to:%3$s) successful",
            		network_id, from, to));
        } else {
            s_logger.debug(String.format("Destroy tunnel(account:%1$s," + "from:%2$s, to:%3$s) failed", network_id, from, to));
        }
    }

    @DB
    private void handleDestroyBridgeAnswer(Answer ans,
    		long host_id, long network_id) {

        if (ans.getResult()) {
            OvsTunnelNetworkVO lock = _tunnelNetworkDao.acquireInLockTable(Long.valueOf(1));
            if (lock == null) {
                s_logger.warn("failed to lock ovs_tunnel_network," + "remove record");
                return;
            }

            _tunnelNetworkDao.removeByFromNetwork(host_id, network_id);
            _tunnelNetworkDao.releaseFromLockTable(lock.getId());

            s_logger.debug(String.format("Destroy bridge for" +
            		"network %1$s successful", network_id));
        } else {
        	s_logger.debug(String.format("Destroy bridge for" +
        			"network %1$s failed", network_id));
        }
    }

    private void handleSetupBridgeAnswer(Answer[] answers) {
        //TODO: Add some error management here?
        s_logger.debug("Placeholder for something more meanginful to come");
    }

    @Override
    public void CheckAndDestroyTunnel(VirtualMachine vm, Network nw) {
		// if (!_isEnabled) {
		// return;
		// }

        List<UserVmVO> userVms = _userVmDao.listByAccountIdAndHostId(vm.getAccountId(), vm.getHostId());
        if (vm.getType() == VirtualMachine.Type.User) {
            if (userVms.size() > 1) {
                return;
            }

            List<DomainRouterVO> routers = _routerDao.findByNetwork(nw.getId());
            for (DomainRouterVO router : routers) {
                if (router.getHostId() == vm.getHostId()) {
                    return;
                }
            }
        } else if (vm.getType() == VirtualMachine.Type.DomainRouter && userVms.size() != 0) {
            return;
        }
        try {
            /* Now we are last one on host, destroy the bridge with all
             * the tunnels for this network  */
            int key = getGreKey(nw);
            Command cmd = new OvsDestroyBridgeCommand(nw.getId(), key);
            s_logger.debug("Destroying bridge for network " + nw.getId() + " on host:" + vm.getHostId());
            Answer ans = _agentMgr.send(vm.getHostId(), cmd);
            handleDestroyBridgeAnswer(ans, vm.getHostId(), nw.getId());

            /* Then ask hosts have peer tunnel with me to destroy them */
            List<OvsTunnelNetworkVO> peers =
            		_tunnelNetworkDao.listByToNetwork(vm.getHostId(),
            				nw.getId());
            for (OvsTunnelNetworkVO p : peers) {
            	// If the tunnel was not successfully created don't bother to remove it
            	if (p.getState().equals("SUCCESS")) {
	                cmd = new OvsDestroyTunnelCommand(p.getNetworkId(), key,
	                		p.getPortName());
	                s_logger.debug("Destroying tunnel to " + vm.getHostId() +
	                		" from " + p.getFrom());
	                ans = _agentMgr.send(p.getFrom(), cmd);
	                handleDestroyTunnelAnswer(ans, p.getFrom(),
	                		p.getTo(), p.getNetworkId());
            	}
            }
        } catch (Exception e) {
            s_logger.warn(String.format("Destroy tunnel(account:%1$s," + "hostId:%2$s) failed", vm.getAccountId(), vm.getHostId()), e);
        }
    }

}
