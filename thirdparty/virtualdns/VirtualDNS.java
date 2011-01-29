import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import org.xbill.DNS.*;
/* 
 * A simple DNS server that maps a-b-c-d.foo.com to a.b.c.d and rejects everything else 
 */
public class VirtualDNS {

	static Logger logger = Logger.getLogger(VirtualDNS.class.getName());

	
	String domain;
	Zone zone;

	// Timeout values, in milliseconds. These are pretty arbitrary. We're
	// just trying to shut down unused connections promptly.
	static private final int TCP_TIMEOUT = 60000;

	/*********************************************************************
	 * Create and run a new DNS server.
	 *********************************************************************
	 * @param port The port on which to run the server.
	 */
	public VirtualDNS(String domain, String zonefile, short port) throws IOException {
		this.domain = domain;
		this.zone = new Zone(new Name(domain + "."), zonefile);
		// Start our two server processes.
		addUDP(port);
		addTCP(port);
	}

	// Create a new TCP listener process.
	private void addTCP(final short port) {
		Thread t;
		t = new Thread(new Runnable() {public void run() {serveTCP(port);}});
		t.start();
	}

	// Create a new UDP listener process.
	private void addUDP(final short port) {
		Thread t;
		t = new Thread(new Runnable() {public void run() {serveUDP(port);}});
		t.start();
	}

	// Our TCP listener process.
	private void serveTCP(short port) {
		try {
			ServerSocket sock = new ServerSocket(port);
			while (true) {
				final Socket s = sock.accept();
				Thread t = new Thread(new Runnable() {
					public void run () {
						processTCP(s);
					}
				});
				t.start();
			}
		}
		catch (IOException e) {
			logger.error("serveTCP failed: " + e);
		}
	}

	// Read and respond to a TCP request.
	private void processTCP (Socket s)
	{
		try {
			int inLength;
			DataInputStream dataIn;
			DataOutputStream dataOut;
			byte [] in;

			s.setSoTimeout(TCP_TIMEOUT);

			try {
				InputStream is = s.getInputStream();
				dataIn = new DataInputStream(is);
				inLength = dataIn.readUnsignedShort();
				in = new byte[inLength];
				dataIn.readFully(in);
			}
			catch (InterruptedIOException e) {
				s.close();
				return;
			}
			Message query, response;
			try {
				query = new Message(in);
				response = generateReply(query, in, s, s.getInetAddress());
				if (response == null)
					return;
			}
			catch (IOException e) {
				response = ErrorMessages.makeFormatErrorMessage(in);
			}
			byte [] out = response.toWire();
			dataOut = new DataOutputStream(s.getOutputStream());
			dataOut.writeShort(out.length);
			dataOut.write(out);

		} catch (IOException e) {
			logger.error("processTCP: " + e);	
		} finally {
			try {
				s.close();
			} catch (IOException e) {}
		}
	}

	// Our UDP listener process.
	private void serveUDP(short port) {
		try {
			DatagramSocket sock = new DatagramSocket(port);
			while (true) {
				short udpLength = 512;
				DatagramPacket dp = new DatagramPacket(new byte[512],
						512);
				try {
					sock.receive(dp);
				}
				catch (InterruptedIOException e) {
					continue;
				}
				byte [] in = new byte[dp.getLength()];
				System.arraycopy(dp.getData(), 0, in, 0, in.length);
				Message query, response;
				try {
					query = new Message(in);
					response = generateReply(query, in, null, dp.getAddress());
					if (response == null)
						continue;
				}
				catch (IOException e) {
					response = ErrorMessages.makeFormatErrorMessage(in);
				}
				byte [] out = response.toWire();

				dp = new DatagramPacket(out, out.length,
						dp.getAddress(), dp.getPort());
				sock.send(dp);
			}
		}
		catch (IOException e) {
			logger.error("serveUDP: " + e);
		}
	}

