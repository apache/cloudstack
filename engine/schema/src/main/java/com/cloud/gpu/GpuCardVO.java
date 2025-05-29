package com.cloud.gpu;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.gpu.GpuCard;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "gpu_card")
public class GpuCardVO implements GpuCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "name")
    private String name;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "vendor_id")
    private String vendorId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public GpuCardVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public GpuCardVO(String deviceId, String deviceName, String name, String vendorName,
                     String vendorId) {
        this.uuid = UUID.randomUUID().toString();
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.name = name;
        this.vendorName = vendorName;
        this.vendorId = vendorId;
        this.created = new Date();
    }

    @Override
    public String toString() {
        return String.format("GPUCard %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "uuid", "deviceId", "deviceName", "displayName", "vendorId"));
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    @Override
    public Date getCreated() {
        return created;
    }
}
