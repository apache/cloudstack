// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.Profiler;

/**
  * Wrapper class for global database lock to reduce contention for database connections from within process
  * This class is used to acquire a global lock for a specific operation, identified by a unique name.
  * Example of using dynamic named locks
  * <p>
  *        GlobalLock lock = GlobalLock.getInternLock("some table name" + rowId);
  *
  *        if(lock.lock()) {
  *            try {
  *                do something
  *            } finally {
  *                lock.unlock();
  *            }
  *        }
  *        lock.releaseRef();
  * </p>
  */
public class GlobalLock {
    protected final static Logger logger = LogManager.getLogger(GlobalLock.class);

    private String name;

    /**
     * DB lock count.
     * Increments on {@link GlobalLock#lock(int)} and decrements on {@link GlobalLock#unlock()}.
     * Upon {@link GlobalLock#unlock()}, if {@link GlobalLock#lockCount} is less than 1, then lock removed from DB
     */
    private int lockCount;

    /**
     * Internal (in-memory) lock count.
     * Increments on {@link GlobalLock#addRef()} and indirectly on {@link GlobalLock#getInternLock(String)} and
     * decrements on {@link GlobalLock#releaseRef()}, {@link GlobalLock#unlock()} and on {@link GlobalLock#lock(int)}
     * if DB lock is unsuccessful
     */
    private int referenceCount;

    /**
     * Thread that owns lock. If lock called from different thread, it will be waiting for the owner to unlock it
     * within requested timeout. If owner thread call {@link GlobalLock#lock(int)} again, then
     * {@link GlobalLock#lockCount} will be incremented.
     * If {@link GlobalLock#unlock()} called by owner thread, or DB lock will be unsuccessful, then owner thread will be
     * nullified.
     */
    private Thread ownerThread;

    /**
     * Variable to hold lock duration in milliseconds. Used for information only.
     */
    private long holdingStartTick;

    /**
     * Holds all created locks.
     */
    private static Map<String, GlobalLock> s_lockMap = new HashMap<>();

    /**
     * Create lock.
     *
     * @param name lock name
     */
    private GlobalLock(String name) {
        this.name = name;
    }

    /**
     * Increment reference count to lock.
     *
     * @return reference count
     */
    public int addRef() {
        synchronized (this) {
            referenceCount++;
            return referenceCount;
        }
    }

    /**
     * Decrement reference count to lock.
     *
     * @return reference count
     */
    public int releaseRef() {
        boolean needToRemove = false;
        synchronized (this) {
            if (logger.isDebugEnabled()) {
                logger.debug("Releasing reference for internal lock {}, reference count: {}, lock count: {}",
                        name, referenceCount, lockCount);
            }
            referenceCount--;

            if (referenceCount < 0) {
                logger.warn("Unmatched internal lock {} reference usage detected (reference count: {}, " +
                        "lock count: {}), check your code!", name, referenceCount, lockCount);
            } else if (referenceCount < 1) {
                needToRemove = true;
            }
        }

        if (needToRemove) {
            if (logger.isDebugEnabled()) {
                logger.debug("Need to release internal lock {}", name);
            }
            releaseInternLock(name);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Released reference for lock {}, reference count: {}", name, referenceCount);
        }
        return referenceCount;
    }

