package org.apache.cloudstack.storage.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDaoBase;

@Entity
@Table(name = "object_datastore_ref")
public class ObjectInDataStoreVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "datastore_id")
    private long dataStoreId;
    
    @Column(name = "datastore_type")
    private String dataStoreType;

    @Column(name = "ojbect_id")
    long objectId;
    
    @Column(name = "object_type")
    String objectType;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    Date created = null;

    @Column(name = "last_updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date lastUpdated = null;

    @Column(name = "download_pct")
    int downloadPercent;

    @Column(name = "download_state")
    @Enumerated(EnumType.STRING)
    Status downloadState;

    @Column(name = "local_path")
    String localDownloadPath;

    @Column(name = "error_str")
    String errorString;

    @Column(name = "job_id")
    String jobId;

    @Column(name = "install_path")
    String installPath;

    @Column(name = "size")
    long size;
    
    @Column(name = "state")
    String state;

    @Column(name="update_count", updatable = true, nullable=false)
    protected long updatedCount;
    
    public long getId() {
        return this.id;
    }
    
    public long getDataStoreId() {
        return this.dataStoreId;
    }
    
    public void setDataStoreId(long id) {
        this.dataStoreId = id;
    }
    
    public String getDataStoreType() {
        return this.dataStoreType;
    }
    
    public void setDataStoreType(String type) {
        this.dataStoreType = type;
    }
    
    public long getObjectId() {
        return this.objectId;
    }
    
    public void setObjectId(long id) {
        this.objectId = id;
    }
    
    public String getObjectType() {
        return this.objectType;
    }
    
    public void setObjectType(String type) {
        this.objectType = type;
    }
}
