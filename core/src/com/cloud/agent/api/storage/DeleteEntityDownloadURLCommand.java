package com.cloud.agent.api.storage;

public class DeleteEntityDownloadURLCommand extends AbstractDownloadCommand {
    
    String path;

    public DeleteEntityDownloadURLCommand(String path) {
        super();
        this.path = path;
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
    

}
