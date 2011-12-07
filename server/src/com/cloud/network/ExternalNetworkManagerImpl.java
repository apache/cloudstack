/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.network;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.ExternalNetworkResourceUsageAnswer;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.InlineLoadBalancerNicMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.AccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.Nic.State;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

@Local(value = {ExternalNetworkManager.class})
public class ExternalNetworkManagerImpl implements ExternalNetworkManager {
	public enum ExternalNetworkResourceName {
		JuniperSrx,
		F5BigIp;
	}
	
	@Inject AgentManager _agentMgr;
	@Inject NetworkManager _networkMgr;
	@Inject HostDao _hostDao;
	@Inject DataCenterDao _dcDao;
	@Inject AccountDao _accountDao;
	@Inject DomainRouterDao _routerDao;
	@Inject IPAddressDao _ipAddressDao;
	@Inject VlanDao _vlanDao;
	@Inject UserStatisticsDao _userStatsDao;
	@Inject NetworkDao _networkDao;
	@Inject PortForwardingRulesDao _portForwardingRulesDao;
	@Inject LoadBalancerDao _loadBalancerDao;
	@Inject InlineLoadBalancerNicMapDao _inlineLoadBalancerNicMapDao;
	@Inject ConfigurationDao _configDao;
	@Inject HostDetailsDao _detailsDao;
	@Inject NetworkOfferingDao _networkOfferingDao;
	@Inject NicDao _nicDao;
	
	ScheduledExecutorService _executor;
	int _externalNetworkStatsInterval;
	
	private static final org.apache.log4j.Logger s_logger = Logger.getLogger(ExternalNetworkManagerImpl.class);
	protected String _name;
	
	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		_externalNetworkStatsInterval = NumbersUtil.parseInt(_configDao.getValue(Config.ExternalNetworkStatsInterval.key()), 300);
		if (_externalNetworkStatsInterval > 0){
			_executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ExternalNetworkMonitor"));		
		}
    	return true;
    }
	
	@Override
    public boolean start() {
		if (_externalNetworkStatsInterval > 0){
			_executor.scheduleAtFixedRate(new ExternalNetworkUsageTask(), _externalNetworkStatsInterval, _externalNetworkStatsInterval, TimeUnit.SECONDS);
		}
    	return true;
    }

	@Override
    public boolean stop() {
    	return true;
    }

	@Override
    public String getName() {
    	return _name;
    }
	
	public String getExternalNetworkResourceGuid(long zoneId, ExternalNetworkResourceName name, String ip) {
		return zoneId + "-" + name + "-" + ip;
	}
	
	protected HostVO getExternalNetworkAppliance(long zoneId, Host.Type type) {
		DataCenterVO zone = _dcDao.findById(zoneId);
		if (!_networkMgr.zoneIsConfiguredForExternalNetworking(zoneId)) {
			s_logger.debug("Zone " + zone.getName() + " is not configured for external networking.");
			return null;
		} else {
			List<HostVO> externalNetworkAppliancesInZone = _hostDao.listBy(type, zoneId);
			if (externalNetworkAppliancesInZone.size() != 1) {
				return null;
			} else {
				return externalNetworkAppliancesInZone.get(0);
			}			
		}
	}
	
	public NicVO savePlaceholderNic(Network network, String ipAddress) {
		NicVO nic = new NicVO(null, null, network.getId(), null);
		nic.setIp4Address(ipAddress);
		nic.setReservationStrategy(ReservationStrategy.PlaceHolder);
		nic.setState(State.Reserved);
		return _nicDao.persist(nic);
	}
	
	protected boolean externalLoadBalancerIsInline(HostVO externalLoadBalancer) {
        DetailVO detail = _detailsDao.findDetail(externalLoadBalancer.getId(), "inline");
        return (detail != null && detail.getValue().equals("true"));
	}
	
	public int getGloballyConfiguredCidrSize() {
        try {
            String globalVlanBits = _configDao.getValue(Config.GuestVlanBits.key());
            return 8 + Integer.parseInt(globalVlanBits);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to read the globally configured VLAN bits size.");
        }
    }
	
	public int getVlanOffset(DataCenter zone, int vlanTag) {
        if (zone.getVnet() == null) {
            throw new CloudRuntimeException("Could not find vlan range for zone " + zone.getName() + ".");
        }

        String vlanRange[] = zone.getVnet().split("-");
        int lowestVlanTag = Integer.valueOf(vlanRange[0]);
        return vlanTag - lowestVlanTag;
    }
	
