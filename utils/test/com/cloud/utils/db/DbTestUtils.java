/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.cloud.utils.PropertiesUtil;

public class DbTestUtils {
    
    public static void executeScript(String file, boolean autoCommit, boolean stopOnError) {
        File cleanScript = PropertiesUtil.findConfigFile(file);
        if (cleanScript == null) {
            throw new RuntimeException("Unable to clean the database because I can't find " + file);
        }
        
        Connection conn = Transaction.getStandaloneConnection();
        
        ScriptRunner runner = new ScriptRunner(conn, autoCommit, stopOnError);
        FileReader reader;
        try {
            reader = new FileReader(cleanScript);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to read " + file, e);
        } 
        try {
            runner.runScript(reader);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + file, e);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to execute " + file, e);
        }
        
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to close DB connection", e);
        }
    }

    public static void executeUsageScript(String file, boolean autoCommit, boolean stopOnError) {
        File cleanScript = PropertiesUtil.findConfigFile(file);
        if (cleanScript == null) {
            throw new RuntimeException("Unable to clean the database because I can't find " + file);
        }
        
        Connection conn = Transaction.getStandaloneUsageConnection();
        
        ScriptRunner runner = new ScriptRunner(conn, autoCommit, stopOnError);
        FileReader reader;
        try {
            reader = new FileReader(cleanScript);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to read " + file, e);
        } 
        try {
            runner.runScript(reader);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + file, e);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to execute " + file, e);
        }
        
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to close DB connection", e);
        }
    }

}
