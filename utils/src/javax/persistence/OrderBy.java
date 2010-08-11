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
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation specifies the ordering of the elements of a 
 * collection valued association at the point when the association 
 * is retrieved.
 * 
 * <p> The syntax of the <code>value</code> ordering element is an 
 * <code>orderby_list</code>, as follows:
 * 
 * <pre>
 *    orderby_list::= orderby_item [,orderby_item]*
 *    orderby_item::= property_or_field_name [ASC | DESC]
 * </pre>
 * 
 * <p> If <code>ASC</code> or <code>DESC</code> is not specified, 
 * <code>ASC</code> (ascending order) is assumed.
 *
 * <p> If the ordering element is not specified, ordering by 
 * the primary key of the associated entity is assumed.
 *
 * <p> The property or field name must correspond to that of a 
 * persistent property or field of the associated class. The 
 * properties or fields used in the ordering must correspond to 
 * columns for which comparison operators are supported.
 *
 * <pre>
 *    Example:
 *    
 *    &#064;Entity public class Course {
 *     ...
 *     &#064;ManyToMany
 *     &#064;OrderBy("lastname ASC")
 *     public List<Student> getStudents() {...};
 *     ...
 *    }
 *    
 *    &#064;Entity public class Student {
 *      ...
 *      &#064;ManyToMany(mappedBy="students")
 *      &#064;OrderBy // PK is assumed
 *      public List<Course> getCourses() {...};
 *      ...
 *    }
 * </pre>
 *
 * @since Java Persistence 1.0
 */
@Target({METHOD, FIELD}) 
@Retention(RUNTIME)

public @interface OrderBy {

    /**
    * An <code>orderby_list</code>, specified as follows:
    *
    * <pre>
    *    orderby_list::= orderby_item [,orderby_item]*
    *    orderby_item::= property_or_field_name [ASC | DESC]
    * </pre>
    *
    * <p> If <code>ASC</code> or <code>DESC</code> is not specified,
    * <code>ASC</code> (ascending order) is assumed.
    *
    * <p> If the ordering element is not specified, ordering by
    * the primary key of the associated entity is assumed.
    */
    String value() default "";
}
