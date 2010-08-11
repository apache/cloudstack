package com.cloud.hypervisor.vmware.resource;

import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ShareAnswer;
import com.cloud.agent.api.storage.ShareCommand;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.storage.resource.StoragePoolResource;

public class VmwareResource implements StoragePoolResource, ServerResource {

	@Override
	public DownloadAnswer execute(PrimaryStorageDownloadCommand cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer execute(DestroyCommand cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ShareAnswer execute(ShareCommand cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CopyVolumeAnswer execute(CopyVolumeCommand cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateAnswer execute(CreateCommand cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Answer executeRequest(Command cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IAgentControl getAgentControl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StartupCommand[] initialize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}
}