protected class ExternalNetworkUsageTask implements Runnable {				
		
		public ExternalNetworkUsageTask() {		
		}
		
		private boolean updateBytes(UserStatisticsVO userStats, long newCurrentBytesSent, long newCurrentBytesReceived) {			
			long oldNetBytesSent = userStats.getNetBytesSent();
			long oldNetBytesReceived = userStats.getNetBytesReceived();
			long oldCurrentBytesSent = userStats.getCurrentBytesSent();
			long oldCurrentBytesReceived = userStats.getCurrentBytesReceived();
			String warning = "Received an external network stats byte count that was less than the stored value. Zone ID: " + userStats.getDataCenterId() + ", account ID: " + userStats.getAccountId() + ".";
						
			userStats.setCurrentBytesSent(newCurrentBytesSent);
			if (oldCurrentBytesSent > newCurrentBytesSent) {
				s_logger.warn(warning + "Stored bytes sent: " + oldCurrentBytesSent + ", new bytes sent: " + newCurrentBytesSent + ".");			
				userStats.setNetBytesSent(oldNetBytesSent + oldCurrentBytesSent);
			} 
			
			userStats.setCurrentBytesReceived(newCurrentBytesReceived);
			if (oldCurrentBytesReceived > newCurrentBytesReceived) {
				s_logger.warn(warning + "Stored bytes received: " + oldCurrentBytesReceived + ", new bytes received: " + newCurrentBytesReceived + ".");						
				userStats.setNetBytesReceived(oldNetBytesReceived + oldCurrentBytesReceived);
			} 
					
			return _userStatsDao.update(userStats.getId(), userStats);
		}
		
		/*
		 * Creates a new stats entry for the specified parameters, if one doesn't already exist.
		 */
		private boolean createStatsEntry(long accountId, long zoneId, long networkId, String publicIp, long hostId) {
		    HostVO host = _hostDao.findById(hostId);
			UserStatisticsVO userStats = _userStatsDao.findBy(accountId, zoneId, networkId, publicIp, hostId, host.getType().toString());
			if (userStats == null) {
				return (_userStatsDao.persist(new UserStatisticsVO(accountId, zoneId, publicIp, hostId, host.getType().toString(), networkId)) != null);
			} else {
				return true;
			}
		}
		
		/*
		 * Updates an existing stats entry with new data from the specified usage answer.
		 */
		private boolean updateStatsEntry(long accountId, long zoneId, long networkId, String publicIp, long hostId, ExternalNetworkResourceUsageAnswer answer) {
			AccountVO account = _accountDao.findById(accountId);
			DataCenterVO zone = _dcDao.findById(zoneId);
			NetworkVO network = _networkDao.findById(networkId);
			HostVO host = _hostDao.findById(hostId);
			String statsEntryIdentifier = "account " + account.getAccountName() + ", zone " + zone.getName() + ", network ID " + networkId + ", host ID " + host.getName();			
			
			long newCurrentBytesSent = 0;
			long newCurrentBytesReceived = 0;
			
			if (publicIp != null) {
				long[] bytesSentAndReceived = null;
				statsEntryIdentifier += ", public IP: " + publicIp;
				
				if (host.getType().equals(Host.Type.ExternalLoadBalancer) && externalLoadBalancerIsInline(host)) {
					// Look up stats for the guest IP address that's mapped to the public IP address
					InlineLoadBalancerNicMapVO mapping = _inlineLoadBalancerNicMapDao.findByPublicIpAddress(publicIp);
					
					if (mapping != null) {
						NicVO nic = _nicDao.findById(mapping.getNicId());
						String loadBalancingIpAddress = nic.getIp4Address();
						bytesSentAndReceived = answer.ipBytes.get(loadBalancingIpAddress);
						
						if (bytesSentAndReceived != null) {
							bytesSentAndReceived[0] = 0;
						}
					}
				} else {
					bytesSentAndReceived = answer.ipBytes.get(publicIp);
				}
				
				if (bytesSentAndReceived == null) {
		    		s_logger.debug("Didn't get an external network usage answer for public IP " + publicIp);
		    	} else {
		        	newCurrentBytesSent += bytesSentAndReceived[0];
		        	newCurrentBytesReceived += bytesSentAndReceived[1];
		    	}
			} else {
			    URI broadcastURI = network.getBroadcastUri();
                if (broadcastURI == null) {
                    s_logger.debug("Not updating stats for guest network with ID " + network.getId() + " because the network is not implemented.");
                    return true;
                } else {
                    long vlanTag = Integer.parseInt(broadcastURI.getHost());
                    long[] bytesSentAndReceived = answer.guestVlanBytes.get(String.valueOf(vlanTag));                                   
                    
                    if (bytesSentAndReceived == null) {
                        s_logger.warn("Didn't get an external network usage answer for guest VLAN " + vlanTag);                      
                    } else {
                        newCurrentBytesSent += bytesSentAndReceived[0];
                        newCurrentBytesReceived += bytesSentAndReceived[1];
                    }
                }
			}
			
			UserStatisticsVO userStats;
			try {
				userStats = _userStatsDao.lock(accountId, zoneId, networkId, publicIp, hostId, host.getType().toString());
			} catch (Exception e) {
				s_logger.warn("Unable to find user stats entry for " + statsEntryIdentifier);
				return false;
			}
   	           			
        	if (updateBytes(userStats, newCurrentBytesSent, newCurrentBytesReceived)) {
        		s_logger.debug("Successfully updated stats for " + statsEntryIdentifier);
        		return true;
        	} else {
        		s_logger.debug("Failed to update stats for " + statsEntryIdentifier);
        		return false;
        	}
		}				
		
