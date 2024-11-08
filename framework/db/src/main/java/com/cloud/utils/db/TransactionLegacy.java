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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Transaction abstracts away the Connection object in JDBC.  It allows the
 * following things that the Connection object does not.
 *
 *   1. Transaction can be started at an entry point and whether the DB
 *      actions should be auto-commit or not determined at that point.
 *   2. DB Connection is allocated only when it is needed.
 *   3. Code does not need to know if a transaction has been started or not.
 *      It just starts/ends a transaction and we resolve it correctly with
 *      the previous actions.
 *
 * Note that this class is not synchronous but it doesn't need to be because
 * it is stored with TLS and is one per thread.  Use appropriately.
 */
public class TransactionLegacy implements Closeable {
    protected static Logger LOGGER = LogManager.getLogger(Transaction.class.getName() + "." + "Transaction");
    protected Logger stmtLogger = LogManager.getLogger(Transaction.class.getName() + "." + "Statement");
    protected Logger lockLogger = LogManager.getLogger(Transaction.class.getName() + "." + "Lock");
    protected static Logger CONN_LOGGER = LogManager.getLogger(Transaction.class.getName() + "." + "Connection");

    private static final ThreadLocal<TransactionLegacy> tls = new ThreadLocal<TransactionLegacy>();
    private static final String START_TXN = "start_txn";
    private static final String CURRENT_TXN = "current_txn";
    private static final String CREATE_TXN = "create_txn";
    private static final String CREATE_CONN = "create_conn";
    private static final String STATEMENT = "statement";
    private static final String ATTACHMENT = "attachment";

    public static final short CLOUD_DB = 0;
    public static final short USAGE_DB = 1;
    public static final short SIMULATOR_DB = 3;

    public static final short CONNECTED_DB = -1;
    public static final String CONNECTION_PARAMS = "scrollTolerantForwardOnly=true";

    private static AtomicLong s_id = new AtomicLong();
    private static final TransactionMBeanImpl s_mbean = new TransactionMBeanImpl();
    static {
        try {
            JmxUtil.registerMBean("Transaction", "Transaction", s_mbean);
        } catch (Exception e) {
            LOGGER.error("Unable to register mbean for transaction", e);
        }
    }

    private static final String CONNECTION_POOL_LIB_DBCP = "dbcp";

    private final LinkedList<StackElement> _stack;
    private long _id;

    private final LinkedList<Pair<String, Long>> _lockTimes = new LinkedList<Pair<String, Long>>();

    private String _name;
    private Connection _conn;
    private boolean _txn;
    private short _dbId;
    private long _txnTime;
    private Statement _stmt;
    private String _creator;

    public static TransactionLegacy currentTxn() {
        return currentTxn(true);
    }

    protected static TransactionLegacy currentTxn(boolean check) {
        TransactionLegacy txn = tls.get();
        if (check) {
            assert txn != null : "No Transaction on stack.  Did you mark the method with @DB?";
        }
        return txn;
    }

    public static TransactionLegacy open(final short databaseId) {
        String name = buildName();
        if (name == null) {
            name = CURRENT_TXN;
        }
        return open(name, databaseId, true);
    }

    //
    // Usage of this transaction setup should be limited, it will always open a new transaction context regardless of whether or not there is other
    // transaction context in the stack. It is used in special use cases that we want to control DB connection explicitly and in the mean time utilize
    // the existing DAO features
    //
    public void transitToUserManagedConnection(Connection conn) {
        if (_conn != null)
            throw new IllegalStateException("Can't change to a user managed connection unless the db connection is null");

        _conn = conn;
        _dbId = CONNECTED_DB;
    }

    public void transitToAutoManagedConnection(short dbId) {
        // assert(_stack.size() <= 1) : "Can't change to auto managed connection unless your stack is empty";
        _dbId = dbId;
        _conn = null;
    }

    public static TransactionLegacy open(final String name) {
        return open(name, TransactionLegacy.CLOUD_DB, false);
    }

