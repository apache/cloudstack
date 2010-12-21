/*
 * Copyright (c) 2006-2010 Citrix Systems, Inc.
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

import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VersionException;
import com.xensource.xenapi.Types.XenAPIException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Data sources for logging in RRDs
 *
 * @author Citrix Systems, Inc.
 */
public class DataSource extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    DataSource(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a DataSource, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof DataSource)
        {
            DataSource other = (DataSource) obj;
            return other.ref.equals(this.ref);
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return ref.hashCode();
    }

    /**
     * Represents all the fields in a DataSource
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "enabled", this.enabled);
            print.printf("%1$20s: %2$s\n", "standard", this.standard);
            print.printf("%1$20s: %2$s\n", "units", this.units);
            print.printf("%1$20s: %2$s\n", "min", this.min);
            print.printf("%1$20s: %2$s\n", "max", this.max);
            print.printf("%1$20s: %2$s\n", "value", this.value);
            return writer.toString();
        }

        /**
         * Convert a data_source.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("name_label", this.nameLabel == null ? "" : this.nameLabel);
            map.put("name_description", this.nameDescription == null ? "" : this.nameDescription);
            map.put("enabled", this.enabled == null ? false : this.enabled);
            map.put("standard", this.standard == null ? false : this.standard);
            map.put("units", this.units == null ? "" : this.units);
            map.put("min", this.min == null ? 0.0 : this.min);
            map.put("max", this.max == null ? 0.0 : this.max);
            map.put("value", this.value == null ? 0.0 : this.value);
            return map;
        }

        /**
         * a human-readable name
         */
        public String nameLabel;
        /**
         * a notes field containg human-readable description
         */
        public String nameDescription;
        /**
         * true if the data source is being logged
         */
        public Boolean enabled;
        /**
         * true if the data source is enabled by default. Non-default data sources cannot be disabled
         */
        public Boolean standard;
        /**
         * the units of the value
         */
        public String units;
        /**
         * the minimum value of the data source
         */
        public Double min;
        /**
         * the maximum value of the data source
         */
        public Double max;
        /**
         * current value of the data source
         */
        public Double value;
    }

}