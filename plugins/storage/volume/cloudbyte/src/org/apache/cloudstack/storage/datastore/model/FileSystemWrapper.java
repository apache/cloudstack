package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class FileSystemWrapper {

    @SerializedName("filesystem")
    private FileSystem fileSystem;

    public FileSystem getFileSystem() {
        return fileSystem;
    }

}
