package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class FilesystemQoSWrapper {

    @SerializedName("monitorFilesystemQoS")
    private FileSystem fileSystem[];

    public FileSystem[] getFileSystemQos() {
        return fileSystem;
    }

}