    public static TransactionLegacy open(final String name, final short databaseId, final boolean forceDbChange) {
        TransactionLegacy txn = tls.get();
        if (txn == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Creating the transaction: " + name);
            }
            txn = new TransactionLegacy(name, false, databaseId);
            tls.set(txn);
            s_mbean.addTransaction(txn);
        } else if (forceDbChange) {
            final short currentDbId = txn.getDatabaseId();
            if (currentDbId != databaseId) {
                // we need to end the current transaction and switch databases
                if (txn.close(txn.getName()) && txn.getCurrentConnection() == null) {
                    s_mbean.removeTransaction(txn);
                }

                txn = new TransactionLegacy(name, false, databaseId);
                tls.set(txn);
                s_mbean.addTransaction(txn);
            }
        }
        txn.checkConnection();
        txn.takeOver(name, false);
        return txn;
    }

    public void checkConnection() {
        try {
            if (_conn != null && !_conn.isValid(3)) {
                _conn = null;
            }
        } catch (SQLException e) {
            _conn = null;
        }
    }

    protected StackElement peekInStack(Object obj) {
        final Iterator<StackElement> it = _stack.iterator();
        while (it.hasNext()) {
            StackElement next = it.next();
            if (next.type == obj) {
                return next;
            }
        }
        return null;
    }

    public void registerLock(String sql) {
        if (_txn && lockLogger.isDebugEnabled()) {
            Pair<String, Long> time = new Pair<String, Long>(sql, System.currentTimeMillis());
            _lockTimes.add(time);
        }
    }

    public boolean dbTxnStarted() {
        return _txn;
    }

    public static Connection getStandaloneConnectionWithException() throws SQLException {
        Connection conn = s_ds.getConnection();
        if (CONN_LOGGER.isTraceEnabled()) {
            CONN_LOGGER.trace("Retrieving a standalone connection: dbconn" + System.identityHashCode(conn));
        }
        return conn;
    }

    public static Connection getStandaloneConnection() {
        try {
            return getStandaloneConnectionWithException();
        } catch (SQLException e) {
            LOGGER.error("Unexpected exception: ", e);
            return null;
        }
    }

    public static Connection getStandaloneUsageConnection() {
        try {
            Connection conn = s_usageDS.getConnection();
            if (CONN_LOGGER.isTraceEnabled()) {
                CONN_LOGGER.trace("Retrieving a standalone connection for usage: dbconn" + System.identityHashCode(conn));
            }
            return conn;
        } catch (SQLException e) {
            LOGGER.warn("Unexpected exception: ", e);
            return null;
        }
    }

    public static Connection getStandaloneSimulatorConnection() {
        try {
            Connection conn = s_simulatorDS.getConnection();
            if (CONN_LOGGER.isTraceEnabled()) {
                CONN_LOGGER.trace("Retrieving a standalone connection for simulator: dbconn" + System.identityHashCode(conn));
            }
            return conn;
        } catch (SQLException e) {
            LOGGER.warn("Unexpected exception: ", e);
            return null;
        }
    }

    protected void attach(TransactionAttachment value) {
        _stack.push(new StackElement(ATTACHMENT, value));
    }

    protected TransactionAttachment detach(String name) {
        Iterator<StackElement> it = _stack.descendingIterator();
        while (it.hasNext()) {
            StackElement element = it.next();
            if (element.type == ATTACHMENT) {
                TransactionAttachment att = (TransactionAttachment)element.ref;
                if (name.equals(att.getName())) {
                    it.remove();
                    return att;
                }
            }
        }
        assert false : "Are you sure you attached this: " + name;
        return null;
    }

    public static void attachToTxn(TransactionAttachment value) {
        TransactionLegacy txn = tls.get();
        assert txn != null && txn.peekInStack(CURRENT_TXN) != null : "Come on....how can we attach something to the transaction if you haven't started it?";

        txn.attach(value);
    }

    public static TransactionAttachment detachFromTxn(String name) {
        TransactionLegacy txn = tls.get();
        assert txn != null : "No Transaction in TLS";
        return txn.detach(name);
    }

    protected static boolean checkAnnotation(int stack, TransactionLegacy txn) {
        final StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
        StackElement se = txn.peekInStack(CURRENT_TXN);
        if (se == null) {
            return false;
        }

        StringBuffer sb = new StringBuffer();
        for (; stack < stacks.length; stack++) {
            String methodName = stacks[stack].getMethodName();
            sb.append(" ").append(methodName);
            if (methodName.equals(se.ref)) {
                return true;
            }
        }

        // relax stack structure for several places that @DB required injection is not in place
        LOGGER.warn("Non-standard stack context that Transaction context is manaully placed into the calling chain. Stack chain: " + sb);
        return true;
    }

    protected static String buildName() {
        if (LOGGER.isDebugEnabled()) {
            final StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
            final StringBuilder str = new StringBuilder();
            int i = 3, j = 3;
            while (j < 15 && i < stacks.length) {
                StackTraceElement element = stacks[i];
                String filename = element.getFileName();
                String method = element.getMethodName();
                if ((filename != null && filename.equals("<generated>")) || (method != null && method.equals("invokeSuper"))) {
                    i++;
                    continue;
                }

                str.append("-")
                .append(stacks[i].getClassName().substring(stacks[i].getClassName().lastIndexOf(".") + 1))
                .append(".")
                .append(stacks[i].getMethodName())
                .append(":")
                .append(stacks[i].getLineNumber());
                j++;
                i++;
            }
            return str.toString();
        }

        return "";
    }

    private TransactionLegacy(final String name, final boolean forLocking, final short databaseId) {
        _name = name;
        _conn = null;
        _stack = new LinkedList<StackElement>();
        _txn = false;
        _dbId = databaseId;
        _id = s_id.incrementAndGet();
        _creator = Thread.currentThread().getName();
    }

    public String getCreator() {
        return _creator;
    }

    public long getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public Short getDatabaseId() {
        return _dbId;
    }

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder((_name != null ? _name : ""));
        str.append(" : ");
        for (final StackElement se : _stack) {
            if (se.type == CURRENT_TXN) {
                str.append(se.ref).append(", ");
            }
        }

        return str.toString();
    }

    protected void mark(final String name) {
        _stack.push(new StackElement(CURRENT_TXN, name));
    }

    public boolean lock(final String name, final int timeoutSeconds) {
        Merovingian2 lockController = Merovingian2.getLockController();
        if (lockController == null) {
            throw new CloudRuntimeException("There's no support for locking yet");
        }
        return lockController.acquire(name, timeoutSeconds);
    }

    public boolean release(final String name) {
        Merovingian2 lockController = Merovingian2.getLockController();
        if (lockController == null) {
            throw new CloudRuntimeException("There's no support for locking yet");
        }
        return lockController.release(name);
    }

    /**
     * @deprecated Use {@link Transaction} for new code
     */
    @Deprecated
    public void start() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("txn: start requested by: " + buildName());
        }

        _stack.push(new StackElement(START_TXN, null));

        if (_txn) {
            LOGGER.trace("txn: has already been started.");
            return;
        }

        _txn = true;

        _txnTime = System.currentTimeMillis();
        if (_conn != null) {
            try {
                LOGGER.trace("txn: set auto commit to false");
                _conn.setAutoCommit(false);
            } catch (final SQLException e) {
                LOGGER.warn("Unable to set auto commit: ", e);
                throw new CloudRuntimeException("Unable to set auto commit: ", e);
            }
        }
    }

    protected void closePreviousStatement() {
        if (_stmt != null) {
            try {
                if (stmtLogger.isTraceEnabled()) {
                    stmtLogger.trace("Closing: " + _stmt.toString());
                }
                try {
                    ResultSet rs = _stmt.getResultSet();
                    if (rs != null && _stmt.getResultSetHoldability() != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
                        rs.close();
                    }
                } catch (SQLException e) {
                    stmtLogger.trace("Unable to close resultset");
                }
                _stmt.close();
            } catch (final SQLException e) {
                stmtLogger.trace("Unable to close statement: " + _stmt.toString());
            } finally {
                _stmt = null;
            }
        }
    }

    /**
     * Prepares an auto close statement.  The statement is closed automatically if it is
     * retrieved with this method.
     *
     * @param sql sql String
     * @return PreparedStatement
     * @throws SQLException if problem with JDBC layer.
     *
     * @see java.sql.Connection
     */
    public PreparedStatement prepareAutoCloseStatement(final String sql) throws SQLException {
        PreparedStatement stmt = prepareStatement(sql);
        closePreviousStatement();
        _stmt = stmt;
        return stmt;
    }

    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        final Connection conn = getConnection();
        final PreparedStatement pstmt = conn.prepareStatement(sql);
        if (stmtLogger.isTraceEnabled()) {
            stmtLogger.trace("Preparing: " + sql);
        }
        return pstmt;
    }

    /**
     * Prepares an auto close statement.  The statement is closed automatically if it is
     * retrieved with this method.
     *
     * @param sql sql String
     * @param autoGeneratedKeys keys that are generated
     * @return PreparedStatement
     * @throws SQLException if problem with JDBC layer.
     *
     * @see java.sql.Connection
     */
    public PreparedStatement prepareAutoCloseStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        final Connection conn = getConnection();
        final PreparedStatement pstmt = conn.prepareStatement(sql, autoGeneratedKeys);
        if (stmtLogger.isTraceEnabled()) {
            stmtLogger.trace("Preparing: " + sql);
        }
        closePreviousStatement();
        _stmt = pstmt;
        return pstmt;
    }

    /**
     * Prepares an auto close statement.  The statement is closed automatically if it is
     * retrieved with this method.
     *
     * @param sql sql String
     * @param columnNames names of the columns
     * @return PreparedStatement
     * @throws SQLException if problem with JDBC layer.
     *
     * @see java.sql.Connection
     */
    public PreparedStatement prepareAutoCloseStatement(final String sql, final String[] columnNames) throws SQLException {
        final Connection conn = getConnection();
        final PreparedStatement pstmt = conn.prepareStatement(sql, columnNames);
        if (stmtLogger.isTraceEnabled()) {
            stmtLogger.trace("Preparing: " + sql);
        }
        closePreviousStatement();
        _stmt = pstmt;
        return pstmt;
    }

    /**
     * Prepares an auto close statement.  The statement is closed automatically if it is
     * retrieved with this method.
     *
     * @param sql sql String
     * @return PreparedStatement
     * @throws SQLException if problem with JDBC layer.
     *
     * @see java.sql.Connection
     */
    public PreparedStatement prepareAutoCloseStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        final Connection conn = getConnection();
        final PreparedStatement pstmt = conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        if (stmtLogger.isTraceEnabled()) {
            stmtLogger.trace("Preparing: " + sql);
        }
        closePreviousStatement();
        _stmt = pstmt;
        return pstmt;
    }

    /**
     * Returns the db connection.
     *
     * Note: that you can call getConnection() but beaware that
     * all prepare statements from the Connection are not garbage
     * collected!
     *
     * @return DB Connection but make sure you understand that
     *         you are responsible for closing the PreparedStatement.
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        if (_conn == null) {
            switch (_dbId) {
            case CLOUD_DB:
                if (s_ds != null) {
                    _conn = s_ds.getConnection();
                } else {
                    LOGGER.warn("A static-initialized variable becomes null, process is dying?");
                    throw new CloudRuntimeException("Database is not initialized, process is dying?");
                }
                break;
            case USAGE_DB:
                if (s_usageDS != null) {
                    _conn = s_usageDS.getConnection();
                } else {
                    LOGGER.warn("A static-initialized variable becomes null, process is dying?");
                    throw new CloudRuntimeException("Database is not initialized, process is dying?");
                }
                break;
            case SIMULATOR_DB:
                if (s_simulatorDS != null) {
                    _conn = s_simulatorDS.getConnection();
                } else {
                    LOGGER.warn("A static-initialized variable becomes null, process is dying?");
                    throw new CloudRuntimeException("Database is not initialized, process is dying?");
                }
                break;
            default:

                throw new CloudRuntimeException("No database selected for the transaction");
            }
            _conn.setAutoCommit(!_txn);

            //
            // MySQL default transaction isolation level is REPEATABLE READ,
            // to reduce chances of DB deadlock, we will use READ COMMITED isolation level instead
            // see http://dev.mysql.com/doc/refman/5.0/en/innodb-deadlocks.html
            //
            _stack.push(new StackElement(CREATE_CONN, null));
            if (CONN_LOGGER.isTraceEnabled()) {
                CONN_LOGGER.trace("Creating a DB connection with " + (_txn ? " txn: " : " no txn: ") + " for " + _dbId + ": dbconn" + System.identityHashCode(_conn) +
                        ". Stack: " + buildName());
            }
        } else {
            LOGGER.trace("conn: Using existing DB connection");
        }

        return _conn;
    }

    protected boolean takeOver(final String name, final boolean create) {
        if (_stack.size() != 0) {
            if (!create) {
                // If it is not a create transaction, then let's just use the current one.
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Using current transaction: " + toString());
                }
                mark(name);
                return false;
            }

            final StackElement se = _stack.getFirst();
            if (se.type == CREATE_TXN) {
                // This create is called inside of another create.  Which is ok?
                // We will let that create be responsible for cleaning up.
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Create using current transaction: " + toString());
                }
                mark(name);
                return false;
            }

            LOGGER.warn("Encountered a transaction that has leaked.  Cleaning up. " + toString());
            cleanup();
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Took over the transaction: " + name);
        }
        _stack.push(new StackElement(create ? CREATE_TXN : CURRENT_TXN, name));
        _name = name;
        return true;
    }

    public void cleanup() {
        closePreviousStatement();

        removeUpTo(null, null);
        if (_txn) {
            rollbackTransaction();
        }
        _txn = false;
        _name = null;

        closeConnection();

        _stack.clear();
        Merovingian2 lockController = Merovingian2.getLockController();
        if (lockController != null) {
            lockController.cleanupThread();
        }
    }

    @Override
    public void close() {
        removeUpTo(CURRENT_TXN, null);

        if (_stack.size() == 0) {
            LOGGER.trace("Transaction is done");
            cleanup();
        }
    }

    /**
     * close() is used by endTxn to close the connection.  This method only
     * closes the connection if the name is the same as what's stored.
     *
     * @param name
     * @return true if this close actually closes the connection.  false if not.
     */
    public boolean close(final String name) {
        if (_name == null) {    // Already cleaned up.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Already cleaned up." + buildName());
            }
            return true;
        }

        if (!_name.equals(name)) {
            close();
            return false;
        }

        if (LOGGER.isDebugEnabled() && _stack.size() > 2) {
            LOGGER.debug("Transaction is not closed properly: " + toString() + ".  Called by " + buildName());
        }

        cleanup();

        LOGGER.trace("All done");
        return true;
    }

    protected boolean hasTxnInStack() {
        return peekInStack(START_TXN) != null;
    }

    protected void clearLockTimes() {
        if (lockLogger.isDebugEnabled()) {
            for (Pair<String, Long> time : _lockTimes) {
                lockLogger.trace("SQL " + time.first() + " took " + (System.currentTimeMillis() - time.second()));
            }
            _lockTimes.clear();
        }
    }

    public boolean commit() {
        if (!_txn) {
            LOGGER.warn("txn: Commit called when it is not a transaction: " + buildName());
            return false;
        }

        Iterator<StackElement> it = _stack.iterator();
        while (it.hasNext()) {
            StackElement st = it.next();
            if (st.type == START_TXN) {
                it.remove();
                break;
            }
        }

        if (hasTxnInStack()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("txn: Not committing because transaction started elsewhere: " + buildName() + " / " + toString());
            }
            return false;
        }

        _txn = false;
        try {
            if (_conn != null) {
                _conn.commit();
                LOGGER.trace("txn: DB Changes committed. Time = " + (System.currentTimeMillis() - _txnTime));
                clearLockTimes();
                closeConnection();
            }
            return true;
        } catch (final SQLException e) {
            rollbackTransaction();
            throw new CloudRuntimeException("Unable to commit or close the connection. ", e);
        }
    }

    protected void closeConnection() {
        closePreviousStatement();

        if (_conn == null) {
            return;
        }

        if (_txn) {
            CONN_LOGGER.trace("txn: Not closing DB connection because we're still in a transaction.");
            return;
        }

        try {
            // we should only close db connection when it is not user managed
            if (_dbId != CONNECTED_DB) {
                if (CONN_LOGGER.isTraceEnabled()) {
                    CONN_LOGGER.trace("Closing DB connection: dbconn" + System.identityHashCode(_conn));
                }
                _conn.close();
                _conn = null;
                s_mbean.removeTransaction(this);
            }
        } catch (final SQLException e) {
            LOGGER.warn("Unable to close connection", e);
        }
    }

    protected void removeUpTo(String type, Object ref) {
        boolean rollback = false;
        Iterator<StackElement> it = _stack.iterator();
        while (it.hasNext()) {
            StackElement item = it.next();

            it.remove();

            try {
                if ( (type == null || type.equals(item.type)) && (ref == null || ref.equals(item.ref))) {
                    break;
                }

                if (item.type == CURRENT_TXN) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Releasing the current txn: " + (item.ref != null ? item.ref : ""));
                    }
                } else if (item.type == CREATE_CONN) {
                    closeConnection();
                } else if (item.type == START_TXN) {
                    if (item.ref == null) {
                        rollback = true;
                    } else {
                        try {
                            _conn.rollback((Savepoint)ref);
                            rollback = false;
                        } catch (final SQLException e) {
                            LOGGER.warn("Unable to rollback Txn.", e);
                        }
                    }
                } else if (item.type == STATEMENT) {
                    try {
                        if (stmtLogger.isTraceEnabled()) {
                            stmtLogger.trace("Closing: " + ref.toString());
                        }
                        Statement stmt = (Statement)ref;
                        try {
                            ResultSet rs = stmt.getResultSet();
                            if (rs != null) {
                                rs.close();
                            }
                        } catch (SQLException e) {
                            stmtLogger.trace("Unable to close resultset");
                        }
                        stmt.close();
                    } catch (final SQLException e) {
                        stmtLogger.trace("Unable to close statement: " + item);
                    }
                } else if (item.type == ATTACHMENT) {
                    TransactionAttachment att = (TransactionAttachment)item.ref;
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Cleaning up " + att.getName());
                    }
                    att.cleanup();
                }
            } catch (Exception e) {
                LOGGER.error("Unable to clean up " + item, e);
            }
        }

        if (rollback) {
            rollback();
        }
    }

    protected void rollbackTransaction() {
        closePreviousStatement();
        if (!_txn) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Rollback called for " + _name + " when there's no transaction: " + buildName());
            }
            return;
        }
        assert (!hasTxnInStack()) : "Who's rolling back transaction when there's still txn in stack?";
        _txn = false;
        try {
            if (_conn != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Rolling back the transaction: Time = " + (System.currentTimeMillis() - _txnTime) + " Name =  " + _name + "; called by " + buildName());
                }
                _conn.rollback();
            }
            clearLockTimes();
            closeConnection();
        } catch (final SQLException e) {
            LOGGER.warn("Unable to rollback", e);
        }
    }

    protected void rollbackSavepoint(Savepoint sp) {
        try {
            if (_conn != null) {
                _conn.rollback(sp);
            }
        } catch (SQLException e) {
            LOGGER.warn("Unable to rollback to savepoint " + sp);
        }

        if (!hasTxnInStack()) {
            _txn = false;
            closeConnection();
        }
    }

    public void rollback() {
        Iterator<StackElement> it = _stack.iterator();
        while (it.hasNext()) {
            StackElement st = it.next();
            if (st.type == START_TXN) {
                if (st.ref == null) {
                    it.remove();
                } else {
                    rollback((Savepoint)st.ref);
                    return;
                }
            }
        }

        rollbackTransaction();
    }

    public Savepoint setSavepoint() throws SQLException {
        _txn = true;
        StackElement st = new StackElement(START_TXN, null);
        _stack.push(st);
        final Connection conn = getConnection();
        final Savepoint sp = conn.setSavepoint();
        st.ref = sp;

        return sp;
    }

    public Savepoint setSavepoint(final String name) throws SQLException {
        _txn = true;
        StackElement st = new StackElement(START_TXN, null);
        _stack.push(st);
        final Connection conn = getConnection();
        final Savepoint sp = conn.setSavepoint(name);
        st.ref = sp;

        return sp;
    }

    public void releaseSavepoint(final Savepoint sp) throws SQLException {
        removeTxn(sp);
        if (_conn != null) {
            _conn.releaseSavepoint(sp);
        }

        if (!hasTxnInStack()) {
            _txn = false;
            closeConnection();
        }
    }

    protected boolean hasSavepointInStack(Savepoint sp) {
        Iterator<StackElement> it = _stack.iterator();
        while (it.hasNext()) {
            StackElement se = it.next();
            if (se.type == START_TXN && se.ref == sp) {
                return true;
            }
        }
        return false;
    }

    protected void removeTxn(Savepoint sp) {
        assert hasSavepointInStack(sp) : "Removing a save point that's not in the stack";

        if (!hasSavepointInStack(sp)) {
            return;
        }

        Iterator<StackElement> it = _stack.iterator();
        while (it.hasNext()) {
            StackElement se = it.next();
            if (se.type == START_TXN) {
                it.remove();
                if (se.ref == sp) {
                    return;
                }
            }
        }
    }

    public void rollback(final Savepoint sp) {
        removeTxn(sp);

        rollbackSavepoint(sp);
    }

    public Connection getCurrentConnection() {
        return _conn;
    }

    public List<StackElement> getStack() {
        return _stack;
    }

    private TransactionLegacy() {
        _name = null;
        _conn = null;
        _stack = null;
        _txn = false;
        _dbId = -1;
    }

    @Override
    protected void finalize() throws Throwable {
        if (!(_conn == null && (_stack == null || _stack.size() == 0))) {
            assert (false) : "Oh Alex oh alex...something is wrong with how we're doing this";
            LOGGER.error("Something went wrong that a transaction is orphaned before db connection is closed");
            cleanup();
        }
    }

    protected class StackElement {
        public String type;
        public Object ref;

        public StackElement(String type, Object ref) {
            this.type = type;
            this.ref = ref;
        }

        @Override
        public String toString() {
            return type + "-" + ref;
        }
    }

    private static DataSource s_ds;
    private static DataSource s_usageDS;
    private static DataSource s_simulatorDS;
    protected static boolean s_dbHAEnabled;

    static {
        // Initialize with assumed db.properties file
        initDataSource(DbProperties.getDbProperties());
    }

    public static void initDataSource(String propsFileName) throws IOException {
        Properties dbProps = new Properties();
        File dbPropsFile = PropertiesUtil.findConfigFile(propsFileName);
        if (dbPropsFile != null && dbPropsFile.exists()) {
            PropertiesUtil.loadFromFile(dbProps, dbPropsFile);
            initDataSource(dbProps);
        }
    }

    private static <T extends Number> T parseNumber(String value, Class<T> type) {
        if (value == null) {
            return null;
        }
        try {
            if (type.equals(Long.class)) {
                return type.cast(Long.parseLong(value));
            } else {
                return type.cast(Integer.parseInt(value));
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void initDataSource(Properties dbProps) {
        try {
            if (dbProps.size() == 0)
                return;

            s_dbHAEnabled = Boolean.valueOf(dbProps.getProperty("db.ha.enabled"));
            LOGGER.info("Is Data Base High Availiability enabled? Ans : " + s_dbHAEnabled);
            String loadBalanceStrategy = dbProps.getProperty("db.ha.loadBalanceStrategy");
            // FIXME:  If params are missing...default them????
            final Integer cloudMaxActive = parseNumber(dbProps.getProperty("db.cloud.maxActive"), Integer.class);
            final Integer cloudMaxIdle = parseNumber(dbProps.getProperty("db.cloud.maxIdle"), Integer.class);
            final Long cloudMaxWait = parseNumber(dbProps.getProperty("db.cloud.maxWait"), Long.class);
            final Integer cloudMinIdleConnections = parseNumber(dbProps.getProperty("db.cloud.minIdleConnections"), Integer.class);
            final Long cloudConnectionTimeout = parseNumber(dbProps.getProperty("db.cloud.connectionTimeout"), Long.class);
            final Long cloudKeepAliveTimeout = parseNumber(dbProps.getProperty("db.cloud.keepAliveTime"), Long.class);
            final String cloudUsername = dbProps.getProperty("db.cloud.username");
            final String cloudPassword = dbProps.getProperty("db.cloud.password");
            final String cloudValidationQuery = dbProps.getProperty("db.cloud.validationQuery");
            final String cloudIsolationLevel = dbProps.getProperty("db.cloud.isolation.level");

            int isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
            if (cloudIsolationLevel == null) {
                isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
            } else if (cloudIsolationLevel.equalsIgnoreCase("readcommitted")) {
                isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
            } else if (cloudIsolationLevel.equalsIgnoreCase("repeatableread")) {
                isolationLevel = Connection.TRANSACTION_REPEATABLE_READ;
            } else if (cloudIsolationLevel.equalsIgnoreCase("serializable")) {
                isolationLevel = Connection.TRANSACTION_SERIALIZABLE;
            } else if (cloudIsolationLevel.equalsIgnoreCase("readuncommitted")) {
                isolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED;
            } else {
                LOGGER.warn("Unknown isolation level " + cloudIsolationLevel + ".  Using read uncommitted");
            }

            final boolean cloudTestOnBorrow = Boolean.parseBoolean(dbProps.getProperty("db.cloud.testOnBorrow"));
            final boolean cloudTestWhileIdle = Boolean.parseBoolean(dbProps.getProperty("db.cloud.testWhileIdle"));
            final long cloudTimeBtwEvictionRunsMillis = Long.parseLong(dbProps.getProperty("db.cloud.timeBetweenEvictionRunsMillis"));
            final long cloudMinEvcitableIdleTimeMillis = Long.parseLong(dbProps.getProperty("db.cloud.minEvictableIdleTimeMillis"));

            final boolean useSSL = Boolean.parseBoolean(dbProps.getProperty("db.cloud.useSSL"));
            if (useSSL) {
                System.setProperty("javax.net.ssl.keyStore", dbProps.getProperty("db.cloud.keyStore"));
                System.setProperty("javax.net.ssl.keyStorePassword", dbProps.getProperty("db.cloud.keyStorePassword"));
                System.setProperty("javax.net.ssl.trustStore", dbProps.getProperty("db.cloud.trustStore"));
                System.setProperty("javax.net.ssl.trustStorePassword", dbProps.getProperty("db.cloud.trustStorePassword"));
            }

            Pair<String, String> cloudUriAndDriver = getConnectionUriAndDriver(dbProps, loadBalanceStrategy, useSSL, "cloud");

            DriverLoader.loadDriver(cloudUriAndDriver.second());

            // Default Data Source for CloudStack
            s_ds = createDataSource(dbProps.getProperty("db.cloud.connectionPoolLib"), cloudUriAndDriver.first(),
                    cloudUsername, cloudPassword, cloudMaxActive, cloudMaxIdle, cloudMaxWait,
                    cloudTimeBtwEvictionRunsMillis, cloudMinEvcitableIdleTimeMillis, cloudTestWhileIdle,
                    cloudTestOnBorrow, cloudValidationQuery, cloudMinIdleConnections, cloudConnectionTimeout,
                    cloudKeepAliveTimeout, isolationLevel, "cloud");

            // Configure the usage db
            final Integer usageMaxActive = parseNumber(dbProps.getProperty("db.usage.maxActive"), Integer.class);
            final Integer usageMaxIdle = parseNumber(dbProps.getProperty("db.usage.maxIdle"), Integer.class);
            final Long usageMaxWait = parseNumber(dbProps.getProperty("db.usage.maxWait"), Long.class);
            final Integer usageMinIdleConnections = parseNumber(dbProps.getProperty("db.usage.minIdleConnections"), Integer.class);
            final Long usageConnectionTimeout = parseNumber(dbProps.getProperty("db.usage.connectionTimeout"), Long.class);
            final Long usageKeepAliveTimeout = parseNumber(dbProps.getProperty("db.usage.keepAliveTime"), Long.class);
            final String usageUsername = dbProps.getProperty("db.usage.username");
            final String usagePassword = dbProps.getProperty("db.usage.password");

            Pair<String, String> usageUriAndDriver = getConnectionUriAndDriver(dbProps, loadBalanceStrategy, useSSL, "usage");

            DriverLoader.loadDriver(usageUriAndDriver.second());

            // Data Source for usage server
            s_usageDS = createDataSource(dbProps.getProperty("db.usage.connectionPoolLib"), usageUriAndDriver.first(),
                    usageUsername, usagePassword, usageMaxActive, usageMaxIdle, usageMaxWait, null,
                    null, null, null, null,
                    usageMinIdleConnections, usageConnectionTimeout, usageKeepAliveTimeout, isolationLevel, "usage");

            try {
                // Configure the simulator db
                final Integer simulatorMaxActive = parseNumber(dbProps.getProperty("db.simulator.maxActive"), Integer.class);
                final Integer simulatorMaxIdle = parseNumber(dbProps.getProperty("db.simulator.maxIdle"), Integer.class);
                final Long simulatorMaxWait = parseNumber(dbProps.getProperty("db.simulator.maxWait"), Long.class);
                final Integer simulatorMinIdleConnections = parseNumber(dbProps.getProperty("db.simulator.minIdleConnections"), Integer.class);
                final Long simulatorConnectionTimeout = parseNumber(dbProps.getProperty("db.simulator.connectionTimeout"), Long.class);
                final Long simulatorKeepAliveTimeout = parseNumber(dbProps.getProperty("db.simulator.keepAliveTime"), Long.class);
                final String simulatorUsername = dbProps.getProperty("db.simulator.username");
                final String simulatorPassword = dbProps.getProperty("db.simulator.password");

                String simulatorDriver;
                String simulatorConnectionUri;
                String simulatorUri = dbProps.getProperty("db.simulator.uri");

                if (StringUtils.isEmpty(simulatorUri)) {
                    simulatorDriver = dbProps.getProperty("db.simulator.driver");
                    final int simulatorPort = Integer.parseInt(dbProps.getProperty("db.simulator.port"));
                    final String simulatorDbName = dbProps.getProperty("db.simulator.name");
                    final boolean simulatorAutoReconnect = Boolean.parseBoolean(dbProps.getProperty("db.simulator.autoReconnect"));
                    final String simulatorHost = dbProps.getProperty("db.simulator.host");

                    simulatorConnectionUri = simulatorDriver + "://" + simulatorHost + ":" + simulatorPort + "/" + simulatorDbName + "?autoReconnect=" +
                            simulatorAutoReconnect;
                } else {
                    LOGGER.warn("db.simulator.uri was set, ignoring the following properties on db.properties: [db.simulator.driver, db.simulator.host, db.simulator.port, "
                            + "db.simulator.name, db.simulator.autoReconnect].");
                    String[] splitUri = simulatorUri.split(":");
                    simulatorDriver = String.format("%s:%s", splitUri[0], splitUri[1]);
                    simulatorConnectionUri = simulatorUri;
                }

                DriverLoader.loadDriver(simulatorDriver);

                s_simulatorDS = createDataSource(dbProps.getProperty("db.simulator.connectionPoolLib"),
                        simulatorConnectionUri, simulatorUsername, simulatorPassword, simulatorMaxActive,
                        simulatorMaxIdle, simulatorMaxWait, null, null, null, null,
                        cloudValidationQuery, simulatorMinIdleConnections, simulatorConnectionTimeout,
                        simulatorKeepAliveTimeout, isolationLevel, "simulator");
            } catch (Exception e) {
                LOGGER.debug("Simulator DB properties are not available. Not initializing simulator DS");
            }
        } catch (final Exception e) {
            s_ds = getDefaultDataSource(dbProps.getProperty("db.cloud.connectionPoolLib"), "cloud");
            s_usageDS = getDefaultDataSource(dbProps.getProperty("db.usage.connectionPoolLib"), "cloud_usage");
            s_simulatorDS = getDefaultDataSource(dbProps.getProperty("db.simulator.connectionPoolLib"), "simulator");
            LOGGER.warn(
                    "Unable to load db configuration, using defaults with 5 connections. Falling back on assumed datasource on localhost:3306 using username:password=cloud:cloud. Please check your configuration",
                    e);
        }
    }

    protected static Pair<String, String> getConnectionUriAndDriver(Properties dbProps, String loadBalanceStrategy, boolean useSSL, String schema) {
        String connectionUri;
        String driver;
        String propertyUri = dbProps.getProperty(String.format("db.%s.uri", schema));

        if (StringUtils.isEmpty(propertyUri)) {
            driver = dbProps.getProperty(String.format("db.%s.driver", schema));
            connectionUri = getPropertiesAndBuildConnectionUri(dbProps, loadBalanceStrategy, driver, useSSL, schema);
        } else {
            LOGGER.warn(String.format("db.%s.uri was set, ignoring the following properties for schema %s of db.properties: [host, port, name, driver, autoReconnect, url.params,"
                    + " replicas, ha.loadBalanceStrategy, ha.enable, failOverReadOnly, reconnectAtTxEnd, autoReconnectForPools, secondsBeforeRetrySource, queriesBeforeRetrySource, "
                    + "initialTimeout].", schema, schema));

            String[] splitUri = propertyUri.split(":");
            driver = String.format("%s:%s", splitUri[0], splitUri[1]);

            connectionUri = propertyUri;
        }
        LOGGER.info(String.format("Using the following URI to connect to %s database [%s].", schema, connectionUri));
        return new Pair<>(connectionUri, driver);
    }

    protected static String getPropertiesAndBuildConnectionUri(Properties dbProps, String loadBalanceStrategy, String driver, boolean useSSL, String schema) {
        String host = dbProps.getProperty(String.format("db.%s.host", schema));
        int port = Integer.parseInt(dbProps.getProperty(String.format("db.%s.port", schema)));
        String dbName = dbProps.getProperty(String.format("db.%s.name", schema));
        boolean autoReconnect = Boolean.parseBoolean(dbProps.getProperty(String.format("db.%s.autoReconnect", schema)));
        String urlParams = dbProps.getProperty(String.format("db.%s.url.params", schema));

        String replicas = null;
        String dbHaParams = null;
        if (s_dbHAEnabled) {
            dbHaParams = getDBHAParams(schema, dbProps);
            replicas = dbProps.getProperty(String.format("db.%s.replicas", schema));
            LOGGER.info(String.format("The replicas configured for %s data base are %s.", schema, replicas));
        }

        return buildConnectionUri(loadBalanceStrategy, driver, useSSL, host, replicas, port, dbName, autoReconnect, urlParams, dbHaParams);
    }

    protected static String buildConnectionUri(String loadBalanceStrategy, String driver, boolean useSSL, String host, String replicas, int port, String dbName, boolean autoReconnect,
            String urlParams, String dbHaParams) {

        StringBuilder connectionUri = new StringBuilder();
        connectionUri.append(driver);
        connectionUri.append("://");
        connectionUri.append(host);

        if (s_dbHAEnabled) {
            connectionUri.append(",");
            connectionUri.append(replicas);
        }

        connectionUri.append(":");
        connectionUri.append(port);
        connectionUri.append("/");
        connectionUri.append(dbName);
        connectionUri.append("?autoReconnect=");
        connectionUri.append(autoReconnect);

        if (urlParams != null) {
            connectionUri.append("&");
            connectionUri.append(urlParams);
        }

        if (useSSL) {
            connectionUri.append("&useSSL=true");
        }

        if (s_dbHAEnabled) {
            connectionUri.append("&");
            connectionUri.append(dbHaParams);
            connectionUri.append("&loadBalanceStrategy=");
            connectionUri.append(loadBalanceStrategy);
        }

        connectionUri.append("&");
        connectionUri.append(CONNECTION_PARAMS);

        return connectionUri.toString();
    }

    /**
     * Creates a data source
     */
    private static DataSource createDataSource(String connectionPoolLib, String uri, String username, String password,
               Integer maxActive, Integer maxIdle, Long maxWait, Long timeBtwnEvictionRuns, Long minEvictableIdleTime,
               Boolean testWhileIdle, Boolean testOnBorrow, String validationQuery, Integer minIdleConnections,
               Long connectionTimeout, Long keepAliveTime, Integer isolationLevel, String dsName) {
        LOGGER.debug("Creating datasource for database: {} with connection pool lib: {}", dsName,
                connectionPoolLib);
        if (CONNECTION_POOL_LIB_DBCP.equals(connectionPoolLib)) {
            return createDbcpDataSource(uri, username, password, maxActive, maxIdle, maxWait, timeBtwnEvictionRuns,
                    minEvictableIdleTime, testWhileIdle, testOnBorrow, validationQuery, isolationLevel);
        }
        return createHikaricpDataSource(uri, username, password, maxActive, maxIdle, maxWait, minIdleConnections,
                connectionTimeout, keepAliveTime, isolationLevel, dsName);
    }

    private static DataSource createHikaricpDataSource(String uri, String username, String password,
                                               Integer maxActive, Integer maxIdle, Long maxWait,
                                               Integer minIdleConnections, Long connectionTimeout, Long keepAliveTime,
                                               Integer isolationLevel, String dsName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(uri);
        config.setUsername(username);
        config.setPassword(password);

        config.setPoolName(dsName);

        // Connection pool properties
        config.setMaximumPoolSize(ObjectUtils.defaultIfNull(maxActive, 250));
        config.setIdleTimeout(ObjectUtils.defaultIfNull(maxIdle, 30) * 1000);
        config.setMaxLifetime(ObjectUtils.defaultIfNull(maxWait, 600000L));
        config.setMinimumIdle(ObjectUtils.defaultIfNull(minIdleConnections, 5));
        config.setConnectionTimeout(ObjectUtils.defaultIfNull(connectionTimeout, 30000L));
        config.setKeepaliveTime(ObjectUtils.defaultIfNull(keepAliveTime, 600000L));

        String isolationLevelString = "TRANSACTION_READ_COMMITTED";
        if (isolationLevel == Connection.TRANSACTION_SERIALIZABLE) {
            isolationLevelString = "TRANSACTION_SERIALIZABLE";
        } else if (isolationLevel == Connection.TRANSACTION_READ_UNCOMMITTED) {
            isolationLevelString = "TRANSACTION_READ_UNCOMMITTED";
        } else if (isolationLevel == Connection.TRANSACTION_REPEATABLE_READ) {
            isolationLevelString = "TRANSACTION_REPEATABLE_READ";
        }
        config.setTransactionIsolation(isolationLevelString);

        // Standard datasource config for MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        // Additional config for MySQL
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        HikariDataSource dataSource = new HikariDataSource(config);
        return dataSource;
    }

    private static DataSource createDbcpDataSource(String uri, String username, String password,
                                                Integer maxActive, Integer maxIdle, Long maxWait,
                                                Long timeBtwnEvictionRuns, Long minEvictableIdleTime,
                                                Boolean testWhileIdle, Boolean testOnBorrow,
                                                String validationQuery, Integer isolationLevel) {
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(uri, username, password);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
        GenericObjectPoolConfig config = createPoolConfig(maxActive, maxIdle, maxWait, timeBtwnEvictionRuns, minEvictableIdleTime, testWhileIdle, testOnBorrow);
        ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory, config);
        poolableConnectionFactory.setPool(connectionPool);
        if (validationQuery != null) {
            poolableConnectionFactory.setValidationQuery(validationQuery);
        }
        if (isolationLevel != null) {
            poolableConnectionFactory.setDefaultTransactionIsolation(isolationLevel);
        }
        return new PoolingDataSource<>(connectionPool);
    }

    /**
     * Return a GenericObjectPoolConfig configuration usable on connection pool creation
     */
    private static GenericObjectPoolConfig createPoolConfig(Integer maxActive, Integer maxIdle, Long maxWait,
                                                            Long timeBtwnEvictionRuns, Long minEvictableIdleTime,
                                                            Boolean testWhileIdle, Boolean testOnBorrow) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMaxWaitMillis(maxWait);

        if (timeBtwnEvictionRuns != null) {
            config.setTimeBetweenEvictionRunsMillis(timeBtwnEvictionRuns);
        }
        if (minEvictableIdleTime != null) {
            config.setMinEvictableIdleTimeMillis(minEvictableIdleTime);
        }
        if (testWhileIdle != null) {
            config.setTestWhileIdle(testWhileIdle);
        }
        if (testOnBorrow != null) {
            config.setTestOnBorrow(testOnBorrow);
        }
        return config;
    }

    private static DataSource getDefaultDataSource(final String connectionPoolLib, final String database) {
        LOGGER.debug("Creating default datasource for database: {} with connection pool lib: {}",
                database, connectionPoolLib);
        if (CONNECTION_POOL_LIB_DBCP.equalsIgnoreCase(connectionPoolLib)) {
            return getDefaultDbcpDataSource(database);
        }
        return getDefaultHikaricpDataSource(database);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DataSource getDefaultHikaricpDataSource(final String database) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/" + database + "?" + CONNECTION_PARAMS);
        config.setUsername("cloud");
        config.setPassword("cloud");
        config.setPoolName(database);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(250);
        config.setConnectionTimeout(1000);
        config.setIdleTimeout(1000);
        config.setKeepaliveTime(1000);
        config.setMaxLifetime(1000);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        config.setInitializationFailTimeout(-1L);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DataSource getDefaultDbcpDataSource(final String database) {
        final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:mysql://localhost:3306/" + database  + "?" + CONNECTION_PARAMS, "cloud", "cloud");
        final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
        final GenericObjectPool connectionPool = new GenericObjectPool(poolableConnectionFactory);
        return new PoolingDataSource(connectionPool);
    }

    private static String getDBHAParams(String dbName, Properties dbProps) {
        StringBuilder sb = new StringBuilder();
        sb.append("failOverReadOnly=" + dbProps.getProperty("db." + dbName + ".failOverReadOnly"));
        sb.append("&").append("reconnectAtTxEnd=" + dbProps.getProperty("db." + dbName + ".reconnectAtTxEnd"));
        sb.append("&").append("autoReconnectForPools=" + dbProps.getProperty("db." + dbName + ".autoReconnectForPools"));
        sb.append("&").append("secondsBeforeRetrySource=" + dbProps.getProperty("db." + dbName + ".secondsBeforeRetrySource"));
        sb.append("&").append("queriesBeforeRetrySource=" + dbProps.getProperty("db." + dbName + ".queriesBeforeRetrySource"));
        sb.append("&").append("initialTimeout=" + dbProps.getProperty("db." + dbName + ".initialTimeout"));
        return sb.toString();
    }

    /**
     * Used for unit testing primarily
     *
     * @param conn
     */
    protected void setConnection(Connection conn) {
        _conn = conn;
    }

    /**
     * Receives a list of {@link PreparedStatement} and quietly closes all of them, which
     * triggers also closing their dependent objects, like a {@link ResultSet}
     *
     * @param pstmt2Close
     */
    public static void closePstmts(List<PreparedStatement> pstmt2Close) {
        for (PreparedStatement pstmt : pstmt2Close) {
            try {
                if (pstmt != null && !pstmt.isClosed()) {
                    pstmt.close();
                }
            } catch (SQLException e) {
                // It's not possible to recover from this and we need to continue closing
                e.printStackTrace();
            }
        }
    }

}
