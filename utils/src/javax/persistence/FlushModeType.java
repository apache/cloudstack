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
 * Flush mode setting.
 *
 * <p> When queries are executed within a transaction, if 
 * <code>FlushModeType.AUTO</code> is set on the {@link Query} 
 * object, or if the flush mode setting for the persistence context 
 * is <code>AUTO</code> (the default) and a flush mode setting has 
 * not been specified for the {@link Query} object, the persistence 
 * provider is responsible for ensuring that all updates to the state 
 * of all entities in the persistence context which could potentially 
 * affect the result of the query are visible to the processing 
 * of the query. The persistence provider implementation may achieve 
 * this by flushing those entities to the database or by some other 
 * means. If <code>FlushModeType.COMMIT</code> is set, the effect 
 * of updates made to entities in the persistence context upon 
 * queries is unspecified.
 *
 * <p> If there is no transaction active, the persistence provider 
 * must not flush to the database.
 *
 * @since Java Persistence 1.0
 */
public enum FlushModeType {

    /** Flushing must occur only at transaction commit */
    COMMIT,

    /** (Default) Flushing to occur at query execution */
    AUTO
}
