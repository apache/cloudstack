/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.db;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.storage.ImageStoreObjectDownload;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "image_store_object_download")
public class ImageStoreObjectDownloadVO implements ImageStoreObjectDownload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "download_url", nullable = false)
    private String downloadUrl;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public ImageStoreObjectDownloadVO() {
    }

    public ImageStoreObjectDownloadVO(Long storeId, String path, String downloadUrl) {
        this.storeId = storeId;
        this.path = path;
        this.downloadUrl = downloadUrl;
    }

    public long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getPath() {
        return path;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public Date getCreated() {
        return created;
    }

}
