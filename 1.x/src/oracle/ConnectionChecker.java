package oracle;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * An example of JDBC connection validator class
 * 
 * @author dmitriy
 * 
 */
public class ConnectionChecker {
	/**
	 * 
	 * @param se
	 *            SQLException
	 * @return boolean true if the exception is safe for validity of a
	 *         connection
	 */
	public static boolean validate(SQLException se, Connection conn) {
		try {
			return (se.toString().indexOf("Io exception") < 0) || conn.isClosed() == false;
		} catch (SQLException e) {
		}
		return false;
	}
}
