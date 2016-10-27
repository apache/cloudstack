package com.cloud.utils.db;

import java.sql.SQLException;

public interface DbConnectionObserver {
    void onError(SQLException se);
    void onSuccess();
}
