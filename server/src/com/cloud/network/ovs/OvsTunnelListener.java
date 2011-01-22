package com.cloud.network.ovs;

import java.util.List;

import javax.persistence.EntityExistsException;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.network.ovs.dao.GreTunnelVO;
import com.cloud.network.ovs.dao.OvsTunnelDao;
import com.cloud.network.ovs.dao.OvsTunnelVO;

public class OvsTunnelListener implements Listener {
	public static final Logger s_logger = Logger.getLogger(OvsListener.class.getName());
	HostDao _hostDao;
	OvsTunnelDao _tunnelDao;
	
	public OvsTunnelListener(OvsTunnelDao tunnelDao, HostDao hostDao) {
		this._hostDao = hostDao;
		this._tunnelDao = tunnelDao;
	}
	
	@Override
	public boolean processAnswers(long agentId, long seq, Answer[] answers) {
	
		return true;
	}

	@Override
	public boolean processCommands(long agentId, long seq, Command[] commands) {
		// TODO Auto-generated method stub
		return true;
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
		
		try {
			List<HostVO> hosts = _hostDao.listByType(Host.Type.Routing);
			for (HostVO h : hosts) {
				if (h.getId() == host.getId()) {
					continue;
				}
				
				OvsTunnelVO t = _tunnelDao.getByFromAndTo(host.getId(), h.getId());
				if (t == null) {
					t = new OvsTunnelVO(host.getId(), h.getId());
					try {
						_tunnelDao.persist(t);
					} catch (EntityExistsException e) {
						s_logger.debug(String.format("Already has (from=%1$s, to=%2$s)", host.getId(), h.getId()));
					}
				}

				t = _tunnelDao.getByFromAndTo(h.getId(), host.getId());
				if (t == null) {
					t = new OvsTunnelVO(h.getId(), host.getId());
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
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isRecurring() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getTimeout() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public boolean processTimeout(long agentId, long seq) {
		// TODO Auto-generated method stub
		return true;
	}

}
