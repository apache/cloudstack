package com.cloud.api;

public abstract class BaseAsyncCreateCmd extends BaseAsyncCmd {
    @Parameter(name="portforwardingserviceid")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
