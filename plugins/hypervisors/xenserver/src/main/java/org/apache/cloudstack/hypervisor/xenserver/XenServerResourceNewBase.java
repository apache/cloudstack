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
package org.apache.cloudstack.hypervisor.xenserver;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.StartupCommand;
import com.cloud.hypervisor.xenserver.resource.XenServer620SP1Resource;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Event;
import com.xensource.xenapi.EventBatch;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

/**
 *
 * XenServerResourceNewBase is an abstract base class that encapsulates how
 * CloudStack should interact with XenServer after a special XenServer
 * 6.2 hotfix.  From here on, every Resource for future versions of
 * XenServer should use this as the base class.  This base class lessens
 * the amount of load CloudStack places on Xapi because it doesn't use
 * polling as a means to collect data and figure out task completion.
 *
 * This base class differs from CitrixResourceBase in the following ways:
 *   - VM states are detected using Event.from instead of polling.  This
 *     increases the number of threads CloudStack uses but the threads
 *     are mostly idle just waiting for events from XenServer.
 *   - stats are collected through the http interface rather than Xapi plugin.
 *     This change may be promoted to CitrixResourceBase as it's also possible
 *     in previous versions of XenServer.
 *   - Asynchronous task completion is done throught Event.from rather than
 *     polling.
 *
 */
public class XenServerResourceNewBase extends XenServer620SP1Resource {
    protected VmEventListener _listener = null;

    @Override
    public StartupCommand[] initialize() throws IllegalArgumentException {
        final StartupCommand[] cmds = super.initialize();

        final Connection conn = getConnection();
        Pool pool;
        try {
            pool = Pool.getByUuid(conn, _host.getPool());
            final Pool.Record poolr = pool.getRecord(conn);

            final Host.Record masterRecord = poolr.master.getRecord(conn);
            if (_host.getUuid().equals(masterRecord.uuid)) {
                _listener = new VmEventListener(true);

                //
                // TODO disable event listener for now. Wait until everything else is ready
                //

                // _listener.start();
            } else {
                _listener = new VmEventListener(false);
            }
        } catch (final XenAPIException e) {
            throw new CloudRuntimeException("Unable to determine who is the master", e);
        } catch (final XmlRpcException e) {
            throw new CloudRuntimeException("Unable to determine who is the master", e);
        }
        return cmds;
    }

    protected void waitForTask2(final Connection c, final Task task, final long pollInterval, final long timeout) throws XenAPIException, XmlRpcException, TimeoutException {
        final long beginTime = System.currentTimeMillis();
        if (logger.isTraceEnabled()) {
            logger.trace("Task " + task.getNameLabel(c) + " (" + task.getType(c) + ") sent to " + c.getSessionReference() + " is pending completion with a " + timeout +
                    "ms timeout");
        }
        final Set<String> classes = new HashSet<String>();
        classes.add("Task/" + task.toWireString());
        String token = "";
        final Double t = new Double(timeout / 1000);
        while (true) {
            final EventBatch map = Event.from(c, classes, token, t);
            token = map.token;
            @SuppressWarnings("unchecked")
            final
            Set<Event.Record> events = map.events;
            if (events.size() == 0) {
                final String msg = "No event for task " + task.toWireString();
                logger.warn(msg);
                task.cancel(c);
                throw new TimeoutException(msg);
            }
            for (final Event.Record rec : events) {
                if (!(rec.snapshot instanceof Task.Record)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping over " + rec);
                    }
                    continue;
                }

                final Task.Record taskRecord = (Task.Record)rec.snapshot;

                if (taskRecord.status != Types.TaskStatusType.PENDING) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Task, ref:" + task.toWireString() + ", UUID:" + taskRecord.uuid + " is done " + taskRecord.status);
                    }
                    return;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Task: ref:" + task.toWireString() + ", UUID:" + taskRecord.uuid +  " progress: " + taskRecord.progress);
                    }

                }
            }
            if (System.currentTimeMillis() - beginTime > timeout) {
                final String msg = "Async " + timeout / 1000 + " seconds timeout for task " + task.toString();
                logger.warn(msg);
                task.cancel(c);
                throw new TimeoutException(msg);
            }
        }
    }


    protected class VmEventListener extends Thread {
        boolean _stop = false;
        HashMap<String, Pair<String, VirtualMachine.State>> _changes = new HashMap<String, Pair<String, VirtualMachine.State>>();
        boolean _isMaster;
        Set<String> _classes;
        String _token = "";

        public VmEventListener(final boolean isMaster) {
            _isMaster = isMaster;
            _classes = new HashSet<String>();
            _classes.add("VM");
        }

        @Override
        public void run() {
            setName("XS-Listener-" + _host.getIp());
            while (!_stop) {
                try {
                    final Connection conn = getConnection();
                    EventBatch results;
                    try {
                        results = Event.from(conn, _classes, _token, new Double(30));
                    } catch (final Exception e) {
                        logger.error("Retrying the waiting on VM events due to: ", e);
                        continue;
                    }

                    _token = results.token;
                    @SuppressWarnings("unchecked")
                    final
                    Set<Event.Record> events = results.events;
                    for (final Event.Record event : events) {
                        try {
                            if (!(event.snapshot instanceof VM.Record)) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("The snapshot is not a VM: " + event);
                                }
                                continue;
                            }
                            final VM.Record vm = (VM.Record)event.snapshot;

                            String hostUuid = null;
                            if (vm.residentOn != null && !vm.residentOn.toWireString().contains("OpaqueRef:NULL")) {
                                hostUuid = vm.residentOn.getUuid(conn);
                            }
                            recordChanges(conn, vm, hostUuid);
                        } catch (final Exception e) {
                            logger.error("Skipping over " + event, e);
                        }
                    }
                } catch (final Throwable th) {
                    logger.error("Exception caught in eventlistener thread: ", th);
                }
            }
        }

        protected void recordChanges(final Connection conn, final VM.Record rec, final String hostUuid) {

        }

        @Override
        public void start() {
            if (_isMaster) {
                // Throw away the initial set of events because they're history
                final Connection conn = getConnection();
                EventBatch results;
                try {
                    results = Event.from(conn, _classes, _token, new Double(30));
                } catch (final Exception e) {
                    logger.error("Retrying the waiting on VM events due to: ", e);
                    throw new CloudRuntimeException("Unable to start a listener thread to listen to VM events", e);
                }
                _token = results.token;
                logger.debug("Starting the event listener thread for " + _host.getUuid());
                super.start();
            }
        }

        public boolean isListening() {
            return _isMaster;
        }

        public HashMap<String, Pair<String, VirtualMachine.State>> getChanges() {
            synchronized (_cluster.intern()) {
                if (_changes.size() == 0) {
                    return null;
                }
                final HashMap<String, Pair<String, VirtualMachine.State>> diff = _changes;
                _changes = new HashMap<String, Pair<String, VirtualMachine.State>>();
                return diff;
            }
        }

        public void signalStop() {
            _stop = true;
            interrupt();
        }
    }

}
