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

package com.cloud.utils.events;

public class EventsTest {
	public void onWeatherChange(Object sender, EventArgs args) {
		System.out.println("onWeatherChange, weather: " + ((WeatherChangeEventArgs)args).getWeather());
	}
	
	public void onTrafficChange(Object sender, EventArgs args) {
		System.out.println("onTrafficChange");
	}
	
	public void run() {
		SubscriptionMgr mgr = SubscriptionMgr.getInstance();
		try {
			mgr.subscribe("weather", this, "onWeatherChange");
			mgr.subscribe("traffic", this, "onTrafficChange");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		
		mgr.notifySubscribers("weather", null, new WeatherChangeEventArgs("weather", "Sunny"));
		mgr.notifySubscribers("traffic", null, EventArgs.Empty);
	}

	public static void main(String[] args) {
		EventsTest test = new EventsTest();
		test.run();
	}
}

class WeatherChangeEventArgs extends EventArgs {
	private static final long serialVersionUID = -952166331523609047L;
	
	private String weather;
	
	public WeatherChangeEventArgs() {
	}
	
	public WeatherChangeEventArgs(String subject, String weather) {
		super(subject);
		this.weather = weather;
	}
	
	public String getWeather() { return weather; }
	public void setWeather(String weather) {
		this.weather = weather;
	}
}
