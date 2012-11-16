package org.apache.cloudstack.storage.datastore.driver;

import java.util.List;

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CreateVolumeAnswer;
import org.apache.cloudstack.storage.command.CreateVolumeCommand;
import org.apache.cloudstack.storage.to.VolumeTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;

@Component
public class DefaultPrimaryDataStoreDriverImpl implements
		PrimaryDataStoreDriver {
	private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStoreDriverImpl.class);
	@Override
	public boolean createVolume(VolumeObject vol) {
		//The default driver will send createvolume command to one of hosts which can access its datastore
		List<EndPoint> endPoints = vol.getDataStore().getEndPoints();
		int retries = 3;
		VolumeInfo volInfo = vol;
		CreateVolumeCommand createCmd = new CreateVolumeCommand(new VolumeTO(volInfo));
		Answer answer = null;
		int i = 0;
		boolean result = false;
		
		for (EndPoint ep : endPoints) {
			answer = ep.sendMessage(createCmd);
			if (answer == null) {
				if (i < retries) {
					s_logger.debug("create volume failed, retrying: " + i);
				}
				i++;
			} else {
				CreateVolumeAnswer volAnswer = (CreateVolumeAnswer)answer;
				vol.setPath(volAnswer.getVolumeUuid());
				result = true;
			}
		}
		
		return result;
	}

	@Override
	public boolean deleteVolume(VolumeObject vo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String grantAccess(VolumeObject vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean revokeAccess(VolumeObject vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return false;
	}

}
