package org.apache.cloudstack.storage.datastore.driver;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CreateVolumeAnswer;
import org.apache.cloudstack.storage.command.CreateVolumeCommand;
import org.apache.cloudstack.storage.command.CreateVolumeFromBaseImageCommand;
import org.apache.cloudstack.storage.command.DeleteVolume;
import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeTO;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DefaultPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStoreDriverImpl.class);

    @Override
    public boolean createVolume(VolumeObject vol) {
        // The default driver will send createvolume command to one of hosts
        // which can access its datastore
        List<EndPoint> endPoints = vol.getDataStore().getEndPoints();
        VolumeInfo volInfo = vol;
        CreateVolumeCommand createCmd = new CreateVolumeCommand(new VolumeTO(volInfo));
        Answer answer = sendOutCommand(createCmd, endPoints);

        CreateVolumeAnswer volAnswer = (CreateVolumeAnswer) answer;
        vol.setPath(volAnswer.getVolumeUuid());
        return true;
    }

    @Override
    public boolean deleteVolume(VolumeObject vo) {
        DeleteVolume cmd = new DeleteVolume((VolumeInfo)vo);
        List<EndPoint> endPoints = vo.getDataStore().getEndPoints();
        sendOutCommand(cmd, endPoints);

        return true;
    }

    @Override
    public String grantAccess(VolumeObject vol, EndPoint ep) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean revokeAccess(VolumeObject vol, EndPoint ep) {
        // TODO Auto-generated method stub
        return true;
    }

    protected Answer sendOutCommand(Command cmd, List<EndPoint> endPoints) {
        Answer answer = null;
        int retries = 3;
        int i = 0;
        for (EndPoint ep : endPoints) {
            answer = ep.sendMessage(cmd);
            if (answer == null || answer.getDetails() != null) {
                if (i < retries) {
                    s_logger.debug("create volume failed, retrying: " + i);
                }
                i++;
            } else {
                break;
            }
        }
        
        if (answer == null || answer.getDetails() != null) {
            if (answer == null) {
                throw new CloudRuntimeException("Failed to created volume");
            } else {
                throw new CloudRuntimeException(answer.getDetails());
            }
        }
        
        return answer;
    }

    @Override
    public boolean createVolumeFromBaseImage(VolumeObject volume, TemplateOnPrimaryDataStoreInfo template) {
        VolumeTO vol = new VolumeTO(volume);
        ImageOnPrimayDataStoreTO image = new ImageOnPrimayDataStoreTO(template);
        CreateVolumeFromBaseImageCommand cmd = new CreateVolumeFromBaseImageCommand(vol, image);
        List<EndPoint> endPoints = template.getPrimaryDataStore().getEndPoints();

        Answer answer = sendOutCommand(cmd, endPoints);

        CreateVolumeAnswer volAnswer = (CreateVolumeAnswer) answer;
        volume.setPath(volAnswer.getVolumeUuid());
        return true;
    }

    @Override
    public long getCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getAvailableCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }
}
