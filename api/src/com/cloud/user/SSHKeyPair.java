/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.user;

import com.cloud.acl.ControlledEntity;

public interface SSHKeyPair extends ControlledEntity {

    /**
     * @return The id of the key pair.
     */
    public long getId();

    /**
     * @return The given name of the key pair.
     */
    public String getName();

    /**
     * @return The finger print of the public key.
     */
    public String getFingerprint();

    /**
     * @return The public key of the key pair.
     */
    public String getPublicKey();

    /**
     * @return The private key of the key pair.
     */
    public String getPrivateKey();

}
