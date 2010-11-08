package com.cloud.api;


public abstract class BaseListCmd extends BaseCmd {

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
        Long pageSize = null;
        Integer pageSizeInt = getPageSize();
        if (pageSizeInt != null) {
            if (pageSizeInt.longValue() == -1) {
                pageSize = null;
            } else {
                pageSize = pageSizeInt.longValue();
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
}
