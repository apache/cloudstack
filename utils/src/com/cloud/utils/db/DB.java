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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates a method or a class as using the in-memory transaction.
 * Running with assertions on, will find all classes that are
 * not using this but is using in-memory transactions.
 * 
 * There are only three circumstances where you should use this.
 * 1. Annotate method that starts and commits DB transactions.
 *    Transaction txn = Transaction.currentTxn();
 *    txn.start();
 *    ...
 *    txn.commit();
 * 
 * 2. Annotate methods that uses a DAO's acquire method.
 *    _dao.acquire(id);
 *    ...
 *    _dao.release(id);
 * 
 * 3. Annotate methods that are inside a DAO but doesn't use
 *    the Transaction class.  Generally, these are methods
 *    that are utility methods for setting up searches.  In
 *    this case use @DB(txn=false) to annotate the method.
 *    While this is not required, it helps when you're debugging
 *    the code and it saves on method calls during runtime.
 *
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface DB {
    /**
     * (Optional) Specifies that the method
     * does not use transaction.  This is useful for
     * utility methods within DAO classes which are
     * automatically marked with @DB.  By marking txn=false,
     * the method is not surrounded with transaction code.
     */
    boolean txn() default true;
}