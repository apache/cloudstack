package org.apache.cloudstack.storage.datastore.driver;

import java.util.List;

import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CreateVolumeAnswer;
import org.apache.cloudstack.storage.command.CreateVolumeCommand;
import org.apache.cloudstack.storage.volume.Volume;
import org.apache.cloudstack.storage.volume.VolumeInfo;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;

@Component
public class DefaultPrimaryDataStoreDriverImpl implements
		PrimaryDataStoreDriver {
	private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStoreDriverImpl.class);
	@Override
	public boolean createVolume(Volume vol) {
		//The default driver will send createvolume command to one of hosts which can access its datastore
		List<EndPoint> endPoints = vol.getDataStore().getEndPoints();
		int retries = 3;
		VolumeInfo volInfo = new VolumeInfo(vol);
		CreateVolumeCommand createCmd = new CreateVolumeCommand(volInfo);
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
				vol.setUuid(volAnswer.getVolumeUuid());
				result = true;
			}
		}
		
		return result;
	}

	@Override
	public boolean deleteVolume(Volume vo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String grantAccess(Volume vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean revokeAccess(Volume vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return false;
	}

}
