package com.cloud.offering;

public class DiskOfferingInfo {
    private DiskOffering _diskOffering;
    private Long _size;
    private Long _minIops;
    private Long _maxIops;

    public DiskOfferingInfo() {
    }

    public DiskOfferingInfo(DiskOffering diskOffering) {
        _diskOffering = diskOffering;
    }

    public void setDiskOffering(DiskOffering diskOffering) {
        _diskOffering = diskOffering;
    }

    public DiskOffering getDiskOffering() {
        return _diskOffering;
    }

    public void setSize(Long size) {
        _size = size;
    }

    public Long getSize() {
        return _size;
    }

    public void setMinIops(Long minIops) {
        _minIops = minIops;
    }

    public Long getMinIops() {
        return _minIops;
    }

    public void setMaxIops(Long maxIops) {
        _maxIops = maxIops;
    }

    public Long getMaxIops() {
        return _maxIops;
    }
}