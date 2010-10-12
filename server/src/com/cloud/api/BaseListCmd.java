package com.cloud.api;


public abstract class BaseListCmd extends BaseCmd {
    private static final long DEFAULT_PAGE_SIZE = 50;

    /////////////////////////////////////////////////////
    /////////// BaseList API parameters /////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="keyword", type=CommandType.STRING, description="List by keyword")
    private String keyword;

    // FIXME:  Need to be able to specify next/prev/first/last, so Integer might not be right
    @Parameter(name="page", type=CommandType.INTEGER)
    private Integer page;

    @Parameter(name="pagesize", type=CommandType.INTEGER)
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

    public Long getPageSizeVal() {
        Long pageSize = DEFAULT_PAGE_SIZE;
        Integer pageSizeInt = getPageSize();
        if (pageSizeInt != null) {
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
                startIndex = Long.valueOf(pageSizeVal * (pageNum-1));
            }
        }
        return startIndex;
    }
}
