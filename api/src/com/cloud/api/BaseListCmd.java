package com.cloud.api;

import com.cloud.async.AsyncJob;
import com.cloud.exception.InvalidParameterValueException;

public abstract class BaseListCmd extends BaseCmd {

    private static Long MAX_PAGESIZE = 500L;

    // ///////////////////////////////////////////////////
    // ///////// BaseList API parameters /////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = "keyword", type = CommandType.STRING, description = "List by keyword")
    private String keyword;

    // FIXME: Need to be able to specify next/prev/first/last, so Integer might not be right
    @Parameter(name = ApiConstants.PAGE, type = CommandType.INTEGER)
    private Integer page;

    @Parameter(name = ApiConstants.PAGE_SIZE, type = CommandType.INTEGER)
    private Integer pageSize;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getKeyword() {
        return keyword;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        if (pageSize != null && pageSize.longValue() > MAX_PAGESIZE.longValue()) {
            throw new InvalidParameterValueException("Page size can't exceed max allowed page size value: " + MAX_PAGESIZE.longValue());
        }
        
        return pageSize;
    }

    static void configure() {
        MAX_PAGESIZE = _configService.getDefaultPageSize();
    }

    @Override
    public long getEntityOwnerId() {
        // no owner is needed for list command
        return 0;
    }

    public Long getPageSizeVal() {
        Long pageSize = MAX_PAGESIZE;
        Integer pageSizeInt = getPageSize();
        if (pageSizeInt != null && pageSizeInt.intValue() != -1) {
            pageSize = pageSizeInt.longValue();
        }
        return pageSize;
    }

    public Long getStartIndex() {
        Long startIndex = Long.valueOf(0);
        Long pageSizeVal = getPageSizeVal();

        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeVal * (pageNum - 1));
            }
        }
        return startIndex;
    }

    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.None;
    }
}
