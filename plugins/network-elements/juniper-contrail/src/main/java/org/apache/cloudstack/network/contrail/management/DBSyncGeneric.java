// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.network.contrail.management;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import net.juniper.contrail.api.ApiObjectBase;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.api.Identity;

public class DBSyncGeneric {

    protected Logger logger = LogManager.getLogger(getClass());

    /* for each synchronization VNC class, following methods
     * needs to be defined.
     * For e.q : VirtualNetwork class should have createMethodPrefix+"VirtualNetwork" etc
     */
    private final String createMethodPrefix = "create";
    private final String deleteMethodPrefix = "delete";
    private final String compareMethodPrefix = "compare";
    private final String filterMethodPrefix = "filter";
    private final String equalMethodPrefix = "equal";
    private final String syncMethodPrefix = "sync";
    /* default db, vnc comparators are implemented based on uuid values,
     * if user defined comparators are required, then only add these methods
     */
    private final String dbComparatorMethodPrefix = "dbComparator";
    private final String vncComparatorMethodPrefix = "vncComparator";

    /* sync methods implementation object, if implemented in separate class
     * set the scope object
     */
    private Object _scope;
    private HashMap<String, Method> _methodMap;
    private short _syncMode;

    public static final short SYNC_MODE_UPDATE = 0;
    public static final short SYNC_MODE_CHECK = 1;

    public DBSyncGeneric(Object scope) {
        this._scope = scope;
        this._syncMode = SYNC_MODE_UPDATE;
        setMethodMap();
    }

    public DBSyncGeneric() {
        this._scope = this;
        this._syncMode = SYNC_MODE_UPDATE;
        setMethodMap();
    }

    public void setSyncMode(short mode) {
        this._syncMode = mode;
    }

    public short getSyncMode() {
        return this._syncMode;
    }

    public void setScope(Object scope) {
        this._scope = scope;
        setMethodMap();
    }

