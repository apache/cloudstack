package com.cloud.agent.api.storage;

import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.storage.template.TemplateInfo;

public class ListVolumeAnswer extends Answer {
	private String secUrl;
    private Map<Long, TemplateInfo> templateInfos;
	
	public ListVolumeAnswer() {
		
	}
	
	public ListVolumeAnswer(String secUrl, Map<Long, TemplateInfo> templateInfos) {
	    super(null, true, "success");
	    this.setSecUrl(secUrl);
	    this.templateInfos = templateInfos;    
	}
	
	public Map<Long, TemplateInfo> getTemplateInfo() {
	    return templateInfos;
	}

	public void setTemplateInfo(Map<Long, TemplateInfo> templateInfos) {
	    this.templateInfos = templateInfos;
	}

    public void setSecUrl(String secUrl) {
        this.secUrl = secUrl;
    }

    public String getSecUrl() {
        return secUrl;
    }
}
