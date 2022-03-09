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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.management.StandardMBean;

import org.apache.log4j.Logger;

import com.cloud.utils.DateUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.time.InaccurateClock;

public class Merovingian2 extends StandardMBean implements MerovingianMBean {
    private static final Logger s_logger = Logger.getLogger(Merovingian2.class);

    private static final String ACQUIRE_SQL =
            "INSERT INTO op_lock (op_lock.key, op_lock.mac, op_lock.ip, op_lock.thread, op_lock.acquired_on, waiters) VALUES (?, ?, ?, ?, ?, 1)";
    private static final String INCREMENT_SQL = "UPDATE op_lock SET waiters=waiters+1 where op_lock.key=? AND op_lock.mac=? AND op_lock.ip=? AND op_lock.thread=?";
    private static final String SELECT_SQL = "SELECT op_lock.key, mac, ip, thread, acquired_on, waiters FROM op_lock";
    private static final String INQUIRE_SQL = SELECT_SQL + " WHERE op_lock.key=?";
    private static final String DECREMENT_SQL = "UPDATE op_lock SET waiters=waiters-1 where op_lock.key=? AND op_lock.mac=? AND op_lock.ip=? AND op_lock.thread=?";
    private static final String RELEASE_LOCK_SQL = "DELETE FROM op_lock WHERE op_lock.key = ?";
    private static final String RELEASE_SQL = RELEASE_LOCK_SQL + " AND op_lock.mac=? AND waiters=0";
    private static final String CLEANUP_MGMT_LOCKS_SQL = "DELETE FROM op_lock WHERE op_lock.mac = ?";
    private static final String SELECT_MGMT_LOCKS_SQL = SELECT_SQL + " WHERE mac=?";
    private static final String SELECT_THREAD_LOCKS_SQL = SELECT_SQL + " WHERE mac=? AND ip=?";
    private static final String CLEANUP_THREAD_LOCKS_SQL = "DELETE FROM op_lock WHERE mac=? AND ip=? AND thread=?";

    TimeZone _gmtTimeZone = TimeZone.getTimeZone("GMT");

    private final long _msId;

    private static Merovingian2 s_instance = null;
    private ConnectionConcierge _concierge = null;
    private static ThreadLocal<Count> s_tls = new ThreadLocal<Count>();

