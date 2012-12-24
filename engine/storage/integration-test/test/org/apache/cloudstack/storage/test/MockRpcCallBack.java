package org.apache.cloudstack.storage.test;

import javax.inject.Inject;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;

public class MockRpcCallBack implements Runnable {
    @Inject
    AgentManager agentMgr;
    private Command cmd;
    private long hostId;
    private AsyncCompletionCallback<Answer> callback; 
    
    public void setCmd(Command cmd) {
        this.cmd = cmd;
    }
    
    public void setHostId(long hostId) {
        this.hostId = hostId;
    }
    
    public void setCallback(AsyncCompletionCallback<Answer> callback) {
        this.callback = callback;
    }
    
    @Override
    @DB
    public void run() {
        try {
            Answer answer = agentMgr.send(hostId, cmd);
            
            callback.complete(answer);
        } catch (Throwable e) {
            //s_logger.debug("send command failed:" + e.toString());
        } finally {
            int i =1;
        }
    }
    
}
