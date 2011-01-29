package org.klomp.snark.tracker;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.klomp.snark.ConnectionAcceptor;
import org.klomp.snark.HttpAcceptor;
import org.klomp.snark.Snark;
import org.klomp.snark.Tracker;

/**
 * A basic command line interface to the Snark library.
 * 
 * @author Elizabeth Fong (elizabeth@threerings.net)
 */
public class TrackerApplication
{
    public static void main (String[] args)
    {
        System.out.println(copyright);
        System.out.println();

        // Parse debug, share/ip and torrent file options.
        try {
            ConnectionAcceptor acceptor = parseArguments(args);
            acceptor.start();
            /*
            while (true) {
                
            }
            */
        } catch (IOException ioe) {
            log.log(Level.SEVERE, "Could not open port", ioe);
        }
    }

    /**
     * Prints messages about proper usage of the Snark application.
     */
    protected static void usage (String s)
    {
        PrintStream stream = System.out;
        if (s != null) {
            stream = System.err;
            stream.println("snark: " + s);
        }
        stream.println(
            "Usage: snark [--debug [level]] [--no-commands] [--port <port>] <hash> [<hash> ...]");
        stream.println("  --debug\tShows some extra info and stacktraces");
        stream.println("    level\tHow much debug details to show");
        stream.println("         \t(defaults to " + Level.SEVERE
            + ", with --debug to " + Level.INFO + ", highest level is "
            + Level.ALL + ").");
        stream.println("  --port\tThe port to listen on for incomming connections");
        stream.println("        \t(if not given defaults to 6969)");
        stream.println("  <hash>  \tAn infohash for a torrent file shared using the tracker.");
        System.exit(-1);
    }

    /**
     * Sets debug, ip and torrent variables then creates a Snark instance. Calls
     * usage(), which terminates the program, if non-valid argument list. The
     * given listeners will be passed to all components that take one.
     */
    public static ConnectionAcceptor parseArguments (String[] args)
        throws IOException
    {
        int user_port = DEFAULT_PORT;
        Level level = Level.INFO;
        HashSet<String> hashes = new HashSet<String>();

        int i = 0;
        while (i < args.length) {
            if (args[i].equals("--debug")) {
                level = Level.FINE;
                i++;

                // Try if there is an level argument.
                if (i < args.length) {
                    try {
                        level = Level.parse(args[i]);
                    } catch (IllegalArgumentException iae) {
                        // continue parsing arguments
                    }
                }
            } else if (args[i].equals("--port")) {
                if (args.length - 1 < i + 1) {
                    usage("--port needs port number to listen on");
                }
                try {
                    user_port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException nfe) {
                    usage("--port argument must be a number (" + nfe + ")");
                }
                i += 2;
            } else if (args[i].equals("--help")) {
                usage(null);
            } else {
                hashes.add(args[i]);
                i++;
                break;
            }
        }
        log.setLevel(level);
        Snark.setLogLevel(level);

        if (hashes.isEmpty()) {
            usage("Need at least one <hash>.");
        }

        Tracker tracker = new Tracker(hashes);
        HttpAcceptor httpacceptor = new HttpAcceptor(tracker);
        ConnectionAcceptor acceptor = new ConnectionAcceptor(
            new ServerSocket(user_port), httpacceptor, null);
        return acceptor;
    }

    protected static final String newline = System.getProperty("line.separator");

    protected static final String copyright =
        "The Hunting of the Snark Project - "
        + "Copyright (C) 2003 Mark J. Wielaard, (c) 2006 Three Rings Design"
        + newline
        + newline
        + "Snark comes with ABSOLUTELY NO WARRANTY.  This is free software, and"
        + newline
        + "you are welcome to redistribute it under certain conditions; read the"
        + newline + "COPYING file for details.";

    /** The Java logger used to process our log events. */
    protected static final Logger log = Logger.getLogger("org.klomp.snark.cmd");

    protected static final int DEFAULT_PORT = 6969;
}
