/*
 * StaticSnark - Main snark startup class for staticly linking with gcj.
 * Copyright (C) 2003 Mark J. Wielaard
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

import java.security.Provider;
import java.security.Security;

import org.klomp.snark.cmd.SnarkApplication;

/**
 * Main snark startup class for staticly linking with gcj. It references somee
 * necessary classes that are normally loaded through reflection.
 * 
 * @author Mark Wielaard (mark@klomp.org)
 */
public class StaticSnark
{
    public static void main (String[] args)
    {
        try {
            // The GNU security provider is needed for SHA-1 MessageDigest
            // checking. So make sure it is available as a security provider.
            Provider gnu = (Provider)Class.forName(
                "gnu.java.security.provider.Gnu").newInstance();
            Security.addProvider(gnu);
        } catch (Exception e) {
            System.err.println("Unable to load GNU security provider");
            System.exit(-1);
        }

        // And finally call the normal starting point.
        SnarkApplication.main(args);
    }
}
