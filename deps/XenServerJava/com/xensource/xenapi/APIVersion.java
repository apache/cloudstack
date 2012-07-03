/*
 * Copyright (c) 2007-2009 Citrix Systems, Inc.
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

public enum APIVersion
{
    API_1_1, API_1_2, API_1_3, API_1_4, API_1_5, API_1_6, API_1_7, UNKNOWN;

    public static APIVersion latest()
    {
        return API_1_7;
    }

    public static APIVersion fromMajorMinor(long major, long minor)
    {
        if (major == 1 && minor == 7)
        {
            return API_1_7;
        }
        else if (major == 1 && minor == 6)
        {
            return API_1_6;
        }
        else if (major == 1 && minor == 5)
        {
            return API_1_5;
        }
        else if (major == 1 && minor == 4)
        {
            return API_1_4;
        }
        else if (major == 1 && minor == 3)
        {
            return API_1_3;
        }
        else if (major == 1 && minor == 2)
        {
            return API_1_2;
        }
        else if (major == 1 && minor == 1)
        {
            return API_1_1;
        }
        else
        {
            return UNKNOWN;
        }
    }

    @Override
    public String toString()
    {
        switch (this)
        {
        case API_1_1:
            return "1.1";
        case API_1_2:
            return "1.2";
        case API_1_3:
            return "1.3";
        case API_1_4:
            return "1.4";
        case API_1_5:
            return "1.5";
        case API_1_6:
            return "1.6";
        case API_1_7:
            return "1.7";
        default:
            return "Unknown";
        }
    }
}
