package com.cloud.async.executor;

public class ExtractTemplateParam {
	
	private long userId;
	private long templateId;
	private Long zoneId;
	private long eventId;
	private String url;

	public ExtractTemplateParam() {
	}

	public ExtractTemplateParam(long userId, long templateId, Long zoneId, long eventId, String url) {
		this.userId = userId;
		this.templateId = templateId;
		this.zoneId = zoneId;
		this.eventId = eventId;
		this.url = url;
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

}
