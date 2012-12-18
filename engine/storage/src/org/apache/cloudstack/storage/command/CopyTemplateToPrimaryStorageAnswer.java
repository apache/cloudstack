package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class CopyTemplateToPrimaryStorageAnswer extends Answer {
    private final String path;
    
    public CopyTemplateToPrimaryStorageAnswer(Command cmd, String path) {
        super(cmd);
        this.path = path;
    }
    
    public String getPath() {
        return this.path;
    }
}
