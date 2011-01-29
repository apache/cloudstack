/*
 * HttpAcceptor - Accepts incomming http connections. Copyright (C) 2003 Mark J.
 * Wielaard
 * 
 * This file is part of Snark.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.klomp.snark;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpAcceptor
{
    private static final String SNARKHTML = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>"
        + "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\""
        + "\"http://www.w3.org/TR/html4/loose.dtd\">"
        + "<html>"
        + "<head><title>Snark Client</title></head>"
        + "<body>"
        + "<h1>Snark Client</h1>"
        + "<p>Snark is a client for downloading and sharing files distributed with the BitTorrent protocol. It is not a normal webserver.</p>"
        + "<p><a href=\"announce\">Tracker</a></p>"
        + "<hr><p>For more info see <a href=\"http://www.klomp.org/snark/\">The Hunting of the Snark Project</a></p>"
        + "</body>" + "</html>";

    private static final byte[] SNARKPAGE;

    private static final String ASCII = "US-ASCII";

    private static final byte[] CRLF = new byte[] { '\r', '\n' };

    private static final byte[] HTTP_STATUS;

    private static final byte[] CONTENT_LENGTH;

    private static final byte[] CONTENT_TYPE;

    static {
        try {
            SNARKPAGE = SNARKHTML.getBytes(ASCII);

            String STATUS = "HTTP/1.0 ";
            HTTP_STATUS = STATUS.getBytes(ASCII);
            CONTENT_LENGTH = "Content-Length: ".getBytes(ASCII);
            CONTENT_TYPE = "Content-Type: ".getBytes(ASCII);
        } catch (UnsupportedEncodingException uee) {
            // Cannot happen, US-ASCII unknown?
            throw new InternalError(uee.toString());
        }
    }

    private final Tracker tracker;

    /**
     * Creates a HttpAcceptor that can handle torrent metadata of the given
     * Tracker.
     */
    public HttpAcceptor (Tracker tracker)
    {
        this.tracker = tracker;
    }

    public void connection (Socket sock, BufferedInputStream bis,
        BufferedOutputStream bos) throws IOException
    {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(bis, ASCII));

        String resource = readRequest(br);
        log.log(Level.FINE, "HTTP request for: " + resource);

        if (resource != null) {
            Map headers = readHeaders(br);
            log.log(Level.FINER, headers.toString());

            if (resource.equals("/")) {
                sendData(bos, SNARKPAGE, "text/html");
            } else if (resource.startsWith("/announce")) {
                Map params = parseParams(resource);
                byte[] response = tracker.handleRequest(sock.getInetAddress(),
                    sock.getPort(), params);
                sendData(bos, response, "application/octet-stream");
            } else if (resource.endsWith(".torrent")) {
                MetaInfo info = tracker.getMetaInfo(
                    resource.substring(1, resource.length() - 8));
                if (info != null) {
                    byte[] torrent = info.getTorrentData();
                    sendData(bos, torrent, "application/x-bittorrent");
                } else {
                    sendError(bos, 404, "Unable to locate that hash.");
                }
            } else {
                sendError(bos, 404, "Snark Client. Not a real webserver.");
            }
        } else {
            sendError(bos, 500, "Snark Client. Not a real webserver.");
        }

        sock.close();
    }

    /**
     * Processes an incoming HTTP request. Only handles the most basic GET
     * requests. Returns the (URLEncoded) requested resource or null if the
     * request wasn't a valid GET request.
     */
    private static String readRequest (BufferedReader br) throws IOException
    {
        String request = br.readLine();
        if (request != null && request.startsWith("GET ")) {
            String resource;
            int index = request.indexOf(' ', 4);
            if (index == -1) {
                resource = request.substring(4);
            } else {
                resource = request.substring(4, index);
            }
            return resource;
        } else {
            return null;
        }
    }

    /**
     * Consumes all headers and puts them into a Map mapping header value to
     * header key Strings.
     */
    private static Map<String, String> readHeaders (BufferedReader br)
        throws IOException
    {
        Map<String, String> m = new HashMap<String, String>();
        String header = br.readLine();
        while (header != null && header.length() != 0) {
            header = br.readLine();
            if (header != null && header.length() != 0) {
                int index = header.indexOf(": ");
                if (index != -1) {
                    String key = header.substring(0, index);
                    String value = header.substring(index + 2);
                    m.put(key, value);
                }
            }
        }
        return m;
    }

    /**
     * Sends a HTTP OK, the necessary headers and the data.
     */
    private static void sendData (OutputStream out, byte[] data,
        String content_type) throws IOException
    {
        sendData(out, 200, "OK", data, content_type);
    }

    private static void sendData (OutputStream out, int responseCode,
        String reason, byte[] data, String content_type) throws IOException
    {
        log.log(Level.FINER, "HTTP/1.0 " + responseCode + " " + reason + " "
            + content_type + " (" + data.length + " bytes)");
        byte[] type = content_type.getBytes(ASCII);

        // Status line
        out.write(HTTP_STATUS);
        out.write(Integer.toString(responseCode).getBytes(ASCII));
        out.write(' ');
        out.write(reason.getBytes(ASCII));
        out.write(CRLF);

        // Entity headers
        out.write(CONTENT_LENGTH);
        out.write(Integer.toString(data.length).getBytes(ASCII));
        out.write(CRLF);

        out.write(CONTENT_TYPE);
        out.write(type);
        out.write(CRLF);

        // Start of data
        out.write(CRLF);

        out.write(data);
        out.flush();
    }

    private static void sendError (OutputStream out, int responseCode,
        String reason) throws IOException
    {
        sendData(out, responseCode, reason, reason.getBytes(ASCII),
            "text/plain");
    }

    /**
     * Returns a key to value map of the GET request query string parameters. It
     * expects a '?' and the urlencoded key=value pairs. Note that the key and
     * value are NOT url decoded before putting in the paramaters map.
     */
    private static Map<String, String> parseParams (String request)
    {
        Map<String, String> m = new HashMap<String, String>();
        int index = request.indexOf('?');
        if (index != -1) {
            String params = request.substring(index + 1);
            StringTokenizer st = new StringTokenizer(params, "&");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                index = token.indexOf('=');
                if (index != -1) {
                    String key = token.substring(0, index);
                    String value = token.substring(index + 1);
                    m.put(key, value);
                }
            }
        }
        return m;
    }

    /** The Java logger used to process our log events. */
    protected static final Logger log = Logger.getLogger("org.klomp.snark.server");
}
