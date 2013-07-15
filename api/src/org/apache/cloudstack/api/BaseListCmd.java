// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api;

import com.cloud.exception.InvalidParameterValueException;

public abstract class BaseListCmd extends BaseCmd {

    private static Long MAX_PAGESIZE = null;
    public static Long PAGESIZE_UNLIMITED = -1L;

    // ///////////////////////////////////////////////////
    // ///////// BaseList API parameters /////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.KEYWORD, type = CommandType.STRING, description = "List by keyword")
    private String keyword;

    // FIXME: Need to be able to specify next/prev/first/last, so Integer might not be right
    @Parameter(name = ApiConstants.PAGE, type = CommandType.INTEGER)
    private Integer page;

    @Parameter(name = ApiConstants.PAGE_SIZE, type = CommandType.INTEGER)
    private Integer pageSize;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public BaseListCmd() {
    }
    
    public String getKeyword() {
        return keyword;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        if (pageSize != null && MAX_PAGESIZE.longValue() != PAGESIZE_UNLIMITED && pageSize.longValue() > MAX_PAGESIZE.longValue()) {
            throw new InvalidParameterValueException("Page size can't exceed max allowed page size value: " + MAX_PAGESIZE.longValue());
        }

        if (pageSize != null && pageSize.longValue() == PAGESIZE_UNLIMITED && page != null) {
            throw new InvalidParameterValueException("Can't specify page parameter when pagesize is -1 (Unlimited)");
        }

        return pageSize;
    }

    public void configure() {
    	if(MAX_PAGESIZE == null) {
	        if (_configService.getDefaultPageSize().longValue() != PAGESIZE_UNLIMITED) {
	            MAX_PAGESIZE = _configService.getDefaultPageSize();
	        } else {
	        	MAX_PAGESIZE = PAGESIZE_UNLIMITED;
	        }
    	}
    }

    @Override
    public long getEntityOwnerId() {
        // no owner is needed for list command
        return 0;
    }

    public Long getPageSizeVal() {
        Long defaultPageSize = MAX_PAGESIZE;
        Integer pageSizeInt = getPageSize();
        if (pageSizeInt != null) {
            defaultPageSize = pageSizeInt.longValue();
        }
        if (defaultPageSize.longValue() == PAGESIZE_UNLIMITED) {
            defaultPageSize = null;
        }
        
        return defaultPageSize;
    }

    public Long getStartIndex() {
        Long startIndex = Long.valueOf(0);
        Long pageSizeVal = getPageSizeVal();

        if (pageSizeVal == null) {
            startIndex = null;
        } else if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeVal * (pageNum - 1));
            }
        }
        return startIndex;
    }

    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.None;
    }
}
