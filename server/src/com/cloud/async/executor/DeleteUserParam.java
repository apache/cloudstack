package com.cloud.async.executor;

public class DeleteUserParam {

    private Long userId;
    private Long eventId;

    public DeleteUserParam() {}

    public DeleteUserParam(Long userId, Long eventId) {
        this.userId = userId;
        this.eventId = eventId;

    }

    public Long getUserId() {
        return userId;
    }

    public Long getEventId() {
        return eventId;
    }

    
}
