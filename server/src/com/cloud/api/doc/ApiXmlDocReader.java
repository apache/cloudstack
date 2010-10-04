/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.doc;

import java.io.EOFException;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ApiXmlDocReader {
	public static void main (String[] args) {
		String newFile = "";
		String oldFile = "";
		
		
		HashMap<String, Command> commands = new HashMap<String,Command>();
		HashMap<String, Command> oldCommands = new HashMap<String,Command>();
		ArrayList<Command> newCommands = new ArrayList<Command>();
		XStream xs = new XStream(new DomDriver());
		xs.alias("command", Command.class);
		xs.alias("arg", Argument.class);
		

		List<String> argsList = Arrays.asList(args);
		Iterator<String> iter = argsList.iterator();
		while (iter.hasNext()) {
			String arg = iter.next();
			// populate the file names
			if (arg.equals("-new")) {
				newFile = iter.next();
			}
			if (arg.equals("-old")) {
				oldFile = iter.next();
			}
		}
		try {
			
			try{
				ObjectInputStream inOld = xs.createObjectInputStream(new FileReader(oldFile));
				while (true){
					Command c1 = (Command)inOld.readObject();
					oldCommands.put(c1.getName(), c1);
				}
			} catch (EOFException ex) {
				//EOF exception shows that there is no more objects in ObjectInputStream, so do nothing here
			}
			
			try{
				ObjectInputStream inNew = xs.createObjectInputStream(new FileReader(newFile));
				while (true){
					Command c = (Command)inNew.readObject();
					commands.put(c.getName(), c);
				}
			} catch (EOFException ex) {
				//EOF exception shows that there is no more objects in ObjectInputStream, so do nothing here
			}
		} catch (Exception ex){
			ex.printStackTrace();
		}
		
		
		Iterator i = commands.keySet().iterator();
		while (i.hasNext()) {
			String key = (String)i.next();
			//Check if command existed in old version
			if (!oldCommands.containsKey(key)) {
				newCommands.add(commands.get(key));
			}
				
			//Verify request and response arguments
			else {
				commands.get(key).compareArguments(oldCommands.get(key));
			}
		}
		//Print new commands
		for (Command c : newCommands) {
			System.out.println(c.getName());
		}

	}
}
