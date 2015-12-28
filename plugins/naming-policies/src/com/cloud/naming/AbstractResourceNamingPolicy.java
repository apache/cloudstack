package com.cloud.naming;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Implements the UUID generation and verification methods used by all
 * default naming policies.

 * @author nera
 */
public abstract class AbstractResourceNamingPolicy extends ComponentLifecycleBase {

    @Inject
    EntityManager _entityMgr;
    @Inject
    AccountManager _accountMgr;

    //TODO - Make this configurable.
    private static final int UUID_RETRY = 3;
    protected static final String SEPARATOR = "-";


    protected <T> void checkUuid(String uuid, Class<T> entityType) {

        if (uuid == null)
            return;

        Account caller = CallContext.current().getCallingAccount();

        // Only admin and system allowed to do this
        if (!(caller.getId() == Account.ACCOUNT_ID_SYSTEM || _accountMgr.isRootAdmin(caller.getId()))) {
            throw new PermissionDeniedException("Please check your permissions, you are not allowed to create/update custom id");
        }

        // check format
        if (!isUuidFormat(uuid))
            throw new InvalidParameterValueException("UUID: " + uuid + " doesn't follow the UUID format");

        // check unique
        if (!isUuidUnique(entityType, uuid))
            throw new InvalidParameterValueException("UUID: " + uuid + " already exists so can't create/update with custom id");
    }


    protected <T> boolean checkUuidSimple(String uuid, Class<T> entityType ) {

        if (uuid == null || !isUuidFormat(uuid) || !isUuidUnique(entityType, uuid))
            return false;

        return true;

    }



    protected <T> String generateUuid(Class<T> entityType, Long vmId, Long userId, String customId) {

        if (customId == null) { // if no customid is passed then generate it.
            int retry = UUID_RETRY;
            while (retry-- != 0) {  // there might be collision so retry
                String uuid = UUID.randomUUID().toString();
                if (isUuidUnique(entityType, uuid))
                    return uuid;
            }

            throw new CloudRuntimeException("Unable to generate a unique uuid, please try again");
        } else {
            checkUuid(customId, entityType);
            return customId;
        }
    }

    protected boolean isUuidFormat(String uuid) {

        // Match against UUID regex to check if input is uuid string
        boolean isUuid = uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        return isUuid;
    }

    private <T> boolean isUuidUnique(Class<T> entityType, String uuid) {

        T obj = _entityMgr.findByUuid(entityType, uuid);
        if (obj != null)
            return false;
        else
            return true;
    }


}
