package com.orange.labs.dailymotion.kids.db;

/**
 * A helper class to create and upgrade an SQLite table using the provided {@link DatabaseColumn}
 * objects.
 */
public class TableCreator {
	private final String mName;
	private final String mTableConstraints;
	private final DatabaseColumn[] mColumns;

	public TableCreator(String name, DatabaseColumn[] columns) {
		this(name, columns, null);
	}
	
	public TableCreator(String name, DatabaseColumn[] columns, String tableConstraints) {
		mName = name;
		mColumns = columns;
		mTableConstraints = tableConstraints;
	}

	/**
	 * Return the query allowing the creation of the table for the provided version.
	 */
	public String getCreateTableQuery(int version) {
		String query;
		if (mTableConstraints != null) {
			query = String.format("CREATE TABLE %s (%s, %s);", mName, getColumns(version), mTableConstraints); 
		} else {
			query = String.format("CREATE TABLE %s (%s);", mName, getColumns(version)); 
		}
		
		return  query;
	}

	/**
	 * Return a SQLite query allowing to upgrade the table scheme from oldVersion to newVersion
	 * based on the definition of each column composing the table.
	 */
	public String getUpgradeTableQuery(int oldVersion, int newVersion) {
		StringBuilder builder = new StringBuilder();
		for (DatabaseColumn column : mColumns) {
			int sinceVersion = column.getSinceVersion();
			if (sinceVersion > oldVersion && sinceVersion <= newVersion) {
				builder.append("ALTER TABLE ");
				builder.append(mName);
				builder.append(" ADD COLUMN ");
				builder.append(column.getName());
				builder.append(" ");
				builder.append(column.getType());
				builder.append(";");
			}
		}
		return builder.toString();
	}

	/**
	 * Return all the columns of the table as a String. Columns are comma separated.
	 */
	private String getColumns(int version) {
		StringBuilder builder = new StringBuilder();
		for (DatabaseColumn column : mColumns) {
			if (column.getSinceVersion() <= version) {
				if (builder.length() != 0) {
					builder.append(", ");
				}
				builder.append(column.getName());
				builder.append(" ");
				builder.append(column.getType());
			}
		}
		return builder.toString();
	}
}
