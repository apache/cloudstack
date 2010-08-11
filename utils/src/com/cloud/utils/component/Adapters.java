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
package com.cloud.utils.component;

import java.util.Enumeration;
import java.util.List;

import com.cloud.utils.EnumerationImpl;

/**
 * Adapters is a simple wrapper around the list of adapters. The reason is
 * because we can not simply return the list of adapters given that we want to
 * do dynamic reloading of adapters. It is guaranteed that the elements of the
 * list will not change during dynamic reloading. Therefore, it is safe to use
 * the iterator even during dynamic reloading.
 * 
 **/
public class Adapters<T extends Adapter> {
    private List<T> _adapters;
    protected String      _name;

    public Adapters(String name, List<T> adapters) {
        _name = name;
        _adapters = adapters;
    }

    /**
     * Get the adapter list name.
     * 
     * @return the name of the list of adapters.
     */
    public String getName() {
        return _name;
    }

    public Enumeration<T> enumeration() {
        return new EnumerationImpl<T>(_adapters.iterator());
    }

    protected List<T> get() {
        return _adapters;
    }
    
    protected void set(List<T> adapters) {
        this._adapters = adapters;
    }

    public boolean isSet() {
        return _adapters.size() != 0;
    }
}
