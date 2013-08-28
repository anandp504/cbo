package com.tumri.cbo.utils;

public class CBOMailConfig {

	private String host;
	private int port;
	private String from;
	private String to;
	private String username;
	private String password;
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public void setFrom(String from) {
		this.from = from;
	}
	
	public String getFrom() {
		return from;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getTo() {
		return to;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public String toString() {
		return "CBODatabaseConfig[host=" + getHost() + ",port=" + getPort() +
                ",username=" + getUsername() + ",password=" + getPassword() +
                ",from=" + getFrom() + ",to=" + getTo() +
                "]";
	}
}

