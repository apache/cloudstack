package org.apache.cloudstack.backup.to;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;import java.util.UUID;

public class BackupScreenshotObject implements DataObject {

    private DataTO dataTO;
    private DataStore dataStore;

    public BackupScreenshotObject(DataTO dataTO, DataStore dataStore) {
        this.dataTO = dataTO;
        this.dataStore = dataStore;
    }

    @Override
    public String toString() {
        return String.format("BackupScreenshotObject %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "dataTO", "dataStore"));
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public DataTO getTO() {
        return dataTO;
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public Long getSize() {
        return null;
    }

    @Override
    public long getPhysicalSize() {
        return 0;
    }

    @Override
    public DataObjectType getType() {
        return dataTO.getObjectType();
    }

    @Override
    public String getUuid() {
        return null;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
    }

    @Override
    public void incRefCount() {
    }

    @Override
    public void decRefCount() {
    }

    @Override
    public Long getRefCount() {
        return null;
    }

    @Override
    public String getName() {
        return UUID.randomUUID().toString();
    }
}
