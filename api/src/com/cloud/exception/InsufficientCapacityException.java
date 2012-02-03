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
package com.cloud.exception;

import com.cloud.utils.SerialVersionUID;

/**
 * Generic parent exception class for capacity being reached.
 *
 */
public abstract class InsufficientCapacityException extends CloudException {
    private static final long serialVersionUID = SerialVersionUID.InsufficientCapacityException;

    Long id;
    Class<?> scope;

    protected InsufficientCapacityException() {
        super();
    }

    public InsufficientCapacityException(String msg, Class<?> scope, Long id) {
        super(msg);
        this.scope = scope;
        this.id = id;
    }

    /**
     * @return scope where we are insufficient.  The possible classes are
     *         Host, StoragePool, Cluster, Pod, DataCenter, NetworkConfiguration.
     */
    public Class<?> getScope() {
        return scope;
    }

    /**
     * @return the id of the object that it is insufficient in.  Note that this method is
     *         marked such that if the id is not set, then it will throw NullPointerException.
     *         This is intended as you should check to see if the Scope is present before 
     *         accessing this method.
     */
    public long getId() {
        return id;
    }

    @Override public String toString() {
        String str = super.toString();
        return str + "Scope=" + scope + "; id=" + id;
    }
}
