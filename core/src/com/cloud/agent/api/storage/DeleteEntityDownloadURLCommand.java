package com.cloud.agent.api.storage;

import com.cloud.storage.Upload;

public class DeleteEntityDownloadURLCommand extends AbstractDownloadCommand {
    
    String path;
    Upload.Type type;

    public DeleteEntityDownloadURLCommand(String path, Upload.Type type) {
        super();
        this.path = path;
        this.type = type;
    }

    public DeleteEntityDownloadURLCommand() {
        super();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Upload.Type getType() {
        return type;
    }

    public void setType(Upload.Type type) {
        this.type = type;
    }

}
