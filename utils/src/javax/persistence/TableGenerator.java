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
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation defines a primary key generator that may be 
 * referenced by name when a generator element is specified for 
 * the {@link GeneratedValue} annotation. A table generator 
 * may be specified on the entity class or on the primary key 
 * field or property. The scope of the generator name is global 
 * to the persistence unit (across all generator types).
 *
 * <pre>
 *    Example 1:
 *    
 *    &#064;Entity public class Employee {
 *        ...
 *        &#064;TableGenerator(
 *            name="empGen", 
 *            table="ID_GEN", 
 *            pkColumnName="GEN_KEY", 
 *            valueColumnName="GEN_VALUE", 
 *            pkColumnValue="EMP_ID", 
 *            allocationSize=1)
 *        &#064;Id
 *        &#064;GeneratedValue(strategy=TABLE, generator="empGen")
 *        public int id;
 *        ...
 *    }
 *    
 *    Example 2:
 *    
 *    &#064;Entity public class Address {
 *        ...
 *        &#064;TableGenerator(
 *            name="addressGen", 
 *            table="ID_GEN", 
 *            pkColumnName="GEN_KEY", 
 *            valueColumnName="GEN_VALUE", 
 *            pkColumnValue="ADDR_ID")
 *        &#064;Id
 *        &#064;GeneratedValue(strategy=TABLE, generator="addressGen")
 *        public int id;
 *        ...
 *    }
 * </pre>
 *
 * @since Java Persistence 1.0
 */
@Target({TYPE, METHOD, FIELD}) 
@Retention(RUNTIME)

public @interface TableGenerator {

    /** 
     * (Required) A unique generator name that can be referenced 
     * by one or more classes to be the generator for id values.
     */
    String name();

    /** 
     * (Optional) Name of table that stores the generated id values. 
     * <p> Defaults to a name chosen by persistence provider.
     */
    String table() default "";

    /** (Optional) The catalog of the table. 
     * <p> Defaults to the default catalog.
     */
    String catalog() default "";

    /** (Optional) The schema of the table. 
     * <p> Defaults to the default schema for user.
     */
    String schema() default "";

    /** 
     * (Optional) Name of the primary key column in the table.
     * <p> Defaults to a provider-chosen name.
     */
    String pkColumnName() default "";

    /** 
     * (Optional) Name of the column that stores the last value generated.
     * <p> Defaults to a provider-chosen name.
     */
    String valueColumnName() default "";

    /**
     * (Optional) The primary key value in the generator table 
     * that distinguishes this set of generated values from others 
     * that may be stored in the table.
     * <p> Defaults to a provider-chosen value to store in the 
     * primary key column of the generator table
     */
    String pkColumnValue() default "";

    /** 
     * (Optional) The initial value to be used when allocating id 
     * numbers from the generator. 
     */
    int initialValue() default 0;

    /**
     * (Optional) The amount to increment by when allocating id 
     * numbers from the generator.
     */
    int allocationSize() default 50;

    /**
     * (Optional) Unique constraints that are to be placed on the 
     * table. These are only used if table generation is in effect. 
     * These constraints apply in addition to primary key constraints.
     * <p> Defaults to no additional constraints.
     */
    UniqueConstraint[] uniqueConstraints() default {};
}
