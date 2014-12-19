package model;

import admin.INotificationCallback;

/**
 * Represents all the information of a user containing the username, password, credits and if the user is online or offline.
 */
public class UserInfo{

	private String username;
	private String password;
	private long credits;
	private boolean online;
	private INotificationCallback callback;
	private long threshold;
	
	public UserInfo(String username, String password, long credits, boolean online) {
		this.username = username;
		this.password = password;
		this.credits = credits;
		this.online = online;
	}

	public void setCredits(long credits){
		this.credits = credits;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public long getCredits() {
		return credits;
	}

	public boolean isOnline() {
		return online;
	}
	
	public void setStatus(boolean status){
		online = status;
	}
	
	public void subscribe(INotificationCallback callback, long threshold){
		this.callback = callback;
		this.threshold = threshold;
	}
	
	public INotificationCallback getCallback(){
		return callback;
	}
	
	public long getThreshold(){
		return threshold;
	}

	@Override
	public String toString() {
		if(isOnline()){
			return username + " online Credits: "+credits;
		}
		return username + " offline Credits: "+credits;
	}
}
