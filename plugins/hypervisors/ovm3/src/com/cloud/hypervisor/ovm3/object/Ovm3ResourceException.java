/**
 *
 */
package com.cloud.hypervisor.ovm3.object;

/**
 * @author funs
 *
 */
public class Ovm3ResourceException extends Exception {
    private static final long serialVersionUID = 1L;
    private Throwable cause = null;
    public Ovm3ResourceException() {
        super();
   }

   public Ovm3ResourceException(String message) {
       super(message);
   }

   public Ovm3ResourceException(String message, Throwable cause) {
      super(message, cause);
   }

   public Throwable getCause() {
      return cause;
   }
   public void printStackTrace() {
      super.printStackTrace();
      if (cause != null) {
          cause.printStackTrace();
      }
   }
   public void printStackTrace(java.io.PrintStream ps) {
       super.printStackTrace(ps);
       if (cause != null) {
            cause.printStackTrace(ps);
       }
   }
   public void printStackTrace(java.io.PrintWriter pw) {
       super.printStackTrace(pw);
       if (cause != null) {
           cause.printStackTrace(pw);
       }
   }

}
