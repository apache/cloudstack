 /**
 *  Copyright (C) 2011 Citrix.com, Inc.  All rights reserved.
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
package com.cloud.vm.dao;


public class DetailsMask {
    public final static int FULL_DETAILS=0;
    
    public final static int BASIC_DETAILS=1;
    public final static int NICS_DETAILS=2;
    public final static int STATISTICS_DETAILS=4;
    public final static int SECURITY_GROUP_DETAILS=8;
    
    public final static int TEMPLATE_DETAILS=16;
    public final static int SERVICE_OFFERING_DETAILS=32;
    public final static int ISO_DETAILS=64;
    public final static int VOLUME_DETAILS=128;
    
    int _mask;
    
    public DetailsMask(int i){
        _mask = i;
    }
            
    private final String[] strings = new String[] {
        "basic", "nics", "stats", "secgrp", 
        "tmpl", "servoff", "iso", "volume"
    };

    private final int[] values = new int[] {
        1, 2, 4, 8,
        16, 32, 64, 128,
        256, 512, 1024, 2048,
        4096, 8192, 16384, 32768,
        65536, 131072, 262144, 524288
    };

    public int intValue(String str) {
      for (int i = 0; i < strings.length; i++) {
        if (str.equals(strings[i])) {
          return values[i];
        }
      }
      return -1;
    }
    
    public String stringValue(int mvId) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < values.length; i++) {
          if (mvId == values[i]) {
            return strings[i];
          }
          else if ( (mvId & values[i]) == values[i]) {
            buf.append(strings[i]).append("|");
          }
        }
        if (buf.length() == 0) {
          return "";
        }
        return buf.deleteCharAt(buf.length() - 1).toString();
      }
    
    public boolean idValid(int mvId) {
        if (mvId > 128 || mvId < 0) {
          return false;
        }
        return true;
      }
    
    
    public boolean isAllDetails(){
        return (_mask == 0);
    }

    public boolean isBasicDetails(){
        return (_mask & BASIC_DETAILS ) > 0;
    }

    public boolean isNicsDetails(){
        return (_mask & NICS_DETAILS ) > 0;
    }
    
    public boolean isStatsDetails(){
        return (_mask & STATISTICS_DETAILS ) > 0;
    }

    public boolean isSecurityGroupDetails(){
        return (_mask & STATISTICS_DETAILS ) > 0;
    }
   
    public boolean isTemplateDetails(){
        return (_mask & TEMPLATE_DETAILS ) > 0;
    }

    public boolean isServiceOfferingDetails(){
        return (_mask & SERVICE_OFFERING_DETAILS ) > 0;
    }
    
    public boolean isIsoDetails(){
        return (_mask & ISO_DETAILS ) > 0;
    }

    public boolean isVolumeDetails(){
        return (_mask & VOLUME_DETAILS ) > 0;
    }
    
}

    