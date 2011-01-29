import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class ConsoleProxyStatus {
	ArrayList<ConsoleProxyConnection> connections;
	public ConsoleProxyStatus() {
	}
	public void setConnections(Hashtable<String, ConsoleViewer> connMap) {
		ArrayList<ConsoleProxyConnection> conns = new ArrayList<ConsoleProxyConnection>();
		Enumeration<String> e = connMap.keys();
	    while (e.hasMoreElements()) {
	    	synchronized (connMap) {
		         String key = e.nextElement();
		         ConsoleViewer viewer = connMap.get(key);
		         ConsoleProxyConnection conn = new ConsoleProxyConnection();
		         conn.id = viewer.id;
		         conn.clientInfo = viewer.clientStreamInfo;
		         conn.host = viewer.host;
		         conn.port = viewer.port;
		         conn.createTime = viewer.createTime;
		         conn.lastUsedTime = viewer.lastUsedTime;
		         conns.add(conn);
	    	}
	    }
	    connections = conns;
	}
	public static class ConsoleProxyConnection {
		public int id;
		public String clientInfo;
		public String host;
		public int port;
		public long createTime;
		public long lastUsedTime;
		
		public ConsoleProxyConnection() {
		}
	}
}
