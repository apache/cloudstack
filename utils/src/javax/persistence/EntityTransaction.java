/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package javax.persistence;

/**
 * The <code>EntityTransaction</code> interface is used to control 
 * resource transactions on resource-local entity managers. The 
 * {@link EntityManager#getTransaction EntityManager.getTransaction()} 
 * method returns the <code>EntityTransaction</code> interface.
 *
 * @since Java Persistence 1.0
 */
public interface EntityTransaction {
    /**
     * Start the resource transaction.
     * @throws IllegalStateException if {@link #isActive()} is true.
     */
    public void begin();
 
    /**
     * Commit the current transaction, writing any unflushed
     * changes to the database.
     * @throws IllegalStateException if {@link #isActive()} is false.
     * @throws RollbackException if the commit fails.
     */
    public void commit();
 
    /**
     * Roll back the current transaction
     * @throws IllegalStateException if {@link #isActive()} is false.
     * @throws PersistenceException if an unexpected error
     * condition is encountered.
     */
    public void rollback();

    /**
     * Mark the current transaction so that the only possible
     * outcome of the transaction is for the transaction to be
     * rolled back.
     * @throws IllegalStateException if {@link #isActive()} is false.
     */
    public void setRollbackOnly();

    /**
     * Determine whether the current transaction has been marked
     * for rollback.
     * @throws IllegalStateException if {@link #isActive()} is false.
     */
    public boolean getRollbackOnly();
 
    /**
     * Indicate whether a transaction is in progress.
     * @throws PersistenceException if an unexpected error
     * condition is encountered.
     */
    public boolean isActive();
}
