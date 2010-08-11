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
import static javax.persistence.DiscriminatorType.STRING;

/**
 * Is used to define the discriminator column for the 
 * {@link InheritanceType#SINGLE_TABLE SINGLE_TABLE} and 
 * {@link InheritanceType#JOINED JOINED} inheritance mapping strategies.
 * 
 * <p> The strategy and the discriminator column are only 
 * specified in the root of an entity class hierarchy or
 * subhierarchy in which a different inheritance strategy is applied
 * 
 * <p> If the <code>DiscriminatorColumn</code> annotation is missing, 
 * and a discriminator column is required, the name of the 
 * discriminator column defaults to <code>"DTYPE"</code> and the discriminator 
 * type to {@link DiscriminatorType#STRING DiscriminatorType.STRING}.
 *
 * <pre>
 *     Example:
 *     &#064;Entity
 *     &#064;Table(name="CUST")
 *     &#064;Inheritance(strategy=SINGLE_TABLE)
 *     &#064;DiscriminatorColumn(name="DISC", discriminatorType=STRING,length=20)
 *     public class Customer { ... }
 *
 *     &#064;Entity
 *     public class ValuedCustomer extends Customer { ... }
 * </pre>
 *
 * @since Java Persistence 1.0
 */
@Target({TYPE}) 
@Retention(RUNTIME)

public @interface DiscriminatorColumn {

    /**
     * (Optional) The name of column to be used for the discriminator.
     */
    String name() default "DTYPE";

    /**
     * (Optional) The type of object/column to use as a class discriminator.
     * Defaults to {@link DiscriminatorType#STRING DiscriminatorType.STRING}.
     */
    DiscriminatorType discriminatorType() default STRING;

    /**
     * (Optional) The SQL fragment that is used when generating the DDL 
     * for the discriminator column.
     * <p> Defaults to the provider-generated SQL to create a column 
     * of the specified discriminator type.
     */
    String columnDefinition() default "";

    /** 
     * (Optional) The column length for String-based discriminator types. 
     * Ignored for other discriminator types.
     */
    int length() default 31;
}
