package ocsmanager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yk09.kang
 *
 */
public class DBAccessFrame2 {

	Connection conn = null;
	boolean reconnectRequested = false;
	String dbms;
	String url;
	String user;
	String logPath;
	int port;
	String password;

	public DBAccessFrame2() {
		if (loadConfig()) {
			connectToDB();
		}
		new DBAccessManagerThread().start();
	}

	/**
	 * reconnect 요청시 재접속시도 하는 Thread
	 * 
	 * @author yk09.kang
	 */
	class DBAccessManagerThread extends Thread {
		boolean isAlive = true;

		public void run() {
			try {
				while (isAlive) {
					if (reconnectRequested) {
						if (connectToDB()) {
							reconnectRequested = false;
						}
					}
					sleep(10);
				}
			} catch (Exception e) {
				//
			}
		}
	}

	/**
	 * DB 접속을 위한 설정파일 load
	 * 
	 * @return
	 */
	boolean loadConfig() {
		StringBuffer filePathName = new StringBuffer();
		String fileName = "OCSManager.ini";

		// get current directory
		String path = System.getProperty("user.dir");
		String separator = System.getProperty("file.separator");
		filePathName.append(path).append(separator).append(fileName);

		File f;

		RandomAccessFile raf = null;
		boolean result = true;

		int i, nPos;
		String lineString = "";

		try {
			f = new File(filePathName.toString());
			raf = new RandomAccessFile(f, "r");

			while ((lineString = raf.readLine()) != null) {
				// Database
				if (lineString.indexOf("DBMS") == 0) {
					nPos = lineString.indexOf("=");
					dbms = lineString.substring(nPos + 1);
					dbms = dbms.trim();
				} else if (lineString.indexOf("Url") == 0) {
					nPos = lineString.indexOf("=");
					url = lineString.substring(nPos + 1);
					url = url.trim();
				} else if (lineString.indexOf("User") == 0) {
					nPos = lineString.indexOf("=");
					user = lineString.substring(nPos + 1);
					user = user.trim();
				} else if (lineString.indexOf("Password") == 0) {
					nPos = lineString.indexOf("=");
					password = lineString.substring(nPos + 1);
					password = password.trim();
				} else if (lineString.indexOf("Port") == 0) {
					nPos = lineString.indexOf("=");
					String strPort = lineString.substring(nPos + 1);
					try {
						port = Integer.parseInt(strPort);
					} catch (Exception e1) {
					}
				} else if (lineString.indexOf("Path") == 0) {
					nPos = lineString.indexOf("=");
					logPath = lineString.substring(nPos + 1);
					logPath = logPath.trim();
				}
			}
		} catch (IOException e) {
			StringBuffer sb = new StringBuffer();
			StackTraceElement[] trace = e.getStackTrace();
			for (int k = 0; k < trace.length; k++) {
				sb.append("\n " + trace[k]);
			}
			String strLog = "[DBAccessManager] LoadConfig - IOException: " + sb.toString();
			writeLog(strLog);
			result = false;
		} finally {
			try {
				if (raf != null)
					raf.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * connect To DB
	 * 
	 * @return
	 */
	boolean connectToDB() {
		try {
			DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
			conn = DriverManager.getConnection(url, user, password);
			String strLog = "DB Connected..."; // 로그 추가 by OWJ 2005.12.13.
			writeLog(strLog);

		} catch (SQLException e) {
			StringBuffer sb = new StringBuffer();
			StackTraceElement[] trace = e.getStackTrace();
			for (int k = 0; k < trace.length; k++) {
				sb.append("\n " + trace[k]);
			}
			String strLog = "[DBAccessManager] ConnectToDB - SQLException: " + sb.toString();
			writeLog(strLog);
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param strSql
	 * @return
	 * @throws SQLException
	 */
	protected synchronized ResultSet getRecord(String strSql) throws Exception {

		Connection conn = getConnection();
		ResultSet rs = null;
		Statement stmt = null;

		try {
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(strSql);
		} catch (Exception e) {
			requestDBReconnect();
			closeRecord(rs);
			throw e;
		}
		return rs;
	}

	public Connection getConnection() {
		return conn;
	}

	protected synchronized void closeRecord(ResultSet rs) {
		try {
			if (rs != null) {
				Statement stmt = rs.getStatement();
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
			}
		} catch (SQLException e) {
			String strLog = "[DBAccessFrame] CloseRecord - SQLException: " + e.getMessage();
		}
	}

	private synchronized void execQueuedSqlList() throws Exception {
		Connection conn = getConnection();
		Statement stmt = null;
		try {
			for (String sql : queuedSql) {
				stmt = conn.createStatement();
				stmt.execute(sql);
				queuedSql.remove(sql);
				stmt.close();
			}
		} catch (Exception e) {
			requestDBReconnect();
			throw e;
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Exception ignore) {
				}
			}
		}
	}

	private Set<String> queuedSql = new HashSet<String>();

	/** */
	protected synchronized void execSQL(String sql) throws Exception {
		Connection conn = getConnection();
		Statement stmt = null;
		try {
			execQueuedSqlList();
			stmt = conn.createStatement();
			stmt.execute(sql);
		} catch (Exception e) {
			requestDBReconnect();
			queuedSql.add(sql);
			throw e;
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Exception ignore) {
				}
			}
		}
	}

	public void requestDBReconnect() {
		reconnectRequested = false;
	}

	public String makeString(String string) {
		if (string == null) {
			return "";
		} else {
			return string;
		}
	}

	public int makeInteger(int num) {
		// TODO Auto-generated method stub
		return new Integer(num);
	}

	public boolean getBoolean(String value) {
		if ("FALSE".equalsIgnoreCase(value))
			return false;
		else
			return true;
	}

	public void writeLog(String log) {
		//
	}

}
