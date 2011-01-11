package com.cloud.network.ovs;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingRoutingWithOvsCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.network.ovs.dao.GreTunnelDao;
import com.cloud.network.ovs.dao.GreTunnelVO;
import com.cloud.network.ovs.dao.OvsWorkDao;
import com.cloud.network.ovs.dao.OvsWorkVO.Step;

public class OvsListener implements Listener {
	public static final Logger s_logger = Logger.getLogger(OvsListener.class.getName());
	OvsNetworkManager _ovsNetworkMgr;
	OvsWorkDao _workDao;
	GreTunnelDao _tunnelDao;
	
	public OvsListener(OvsNetworkManager ovsMgr, OvsWorkDao workDao, GreTunnelDao tunnelDao) {
		this._ovsNetworkMgr = ovsMgr;
		this._workDao = workDao;
		this._tunnelDao = tunnelDao;
	}
	
	@Override
	public boolean processAnswers(long agentId, long seq, Answer[] answers) {
		Set<Long> failedFlowVms = new HashSet<Long>();
		try {
			for (Answer ans : answers) {
				if (ans instanceof OvsCreateGreTunnelAnswer) {
					OvsCreateGreTunnelAnswer r = (OvsCreateGreTunnelAnswer) ans;
					String s = String
							.format("(hostIP:%1$s, remoteIP:%2$s, bridge:%3$s, greKey:%4$s)",
									r.getHostIp(), r.getRemoteIp(),
									r.getBridge(), r.getKey());
					if (!r.getResult()) {
						s_logger.warn("Create GRE tunnel failed due to "
								+ r.getDetails() + s);
					} else {
						GreTunnelVO tunnel = _tunnelDao.getByFromAndTo(
								r.getFrom(), r.getTo());
						if (tunnel == null) {
							throw new GreTunnelException(
									"No record matches from = " + r.getFrom() + " to = " + r.getTo());
						} else {
							tunnel.setInPort(r.getPort());
							_tunnelDao.update(tunnel.getId(), tunnel);
							s_logger.info("Create GRE tunnel success" + s
									+ " from " + r.getFrom() + " to "
									+ r.getTo() + " inport=" + r.getPort());
						}
					}
				} else if (ans instanceof OvsSetTagAndFlowAnswer) {
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