		private boolean createOrUpdateStatsEntry(boolean create, long accountId, long zoneId, long networkId, String publicIp, long hostId, ExternalNetworkResourceUsageAnswer answer) {
			if (create) {
				return createStatsEntry(accountId, zoneId, networkId, publicIp, hostId);
			} else {
				return updateStatsEntry(accountId, zoneId, networkId, publicIp, hostId, answer);
			}		
		}
		
		/*
		 * Creates/updates all necessary stats entries for an account and zone.
		 * Stats entries are created for source NAT IP addresses, static NAT rules, port forwarding rules, and load balancing rules
		 */
		private boolean manageStatsEntries(boolean create, long accountId, long zoneId, 
										   HostVO externalFirewall, ExternalNetworkResourceUsageAnswer firewallAnswer,
										   HostVO externalLoadBalancer, ExternalNetworkResourceUsageAnswer lbAnswer) {
			String accountErrorMsg = "Failed to update external network stats entry. Details: account ID = " + accountId;
			Transaction txn = Transaction.open(Transaction.CLOUD_DB);
			try {
				txn.start();
				
				List<NetworkVO> networksForAccount = _networkDao.listBy(accountId, zoneId, Network.GuestIpType.Virtual);
				
				for (NetworkVO network : networksForAccount) {
				    String networkErrorMsg = accountErrorMsg + ", network ID = " + network.getId();				
				    NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
				    
				    if (!offering.isSharedSourceNatService()) {
				        // Manage the entry for this network's source NAT IP address
				        List<IPAddressVO> sourceNatIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), true);
				        if (sourceNatIps.size() == 1) {
				            String publicIp = sourceNatIps.get(0).getAddress().addr();
				            if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer)) {
	                            throw new ExecutionException(networkErrorMsg + ", source NAT IP = " + publicIp);
	                        }
				        }
				        
