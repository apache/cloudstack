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
package com.cloud.hypervisor.xenserver.resource;

import com.cloud.storage.Storage;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reduce bloat inside CitrixResourceBase
 *
 */
public class CitrixHelper {
    protected static Logger LOGGER = LogManager.getLogger(CitrixHelper.class);

    /**
     * Tracks active VM tasks (startVM, shutdownVM, migrateVM) by command sequence.
     * Key: command sequence, Value: ActiveVmTaskInfo
     */
    private static final ConcurrentHashMap<Long, ActiveVmTaskInfo> activeVmTasks = new ConcurrentHashMap<>();

    /**
     * Tracks active VM tasks by VM name for quick lookup.
     * Key: VM name, Value: Set of command sequences
     */
    private static final ConcurrentHashMap<String, Set<Long>> vmNameToTaskSequences = new ConcurrentHashMap<>();

    /**
     * ThreadLocal to store current command context for task registration.
     */
    private static final ThreadLocal<TaskContext> currentTaskContext = new ThreadLocal<>();

    public static class ActiveVmTaskInfo {
        final Task task;
        final Connection connection;
        final long cmdSequence;
        final String vmName;
        final String commandType;
        final long startTime;

        ActiveVmTaskInfo(long cmdSequence, String vmName, String commandType,
                         Task task, Connection connection) {
            this.cmdSequence = cmdSequence;
            this.vmName = vmName;
            this.commandType = commandType;
            this.task = task;
            this.connection = connection;
            this.startTime = System.currentTimeMillis();
        }
    }

    /**
     * Registers an active XenServer task for tracking.
     */
    private static void registerActiveVmTask(long cmdSequence, String vmName, String commandType,
                                             Task task, Connection connection) {
        if (StringUtils.isBlank(vmName)) {
            LOGGER.debug("No active VM to registered VM task for sequence={}, command={}, task={}", cmdSequence, commandType, task);
            return;
        }

        ActiveVmTaskInfo taskInfo = new ActiveVmTaskInfo(cmdSequence, vmName, commandType, task, connection);
        activeVmTasks.put(cmdSequence, taskInfo);

        vmNameToTaskSequences.computeIfAbsent(vmName, k -> ConcurrentHashMap.newKeySet()).add(cmdSequence);

        LOGGER.debug("Registered active VM task: sequence={}, vm={}, command={}, task={}", cmdSequence, vmName, commandType, task);
    }

    /**
     * Unregisters an active XenServer task.
     */
    private static void unregisterActiveVmTask(long cmdSequence) {
        ActiveVmTaskInfo taskInfo = activeVmTasks.remove(cmdSequence);
        if (taskInfo != null) {
            Set<Long> sequences = vmNameToTaskSequences.get(taskInfo.vmName);
            if (sequences != null) {
                sequences.remove(cmdSequence);
                if (sequences.isEmpty()) {
                    vmNameToTaskSequences.remove(taskInfo.vmName);
                }
            }
            LOGGER.debug("Unregistered active VM task: sequence={}, vm={}, command={}, duration={} ms",
                    cmdSequence, taskInfo.vmName, taskInfo.commandType,
                    System.currentTimeMillis() - taskInfo.startTime);
        }
    }

    /**
     * Cancels all active XenServer tasks for a given VM name.
     */
    public static int cancelActiveVmTasks(String vmName) {
        Set<Long> sequences = vmNameToTaskSequences.get(vmName);
        if (sequences == null || sequences.isEmpty()) {
            return 0;
        }

        int cancelled = 0;
        for (Long seq : sequences) {
            if (cancelActiveVmTask(seq)) {
                cancelled++;
            }
        }
        return cancelled;
    }

