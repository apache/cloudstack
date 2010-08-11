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
package com.cloud.utils;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * This is an implementation of the stupid Enumeration interface which is practically useless but is being used by
 * ServletRequest and ServletResponse to return a set. And it does allow for modification.
 * 
 * Why the heck didn't Iterator extend from Enumeration, I will probably never know. Tell me Lee Boyton!
 **/
public class EnumerationImpl<T> implements Enumeration<T> {
    Iterator<T> _it;

    // Can't use this.
    protected EnumerationImpl() {
    }

    public EnumerationImpl(Iterator<T> it) {
        _it = it;
    }

    public boolean hasMoreElements() {
        return _it.hasNext();
    }

    public T nextElement() {
        return _it.next();
    }
}
