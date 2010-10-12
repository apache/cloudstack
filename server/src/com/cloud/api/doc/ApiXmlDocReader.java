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

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
		String newFile = null;
		String oldFile = null;
		String dirName = "";
		
		HashMap<String, Command> commands = new HashMap<String,Command>();
		HashMap<String, Command> oldCommands = new HashMap<String,Command>();
		ArrayList<Command> addedCommands = new ArrayList<Command>();
		ArrayList<Command> removedCommands = new ArrayList<Command>();
		HashMap<String, Command> stableCommands = new HashMap<String, Command>();

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
			if (arg.equals("-d")) {
				dirName = iter.next();
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
		
		//Check if any commands got added in new version
		for (String key : commands.keySet()) {
			if (!oldCommands.containsKey(key)) {
				addedCommands.add(commands.get(key));
			} else {
				stableCommands.put(commands.get(key).getName(), commands.get(key));
			}
		}
		
		//Check if any commands were removed in new version
		for (String key : oldCommands.keySet()) {
			if (!commands.containsKey(key)) {
				removedCommands.add(oldCommands.get(key));
				if (stableCommands.get(key) != null) {
					stableCommands.remove(key);
				}
			} 
		}
		
		
		 try {
		    FileWriter fstream = new FileWriter(dirName + "/diff.txt");
	        BufferedWriter out = new BufferedWriter(fstream);
		    
			//Print added commands
	        out.write("Added commands:\n");
			for (Command c : addedCommands) {
				out.write("\n	" + c.getName() + " (" + c.getDescription() + "\n");
			}
			
			//Print removed commands
			out.write("\nRemoved commands:\n");
			for (Command c : removedCommands) {
				out.write("\n	" + c.getName() + " (" + c.getDescription() + ")\n");
			}

			//Print differences between commands arguments
			out.write("\nChanges in commands arguments:\n");
			for (String key : stableCommands.keySet()){
				ArrayList<Argument> newReqArgs = new ArrayList<Argument>();
				ArrayList<Argument> removedReqArgs = new ArrayList<Argument>();
				HashMap<String, Argument> stableReqArgs = new HashMap<String, Argument>();
				ArrayList<Argument> newRespArgs = new ArrayList<Argument>();
				ArrayList<Argument> removedRespArgs = new ArrayList<Argument>();
				HashMap<String, Argument> stableRespArgs = new HashMap<String, Argument>();
				
				Command newCommand = commands.get(key);
				Command oldCommand = oldCommands.get(key);
				
				//Check if any request arguments were added in new version
				for (Argument arg : newCommand.getRequest()) {
					if (oldCommand.getReqArgByName(arg.getName()) == null) {
						newReqArgs.add(arg);
					} else {
						stableReqArgs.put(arg.getName(), arg);
					}
				}
				
				//Check if any request arguments were removed in new version
				for (Argument arg : oldCommand.getRequest()) {
					if (newCommand.getReqArgByName(arg.getName()) == null) {
						removedReqArgs.add(arg);
						if (stableReqArgs.get(arg.getName()) != null) {
							stableReqArgs.remove(arg.getName());
						}
					}
				}
				
				//Compare stable request arguments of old and new version
				for (Iterator<String> i = stableReqArgs.keySet().iterator(); i.hasNext();) {
				    String argName = i.next();
				    if (oldCommand.getReqArgByName(argName).isRequired() == newCommand.getReqArgByName(argName).isRequired()) {
						i.remove();
					}
				}
				
				//Check if any response arguments were added in new version
				for (Argument arg : newCommand.getResponse()) {
					if (oldCommand.getResArgByName(arg.getName()) == null) {
						newRespArgs.add(arg);
					} 
				}
				
				//Check if any response arguments were removed in new version
				for (Argument arg : oldCommand.getResponse()) {
					if (newCommand.getResArgByName(arg.getName()) == null) {
						removedRespArgs.add(arg);
					}
				}
				
				if (newReqArgs.size() != 0 || newRespArgs.size() != 0 || removedReqArgs.size() != 0 || removedRespArgs.size() != 0 || stableReqArgs.size() != 0 || stableReqArgs.size() != 0) {
					out.write("\n\t" + key);
					//Request
					if (newReqArgs.size() != 0 || removedReqArgs.size() != 0 || stableReqArgs.size() != 0) {
						out.write("\n\t\tRequest");
						if (newReqArgs.size() != 0){
							out.write("\n\t\t\tNew parameters:  ");
							for (Argument newArg: newReqArgs) {
								out.write(newArg.getName());
							}
							out.write("\n");
						}
						if (removedReqArgs.size() != 0){
							out.write("\n\t\t\tRemoved parameters:  ");
							for (Argument removedArg: removedReqArgs) {
								out.write(removedArg.getName());
							}
						}
						out.write("\n");
						if (stableReqArgs.size() != 0){
							out.write("\n\t\t\tChanged parameters:  ");
							for (Argument stableArg: stableReqArgs.values()) {
								String newRequired = "optional";
								String oldRequired = "optional";
								if (oldCommand.getReqArgByName(stableArg.getName()).isRequired() == true)
									oldRequired = "required";
								if (newCommand.getReqArgByName(stableArg.getName()).isRequired() == true)
									newRequired = "required";
								
								out.write(stableArg.getName() + " (old version - " + oldRequired + ", new version - " + newRequired + ") ");
							}
						}
						out.write("\n");
					}
					
					//Response
					if (newRespArgs.size() != 0 || removedRespArgs.size() != 0 || stableRespArgs.size() != 0) {
						out.write("\n\t\tResponse:");
						if (newRespArgs.size() != 0){
							out.write("\n\t\t\tNew parameters:  ");
							for (Argument newArg: newRespArgs) {
								out.write(newArg.getName());
							}
							out.write("\n");
						}
						if (removedRespArgs.size() != 0){
							out.write("\n\t\t\tRemoved parameters:  ");
							for (Argument removedArg: removedRespArgs) {
								out.write(removedArg.getName());
							}
						}
						out.write("\n");
					}
				}	
			}
	        
	        

		    out.close();

		    } catch (IOException e) {
		      e.printStackTrace();
		    } 


	}
}
