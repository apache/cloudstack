// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.template;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ExtractTemplateCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Grouping;
import com.cloud.projects.ProjectManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.storage.GuestOS;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

public abstract class TemplateAdapterBase extends AdapterBase implements TemplateAdapter {
	private final static Logger s_logger = Logger.getLogger(TemplateAdapterBase.class);
	protected @Inject DomainDao _domainDao;
	protected @Inject AccountDao _accountDao;
	protected @Inject ConfigurationDao _configDao;
	protected @Inject UserDao _userDao;
	protected @Inject AccountManager _accountMgr;
	protected @Inject DataCenterDao _dcDao;
	protected @Inject VMTemplateDao _tmpltDao;
	protected @Inject TemplateDataStoreDao _tmpltStoreDao;
	protected @Inject VMTemplateZoneDao _tmpltZoneDao;
	protected @Inject UsageEventDao _usageEventDao;
	protected @Inject HostDao _hostDao;
	protected @Inject UserVmDao _userVmDao;
	protected @Inject GuestOSHypervisorDao _osHyperDao;
	protected @Inject ResourceLimitService _resourceLimitMgr;
	protected @Inject DataStoreManager storeMgr;
	@Inject TemplateManager templateMgr;
    @Inject ConfigurationServer _configServer;
    @Inject ProjectManager _projectMgr;
	
	@Override
	public boolean stop() {
		return true;
	}

	private static boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	    		(accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}

	@Override
    public TemplateProfile prepare(boolean isIso, Long userId, String name, String displayText, Integer bits,
            Boolean passwordEnabled, Boolean requiresHVM, String url, Boolean isPublic, Boolean featured,
            Boolean isExtractable, String format, Long guestOSId, Long zoneId, HypervisorType hypervisorType,
            String accountName, Long domainId, String chksum, Boolean bootable, Map details) throws ResourceAllocationException {
	    return prepare(isIso, userId, name, displayText, bits, passwordEnabled, requiresHVM, url, isPublic, featured, isExtractable, format, guestOSId, zoneId, hypervisorType,
	            chksum, bootable, null, null, details, false, null, false, TemplateType.USER);
	}

	@Override
    public TemplateProfile prepare(boolean isIso, long userId, String name, String displayText, Integer bits,
			Boolean passwordEnabled, Boolean requiresHVM, String url, Boolean isPublic, Boolean featured,
			Boolean isExtractable, String format, Long guestOSId, Long zoneId, HypervisorType hypervisorType,
			String chksum, Boolean bootable, String templateTag, Account templateOwner, Map details, Boolean sshkeyEnabled,
			String imageStoreUuid, Boolean isDynamicallyScalable, TemplateType templateType) throws ResourceAllocationException {
		//Long accountId = null;
		// parameters verification

		if (isPublic == null) {
			isPublic = Boolean.FALSE;
		}

		if (zoneId.longValue() == -1) {
			zoneId = null;
		}

		if (isIso) {
	        if (bootable == null) {
	        	bootable = Boolean.TRUE;
	        }
	        GuestOS noneGuestOs = ApiDBUtils.findGuestOSByDisplayName(ApiConstants.ISO_GUEST_OS_NONE);
	        if ((guestOSId == null || guestOSId == noneGuestOs.getId()) && bootable == true){
	        	throw new InvalidParameterValueException("Please pass a valid GuestOS Id");
	        }
	        if (bootable == false){
	        	guestOSId = noneGuestOs.getId(); //Guest os id of None.
	        }
		} else {
			if (bits == null) {
				bits = Integer.valueOf(64);
			}
			if (passwordEnabled == null) {
				passwordEnabled = false;
			}
			if (requiresHVM == null) {
				requiresHVM = true;
			}
		}

        if (isExtractable == null) {
            isExtractable = Boolean.FALSE;
        }
        if (sshkeyEnabled == null) {
            sshkeyEnabled = Boolean.FALSE;
        }

		boolean isAdmin = _accountDao.findById(templateOwner.getId()).getType() == Account.ACCOUNT_TYPE_ADMIN;

		if (!isAdmin && zoneId == null) {
			throw new InvalidParameterValueException("Please specify a valid zone Id.");
		}

		if (url.toLowerCase().contains("file://")) {
			throw new InvalidParameterValueException("File:// type urls are currently unsupported");
		}

		// check whether owner can create public templates
		boolean allowPublicUserTemplates = Boolean.parseBoolean(_configServer.getConfigValue(Config.AllowPublicUserTemplates.key(), Config.ConfigurationParameterScope.account.toString(), templateOwner.getId()));
		if (!isAdmin && !allowPublicUserTemplates && isPublic) {
			throw new InvalidParameterValueException("Only private templates/ISO can be created.");
		}

		if (!isAdmin || featured == null) {
			featured = Boolean.FALSE;
		}

		ImageFormat imgfmt = ImageFormat.valueOf(format.toUpperCase());
		if (imgfmt == null) {
			throw new IllegalArgumentException("Image format is incorrect " + format + ". Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
		}

        // Check that the resource limit for templates/ISOs won't be exceeded
        UserVO user = _userDao.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unable to find user with id " + userId);
        }

        _resourceLimitMgr.checkResourceLimit(templateOwner, ResourceType.template);

        if (templateOwner.getType() != Account.ACCOUNT_TYPE_ADMIN && zoneId == null) {
        	throw new IllegalArgumentException("Only admins can create templates in all zones");
        }

        // If a zoneId is specified, make sure it is valid
        if (zoneId != null) {
        	DataCenterVO zone = _dcDao.findById(zoneId);
        	if (zone == null) {
        		throw new IllegalArgumentException("Please specify a valid zone.");
        	}
    		Account caller = CallContext.current().getCallingAccount();
    		if(Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())){
    			throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: "+ zoneId );
    		}
        }

