package com.tumri.cbo.utils;

public class CBODatabaseConfig {

	private String driver;
	private String url;
	private String databaseName;
	private String username;
	private String password;
	
	public String getDriver() {
		return driver;
	}
	
	public void setDriver(String driver) {
		this.driver = driver;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getUrl() {
		return url;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getDatabaseName() {
		return databaseName;
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
		return "CBODatabaseConfig[driver=" + getDriver() + ",url=" + getUrl() + ",dbName="
		       + getDatabaseName() + ",username=" + getUsername() + ",password=" + getPassword() +"]";
	}
}

