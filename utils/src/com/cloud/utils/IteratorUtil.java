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

public class IteratorUtil {
	public static <T> Iterable<T> enumerationAsIterable(final Enumeration<T> e) {
		return new Iterable<T> () {
			public Iterator<T> iterator() {
			    return new Iterator<T>() {
			    	public boolean hasNext() {
			    		return e.hasMoreElements();
			    	}

			    	public T next() {
			    		return e.nextElement();
			    	}

			    	public void remove() {
			    		throw new UnsupportedOperationException();
			    	}
			    };
			}
		};
	}
	
	public static <T> Enumeration<T> iteratorAsEnumeration(final Iterator<T> it) {
		return new Enumeration<T>() {
		    public boolean hasMoreElements() {
		        return it.hasNext();
		    }

		    public T nextElement() {
		        return it.next();
		    }
		};
	}
}