        List<VMTemplateVO> systemvmTmplts = _tmpltDao.listAllSystemVMTemplates();
        for (VMTemplateVO template : systemvmTmplts) {
            if (template.getName().equalsIgnoreCase(name) || template.getDisplayText().equalsIgnoreCase(displayText)) {
                throw new IllegalArgumentException("Cannot use reserved names for templates");
            }
        }

        Long id = _tmpltDao.getNextInSequence(Long.class, "id");
        CallContext.current().setEventDetails("Id: " +id+ " name: " + name);
        return new TemplateProfile(id, userId, name, displayText, bits, passwordEnabled, requiresHVM, url, isPublic,
                featured, isExtractable, imgfmt, guestOSId, zoneId, hypervisorType, templateOwner.getAccountName(), templateOwner.getDomainId(),
                templateOwner.getAccountId(), chksum, bootable, templateTag, details, sshkeyEnabled, null, isDynamicallyScalable, templateType);

	}

	@Override
	public TemplateProfile prepare(RegisterTemplateCmd cmd) throws ResourceAllocationException {
	    //check if the caller can operate with the template owner
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.getAccount(cmd.getEntityOwnerId());
        _accountMgr.checkAccess(caller, null, true, owner);

    boolean isRouting = (cmd.isRoutingType() == null) ? false : cmd.isRoutingType();

    return prepare(false, CallContext.current().getCallingUserId(), cmd.getTemplateName(), cmd.getDisplayText(),
                cmd.getBits(), cmd.isPasswordEnabled(), cmd.getRequiresHvm(), cmd.getUrl(), cmd.isPublic(), cmd.isFeatured(),
                cmd.isExtractable(), cmd.getFormat(), cmd.getOsTypeId(), cmd.getZoneId(), HypervisorType.getType(cmd.getHypervisor()),
                cmd.getChecksum(), true, cmd.getTemplateTag(), owner, cmd.getDetails(), cmd.isSshKeyEnabled(), null, cmd.isDynamicallyScalable(),
                isRouting ? TemplateType.ROUTING : TemplateType.USER);

	}

	@Override
    public TemplateProfile prepare(RegisterIsoCmd cmd) throws ResourceAllocationException {
	    //check if the caller can operate with the template owner
	    Account caller = CallContext.current().getCallingAccount();
	    Account owner = _accountMgr.getAccount(cmd.getEntityOwnerId());
	    _accountMgr.checkAccess(caller, null, true, owner);

        return prepare(true, CallContext.current().getCallingUserId(), cmd.getIsoName(), cmd.getDisplayText(), 64, false,
                true, cmd.getUrl(), cmd.isPublic(), cmd.isFeatured(), cmd.isExtractable(), ImageFormat.ISO.toString(), cmd.getOsTypeId(),
                cmd.getZoneId(), HypervisorType.None, cmd.getChecksum(), cmd.isBootable(), null, owner, null, false, cmd.getImageStoreUuid(), cmd.isDynamicallyScalable(),
                TemplateType.USER);
	}

	protected VMTemplateVO persistTemplate(TemplateProfile profile) {
		Long zoneId = profile.getZoneId();
		VMTemplateVO template = new VMTemplateVO(profile.getTemplateId(), profile.getName(), profile.getFormat(), profile.getIsPublic(),
				profile.getFeatured(), profile.getIsExtractable(), profile.getTemplateType(), profile.getUrl(), profile.getRequiresHVM(),
				profile.getBits(), profile.getAccountId(), profile.getCheckSum(), profile.getDisplayText(),
				profile.getPasswordEnabled(), profile.getGuestOsId(), profile.getBootable(), profile.getHypervisorType(), profile.getTemplateTag(),
				profile.getDetails(), profile.getSshKeyEnabled(), profile.IsDynamicallyScalable());


		if (zoneId == null || zoneId.longValue() == -1) {
            List<DataCenterVO> dcs = _dcDao.listAll();

            if (dcs.isEmpty()) {
            	throw new CloudRuntimeException("No zones are present in the system, can't add template");
            }

            template.setCrossZones(true);
        	for (DataCenterVO dc: dcs) {
    			_tmpltDao.addTemplateToZone(template, dc.getId());
    		}

        } else {
			_tmpltDao.addTemplateToZone(template, zoneId);
        }
		return _tmpltDao.findById(template.getId());
	}


	private Long accountAndUserValidation(Account account, long userId, UserVmVO vmInstanceCheck, VMTemplateVO template, String msg)
			throws PermissionDeniedException {

		if (account != null) {
			if (!isAdmin(account.getType())) {
				if ((vmInstanceCheck != null) && (account.getId() != vmInstanceCheck.getAccountId())) {
					throw new PermissionDeniedException(msg + ". Permission denied.");
				}

				if ((template != null)
						&& (!template.isPublicTemplate() && (account.getId() != template.getAccountId()) && (template.getTemplateType() != TemplateType.PERHOST))) {
				    //special handling for the project case
				    Account owner = _accountMgr.getAccount(template.getAccountId());
				    if (owner.getType() == Account.ACCOUNT_TYPE_PROJECT) {
				        if (!_projectMgr.canAccessProjectAccount(account, owner.getId())) {
	                        throw new PermissionDeniedException(msg + ". Permission denied. The caller can't access project's template");
				        }
		            } else {
		                throw new PermissionDeniedException(msg + ". Permission denied.");
		            }
				}
			} else {
				if ((vmInstanceCheck != null) && !_domainDao.isChildDomain(account.getDomainId(), vmInstanceCheck.getDomainId())) {
					throw new PermissionDeniedException(msg + ". Permission denied.");
				}
				// FIXME: if template/ISO owner is null we probably need to
				// throw some kind of exception

				if (template != null) {
					Account templateOwner = _accountDao.findById(template.getAccountId());
					if ((templateOwner != null) && !_domainDao.isChildDomain(account.getDomainId(), templateOwner.getDomainId())) {
						throw new PermissionDeniedException(msg + ". Permission denied.");
					}
				}
			}
		}

		return userId;
	}

	@Override
    public TemplateProfile prepareDelete(DeleteTemplateCmd cmd) {
		Long templateId = cmd.getId();
		Long userId = CallContext.current().getCallingUserId();
		Account account = CallContext.current().getCallingAccount();
		Long zoneId = cmd.getZoneId();

		VMTemplateVO template = _tmpltDao.findById(templateId.longValue());
		if (template == null) {
			throw new InvalidParameterValueException("unable to find template with id " + templateId);
		}

		userId = accountAndUserValidation(account, userId, null, template, "Unable to delete template ");

		UserVO user = _userDao.findById(userId);
		if (user == null) {
			throw new InvalidParameterValueException("Please specify a valid user.");
		}

		if (template.getFormat() == ImageFormat.ISO) {
			throw new InvalidParameterValueException("Please specify a valid template.");
		}

		return new TemplateProfile(userId, template, zoneId);
	}

	public TemplateProfile prepareExtractTemplate(ExtractTemplateCmd cmd) {
		Long templateId = cmd.getId();
		Long userId = CallContext.current().getCallingUserId();
	        Long zoneId = cmd.getZoneId();

		VMTemplateVO template = _tmpltDao.findById(templateId.longValue());
		if (template == null) {
			throw new InvalidParameterValueException("unable to find template with id " + templateId);
		}
		return new TemplateProfile(userId, template, zoneId);
	}

	public TemplateProfile prepareDelete(DeleteIsoCmd cmd) {
		Long templateId = cmd.getId();
        Long userId = CallContext.current().getCallingUserId();
        Account account = CallContext.current().getCallingAccount();
        Long zoneId = cmd.getZoneId();

        VMTemplateVO template = _tmpltDao.findById(templateId.longValue());
        if (template == null) {
            throw new InvalidParameterValueException("unable to find iso with id " + templateId);
        }

        userId = accountAndUserValidation(account, userId, null, template, "Unable to delete iso " );

    	UserVO user = _userDao.findById(userId);
    	if (user == null) {
    		throw new InvalidParameterValueException("Please specify a valid user.");
    	}

    	if (template.getFormat() != ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid iso.");
    	}

    	return new TemplateProfile(userId, template, zoneId);
	}

	@Override
    abstract public VMTemplateVO create(TemplateProfile profile);
	@Override
    abstract public boolean delete(TemplateProfile profile);
}
