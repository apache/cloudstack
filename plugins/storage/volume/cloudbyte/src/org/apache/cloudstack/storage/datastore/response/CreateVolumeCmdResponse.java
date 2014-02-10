package org.apache.cloudstack.storage.datastore.response;

import org.apache.cloudstack.storage.datastore.model.FileSystem;
import org.apache.cloudstack.storage.datastore.model.FileSystemWrapper;

import com.google.gson.annotations.SerializedName;

public class CreateVolumeCmdResponse {

    @SerializedName("adddatasetresponse")
    private FileSystemWrapper fileSystemWrapper;

    public FileSystem getFileSystem() {

        return fileSystemWrapper.getFileSystem();
    }

}
