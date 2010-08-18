/**
 * 
 */
package com.cloud.user;

/**
 * OwnedBy must be inheritted by all objects that can be owned by an account.
 */
public interface OwnedBy {
    /**
     * @return account id that owns this object.
     */
    long getAccountId();
}
