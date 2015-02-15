package de.hundebarf.bestandspruefer.database;

public class DatabaseException extends Exception {
	
	private static final long serialVersionUID = -2461424374096300508L;
	
	private int mStatusCode = -1;
	
	public DatabaseException() {
		
	}
	
	public DatabaseException(String detailMessage, int statusCode) {
		this(detailMessage);
		mStatusCode = statusCode;
	}

	public DatabaseException(String detailMessage) {
		super(detailMessage);
	}

	public DatabaseException(Throwable throwable) {
		super(throwable);
	}

	public DatabaseException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
	
	public int getStatusCode() {
		return mStatusCode;
	}

}
