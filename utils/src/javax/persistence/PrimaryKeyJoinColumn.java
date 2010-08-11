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
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation specifies a primary key column that is used 
 * as a foreign key to join to another table. 
 *
 * <p> It is used to join the primary table of an entity subclass 
 * in the {@link InheritanceType#JOINED JOINED} mapping strategy 
 * to the primary table of its superclass; it is used within a 
 * {@link SecondaryTable} annotation to join a secondary table 
 * to a primary table; and it may be used in a {@link OneToOne} 
 * mapping in which the primary key of the referencing entity 
 * is used as a foreign key to the referenced entity. 
 *
 * <p> If no <code>PrimaryKeyJoinColumn</code> annotation is 
 * specified for a subclass in the {@link InheritanceType#JOINED 
 * JOINED} mapping strategy, the foreign key columns are assumed 
 * to have the same names as the primary key columns of the 
 * primary table of the superclass
 *
 * <pre>
 *
 *    Example: Customer and ValuedCustomer subclass
 *
 *    &#064;Entity
 *    &#064;Table(name="CUST")
 *    &#064;Inheritance(strategy=JOINED)
 *    &#064;DiscriminatorValue("CUST")
 *    public class Customer { ... }
 *    
 *    &#064;Entity
 *    &#064;Table(name="VCUST")
 *    &#064;DiscriminatorValue("VCUST")
 *    &#064;PrimaryKeyJoinColumn(name="CUST_ID")
 *    public class ValuedCustomer extends Customer { ... }
 * </pre>
 *
 * @since Java Persistence 1.0
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)

public @interface PrimaryKeyJoinColumn {

    /** 
     * The name of the primary key column of the current table.
     * <p> Defaults to the same name as the primary key column 
     * of the primary table of the superclass ({@link 
     * InheritanceType#JOINED JOINED} mapping strategy); the same 
     * name as the primary key column of the primary table 
     * ({@link SecondaryTable} mapping); or the same name as the 
     * primary key column for the table for the referencing entity 
     * ({@link OneToOne} mapping)
     */
    String name() default "";

    /** 
     * (Optional) The name of the primary key column of the table 
     * being joined to.
     * <p> Defaults to the same name as the primary key column 
     * of the primary table of the superclass ({@link 
     * InheritanceType#JOINED JOINED} mapping strategy); the same 
     * name as the primary key column of the primary table 
     * ({@link SecondaryTable} mapping); or the same name as the 
     * primary key column for the table for the referencing entity 
     * ({@link OneToOne} mapping)
     */
    String referencedColumnName() default "";

    /**
     * (Optional) The SQL fragment that is used when generating the 
     * DDL for the column. This should not be specified for a 
     * {@link OneToOne} primary key association.
     * <p> Defaults to the generated SQL to create a column of the 
     * inferred type.
     */
    String columnDefinition() default "";
}
