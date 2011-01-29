

public class Logger {
	public static final int ERROR = 10;
	public static final int WARN = 20;
	public static final int INFO = 30;
	public static final int DEBUG = 40;
	public static final int TRACE = 50;
	
	public static void log(int level, String msg) {
		log(level, msg, null);
	}

	public static void log(int level, String msg, Throwable t) {
        if (level > INFO) return;

		System.out.println(msg);
		if (t != null) {
			t.printStackTrace(System.out);
		}
	}
}
