public class AuthenticationException extends Exception {
	private static final long serialVersionUID = -393139302884898842L;
	public AuthenticationException() {
		super();
	}
	public AuthenticationException(String s) {
		super(s);
	}
	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}
	 public AuthenticationException(Throwable cause) {
		 super(cause);
	 }
}