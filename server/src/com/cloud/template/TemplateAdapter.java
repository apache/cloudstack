package com.cloud.template;

import java.util.Map;

import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.RegisterIsoCmd;
import com.cloud.api.commands.RegisterTemplateCmd;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;

public interface TemplateAdapter extends Adapter {
	public static class TemplateAdapterType {
		String _name;
		
		public static final TemplateAdapterType Hypervisor = new TemplateAdapterType("HypervisorAdapter");
		public static final TemplateAdapterType BareMetal = new TemplateAdapterType("BareMetalAdapter");
		
		public TemplateAdapterType(String name) {
			_name = name;
		}
		
		public String getName() {
			return _name;
		}
	}
	
	public TemplateProfile prepare(RegisterTemplateCmd cmd) throws ResourceAllocationException;

	public TemplateProfile prepare(RegisterIsoCmd cmd) throws ResourceAllocationException;

	public VMTemplateVO create(TemplateProfile profile);

	public TemplateProfile prepareDelete(DeleteTemplateCmd cmd);

	public TemplateProfile prepareDelete(DeleteIsoCmd cmd);

	public boolean delete(TemplateProfile profile);
	
	public TemplateProfile prepare(boolean isIso, Long userId, String name, String displayText, Integer bits,
            Boolean passwordEnabled, Boolean requiresHVM, String url, Boolean isPublic, Boolean featured,
            Boolean isExtractable, String format, Long guestOSId, Long zoneId, HypervisorType hypervisorType,
            String accountName, Long domainId, String chksum, Boolean bootable, Map details) throws ResourceAllocationException;
	
    public TemplateProfile prepare(boolean isIso, long userId, String name, String displayText, Integer bits,
            Boolean passwordEnabled, Boolean requiresHVM, String url, Boolean isPublic, Boolean featured,
            Boolean isExtractable, String format, Long guestOSId, Long zoneId, HypervisorType hypervisorType,
            String chksum, Boolean bootable, String templateTag, Account templateOwner, Map details, Boolean sshKeyEnabled) throws ResourceAllocationException;	
}
