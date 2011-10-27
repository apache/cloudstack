package com.cloud.resource;

import java.util.List;
import java.util.Map;

import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.utils.component.Adapter;

public interface ResourceStateAdapter extends Adapter {
    static public enum Event {
        CREATE_HOST_VO_FOR_CONNECTED,
        CREATE_HOST_VO_FOR_DIRECT_CONNECT,
        DELETE_HOST,
    }
    
    static public class DeleteHostAnswer {
        private boolean isContinue;
        private boolean isException;
        
        public DeleteHostAnswer(boolean isContinue) {
            this.isContinue = isContinue;
            this.isException = false;
        }
        
        public DeleteHostAnswer(boolean isContinue, boolean isException) {
            this.isContinue = isContinue;
            this.isException = isException;
        }
        
        public boolean getIsContinue() {
            return this.isContinue;
        }
        
        public boolean getIsException() {
            return this.isException;
        }
    }
    
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd);
    
    public HostVO createHostVOForDirectConnectAgent(HostVO host, final StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags);
    
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException;
}
