/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.object;

import com.cloud.agent.api.to.BucketCredentialTO;
import com.cloud.agent.api.to.BucketKeyTO;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.host.Host;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;

public abstract class BaseObjectStoreDriverImpl implements ObjectStoreDriver {
    protected Logger logger = LogManager.getLogger(getClass());

    protected static final String BUCKET_CREDENTIALS_UNSUPPORTED = "Per-bucket credentials are not supported by this object store provider";

    @Override
    public boolean supportsBucketCredentials(long storeId) {
        return false;
    }

    @Override
    public String bucketCredentialsUnsupportedReason(long storeId) {
        // a provider that implements per-bucket credentials overrides this with its own diagnosis;
        // for the rest, the reason is simply that they do not offer them
        return "This object storage provider does not offer per-bucket credentials";
    }

    @Override
    public boolean accountSupportsBucketCredentials(long accountId, long storeId) {
        return false;
    }

    @Override
    public boolean migrateAccountForBucketCredentials(long accountId, long storeId) {
        throw new CloudRuntimeException(BUCKET_CREDENTIALS_UNSUPPORTED);
    }

    @Override
    public BucketCredentialTO createBucketCredential(BucketTO bucket, long storeId) {
        throw new CloudRuntimeException(BUCKET_CREDENTIALS_UNSUPPORTED);
    }

    @Override
    public BucketKeyTO createBucketCredentialKey(BucketTO bucket, long storeId, Set<String> knownAccessKeys) {
        throw new CloudRuntimeException(BUCKET_CREDENTIALS_UNSUPPORTED);
    }

    @Override
    public boolean removeBucketCredentialKey(BucketTO bucket, long storeId, String accessKey) {
        throw new CloudRuntimeException(BUCKET_CREDENTIALS_UNSUPPORTED);
    }

    @Override
    public boolean deleteBucketCredential(BucketTO bucket, long storeId) {
        throw new CloudRuntimeException(BUCKET_CREDENTIALS_UNSUPPORTED);
    }

    @Override
    public BucketKeyTO rotateAccountKey(long accountId, long storeId) {
        throw new CloudRuntimeException(BUCKET_CREDENTIALS_UNSUPPORTED);
    }

    @Override
    public boolean isAccountKeyRotationPending(long accountId, long storeId) {
        return false;
    }

    @Override
    public Map<String, String> getCapabilities() {
        return null;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    protected class CreateContext<T> extends AsyncRpcContext<T> {
        final DataObject data;

        public CreateContext(AsyncCompletionCallback<T> callback, DataObject data) {
            super(callback);
            this.data = data;
        }
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        copyAsync(srcData, destData, callback);
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
    }
}
