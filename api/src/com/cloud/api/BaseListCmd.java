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

package com.cloud.api;

import com.cloud.async.AsyncJob;
import com.cloud.exception.InvalidParameterValueException;


public abstract class BaseListCmd extends BaseCmd {

    private static Long MAX_PAGESIZE = 500L;

	/////////////////////////////////////////////////////
    /////////// BaseList API parameters /////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="keyword", type=CommandType.STRING, description="List by keyword")
    private String keyword;

    // FIXME:  Need to be able to specify next/prev/first/last, so Integer might not be right
    @Parameter(name=ApiConstants.PAGE, type=CommandType.INTEGER)
    private Integer page;

    @Parameter(name=ApiConstants.PAGE_SIZE, type=CommandType.INTEGER)
    private Integer pageSize;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getKeyword() {
        return keyword;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        return pageSize;
    }
    
    
    static void configure() {
        MAX_PAGESIZE = _configService.getDefaultPageSize();
    }
    
    @Override
    public long getEntityOwnerId() {
        //no owner is needed for list command
        return 0;
    }

    public Long getPageSizeVal() {
        Long pageSize = null;
        Integer pageSizeInt = getPageSize();
        if (pageSizeInt != null) {
        	pageSize = pageSizeInt.longValue();
            if (pageSize == -1) {
                pageSize = null;
            } else if (pageSize > MAX_PAGESIZE){//FIX ME - have a validator and do this.                
                throw new InvalidParameterValueException("The parameter " + ApiConstants.PAGE_SIZE + " exceeded its max value - " + MAX_PAGESIZE);
            }
        } 
        return pageSize;
    }

    public Long getStartIndex() {
        Long startIndex = Long.valueOf(0);
        Long pageSizeVal = getPageSizeVal();
        if (pageSizeVal == null) {
            return null; // there's no limit, so start index is irrelevant
        }

        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeVal * (pageNum-1));
            }
        }
        return startIndex;
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.None;
    }
}
