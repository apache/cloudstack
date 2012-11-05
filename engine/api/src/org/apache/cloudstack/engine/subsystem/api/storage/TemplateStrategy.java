package org.apache.cloudstack.engine.subsystem.api.storage;

import com.cloud.agent.api.storage.DownloadCommand.Proxy;

public interface TemplateStrategy {
	TemplateProfile install(TemplateProfile tp);
	TemplateProfile get(long templateId);
	TemplateProfile register(TemplateProfile tp);
	boolean canRegister(long templateId);
	int getDownloadWait();
	long getMaxTemplateSizeInBytes();
	Proxy getHttpProxy();
}
