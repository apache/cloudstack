package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.Answer;

public class CopyTemplateToPrimaryStorageAnswer extends Answer {
    private final String path;
    
    public CopyTemplateToPrimaryStorageAnswer(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return this.path;
    }
}
