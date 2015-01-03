package de.hundebarf.bestandspruefer.collection;

import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;

public class QueryResult<DATA> {
	public DatabaseConnection connection;
	public DatabaseException exception;
	public DATA data;
}
