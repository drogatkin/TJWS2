package mysql;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionChecker {
	public static boolean validate(SQLException se, Connection conn) {
		for (; se != null; se = se.getNextException())
			try {
				//System.err.println("Checking: " + se.getClass().getName() + ", for " + conn + ", closed:"
					//	+ conn.isClosed());
				return "com.mysql.jdbc.exceptions.jdbc4.CommunicationsException".equals(se.getClass().getName()) == false
						&& conn.isClosed() == false;
			} catch (SQLException e) {
			}//System.err.println("Connection "+conn+" is bad");
		return false;
	}
}
