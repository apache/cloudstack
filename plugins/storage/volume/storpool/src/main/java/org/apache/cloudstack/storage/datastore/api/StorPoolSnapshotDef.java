package org.apache.cloudstack.storage.datastore.api;

import java.io.Serializable;
import java.util.Map;

public class StorPoolSnapshotDef implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Integer deleteAfter;
    private Map<String, String> tags;
    private Boolean bind;
    private Integer iops;
    private String rename;
    private transient String volumeName;

    public StorPoolSnapshotDef(String volumeName, Integer deleteAfter, Map<String, String> tags) {
        super();
        this.volumeName = volumeName;
        this.deleteAfter = deleteAfter;
        this.tags = tags;
    }

    public StorPoolSnapshotDef(String name, Integer deleteAfter, Map<String, String> tags, Boolean bind, Integer iops,
            String rename) {
        super();
        this.name = name;
        this.deleteAfter = deleteAfter;
        this.tags = tags;
        this.bind = bind;
        this.iops = iops;
        this.rename = rename;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getDeleteAfter() {
        return deleteAfter;
    }
    public void setDeleteAfter(Integer deleteAfter) {
        this.deleteAfter = deleteAfter;
    }
    public Map<String, String> getTags() {
        return tags;
    }
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
    public Boolean getBind() {
        return bind;
    }
    public void setBind(Boolean bind) {
        this.bind = bind;
    }
    public Integer getIops() {
        return iops;
    }
    public void setIops(Integer iops) {
        this.iops = iops;
    }
    public String getRename() {
        return rename;
    }
    public void setRename(String rename) {
        this.rename = rename;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

}
