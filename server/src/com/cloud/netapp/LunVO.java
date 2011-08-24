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
 * @author-aj
 */

package com.cloud.netapp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="netapp_lun")
public class LunVO {
		
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
	
	@Column(name="lun_name")
    private String lunName;

	@Column(name="target_iqn")
    private String targetIqn;
	
    @Column(name="path")
	private String path;

    @Column(name="volume_id")
	private Long volumeId;

    public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}


	@Column(name="size")
	private Long size;

	public LunVO(){
		
	}
	
	public LunVO(String path, Long volumeId, Long size, String lunName, String targetIqn) {
		this.path = path;
		this.volumeId = volumeId;
		this.size = size;
		this.lunName = lunName;
		this.targetIqn = targetIqn;
	}
	
	public String getLunName() {
		return lunName;
	}

	public void setLunName(String lunName) {
		this.lunName = lunName;
	}

	public LunVO(Long id, String path, Long volumeId, Long size, String lunName, String targetIqn) {
		this.id = id;
		this.path = path;
		this.volumeId = volumeId;
		this.size = size;
		this.lunName = lunName;
		this.targetIqn = targetIqn;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Long getVolumeId() {
		return volumeId;
	}

	public void setVolumeId(Long volumeId) {
		this.volumeId = volumeId;
	}
	
	public void setTargetIqn(String iqn){
		this.targetIqn = iqn;
	}
	
	public String getTargetIqn(){
		return targetIqn;
	}


}