    public static boolean isLockAvailable(String name) {
        if (logger.isDebugEnabled()) {
            logger.debug("Checking lock present for {}", name);
        }
        boolean result = false;
        try {
            result = DbUtil.isFreeLock(name);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Result of checking lock present for {}: {}", name, result);
            }
        }
        return result;
    }

    /**
     * Registers internal lock (in memory) object. Does not create any lock in DB yet.
     *
     * @param name lock name
     * @return lock object
     */
    public static GlobalLock getInternLock(String name) {
        synchronized (s_lockMap) {
            GlobalLock lock;
            if (s_lockMap.containsKey(name)) {
                lock = s_lockMap.get(name);
                if (logger.isDebugEnabled()) {
                    logger.debug("Internal lock {} already exists with reference count {} and lock count {}",
                            name, lock.referenceCount, lock.lockCount);
                }
            } else {
                lock = new GlobalLock(name);
                if (logger.isDebugEnabled()) {
                    logger.debug("Internal lock {} does not exist, adding", name);
                }
                s_lockMap.put(name, lock);
            }
            lock.addRef();
            if (logger.isDebugEnabled()) {
                logger.debug("Added reference to internal lock {}, reference count {}, lock count {}",
                        name, lock.referenceCount, lock.lockCount);
            }
            return lock;
        }
    }

    /**
     * Unregister internal lock (in memory) object. Does not remove any lock from DB.
     *
     * @param name lock name
     */
    private void releaseInternLock(String name) {
        synchronized (s_lockMap) {
            GlobalLock lock = s_lockMap.get(name);
            if (lock != null) {
                if (lock.referenceCount == 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Released internal lock {}", name);
                    }
                    s_lockMap.remove(name);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Not releasing internal lock {} as it has references count: {}, lock count: {}",
                                name, lock.referenceCount, lock.lockCount);
                    }
                }
            } else {
                logger.warn("Internal lock {} already released", name);
            }
        }
    }

    /**
     * Acquire or join existing DB lock.
     *
     * @param timeoutSeconds time in seconds during which lock needs to be obtained (it is not the lock duration)
     * @return true if lock successfully obtained
     */
    public boolean lock(int timeoutSeconds) {
        int remainingMilliSeconds = timeoutSeconds * 1000;
        Profiler profiler = new Profiler();
        boolean interrupted = false;
        try {
            while (true) {
                synchronized (this) {
                    if (ownerThread == Thread.currentThread()) {
                        logger.warn("Global lock {} re-entrance detected, owner thread: {}, reference count: {}, " +
                                "lock count: {}", getThreadName(ownerThread), name, referenceCount, lockCount);
                        // if it is re-entrance, then we may have more lock counts than needed?
                        lockCount++;

                        if (logger.isDebugEnabled()) {
                            logger.debug("Global lock {} joined, reference count: {}, lock count: {}",
                                    name, referenceCount, lockCount);
                        }
                        return true;
                    } else if (ownerThread != null) {
                        profiler.start();
                        try {
                            logger.debug("Waiting {} seconds to acquire global lock {}", timeoutSeconds, name);
                            wait(timeoutSeconds * 1000L);
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                        profiler.stop();

                        remainingMilliSeconds -= profiler.getDurationInMillis();
                        if (remainingMilliSeconds < 0) {
                            logger.warn("Timeout of {} seconds to acquire global lock {} has been reached, " +
                                    "owner thread {}, reference count: {}, lock count: {}", timeoutSeconds, name, getThreadName(ownerThread), referenceCount, lockCount);
                            return false;
                        }

                        continue;
                    } else {
                        // take ownership temporarily to prevent others enter into stage of acquiring DB lock
                        ownerThread = Thread.currentThread();
                        // XXX: do we need it here (???)
                        addRef();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Taking ownership on global lock {} to acquire, owner thread: {}, "
                                    + "reference count: {}, lock count: {}", name, getThreadName(ownerThread), referenceCount, lockCount);
                        }
                    }
                }

                int remainingSeconds = remainingMilliSeconds / 1000;
                if (logger.isDebugEnabled()) {
                    logger.debug("Acquiring global lock {} in DB within remaining {} seconds", name, remainingSeconds);
                }
                if (DbUtil.getGlobalLock(name, remainingSeconds)) {
                    synchronized (this) {
                        lockCount++;
                        holdingStartTick = System.currentTimeMillis();

                        if (logger.isDebugEnabled()) {
                            logger.debug("Global lock {} acquired, reference count: {}, lock count: {}",
                                    name, referenceCount, lockCount);
                        }
                        return true;
                    }
                } else {
                    synchronized (this) {
                        ownerThread = null;
                        releaseRef();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Failed to acquire global lock in DB {}, reference count: {}, " +
                                    "lock count: {}", name, referenceCount, lockCount);
                        }
                        return false;
                    }
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String getThreadName(Thread thread) {
        return Optional.ofNullable(thread).map(Thread::getName).orElse("N/A");
    }

    /**
     * Decrements lock count, decrements lock reference if no more locks left, and remove lock if no more references
     * left. Does the job only if lock owned by current thread.
     *
     * @return true if lock is owned by current thread
     */
    public boolean unlock() {
        synchronized (this) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unlock {}", name);
            }
            if (ownerThread == Thread.currentThread()) {
                lockCount--;
                if (lockCount < 0) {
                    ownerThread = null;
                    boolean result = DbUtil.releaseGlobalLock(name);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Global lock {} is returned to {} state, total holding time {} seconds",
                                name, result ? "successful" : "unsuccessful", (System.currentTimeMillis() - holdingStartTick) / 1000);
                    }
                    holdingStartTick = 0;

                    // release holding position in intern map when we released the DB connection
                    releaseRef();
                    notifyAll();
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("lock {} released, lock count: {}", name, lockCount);
                }
                return true;
            }
            return false;
        }
    }

    public String getName() {
        return name;
    }
}
