package com.cloud.api;

public abstract class BaseAsyncCreateCmd extends BaseAsyncCmd {
    private Long id;

    public abstract Object createObject();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
