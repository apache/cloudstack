package org.apache.cloudstack.platform.subsystem.api.storage;

public interface TemplateStrategy {
	TemplateProfile install(TemplateProfile tp);
	TemplateProfile get(long templateId);
	int getDownloadWait();
}
