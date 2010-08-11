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

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation is used to specify a secondary table for 
 * the annotated entity class. Specifying one or more secondary 
 * tables indicates that the data for the entity class is stored 
 * across multiple tables.
 *
 * <p> If no <code>SecondaryTable</code> annotation is specified, 
 * it is assumed that all persistent fields or properties of the 
 * entity are mapped to the primary table. If no primary key join 
 * columns are specified, the join columns are assumed to reference 
 * the primary key columns of the primary table, and have the same 
 * names and types as the referenced primary key columns of the 
 * primary table.
 *
 * <pre>
 * Example 1: Single secondary table with a single primary key column.
 *
 *    &#064;Entity
 *    &#064;Table(name="CUSTOMER")
 *    &#064;SecondaryTable(name="CUST_DETAIL", 
 *        pkJoinColumns=&#064;PrimaryKeyJoinColumn(name="CUST_ID"))
 *    public class Customer { ... } 
 *
 *    Example 2: Single secondary table with multiple primary key columns.
 *
 *    &#064;Entity
 *    &#064;Table(name="CUSTOMER")
 *    &#064;SecondaryTable(name="CUST_DETAIL",
 *        pkJoinColumns={
 *            &#064;PrimaryKeyJoinColumn(name="CUST_ID"),
 *            &#064;PrimaryKeyJoinColumn(name="CUST_TYPE")})
 *    public class Customer { ... }
 * </pre>
 *
 * @since Java Persistence 1.0
 */
@Target(TYPE) 
@Retention(RUNTIME)

public @interface SecondaryTable {

    /** (Required) The name of the table. */
    String name();

    /** (Optional) The catalog of the table.
     * <p> Defaults to the default catalog.
     */
    String catalog() default "";

    /** (Optional) The schema of the table.
     * <p> Defaults to the default schema for user.
     */
    String schema() default "";

    /** 
     * (Optional) The columns that are used to join with 
     * the primary table.
     * <p> Defaults to the column(s) of the same name(s) 
     * as the primary key column(s) in the primary table
     */
    PrimaryKeyJoinColumn[] pkJoinColumns() default {};

    /**
     * (Optional) Unique constraints that are to be placed on the 
     * table. These are typically only used if table generation 
     * is in effect. These constraints apply in addition to any 
     * constraints specified by the {@link Column} and {@link JoinColumn} 
     * annotations and constraints entailed by primary key mappings.
     * <p> Defaults to no additional constraints.
     */
    UniqueConstraint[] uniqueConstraints() default {};

    /**
     * (Optional) join type (supports inner, left, right) to use
     * for querying data from the joined tables.
     */
    String join() default "";
}
