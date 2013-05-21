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
package org.apache.cloudstack.storage.cache.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.cache.allocator.StorageCacheAllocator;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.log4j.Logger;

import com.cloud.utils.component.Manager;

public class StorageCacheManagerImpl implements StorageCacheManager, Manager {
    private static final Logger s_logger = Logger
            .getLogger(StorageCacheManagerImpl.class);
    @Inject
    List<StorageCacheAllocator> storageCacheAllocator;
    @Inject
    DataMotionService dataMotionSvr;
    @Inject
    ObjectInDataStoreManager objectInStoreMgr;

    @Override
    public DataStore getCacheStorage(Scope scope) {
        for (StorageCacheAllocator allocator : storageCacheAllocator) {
            DataStore store = allocator.getCacheStore(scope);
            if (store != null) {
                return store;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }



    private class CreateCacheObjectContext<T> extends AsyncRpcConext<T> {
        final AsyncCallFuture<CopyCommandResult> future;
        /**
         * @param callback
         */
        public CreateCacheObjectContext(AsyncCompletionCallback<T> callback, AsyncCallFuture<CopyCommandResult> future) {
            super(callback);
            this.future = future;
        }

    }

	@Override
	public DataObject createCacheObject(DataObject data, Scope scope) {
		DataStore cacheStore = this.getCacheStorage(scope);
		DataObjectInStore obj = objectInStoreMgr.findObject(data, cacheStore);
		if (obj != null && obj.getState() == ObjectInDataStoreStateMachine.State.Ready) {
			s_logger.debug("there is already one in the cache store");
			return objectInStoreMgr.get(data, cacheStore);
		}

		//TODO: consider multiple thread to create
		DataObject objOnCacheStore = cacheStore.create(data);

		AsyncCallFuture<CopyCommandResult> future = new AsyncCallFuture<CopyCommandResult>();
		CopyCommandResult result = null;
		try {
		    objOnCacheStore.processEvent(Event.CreateOnlyRequested);

		    dataMotionSvr.copyAsync(data, objOnCacheStore, future);
		    result = future.get();

		    if (result.isFailed()) {
		        objOnCacheStore.processEvent(Event.OperationFailed);
		    } else {
		        objOnCacheStore.processEvent(Event.OperationSuccessed, result.getAnswer());
		        return objOnCacheStore;
		    }
        } catch (InterruptedException e) {
            s_logger.debug("create cache storage failed: " + e.toString());
        } catch (ExecutionException e) {
            s_logger.debug("create cache storage failed: " + e.toString());
        } catch (Exception e) {
            s_logger.debug("create cache storage failed: " + e.toString());
        } finally {
            if (result == null) {
                objOnCacheStore.processEvent(Event.OperationFailed);
            }
        }

		return null;
	}

	@Override
    public DataObject getCacheObject(DataObject data, Scope scope) {
        DataStore cacheStore = this.getCacheStorage(scope);
        DataObject objOnCacheStore = cacheStore.create(data);

        return objOnCacheStore;
    }

	protected Void createCacheObjectCallBack(AsyncCallbackDispatcher<StorageCacheManagerImpl, CopyCommandResult> callback,
	        CreateCacheObjectContext<CopyCommandResult> context) {
	    AsyncCallFuture<CopyCommandResult> future = context.future;
	    future.complete(callback.getResult());
	    return null;
	}

    @Override
    public boolean deleteCacheObject(DataObject data) {
        return objectInStoreMgr.delete(data);
    }
}