package com.orange.labs.dailymotion.kids.db;

/**
 * Base class for defining table columns. A column is basically:
 * 
 * <ul>
 * 	<li>A name
 * 	<li>A type
 * 	<li>A version since it has been introduced at
 */
public interface DatabaseColumn {
    /** The name of the column. */
    public String getName();

    /** The type of the column in the SQLite database. */
    public String getType();

    /** The version of the database in which this column was introduced. */
    public int getSinceVersion();
   
}
