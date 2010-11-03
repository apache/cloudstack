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
package com.cloud.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernamePasswordValidator {
	private Pattern usernamePattern;
	private Pattern passwordPattern;
	private Matcher matcher;

	private static final String USERNAME_PATTERN = "^[a-zA-Z0-9][a-zA-Z0-9@._-]{2,63}$";
	private static final String PASSWORD_PATTERN = "^[a-zA-Z0-9][a-zA-Z0-9@#+=._-]{2,31}$";


	public UsernamePasswordValidator(){
		usernamePattern = Pattern.compile(USERNAME_PATTERN);
		passwordPattern = Pattern.compile(PASSWORD_PATTERN);

	}

	public boolean validateUsername(final String username){
		matcher = usernamePattern.matcher(username);
		return matcher.matches();
	}

	public boolean validatePassword(final String password){
		matcher = passwordPattern.matcher(password);
		return matcher.matches();
	}

}