	byte[] getAddr(String hostname) {
		byte[] result = new byte[4];
		String[] fields = hostname.split("-", 10);
		if (fields.length != 4) {
			return null;
		}
		for (int i = 0; i < 4; i++) {
			try {
				int n = Integer.parseInt(fields[i]);
				if (n > 255) {
					return null;
				}
				result[i] = (byte)n;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return result;
	}
	
    void addRRset(Message response, RRset rrset, int section) {
    	Iterator it = rrset.rrs();
    	while(it.hasNext()) {
    		Record r = (Record)it.next();
    		response.addRecord(r, section);
    	}
    }
	// Construct a proper reply packet.
	private Message generateReply(Message query, byte [] in, Socket s, InetAddress src) {

		// Refuse everything but standard DNS queries.
		if (query.getHeader().getOpcode() != Opcode.QUERY)
			return ErrorMessages.makeErrorMessage(zone.getSOA(), query, (short)Rcode.NOTIMPL);

		// Get some useful information from the query.
		// XXX - What should we do if we get the wrong query version?
		Record queryRecord = query.getQuestion();
		OPTRecord queryOPT = query.getOPT();
		Name name = queryRecord.getName();
		short type = (short)queryRecord.getType();
		short dclass = (short)queryRecord.getDClass();
		
		logger.info("request id=" + query.getHeader().getID() + " src=" + src + " name=" + name + " type=" + type);
		
		if (dclass != DClass.IN) {
			return ErrorMessages.makeErrorMessage(zone.getSOA(), query, (short)Rcode.NOTIMPL);
		}

		// XXX - Is this the right thing to do with TSIGs?
		if (query.getTSIG() != null)
			return ErrorMessages.makeErrorMessage(zone.getSOA(), query, (short)Rcode.NOTIMPL);

		// Start building our response packet.
		Message response = new Message();
		response.getHeader().setID(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		response.getHeader().setFlag(Flags.AA);
		response.addRecord(queryRecord, Section.QUESTION);

		// Reject zone transfer requests--our zone is dynamic.
		if (type == Type.AXFR && s != null)
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.REFUSED);

		// Return an error if we get asked for unsupported record types.
		if (!Type.isRR(type) && type != Type.ANY)
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.NOTIMPL);
		if (type == Type.SIG)
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.NOTIMPL);

		logger.trace("Querying zone name=" + name + " type=" + type);
		response.getHeader().setFlag(Flags.AA);

		SetResponse staticResponse = zone.findRecords(name, type);
		if (staticResponse.isSuccessful()) {
			logger.trace("Zone query successful");
		    RRset [] rrsets = staticResponse.answers();
		    for (int j = 0; j < rrsets.length; j++) {
			    addRRset(response, rrsets[j], Section.ANSWER);
		    }
		    return response;
		}
		if (staticResponse.isNXRRSET()) {
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.NOERROR);
		}

//		logger.trace("Zone query failed isNXDOMAIN="+staticResponse.isNXDOMAIN() + " isNXRRSET=" + staticResponse.isNXRRSET());

		//System.out.println("name = " + name);
		
		String fullname = name.toString();
		
		if (fullname.charAt(fullname.length() - 1) != '.') {
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.FORMERR);
		}
		
		fullname = fullname.substring(0, fullname.length() - 1);
		int i = fullname.indexOf('.');
		if (i < 0) {
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.NXDOMAIN);
		}
		
		String hostname = fullname.substring(0, i);
		String domainname = fullname.substring(i+1);
		
		if (!domainname.equals(domain)) {
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.NXDOMAIN);
		}

		//System.out.println("hostname = " + hostname);
		
		byte[] addr = getAddr(hostname);
		
		if (addr == null) {
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.NXDOMAIN);
		}
		InetAddress iaddr;
		try {
			iaddr = InetAddress.getByAddress(fullname, addr);
		} catch (UnknownHostException e) {
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.NXDOMAIN);
		}

		if (type != Type.A) {
			// DNS servers return NOERROR for entries that exist but have different type of records
			return ErrorMessages.makeErrorMessage(zone.getSOA(), response, (short)Rcode.NOERROR);
		}
		
		ARecord record = new ARecord(name, DClass.IN, 24 * 3600, iaddr);
		
		response.addRecord(record, Section.ANSWER);
		RRset nss = zone.getNS();
	    addRRset(response, nss, Section.AUTHORITY);
    	Iterator it = nss.rrs();
    	while(it.hasNext()) {
    		NSRecord r = (NSRecord)it.next();
        	RRset a = zone.findExactMatch(r.getTarget(), Type.A);
    		addRRset(response, a, Section.ADDITIONAL);
    	}

		return response;
	}

	public static void main(String [] args) {
                if (args.length != 4) {
                        System.err.println("Usage: VirtualDNS <domain name> <zonefile> <port> <log4j config>");
                        System.exit(1);
                }
                try {
			PropertyConfigurator.configure(args[3]);
                        short port = (short)Integer.parseInt(args[2]);
                        VirtualDNS s = new VirtualDNS(args[0], args[1], port);
                }
                catch (Exception e) {
                        e.printStackTrace();
                }
	}
}
