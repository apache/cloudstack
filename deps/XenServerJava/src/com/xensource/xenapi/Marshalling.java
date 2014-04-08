/*
 * Copyright (c) Citrix Systems, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   1) Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 * 
 *   2) Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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
