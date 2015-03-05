package de.hundebarf.bestandspruefer.collection;

public class FundusAccount {
	private String mUser;
	private String mPassword;
	
	public FundusAccount(String user, String password) {
		mUser = user;
		mPassword = password;
	}

	public String getUser() {
		return mUser;
	}

	public String getPassword() {
		return mPassword;
	}
}
