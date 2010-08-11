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

public class CopyTemplateParam {
	
	private long userId;
	private long templateId;
	private Long sourceZoneId;
	private Long destZoneId;
	private long eventId;

	public CopyTemplateParam() {
	}

	public CopyTemplateParam(long userId, long templateId, Long sourceZoneId, Long destZoneId, long eventId) {
		this.userId = userId;
		this.templateId = templateId;
		this.sourceZoneId = sourceZoneId;
		this.destZoneId = destZoneId;
		this.eventId = eventId;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public long getTemplateId() {
		return templateId;
	}
	
	public Long getSourceZoneId() {
		return sourceZoneId;
	}
	
	public Long getDestZoneId() {
		return destZoneId;
	}

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getEventId() {
        return eventId;
    }
	
}
