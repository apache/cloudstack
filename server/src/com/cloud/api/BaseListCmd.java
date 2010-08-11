package com.cloud.api;


public abstract class BaseListCmd extends BaseCmd {
    /////////////////////////////////////////////////////
    /////////// BaseList API parameters /////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="keyword", type=CommandType.STRING)
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
}
