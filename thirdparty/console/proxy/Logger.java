
public class Logger {
	public static final int ERROR = org.apache.log4j.Level.ERROR_INT;
	public static final int WARN = org.apache.log4j.Level.WARN_INT;
	public static final int INFO = org.apache.log4j.Level.INFO_INT;
	public static final int DEBUG = org.apache.log4j.Level.DEBUG_INT;
	public static final int TRACE = org.apache.log4j.Level.TRACE_INT;
	
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
	
	static org.apache.log4j.PropertyConfigurator pc = 
		new org.apache.log4j.PropertyConfigurator(); // This loads log4j.properties
	
	public static void log(int level, String msg) {
		logger.log(org.apache.log4j.Level.toLevel(level), msg);
	}

	public static void log(int level, String msg, Throwable t) {
		logger.log(org.apache.log4j.Level.toLevel(level), msg, t);
	}
}