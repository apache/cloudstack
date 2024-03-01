package org.apache.cloudstack.storage.datastore.api;

import java.io.Serializable;
import java.util.Map;

public class StorPoolVolume implements Serializable {

    private static final long serialVersionUID = 1L;
    private transient String name;
    private Long size;
    private Map<String, String> tags;
    private String parent;
    private Long iops;
    private String template;
    private String baseOn;
    private String rename;
    private Boolean shrinkOk;

    public StorPoolVolume() {
    }

    public StorPoolVolume(String name, Long size, Map<String, String> tags, String parent, Long iops, String template,
            String baseOn, String rename, Boolean shrinkOk) {
        super();
        this.name = name;
        this.size = size;
        this.tags = tags;
        this.parent = parent;
        this.iops = iops;
        this.template = template;
        this.baseOn = baseOn;
        this.rename = rename;
        this.shrinkOk = shrinkOk;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getSize() {
        return size;
    }
    public void setSize(Long size) {
        this.size = size;
    }
    public Map<String, String> getTags() {
        return tags;
    }
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
    public String getParent() {
        return parent;
    }
    public void setParent(String parent) {
        this.parent = parent;
    }
    public Long getIops() {
        return iops;
    }
    public void setIops(Long iops) {
        this.iops = iops;
    }
    public String getTemplate() {
        return template;
    }
    public void setTemplate(String template) {
        this.template = template;
    }
    public String getBaseOn() {
        return baseOn;
    }
    public void setBaseOn(String baseOn) {
        this.baseOn = baseOn;
    }
    public String getRename() {
        return rename;
    }
    public void setRename(String rename) {
        this.rename = rename;
    }

    public Boolean getShrinkOk() {
        return shrinkOk;
    }

    public void setShrinkOk(Boolean shrinkOk) {
        this.shrinkOk = shrinkOk;
    }
}
