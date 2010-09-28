/**
 * 
 */
package com.cloud.acl;

import java.security.acl.NotOwnerException;

import com.cloud.domain.PartOf;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.OwnedBy;
import com.cloud.user.User;
import com.cloud.utils.component.Adapter;

/**
 * SecurityChecker checks the ownership and access control to objects within
 * the management stack for users and accounts. 
 */
public interface SecurityChecker extends Adapter {
    /**
     * Checks if the account owns the object.
     * 
     * @param account account to check against.
     * @param object object that the account is trying to access.
     * @return true if access allowed.  false if this adapter cannot authenticate ownership.
     * @throws PermissionDeniedException if this adapter is suppose to authenticate ownership and the check failed.
     */
    boolean checkOwnership(Account account, OwnedBy object) throws NotOwnerException;
    
    /**
     * Checks if the user belongs to an account that owns the object.
     * 
     * @param user user to check against.
     * @param object object that the account is trying to access.
     * @return true if access allowed.  false if this adapter cannot authenticate ownership.
     * @throws PermissionDeniedException if this adapter is suppose to authenticate ownership and the check failed.
     */
    boolean checkOwnership(User user, OwnedBy object) throws NotOwnerException;
    
    /**
     * Checks if the account can access the object.
     * 
     * @param account account to check against.
     * @param object object that the account is trying to access.
     * @return true if access allowed.  false if this adapter cannot provide permission.
     * @throws PermissionDeniedException if this adapter is suppose to authenticate ownership and the check failed.
     */
    boolean checkAccess(Account account, PartOf object) throws PermissionDeniedException;

    /**
     * Checks if the user belongs to an account that can access the object.
     * 
     * @param user user to check against.
     * @param object object that the account is trying to access.
     * @return true if access allowed.  false if this adapter cannot authenticate ownership.
     * @throws PermissionDeniedException if this adapter is suppose to authenticate ownership and the check failed.
     */
    boolean checkAccess(User user, PartOf object) throws PermissionDeniedException;

// We should be able to use this method to check against commands.  For example, we can
// annotate the command with access annotations and this method can use it to extract
// OwnedBy and PartOf interfaces on the object and use it to verify against a user.
// I leave this empty for now so Kris and the API team can see if it is useful.
//    boolean checkAuthorization(User user, Command cmd) throws PermissionDeniedException;

}