    /**
     * Checks whether an active XenServer task is Cancellable or not.
     */
    public static boolean isActiveVmTaskCancellable(long cmdSequence) {
        ActiveVmTaskInfo taskInfo = activeVmTasks.get(cmdSequence);
        if (taskInfo == null) {
            return true;
        }

        try {
            return taskInfo.task.getStatus(taskInfo.connection) == Types.TaskStatusType.PENDING;
        } catch (Exception e) {
            LOGGER.warn("Failed to verify XenServer task cancellability for sequence={}, vm={}, error={}",
                    cmdSequence, taskInfo.vmName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cancels an active XenServer task by command sequence.
     */
    public static boolean cancelActiveVmTask(long cmdSequence) {
        ActiveVmTaskInfo taskInfo = activeVmTasks.get(cmdSequence);
        if (taskInfo == null) {
            return true;
        }

        try {
            if (taskInfo.task.getStatus(taskInfo.connection) != Types.TaskStatusType.PENDING) {
                LOGGER.info("XenServer VM task is not cancellable: sequence={}, vm={}, command={}",
                        cmdSequence, taskInfo.vmName, taskInfo.commandType);
                return false;
            }

            // Cancel the XenServer task
            taskInfo.task.cancel(taskInfo.connection);
            LOGGER.info("Cancelled XenServer VM task: sequence={}, vm={}, command={}, task={}",
                    cmdSequence, taskInfo.vmName, taskInfo.commandType, taskInfo.task.getUuid(taskInfo.connection));

            // Unregister the task
            unregisterActiveVmTask(cmdSequence);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to cancel XenServer VM task: sequence={}, vm={}, error={}",
                    cmdSequence, taskInfo.vmName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets all active XenServer tasks for a given VM name.
     */
    public static List<ActiveVmTaskInfo> getActiveVmTasks(String vmName) {
        Set<Long> sequences = vmNameToTaskSequences.get(vmName);
        if (sequences == null || sequences.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActiveVmTaskInfo> tasks = new ArrayList<>();
        for (Long seq : sequences) {
            ActiveVmTaskInfo taskInfo = activeVmTasks.get(seq);
            if (taskInfo != null) {
                tasks.add(taskInfo);
            }
        }
        return tasks;
    }

    /**
     * Gets all active XenServer tasks.
     */
    public static List<ActiveVmTaskInfo> getAllActiveVmTasks() {
        return new ArrayList<>(activeVmTasks.values());
    }

    public static class TaskContext {
        private final CitrixResourceBase resource;
        private final long cmdSequence;
        private final String vmName;
        private final String commandType;

        public TaskContext(CitrixResourceBase resource, long cmdSequence, String vmName, String commandType) {
            this.resource = resource;
            this.cmdSequence = cmdSequence;
            this.vmName = vmName;
            this.commandType = commandType;
        }

        public void registerTask(Task task, Connection connection) {
            registerActiveVmTask(cmdSequence, vmName, commandType, task, connection);
        }

        public void unregisterTask() {
            unregisterActiveVmTask(cmdSequence);
        }
    }

    public static void setTaskContext(CitrixResourceBase resource, long cmdSequence, String vmName, String commandType) {
        LOGGER.debug("Setting task context: sequence={}, vm={}, command={}", cmdSequence, vmName, commandType);
        currentTaskContext.set(new TaskContext(resource, cmdSequence, vmName, commandType));
    }

    public static void clearTaskContext() {
        TaskContext taskContext = currentTaskContext.get();
        if (taskContext != null) {
            LOGGER.debug("Clearing task context: sequence={}, vm={}, command={}", taskContext.cmdSequence, taskContext.vmName, taskContext.commandType);
        }
        currentTaskContext.remove();
    }

    public static TaskContext getCurrentTaskContext() {
        return currentTaskContext.get();
    }

    public static String getProductVersion(final Host.Record record) {
        String prodVersion = record.softwareVersion.get("product_version");
        if (prodVersion == null) {
            prodVersion = record.softwareVersion.get("platform_version").trim();
        } else {
            prodVersion = prodVersion.trim();
        }
        return prodVersion;
    }

    public static String getPVbootloaderArgs(String guestOS) {
        if (guestOS.startsWith("SUSE Linux Enterprise Server")) {
            if (guestOS.contains("64-bit")) {
                return "--kernel /boot/vmlinuz-xen --ramdisk /boot/initrd-xen";
            } else if (guestOS.contains("32-bit")) {
                return "--kernel /boot/vmlinuz-xenpae --ramdisk /boot/initrd-xenpae";
            }
        }
        return "";
    }

    public static String getSRNameLabel(final String poolUuid,
                                        final Storage.StoragePoolType poolType,
                                        final String poolPath) {
        if (Storage.StoragePoolType.PreSetup.equals(poolType) &&
                !poolPath.contains(poolUuid)) {
            return  poolPath.replaceFirst("/", "");
        }
        return poolUuid;
    }
}
