package org.apache.cloudstack.storage.datastore.driver;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCallbackHandler;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CreateVolumeAnswer;
import org.apache.cloudstack.storage.command.CreateVolumeCommand;
import org.apache.cloudstack.storage.command.CreateVolumeFromBaseImageCommand;
import org.apache.cloudstack.storage.command.DeleteVolume;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.to.ImageOnPrimayDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeTO;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.exception.CloudRuntimeException;

public class DefaultPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStoreDriverImpl.class);
    protected PrimaryDataStore dataStore;
    public DefaultPrimaryDataStoreDriverImpl(PrimaryDataStore dataStore) {
        this.dataStore = dataStore;
    }
    
    public DefaultPrimaryDataStoreDriverImpl() {
        
    }
    
    @Override
    public void setDataStore(PrimaryDataStore dataStore) {
        this.dataStore = dataStore;
    }
    
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
    public void createVolumeFromBaseImageAsync(VolumeObject volume, TemplateOnPrimaryDataStoreInfo template, AsyncCompletionCallback<CommandResult> callback) {
        VolumeTO vol = new VolumeTO(volume);
        ImageOnPrimayDataStoreTO image = new ImageOnPrimayDataStoreTO(template);
        CreateVolumeFromBaseImageCommand cmd = new CreateVolumeFromBaseImageCommand(vol, image);
        List<EndPoint> endPoints = template.getPrimaryDataStore().getEndPoints();
        EndPoint ep = endPoints.get(0);
        
        AsyncCallbackDispatcher caller = new AsyncCallbackDispatcher(this)
        .setParentCallback(callback)
        .setOperationName("primarydatastoredriver.createvolumefrombaseImage")
        .setContextParam("volume", volume);
        ep.sendMessageAsync(cmd, caller);
        
       
    }
    
    @AsyncCallbackHandler(operationName="primarydatastoredriver.createvolumefrombaseImage")
    public void createVolumeFromBaseImageAsyncCallback(AsyncCallbackDispatcher callback) {
        CreateVolumeAnswer answer = (CreateVolumeAnswer)callback.getResult();
        CommandResult result = new CommandResult();
        if (answer == null || answer.getDetails() != null) {
            result.setSucess(false);
            if (answer != null) {
                result.setResult(answer.getDetails());
            }
        } else {
            result.setSucess(true);
            VolumeObject volume = callback.getContextParam("volume");
            volume.setPath(answer.getVolumeUuid());
        }
        AsyncCallbackDispatcher parentCall = callback.getParentCallback();
        parentCall.complete(result);
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

    @Override
    public boolean initialize(Map<String, String> params) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean grantAccess(EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean revokeAccess(EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

   
}