    public void setMethodMap() {
        _methodMap = new HashMap<String, Method>();
        Method methods[] = _scope.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            _methodMap.put(methods[i].getName(), methods[i]);
        }
    }

    public static String getClassName(Class<?> cls) {
        String clsname = cls.getName();
        int loc = clsname.lastIndexOf('.');
        if (loc > 0) {
            clsname = clsname.substring(loc + 1);
        }
        return clsname;
    }

    /*
     *  This API can be used to sync a particular vnc class
     */
    public Boolean sync(Class<?> cls, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String syncMethod = syncMethodPrefix + getClassName(cls);
        Method method = _methodMap.get(syncMethod);
        if (method == null)
            throw new NoSuchMethodException(getClassName(_scope.getClass()) + ":" + syncMethod);
        return (Boolean)method.invoke(_scope, parameters);
    }

    private void create(Class<?> cls, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String createMethod = createMethodPrefix + getClassName(cls);
        Method method = _methodMap.get(createMethod);
        if (method == null)
            throw new NoSuchMethodException(getClassName(_scope.getClass()) + ":" + createMethod);
        method.invoke(_scope, parameters);
    }

    private void delete(Class<?> cls, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String deleteMethod = deleteMethodPrefix + getClassName(cls);
        Method method = _methodMap.get(deleteMethod);
        if (method == null)
            throw new NoSuchMethodException(getClassName(_scope.getClass()) + ":" + deleteMethod);
        method.invoke(_scope, parameters);
    }

    private Integer compare(Class<?> cls, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String compareMethod = compareMethodPrefix + getClassName(cls);
        Method method = _methodMap.get(compareMethod);
        if (method == null)
            throw new NoSuchMethodException(getClassName(_scope.getClass()) + ":" + compareMethod);
        return (Integer)method.invoke(_scope, parameters);
    }

    private Boolean filter(Class<?> cls, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String filterMethod = filterMethodPrefix + getClassName(cls);
        Method method = _methodMap.get(filterMethod);
        if (method == null) {
            logger.debug("Method not implemented: " + getClassName(_scope.getClass()) + ":" + filterMethod);
            return false;
        }
        return (Boolean)method.invoke(_scope, parameters);
    }

    private Boolean equal(Class<?> cls, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String equalMethod = equalMethodPrefix + getClassName(cls);
        Method method = _methodMap.get(equalMethod);
        if (method == null) {
            logger.debug("Method not implemented: " + getClassName(_scope.getClass()) + ":" + equalMethod);
            return true;
        }
        return (Boolean)method.invoke(_scope, parameters);
    }

    @SuppressWarnings("rawtypes")
    private Comparator dbComparator(Class<?> cls, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String dbComparatorMethod = dbComparatorMethodPrefix + getClassName(cls);
        Method method = _methodMap.get(dbComparatorMethod);
        if (method == null)
            return dbComparatorDefault();
        return (Comparator)method.invoke(_scope, parameters);
    }

    @SuppressWarnings("rawtypes")
    private Comparator vncComparator(Class<?> cls, Object... parameters) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String vncComparatorMethod = vncComparatorMethodPrefix + getClassName(cls);
        Method method = _methodMap.get(vncComparatorMethod);
        if (method == null)
            return vncComparatorDefault();
        return (Comparator)method.invoke(_scope, parameters);
    }

    @SuppressWarnings("rawtypes")
    public Comparator dbComparatorDefault() {
        Comparator comparator = new Comparator<Identity>() {
            @Override
            public int compare(Identity u1, Identity u2) {
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    @SuppressWarnings("rawtypes")
    public Comparator vncComparatorDefault() {
        Comparator comparator = new Comparator<ApiObjectBase>() {
            @Override
            public int compare(ApiObjectBase u1, ApiObjectBase u2) {
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    public static class SyncStats {
        public int create;
        public int delete;
        public int equal;
        public int diff;
        public int filter;
        public StringBuffer logMsg;

        SyncStats() {
            logMsg = new StringBuffer();
        }

        void log(String str) {
            logMsg.append(str);
            logMsg.append('\n');
        }

        public boolean isSynchronized() {
            return create == 0 && delete == 0 && diff == 0;
        }

        @Override
        public String toString() {
            StringBuffer str = new StringBuffer();
            str.append("create: " + create);
            str.append(", delete: " + delete);
            if (filter > 0) {
                str.append(", filter: " + filter);
            }
            str.append(", equal: " + equal);
            str.append(", diff:" + diff);
            return str.toString();
        }
    }

    public void syncCollections(Class<?> cls, Collection<?> lhsList, Collection<?> rhsList, boolean modifyMode, SyncStats stats) throws InvocationTargetException,
        IllegalAccessException, NoSuchMethodException {
        java.util.Iterator<?> lhsIter = lhsList.iterator();
        java.util.Iterator<?> rhsIter = rhsList.iterator();

        Object lhsItem = lhsIter.hasNext() ? lhsIter.next() : null;
        Object rhsItem = rhsIter.hasNext() ? rhsIter.next() : null;

        while (lhsItem != null && rhsItem != null) {
            Integer cmp = this.compare(cls, lhsItem, rhsItem, stats.logMsg);
            if (cmp < 0) {
                // Create
                if (modifyMode) {
                    this.create(cls, lhsItem, stats.logMsg);
                }
                stats.create++;
                lhsItem = lhsIter.hasNext() ? lhsIter.next() : null;
            } else if (cmp > 0) {
                // Delete
                if (!this.filter(cls, rhsItem, stats.logMsg)) {
                    if (modifyMode) {
                        this.delete(cls, rhsItem, stats.logMsg);
                    }
                    stats.delete++;
                } else {
                    stats.filter++;
                }
                rhsItem = rhsIter.hasNext() ? rhsIter.next() : null;
            } else {
                // Equal
                if (this.equal(cls, lhsItem, rhsItem, stats.logMsg)) {
                    stats.equal++;
                } else {
                    stats.diff++;
                }
                lhsItem = lhsIter.hasNext() ? lhsIter.next() : null;
                rhsItem = rhsIter.hasNext() ? rhsIter.next() : null;
            }
        }

        while (lhsItem != null) {
            // Create
            if (modifyMode) {
                this.create(cls, lhsItem, stats.logMsg);
            }
            stats.create++;
            lhsItem = lhsIter.hasNext() ? lhsIter.next() : null;
        }

        while (rhsItem != null) {
            // Delete
            if (!this.filter(cls, rhsItem, stats.logMsg)) {
                if (modifyMode) {
                    this.delete(cls, rhsItem, stats.logMsg);
                }
                stats.delete++;
            } else {
                stats.filter++;
            }
            rhsItem = rhsIter.hasNext() ? rhsIter.next() : null;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean syncGeneric(Class<?> cls, List<?> dbList, List<?> vncList) throws Exception {
        SyncStats stats = new SyncStats();
        stats.log("Sync log for <" + getClassName(cls) + ">");

        logger.debug("Generic db sync : " + getClassName(cls));

        java.util.Collections.sort(dbList, this.dbComparator(cls));
        java.util.Collections.sort(vncList, this.vncComparator(cls));

        syncCollections(cls, dbList, vncList, _syncMode != SYNC_MODE_CHECK, stats);

        if (_syncMode != SYNC_MODE_CHECK) {
            logger.debug("Sync stats<" + getClassName(cls) + ">:  " + stats.toString());
            logger.debug(stats.logMsg);
            logger.debug("Generic db sync : " + getClassName(cls) + " done");
        } else {
            logger.debug("Sync state checking stats<" + getClassName(cls) + ">: " + stats.toString());
            if (!stats.isSynchronized()) {
                logger.debug("DB and VNC objects out of sync is detected : " + getClassName(cls));
                logger.debug("Log message: \n" + stats.logMsg);
            } else {
                logger.debug("DB and VNC objects are in sync : " + getClassName(cls));
            }
        }

        /* return value of this method indicates state of the db & vnc before sync
         * false: out of sync, true: in sync;
         * it does not indicate whether sync operation is performed or not;
         * Actual sync is done only if _syncMode is UPDATE
         */
        return stats.isSynchronized();
    }

}
