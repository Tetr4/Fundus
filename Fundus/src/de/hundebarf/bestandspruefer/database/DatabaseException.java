package de.hundebarf.bestandspruefer.database;

public class DatabaseException extends Exception {

	private static final long serialVersionUID = -8170247567364161008L;

	public DatabaseException() {
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

}
