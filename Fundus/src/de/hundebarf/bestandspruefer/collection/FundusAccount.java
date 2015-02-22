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

	public void setUser(String user) {
		mUser = user;
	}

	public String getPassword() {
		return mPassword;
	}

	public void setpassword(String password) {
		mPassword = password;
	}
}
