package com.cloud.async.executor;

import com.cloud.storage.Upload;

public class ExtractTemplateParam {
	
	private long userId;
	private long templateId;
	private Long zoneId;
	private long eventId;
	private String url;
	private Upload.Mode extractMode;

	public ExtractTemplateParam() {
	}
	
    public ExtractTemplateParam(long userId, long templateId, Long zoneId, long eventId, String url) {
        this.userId = userId;
        this.templateId = templateId;
        this.zoneId = zoneId;
        this.eventId = eventId;
        this.url = url;
        this.extractMode = Upload.Mode.FTP_UPLOAD;
    }
	
	public ExtractTemplateParam(long userId, long templateId, Long zoneId, long eventId, String url, Upload.Mode mode) {
		this.userId = userId;
		this.templateId = templateId;
		this.zoneId = zoneId;
		this.eventId = eventId;
		this.url = url;
		this.extractMode = mode;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getUserId() {
		return userId;
	}
	
	public long getTemplateId() {
		return templateId;
	}
	
	public Long getZoneId() {
		return zoneId;
	}

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }

    public Upload.Mode getExtractMode() {
        return extractMode;
    }

    public void setExtractMode(Upload.Mode extractMode) {
        this.extractMode = extractMode;
    }

}