    private Merovingian2(long msId) {
        super(MerovingianMBean.class, false);
        _msId = msId;
        Connection conn = null;
        try {
            conn = TransactionLegacy.getStandaloneConnectionWithException();
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(true);
            _concierge = new ConnectionConcierge("LockController", conn, true);
        } catch (SQLException e) {
            s_logger.error("Unable to get a new db connection", e);
            throw new CloudRuntimeException("Unable to initialize a connection to the database for locking purposes", e);
        } finally {
            if (_concierge == null && conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    s_logger.debug("closing connection failed after everything else.", e);
                }
            }
        }
    }

    public static synchronized Merovingian2 createLockController(long msId) {
        assert s_instance == null : "No lock can serve two controllers.  Either we will hate the one and love the other, or we will be devoted to the one and despise the other.";
        s_instance = new Merovingian2(msId);
        s_instance.cleanupThisServer();
        try {
            JmxUtil.registerMBean("Locks", "Locks", s_instance);
        } catch (Exception e) {
            s_logger.error("Unable to register for JMX", e);
        }
        return s_instance;
    }

    public static Merovingian2 getLockController() {
        return s_instance;
    }

    protected void incrCount() {
        Count count = s_tls.get();
        if (count == null) {
            count = new Count();
            s_tls.set(count);
        }

        count.count++;
    }

    protected void decrCount() {
        Count count = s_tls.get();
        if (count == null) {
            return;
        }

        count.count--;
    }

    public boolean acquire(String key, int timeInSeconds) {
        Thread th = Thread.currentThread();
        String threadName = th.getName();
        int threadId = System.identityHashCode(th);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Acquiring lck-" + key + " with wait time of " + timeInSeconds);
        }
        long startTime = InaccurateClock.getTime();

        while ((InaccurateClock.getTime() - startTime) < (timeInSeconds * 1000l)) {
            int count = owns(key);

            if (count >= 1) {
                return increment(key, threadName, threadId);
            } else if (count == 0) {
                if (doAcquire(key, threadName, threadId)) {
                    return true;
                }
            }
            try {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Sleeping more time while waiting for lck-" + key);
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interrupted while aquiring " + key);
            }
        }
        String msg = "Timed out on acquiring lock " + key + " .  Waited for " + ((InaccurateClock.getTime() - startTime)/1000) +  "seconds";
        Exception e = new CloudRuntimeException(msg);
        s_logger.warn(msg, e);
        return false;
    }

    protected boolean increment(String key, String threadName, int threadId) {
      try (PreparedStatement pstmt = _concierge.conn().prepareStatement(INCREMENT_SQL);){
            pstmt.setString(1, key);
            pstmt.setLong(2, _msId);
            pstmt.setString(3, threadName);
            pstmt.setInt(4, threadId);
            int rows = pstmt.executeUpdate();
            assert (rows <= 1) : "hmm...non unique key? " + pstmt;
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("lck-" + key + (rows == 1 ? " acquired again" : " failed to acquire again"));
            }
            if (rows == 1) {
                incrCount();
                return true;
            }
            return false;
        } catch (Exception e) {
            s_logger.error("increment:Exception:"+e.getMessage());
            throw new CloudRuntimeException("increment:Exception:"+e.getMessage(), e);
        }
    }

    protected boolean doAcquire(String key, String threadName, int threadId) {
        long startTime = InaccurateClock.getTime();
        try(PreparedStatement pstmt = _concierge.conn().prepareStatement(ACQUIRE_SQL);) {
            pstmt.setString(1, key);
            pstmt.setLong(2, _msId);
            pstmt.setString(3, threadName);
            pstmt.setInt(4, threadId);
            pstmt.setString(5, DateUtil.getDateDisplayString(_gmtTimeZone, new Date()));
            try {
                int rows = pstmt.executeUpdate();
                if (rows == 1) {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Acquired for lck-" + key);
                    }
                    incrCount();
                    return true;
                }
            } catch (SQLException e) {
                if (!(e.getSQLState().equals("23000") && e.getErrorCode() == 1062)) {
                    throw new CloudRuntimeException("Unable to lock " + key + ".  Waited " + (InaccurateClock.getTime() - startTime), e);
                }
            }
        } catch (SQLException e) {
            s_logger.error("doAcquire:Exception:"+e.getMessage());
            throw new CloudRuntimeException("Unable to lock " + key + ".  Waited " + (InaccurateClock.getTime() - startTime), e);
        }

        s_logger.trace("Unable to acquire lck-" + key);
        return false;
    }

    protected Map<String, String> isLocked(String key) {
        try (PreparedStatement pstmt = _concierge.conn().prepareStatement(INQUIRE_SQL);){
            pstmt.setString(1, key);
            try(ResultSet rs = pstmt.executeQuery();)
            {
                if (!rs.next()) {
                    return null;
                }
                return toLock(rs);
            }catch (SQLException e) {
                s_logger.error("isLocked:Exception:"+e.getMessage());
                throw new CloudRuntimeException("isLocked:Exception:"+e.getMessage(), e);
            }
        } catch (SQLException e) {
            s_logger.error("isLocked:Exception:"+e.getMessage());
            throw new CloudRuntimeException("isLocked:Exception:"+e.getMessage(), e);
        }
    }

    public void cleanupThisServer() {
        cleanupForServer(_msId);
    }

    @Override
    public void cleanupForServer(long msId) {
        s_logger.info("Cleaning up locks for " + msId);
        try {
            synchronized (_concierge.conn()) {
                try(PreparedStatement pstmt = _concierge.conn().prepareStatement(CLEANUP_MGMT_LOCKS_SQL);) {
                    pstmt.setLong(1, msId);
                    int rows = pstmt.executeUpdate();
                    s_logger.info("Released " + rows + " locks for " + msId);
                }catch (Exception e) {
                    s_logger.error("cleanupForServer:Exception:"+e.getMessage());
                    throw new CloudRuntimeException("cleanupForServer:Exception:"+e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            s_logger.error("cleanupForServer:Exception:"+e.getMessage());
            throw new CloudRuntimeException("cleanupForServer:Exception:"+e.getMessage(), e);
        }
    }

    public boolean release(String key) {
        Thread th = Thread.currentThread();
        String threadName = th.getName();
        int threadId = System.identityHashCode(th);
        try (PreparedStatement pstmt = _concierge.conn().prepareStatement(DECREMENT_SQL);)
        {
            pstmt.setString(1, key);
            pstmt.setLong(2, _msId);
            pstmt.setString(3, threadName);
            pstmt.setLong(4, threadId);
            int rows = pstmt.executeUpdate();
            assert (rows <= 1) : "hmmm....keys not unique? " + pstmt;

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("lck-" + key + " released");
            }
            if (rows == 1) {
                try (PreparedStatement rel_sql_pstmt = _concierge.conn().prepareStatement(RELEASE_SQL);) {
                    rel_sql_pstmt.setString(1, key);
                    rel_sql_pstmt.setLong(2, _msId);
                    int result = rel_sql_pstmt.executeUpdate();
                    if (result == 1 && s_logger.isTraceEnabled()) {
                        s_logger.trace("lck-" + key + " removed");
                    }
                    decrCount();
                }catch (Exception e) {
                    s_logger.error("release:Exception:"+ e.getMessage());
                    throw new CloudRuntimeException("release:Exception:"+ e.getMessage(), e);
                }
            } else if (rows < 1) {
                String msg = ("Was unable to find lock for the key " + key + " and thread id " + threadId);
                Exception e = new CloudRuntimeException(msg);
                s_logger.warn(msg, e);
            }
            return rows == 1;
        } catch (Exception e) {
            s_logger.error("release:Exception:"+ e.getMessage());
            throw new CloudRuntimeException("release:Exception:"+ e.getMessage(), e);
        }
    }

    protected Map<String, String> toLock(ResultSet rs) throws SQLException {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", rs.getString(1));
        map.put("mgmt", rs.getString(2));
        map.put("name", rs.getString(3));
        map.put("tid", Integer.toString(rs.getInt(4)));
        map.put("date", rs.getString(5));
        map.put("count", Integer.toString(rs.getInt(6)));
        return map;

    }

    protected List<Map<String, String>> toLocks(ResultSet rs) throws SQLException {
        LinkedList<Map<String, String>> results = new LinkedList<Map<String, String>>();
        while (rs.next()) {
            results.add(toLock(rs));
        }
        return results;
    }

    protected List<Map<String, String>> getLocks(String sql, Long msId) {
        try (PreparedStatement pstmt = _concierge.conn().prepareStatement(sql);)
        {
            if (msId != null) {
                pstmt.setLong(1, msId);
            }
            try(ResultSet rs = pstmt.executeQuery();)
            {
                return toLocks(rs);
            }catch (Exception e) {
                s_logger.error("getLocks:Exception:"+e.getMessage());
                throw new CloudRuntimeException("getLocks:Exception:"+e.getMessage(), e);
            }
       } catch (Exception e) {
            s_logger.error("getLocks:Exception:"+e.getMessage());
            throw new CloudRuntimeException("getLocks:Exception:"+e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, String>> getAllLocks() {
        return getLocks(SELECT_SQL, null);
    }

    @Override
    public List<Map<String, String>> getLocksAcquiredByThisServer() {
        return getLocks(SELECT_MGMT_LOCKS_SQL, _msId);
    }

    public int owns(String key) {
        Thread th = Thread.currentThread();
        int threadId = System.identityHashCode(th);
        Map<String, String> owner = isLocked(key);
        if (owner == null) {
            return 0;
        }
        if (owner.get("mgmt").equals(Long.toString(_msId)) && owner.get("tid").equals(Integer.toString(threadId))) {
            return Integer.parseInt(owner.get("count"));
        }
        return -1;
    }

    public List<Map<String, String>> getLocksAcquiredBy(long msId, String threadName) {
        try (PreparedStatement pstmt = _concierge.conn().prepareStatement(SELECT_THREAD_LOCKS_SQL);){
            pstmt.setLong(1, msId);
            pstmt.setString(2, threadName);
            try (ResultSet rs =pstmt.executeQuery();) {
                return toLocks(rs);
            }
            catch (Exception e) {
                s_logger.error("getLocksAcquiredBy:Exception:"+e.getMessage());
                throw new CloudRuntimeException("Can't get locks " + pstmt, e);
            }
        } catch (Exception e) {
            s_logger.error("getLocksAcquiredBy:Exception:"+e.getMessage());
            throw new CloudRuntimeException("getLocksAcquiredBy:Exception:"+e.getMessage(), e);
        }
    }

    public void cleanupThread() {

        Count count = s_tls.get();
        if (count == null || count.count == 0) {
            return;
        }
        int c = count.count;
        count.count = 0;

        Thread th = Thread.currentThread();
        String threadName = th.getName();
        int threadId = System.identityHashCode(th);
        try (PreparedStatement pstmt = _concierge.conn().prepareStatement(CLEANUP_THREAD_LOCKS_SQL);)
        {
            pstmt.setLong(1, _msId);
            pstmt.setString(2, threadName);
            pstmt.setInt(3, threadId);
            int rows = pstmt.executeUpdate();
            assert (false) : "Abandon hope, all ye who enter here....There were still " + rows + ":" + c +
            " locks not released when the transaction ended, check for lock not released or @DB is not added to the code that using the locks!";
        } catch (Exception e) {
            s_logger.error("cleanupThread:Exception:" +  e.getMessage());
            throw new CloudRuntimeException("cleanupThread:Exception:" +  e.getMessage(), e);
        }
    }

    @Override
    public boolean releaseLockAsLastResortAndIReallyKnowWhatIAmDoing(String key) {
        s_logger.info("Releasing a lock from JMX lck-" + key);
        try (PreparedStatement pstmt = _concierge.conn().prepareStatement(RELEASE_LOCK_SQL);)
        {
            pstmt.setString(1, key);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            s_logger.error("releaseLockAsLastResortAndIReallyKnowWhatIAmDoing : Exception: " +  e.getMessage());
            return  false;
        }
    }

    protected static class Count {
        public int count = 0;
    }
}
