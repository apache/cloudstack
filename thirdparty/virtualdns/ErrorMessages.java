/*  VirtualDNS - A modular DNS server.
 *  Copyright (C) 2000 Eric Kidd
 *  Copyright (C) 1999 Brian Wellington
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import java.io.*;
import org.xbill.DNS.*;
import org.xbill.DNS.utils.*;

/*************************************************************************
 * Routines for creating different kinds of DNS error responses.
 *************************************************************************
 * This code was adapted from Brian Wellington's jnamed code.
 * @see org.xbill.DNS.Message
 */

public class ErrorMessages {

	/*********************************************************************
	 * Create a format error message (FORMERR).
	 *********************************************************************
	 * @param in The malformed packet.
	 * @return A DNS error message.
	 */
	public static Message makeFormatErrorMessage(byte [] in) {
		Header header;
		try {
			header = new Header(in);
		}
		catch (IOException e) {
			header = new Header(0);
		}
		Message response = new Message();
		response.setHeader(header);
		for (int i = 0; i < 4; i++)
			response.removeAllRecords(i);
		header.setRcode(Rcode.FORMERR);
		return response;
	}

	/*********************************************************************
	 * Create an arbitrary DNS error message.
	 *********************************************************************
	 * @param query The query sent by the user.
	 * @param rcode The response code to use for this error.
	 * @return A DNS error message.
	 * @see org.xbill.DNS.Rcode
	 */
	public static Message makeErrorMessage(SOARecord soa, Message response, short rcode) {
		Header header = response.getHeader();
//		Message response = new Message();
//		response.setHeader(header);
		for (int i = 1; i < 4; i++)
			response.removeAllRecords(i);
		response.addRecord(soa, Section.AUTHORITY);
		header.setRcode(rcode);
		return response;
	}
}
