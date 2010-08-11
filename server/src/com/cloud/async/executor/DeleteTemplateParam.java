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

package com.cloud.async.executor;

public class DeleteTemplateParam {
	
	private long userId;
	private long templateId;
	private Long zoneId;
	private long eventId;

	public DeleteTemplateParam() {
	}

	public DeleteTemplateParam(long userId, long templateId, Long zoneId, long eventId) {
		this.userId = userId;
		this.templateId = templateId;
		this.zoneId = zoneId;
		this.eventId = eventId;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public long getTemplateId() {
		return templateId;
	}
	
	public Long getZoneId() {
		return zoneId;
	}

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }
	
}
