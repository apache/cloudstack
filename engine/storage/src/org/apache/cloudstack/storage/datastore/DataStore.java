package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;

public interface DataStore {
    String grantAccess(VolumeInfo volume, EndPoint ep);
    boolean revokeAccess(VolumeInfo volume, EndPoint ep);
    String grantAccess(TemplateInfo template, EndPoint ep);
    boolean revokeAccess(TemplateInfo template, EndPoint ep);
    String grantAccess(SnapshotInfo snapshot, EndPoint ep);
    boolean revokeAccess(SnapshotInfo snapshot, EndPoint ep);
    String getRole();
    long getId();
}
