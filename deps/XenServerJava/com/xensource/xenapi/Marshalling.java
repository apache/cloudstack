/*
 * Copyright (c) 2007-2008 Citrix Systems, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of version 2 of the GNU General Public License as published
 * by the Free Software Foundation, with the additional linking exception as
 * follows:
 * 
 *   Linking this library statically or dynamically with other modules is
 *   making a combined work based on this library. Thus, the terms and
 *   conditions of the GNU General Public License cover the whole combination.
 * 
 *   As a special exception, the copyright holders of this library give you
 *   permission to link this library with independent modules to produce an
 *   executable, regardless of the license terms of these independent modules,
 *   and to copy and distribute the resulting executable under terms of your
 *   choice, provided that you also meet, for each linked independent module,
 *   the terms and conditions of the license of that module. An independent
 *   module is a module which is not derived from or based on this library. If
 *   you modify this library, you may extend this exception to your version of
 *   the library, but you are not obligated to do so. If you do not wish to do
 *   so, delete this exception statement from your version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.xensource.xenapi;

import java.util.*;

/**
 * Marshalls Java types onto the wire.
 * Does not cope with records.  Use individual record.toMap()
 */
public final class Marshalling {
    /**
     * Converts Integers to Strings
     * and Sets to Lists recursively.
     */
    public static Object toXMLRPC(Object o) {
        if (o instanceof String ||
            o instanceof Boolean ||
            o instanceof Double ||
            o instanceof Date) {
            return o;
	} else if (o instanceof Long) {
	    return o.toString();
        } else if (o instanceof Map) {
            Map<Object, Object> result = new HashMap<Object, Object>();
            Map m = (Map)o;
            for (Object k : m.keySet())
            {
                result.put(toXMLRPC(k), toXMLRPC(m.get(k)));
            }
            return result;
        } else if (o instanceof Set) {
            List<Object> result = new ArrayList<Object>();
            for (Object e : ((Set)o))
            {
                result.add(toXMLRPC(e));
            }
            return result;
	} else if (o instanceof XenAPIObject) {
	    return ((XenAPIObject) o).toWireString();
	} else if (o instanceof Enum) {
	    return o.toString();
	}else if (o == null){
	    return "";
        } else {
		throw new RuntimeException ("=============don't know how to marshall:({[" + o + "]})");
        }
    }
}
