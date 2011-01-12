package com.cloud.agent.api.storage;

import com.cloud.storage.Upload;

public class DeleteEntityDownloadURLCommand extends AbstractDownloadCommand {
    
    private String path;
    private String extractUrl; 
    private Upload.Type type;

    public DeleteEntityDownloadURLCommand(String path, Upload.Type type, String url) {
        super();
        this.path = path;
        this.type = type;
        this.extractUrl = url;
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

	public String getExtractUrl() {
		return extractUrl;
	}

	public void setExtractUrl(String extractUrl) {
		this.extractUrl = extractUrl;
	}

}