				        // Manage one entry for each static NAT rule in this network
	                    List<IPAddressVO> staticNatIps = _ipAddressDao.listStaticNatPublicIps(network.getId());
	                    for (IPAddressVO staticNatIp : staticNatIps) {
	                        String publicIp = staticNatIp.getAddress().addr();
	                        if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer)) {
	                            throw new ExecutionException(networkErrorMsg + ", static NAT rule public IP = " + publicIp);
	                        }  
	                    }
	                    
	                    // Manage one entry for each port forwarding rule in this network
	                    List<PortForwardingRuleVO> portForwardingRules = _portForwardingRulesDao.listByNetwork(network.getId());
	                    for (PortForwardingRuleVO portForwardingRule : portForwardingRules) {
	                        String publicIp = _networkMgr.getIp(portForwardingRule.getSourceIpAddressId()).getAddress().addr();                 
	                        if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer)) {
	                            throw new ExecutionException(networkErrorMsg + ", port forwarding rule public IP = " + publicIp);
	                        }   
	                    }
				    } else {
				        // Manage the account-wide entry for the external firewall
				        if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), null, externalFirewall.getId(), firewallAnswer)) {
	                        throw new ExecutionException(networkErrorMsg);
	                    }
				    }				    				    
				    
                    // If an external load balancer is added, manage one entry for each load balancing rule in this network
                    if (externalLoadBalancer != null && lbAnswer != null) {
                        List<LoadBalancerVO> loadBalancers = _loadBalancerDao.listByNetworkId(network.getId());
                        for (LoadBalancerVO loadBalancer : loadBalancers) {
                            String publicIp = _networkMgr.getIp(loadBalancer.getSourceIpAddressId()).getAddress().addr();               
                            if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalLoadBalancer.getId(), lbAnswer)) {
                                throw new ExecutionException(networkErrorMsg + ", load balancing rule public IP = " + publicIp);
                            }   
                        }
                    }
				    				  
				}				
				
				return txn.commit();
			} catch (Exception e) {
				s_logger.warn("Exception: ", e);
				txn.rollback();
				return false;
			} finally {
				txn.close();
			}
		}
		
		private void runExternalNetworkUsageTask() {
			s_logger.debug("External network stats collector is running...");
			for (DataCenterVO zone : _dcDao.listAll()) {				
				// Make sure the zone is configured for external networking
				if (!_networkMgr.zoneIsConfiguredForExternalNetworking(zone.getId())) {
					s_logger.debug("Zone " + zone.getName() + " is not configured for external networking, so skipping usage check.");
					continue;
				}
				
				// Only collect stats if there is an external firewall in this zone
				HostVO externalFirewall = getExternalNetworkAppliance(zone.getId(), Host.Type.ExternalFirewall);
				HostVO externalLoadBalancer = getExternalNetworkAppliance(zone.getId(), Host.Type.ExternalLoadBalancer);
				
				if (externalFirewall == null) {
					s_logger.debug("Skipping usage check for zone " + zone.getName());
					continue;
				}
				
				s_logger.debug("Collecting external network stats for zone " + zone.getName());
				
				ExternalNetworkResourceUsageCommand cmd = new ExternalNetworkResourceUsageCommand();
		
				// Get network stats from the external firewall
				ExternalNetworkResourceUsageAnswer firewallAnswer = (ExternalNetworkResourceUsageAnswer) _agentMgr.easySend(externalFirewall.getId(), cmd);
				if (firewallAnswer == null || !firewallAnswer.getResult()) {
					String details = (firewallAnswer != null) ? firewallAnswer.getDetails() : "details unavailable";
					String msg = "Unable to get external firewall stats for " + zone.getName() + " due to: " + details + ".";
					s_logger.error(msg);
					continue;
				} 
											
				ExternalNetworkResourceUsageAnswer lbAnswer = null;
				if (externalLoadBalancer != null) {
				    // Get network stats from the external load balancer
				    lbAnswer = (ExternalNetworkResourceUsageAnswer) _agentMgr.easySend(externalLoadBalancer.getId(), cmd);
				    if (lbAnswer == null || !lbAnswer.getResult()) {
					    String details = (lbAnswer != null) ? lbAnswer.getDetails() : "details unavailable";
					    String msg = "Unable to get external load balancer stats for " + zone.getName() + " due to: " + details + ".";
					    s_logger.error(msg);
				    }    				
				}
				
				List<DomainRouterVO> domainRoutersInZone = _routerDao.listByDataCenter(zone.getId());
				for (DomainRouterVO domainRouter : domainRoutersInZone) {
					long accountId = domainRouter.getAccountId();
					long zoneId = domainRouter.getDataCenterIdToDeployIn();
					
					AccountVO account = _accountDao.findById(accountId);
					if (account == null) {
						s_logger.debug("Skipping stats update for account with ID " + accountId);
						continue;
					}
					
					if (!manageStatsEntries(true, accountId, zoneId, externalFirewall, firewallAnswer, externalLoadBalancer, lbAnswer)) {
						continue;
					}
					
					manageStatsEntries(false, accountId, zoneId, externalFirewall, firewallAnswer, externalLoadBalancer, lbAnswer);
				}				
			}									
		}
		
		@Override
		public void run() {			
			GlobalLock scanLock = GlobalLock.getInternLock("ExternalNetworkManagerImpl");
            try {
                if (scanLock.lock(20)) {
                    try {
                    	runExternalNetworkUsageTask();
                    } finally {
                        scanLock.unlock();
                    }
                }
            } catch (Exception e) {
            	s_logger.warn("Problems while getting external network usage", e);
            } finally {
                scanLock.releaseRef();
            }
		}
	}
}
