package com.cloud.network.ovs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityExistsException;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingRoutingWithOvsCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.network.ovs.dao.GreTunnelDao;
import com.cloud.network.ovs.dao.GreTunnelVO;
import com.cloud.network.ovs.dao.OvsWorkDao;
import com.cloud.network.ovs.dao.OvsWorkVO.Step;
import com.cloud.network.ovs.dao.VlanMappingDao;
import com.cloud.network.ovs.dao.VlanMappingVO;
import com.cloud.utils.component.Inject;

public class OvsListener implements Listener {
	public static final Logger s_logger = Logger.getLogger(OvsListener.class.getName());
	OvsNetworkManager _ovsNetworkMgr;
	OvsWorkDao _workDao;
	GreTunnelDao _tunnelDao;
	VlanMappingDao _mappingDao;
	HostDao _hostDao;
	
	public OvsListener(OvsNetworkManager ovsMgr, OvsWorkDao workDao, GreTunnelDao tunnelDao,
			VlanMappingDao mappingDao, HostDao hostDao) {
		this._ovsNetworkMgr = ovsMgr;
		this._workDao = workDao;
		this._tunnelDao = tunnelDao;
		this._mappingDao = mappingDao;
		this._hostDao = hostDao;
	}
	
	@Override
	public boolean processAnswers(long agentId, long seq, Answer[] answers) {
		Set<Long> failedFlowVms = new HashSet<Long>();
		try {
			for (Answer ans : answers) {
				if (ans instanceof OvsSetTagAndFlowAnswer) {
					OvsSetTagAndFlowAnswer r = (OvsSetTagAndFlowAnswer) ans;
					if (!r.getResult()) {
						s_logger.warn("Failed to set flow for VM "
								+ r.getVmId());
						_workDao.updateStep(r.getVmId(), r.getSeqNo(),
								Step.Error);
						failedFlowVms.add(r.getVmId());
					} else {
						s_logger.info("Success to set flow for VM "
								+ r.getVmId());
						_workDao.updateStep(r.getVmId(), r.getSeqNo(),
								Step.Done);
					}
				}
				// TODO: handle delete failure
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (failedFlowVms.size() > 0) {
			_ovsNetworkMgr.scheduleFlowUpdateToHosts(failedFlowVms, false, new Long(10*1000l));
		}
		
		return true;
	}

	@Override
	public boolean processCommands(long agentId, long seq, Command[] commands) {
		boolean processed = false;
        for (Command cmd : commands) {
            if (cmd instanceof PingRoutingWithOvsCommand) {
            	PingRoutingWithOvsCommand ping = (PingRoutingWithOvsCommand)cmd;
                if (ping !=null && ping.getStates().size() > 0) {
                    _ovsNetworkMgr.fullSync(ping.getStates());
                }
                processed = true;
            }
        }
        return processed;
	}

	@Override
	public AgentControlAnswer processControlCommand(long agentId,
			AgentControlCommand cmd) {
		return null;
	}

	@Override
	public void processConnect(HostVO host, StartupCommand cmd)
			throws ConnectionException {
		if (host.getType() != Host.Type.Routing) {
			return;
		}
		
		List<VlanMappingVO> maps = _mappingDao.listByHostId(host.getId());
		if (maps.size() == 0) {
			for (int i=0; i<512; i++) {
				VlanMappingVO vo = new VlanMappingVO(0, host.getId(), i);
				_mappingDao.persist(vo);
			}
		}
		
		try {
			List<HostVO> hosts = _hostDao.listByType(Host.Type.Routing);
			for (HostVO h : hosts) {
				if (h.getId() == host.getId()) {
					continue;
				}
				
				GreTunnelVO t = _tunnelDao.getByFromAndTo(host.getId(), h.getId());
				if (t == null) {
					t = new GreTunnelVO(host.getId(), h.getId());
					try {
						_tunnelDao.persist(t);
					} catch (EntityExistsException e) {
						s_logger.debug(String.format("Already has (from=%1$s, to=%2$s)", host.getId(), h.getId()));
					}
				}

				t = _tunnelDao.getByFromAndTo(h.getId(), host.getId());
				if (t == null) {
					t = new GreTunnelVO(h.getId(), host.getId());
					try {
						_tunnelDao.persist(t);
					} catch (EntityExistsException e) {
						s_logger.debug(String.format("Already has (from=%1$s, to=%2$s)", h.getId(), host.getId()));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean processDisconnect(long agentId, Status state) {
		return true;
	}

	@Override
	public boolean isRecurring() {
		return false;
	}

	@Override
	public int getTimeout() {
		return -1;
	}

	@Override
	public boolean processTimeout(long agentId, long seq) {
		return true;
	}

}
