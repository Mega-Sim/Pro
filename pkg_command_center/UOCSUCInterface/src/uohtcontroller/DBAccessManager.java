package uohtcontroller;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

public class DBAccessManager {

	// DB Connection
	String m_strDBMS = "";
	String m_strUrl = "";
	String m_strUser = "";
	String m_strPassword = "";
	Connection m_conn = null;
	private Statement m_stBatch = null;
	private boolean m_bOnReconnect = false;
	private int m_nDBReconnectCnt = 0;
	private boolean m_bActive = false;
	private int m_nConnectedOperation = 0;

	Vector m_vtSqlList = new Vector();	// 처리안된 Sql 문
	Vector m_vtBatchSqlList = new Vector();	// Batch 처리 Sql 문
	Vector m_vtExecSqlList = new Vector();

	// SystemConfig
	private int m_nDBFrameLogEnabled = 1;
	
	// 2019.09.02 by JJW : HostType 변수 선언
	String m_HostType = "";

	// Log
	String m_sLogPath = "";
	int m_nDeleteLogDay = -1;
	long m_lLastDeleteLogTime = 0;

	// Timer Thread
	private OperationThread m_OperationThread = null; // 1초

	// DB Frame 자체 발생 Alarm ID
	final int DBFRAME_ALID_RECONNECT = -90001;

	public DBAccessManager() {
		Initialize();
	}

	void Initialize() {
		if (LoadConfig()) {
			ConnectToDB();
		}

		// Main Operation Timer Thread 생성 및 실행
		m_OperationThread = new OperationThread();
		m_OperationThread.start();
	}

	/**
	 * Main Operation Timer - 1초 Timer
	 * 정상 및 비정상 Case 검색 및 처리
	 * <p>Title: </p>
	 * <p>Description: </p>
	 * <p>Copyright: Copyright (c) 2005</p>
	 * <p>Company: </p>
	 * @author not attributable
	 * @version 1.0
	 */
	class OperationThread extends Thread {
		boolean m_bRun = true;

		public void run() {
			try {
				while (m_bRun) {
					if (m_bOnReconnect == true) {
						if (ConnectToDB() == true) {
							m_bOnReconnect = false;
						}
					}
					sleep(10);
				}
			} catch (Exception e) {
				String strLog = "OperationThread - Exception: " + e.getMessage();
				WriteLog(strLog);
			}
		}
	}

	// Ini File로부터 Configuration Data 읽기
	boolean LoadConfig() {
		StringBuffer FilePathName = new StringBuffer();
		String strFileName = "UOHTController.ini";

		// get current directory
		String Path = System.getProperty("user.dir");
		String Separator = System.getProperty("file.separator");
		FilePathName.append(Path).append(Separator).append(strFileName);

		File f;
		RandomAccessFile raf = null;
		boolean bReturn = true;

		int nPos;
		String sLine = "";

		try {
			f = new File(FilePathName.toString());
			raf = new RandomAccessFile(f, "r");

			while ((sLine = raf.readLine()) != null) {
				// Database
				if (sLine.indexOf("DBMS") == 0) {
					nPos = sLine.indexOf("=");
					m_strDBMS = sLine.substring(nPos + 1);
					m_strDBMS = m_strDBMS.trim();
				}
				else if (sLine.indexOf("Url") == 0) {
					nPos = sLine.indexOf("=");
					m_strUrl = sLine.substring(nPos + 1);
					m_strUrl = m_strUrl.trim();
				}
				else if (sLine.indexOf("User") == 0) {
					nPos = sLine.indexOf("=");
					m_strUser = sLine.substring(nPos + 1);
					m_strUser = m_strUser.trim();
				}
				else if (sLine.indexOf("Password") == 0) {
					nPos = sLine.indexOf("=");
					m_strPassword = sLine.substring(nPos + 1);
					m_strPassword = m_strPassword.trim();
				}
				else if (sLine.indexOf("Path") == 0) {
					nPos = sLine.indexOf("=");
					m_sLogPath = sLine.substring(nPos + 1);
					m_sLogPath = m_sLogPath.trim();
				}
				// 2019.09.02 by JJW : HostType 값  읽기
				else if (sLine.indexOf("HostServiceType") == 0) {
					nPos = sLine.indexOf("=");
					m_HostType = sLine.substring(nPos + 1);
					m_HostType = m_HostType.trim();
				}
				else if (sLine.indexOf("DeleteLogDay") == 0) {
					nPos = sLine.indexOf("=");
					String strDeleteLogDay = sLine.substring(nPos + 1);
					try {
						m_nDeleteLogDay = Integer.parseInt(strDeleteLogDay);
					} catch (Exception e1) {
					}
				}
			}
		} catch (IOException e) {
			String strLog = "[DBAccessManager] LoadConfig - IOException: " + e.getMessage();
			WriteLog(strLog);

			bReturn = false;
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return bReturn;
	}

	boolean ConnectToDB() {
		try {
			DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
		} catch (SQLException e) {
			String strLog = "[DBAccessManager] ConnectToDB - SQLException: " + e.getMessage();
			WriteLog(strLog);
			return false;
		}
		try {
			// 2005. 7. 6. 수정
			// SelectMethod 속성 변경 : Direct(Default) -> Cursor
			// 사유 : AutoCommit 모드를 false로 사용할 경우 아래와 같은 오류 발생
			// 오류 메시지 : Can't start a cloned connection while in manual transaction mode
			//      m_conn = DriverManager.getConnection(m_strUrl+";SelectMethod=Cursor", m_strUser, m_strPassword);
			m_conn = DriverManager.getConnection(m_strUrl, m_strUser, m_strPassword);

			// 2005. 7. 6. 추가
			// 갱신문에 대한 rollback을 사용하기 위해서는 AutoCommit이 false가 되어야 함.
			// rollback 사유 : executeBatch() 실행 시 일부만 갱신되고 일부는 갱신이 안되는 오류 발생 현상 감지
			// executeBatch() 실행 시 일부라도 fail 발생할 경우 전체 transaction을 rollback 처리하도록 함.
			//      2005. 10. 7
			//      AutoCommit = true로 복원
			//      m_conn.setAutoCommit(false);

			String strLog = "DB Connected..."; // 로그 추가 by OWJ 2005.12.13.
			WriteLog(strLog);
		} catch (SQLException e) {
			String strLog = "ConnectToDB - SQLException: " + e.getMessage();
			WriteLog(strLog);
			return false;
		}

		return true;
	}

	void CloseProcess() {
		try {
			if (m_conn != null) {
				m_conn.close();
				m_conn = null;
				WriteLog("[DBAccessManager] DB closed.");
			}
		} catch (SQLException e) {
		}
	}

	void WriteLog(String strLog) {
		if (m_nDBFrameLogEnabled > 0) {
			// m_Owner.m_pUtility.WriteReturnLog("DBFramework", strLog, "", true);
		}
	}

	synchronized ResultSet GetRecord(String strSql) throws SQLException {
		ResultSet rs = null;
		Statement stmt = null;

		WriteLog(System.currentTimeMillis() + " GetRecord " + strSql);
		try {
			if (m_conn != null) {
				stmt = m_conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
				rs = stmt.executeQuery(strSql);
			}
			return rs;
		} catch (SQLException e) {
			ReconnectToDB();
			throw e;
		}
	}

	synchronized void CloseRecord(ResultSet rs) {
		try {
			if (rs != null) {
				Statement stmt = rs.getStatement();
				rs.close();
				rs = null;
				stmt.close();
				stmt = null;
			}
		} catch (SQLException e) {
			String strLog = "[DBAccessManager] CloseRecord - SQLException: " + e.getMessage();
			WriteLog(strLog);
			ReconnectToDB();
		}
	}

	/**
	 * Transaction fail 시에도 retry를 하지 않도록 하는 SQL 실행함수
	 * @param strSql String
	 * @throws SQLException
	 */
	synchronized void ExecSQL_NotFailGuaranteed(String strSql) throws SQLException {
		Statement st = null;
		try {
			st = m_conn.createStatement();
			st.execute(strSql);

			m_nDBReconnectCnt = 0; // Transaction 정상 수행시 DBReconnectCnt초기화 by OWJ 2005.12.13

			// 2005. 10. 30. 수정. 정정주
			// 주기적으로 실행하는 db transaction에 대한 log는 기록하지 않도록 함.
			// WriteLog("[DBAccessManager] ExecSQL: " + strSql);
		} catch (SQLException e) {
			ReconnectToDB();
			throw e;
		} finally {
			if (st != null) {
				st.close();
			}
			st = null;
		}
	}

	/**
	 * 2006. 1. 23. 추가. 정정주
	 * 처리 실패한 Sql문을 List에 저장
	 * 바로 전에 등록된 Sql문과 동일한 sql문 저장하고자 할 경우 fail 처리
	 * 3번 retry fail 시에는 해당 sql문 제거
	 * @param strSql String
	 */
	void AddFailedSqlToList(String strSql) {
		String strLog = "";
		m_vtSqlList.add(strSql);

		strLog = "SQLList added [" + String.valueOf(m_vtSqlList.size()) + "] - " + strSql;
		WriteLog(strLog);
	}

	/**
	 * SQL문 실행
	 * @param strSql String : 실행할 SQL문
	 * @throws SQLException : Exception
	 */
	synchronized void ExecSQL(String strSql) throws SQLException {
		//    WriteLog(System.currentTimeMillis()+" ExecSQL "+strSql);
		//    AccessSql(strSql, null);

		boolean bAddList = false;
		String strLog = "";
		boolean bSQLExceptionThrowed = false;

		WriteLog(System.currentTimeMillis() + " ExecSQL " + strSql);
		try {
			Statement st = null;
			try {
				if ((m_conn != null) && (m_conn.isClosed() == false)) {
					try {
						st = m_conn.createStatement();
					} catch (SQLException e) {
						strLog = "SQLException [" + e.getMessage() + "] ExecSQL() createStatement Failed - " + strSql;
						WriteLog(strLog);

						// 실행되지 못한 구문을 SqlList에 저장
						if (bAddList == false) {
							AddFailedSqlToList(strSql);
							bAddList = true;
						}

						ReconnectToDB();
						bSQLExceptionThrowed = true;
						throw e;
					}

					try {
						// Queued 되어 있는 Sql문 실행
						ExecQueuedSqlList();

						st.execute(strSql);

						// 2005. 7. 6. 수정
						// 2005. 10. 7. 수정. 정정주
						// autocommit = true
						// m_conn.commit();

						m_nDBReconnectCnt = 0; // Transaction 정상 수행시 DBReconnectCnt초기화 by OWJ 2005.12.13
					} catch (SQLException e2) {
						strLog = "SQLException [" + e2.getMessage() + "] ExecSQL() execute Failed - " + strSql;
						WriteLog(strLog);

						// 실행되지 못한 구문을 SqlList에 저장
						if (bAddList == false) {
							AddFailedSqlToList(strSql);
							bAddList = true;
						}

						ReconnectToDB();
						bSQLExceptionThrowed = true;
						throw e2;
					}
				}
				else {
					strLog = "Exception [Connection is null] ExecSQL() - " + strSql;
					WriteLog(strLog);

					// 실행되지 못한 구문을 SqlList에 저장
					if (bAddList == false) {
						AddFailedSqlToList(strSql);
						bAddList = true;
					}

					ReconnectToDB();
					bSQLExceptionThrowed = true;
					throw new SQLException("Connection is null");
				}
			} catch (SQLException e) {
				// 2005. 10. 30. 수정. 정정주
				// 내부 throw에 의한 SQLException 재호출은 무시하도록 함.
				if (bSQLExceptionThrowed == false) {
					strLog = "SQLException [" + e.getMessage() + "] ExecSQL() Failed - " +
							strSql;
					WriteLog(strLog);

					// 실행되지 못한 구문을 SqlList에 저장
					if (bAddList == false) {
						AddFailedSqlToList(strSql);
						bAddList = true;
					}

					ReconnectToDB();
				}
				throw e;
			} finally {
				if (st != null) {
					st.close();
					st = null;
				}
			}
		} catch (Exception e1) {
			strLog = "Exception [" + e1.getMessage() + "] ExecSQL() Failed - " + strSql;
			WriteLog(strLog);

			// 실행되지 못한 구문을 SqlList에 저장
			if (bAddList == false) {
				AddFailedSqlToList(strSql);
				bAddList = true;
			}

			ReconnectToDB();
			throw new SQLException(e1.getMessage());
		}
	}

	/**
	 * SQL문 실행
	 * @param strSql String : 실행할 SQL문
	 * @throws SQLException : Exception
	 */
	synchronized void ExecSQL2(String strSql) throws SQLException {
		boolean bAddList = false;
		String strLog = "";
		boolean bSQLExceptionThrowed = false;

		WriteLog(System.currentTimeMillis() + " ExecSQL " + strSql);
		try {
			Statement st = null;
			try {
				if ((m_conn != null) && (m_conn.isClosed() == false)) {
					try {
						st = m_conn.createStatement();
					} catch (SQLException e) {
						strLog = "SQLException [" + e.getMessage() + "] ExecSQL() createStatement Failed - " + strSql;
						WriteLog(strLog);

						// 실행되지 못한 구문을 SqlList에 저장
						if (bAddList == false) {
							AddFailedSqlToList(strSql);
							bAddList = true;
						}

						ReconnectToDB();
						bSQLExceptionThrowed = true;
						throw e;
					}

					try {
						// Queued 되어 있는 Sql문 실행
						ExecQueuedSqlList();

						st.execute(strSql);

						// 2005. 7. 6. 수정
						// 2005. 10. 7. 수정. 정정주
						// autocommit = true
						// m_conn.commit();

						m_nDBReconnectCnt = 0; // Transaction 정상 수행시 DBReconnectCnt초기화 by OWJ 2005.12.13
					} catch (SQLException e2) {
						strLog = "SQLException [" + e2.getMessage() + "] ExecSQL() execute Failed - " + strSql;
						WriteLog(strLog);

						// 실행되지 못한 구문을 SqlList에 저장
						if (bAddList == false) {
							AddFailedSqlToList(strSql);
							bAddList = true;
						}

						ReconnectToDB();
						bSQLExceptionThrowed = true;
						throw e2;
					}
				}
				else {
					strLog = "Exception [Connection is null] ExecSQL() - " + strSql;
					WriteLog(strLog);

					// 실행되지 못한 구문을 SqlList에 저장
					if (bAddList == false) {
						AddFailedSqlToList(strSql);
						bAddList = true;
					}

					ReconnectToDB();
					bSQLExceptionThrowed = true;
					throw new SQLException("Connection is null");
				}
			} catch (SQLException e) {
				// 2005. 10. 30. 수정. 정정주
				// 내부 throw에 의한 SQLException 재호출은 무시하도록 함.
				if (bSQLExceptionThrowed == false) {
					strLog = "SQLException [" + e.getMessage() + "] ExecSQL() Failed - " +
							strSql;
					WriteLog(strLog);

					// 실행되지 못한 구문을 SqlList에 저장
					if (bAddList == false) {
						AddFailedSqlToList(strSql);
						bAddList = true;
					}

					ReconnectToDB();
				}
				throw e;
			} finally {
				if (st != null) {
					st.close();
					st = null;
				}
			}
		} catch (Exception e1) {
			strLog = "Exception [" + e1.getMessage() + "] ExecSQL() Failed - " + strSql;
			WriteLog(strLog);

			// 실행되지 못한 구문을 SqlList에 저장
			if (bAddList == false) {
				AddFailedSqlToList(strSql);
				bAddList = true;
			}

			ReconnectToDB();
			throw new SQLException(e1.getMessage());
		}
	}

	synchronized void AddBatch(String strSql) throws SQLException {
		//    WriteLog(System.currentTimeMillis()+" AddBatch " + strSql);
		boolean bAddList = false;
		String strLog = "";
		boolean bSQLExceptionThrowed = false;

		try {
			try {
				if (m_stBatch == null) {
					if ((m_conn != null) && (m_conn.isClosed() == false)) {
						try {
							m_stBatch = m_conn.createStatement();
						} catch (SQLException e) {
							strLog = "SQLException [" + e.getMessage() + "] AddBatch() createStatement Failed - " + strSql;
							WriteLog(strLog);

							// 실행되지 못한 구문을 SqlList에 저장
							if (bAddList == false) {
								AddFailedSqlToList(strSql);
								bAddList = true;
							}

							ReconnectToDB();
							bSQLExceptionThrowed = true;
							throw e;
						}
					}
				} // m_stBatch가 null일 경우 createStatement를 통해 statement 생성

				if (m_stBatch != null) {
					try {
						m_stBatch.addBatch(strSql);
						m_vtBatchSqlList.add(strSql);
					} catch (SQLException e) {
						strLog = "SQLException [" + e.getMessage() + "] AddBatch() addBatch Failed - " + strSql;
						WriteLog(strLog);

						// 실행되지 못한 구문을 SqlList에 저장
						if (bAddList == false) {
							AddFailedSqlToList(strSql);
							bAddList = true;
						}
						bSQLExceptionThrowed = true;
						throw e;
					}
				} else {
					// m_stBatch가 생성되지 못했을 경우 DB Reconnection
					strLog = "SQLException [Batch Statement is null] AddBatch() - " +
							strSql;
					WriteLog(strLog);

					// 실행되지 못한 구문을 SqlList에 저장
					if (bAddList == false) {
						AddFailedSqlToList(strSql);
						bAddList = true;
					}

					ReconnectToDB();
					bSQLExceptionThrowed = true;
					throw new SQLException("Batch Statement is null");
				}
			} catch (SQLException e) {
				// 2005. 10. 30. 수정. 정정주
				// 내부 throw에 의해 호출된 SQLException은 무시하도록 함.
				if (bSQLExceptionThrowed == false) {
					strLog = "SQLException [" + e.getMessage() + "] AddBatch() - " + strSql;
					WriteLog(strLog);

					// 실행되지 못한 구문을 SqlList에 저장
					if (bAddList == false) {
						AddFailedSqlToList(strSql);
						bAddList = true;
					}

					ReconnectToDB();
				}
				throw e;
			}
		} catch (Exception e1) {
			strLog = "Exception [" + e1.getMessage() + "] AddBatch() - " + strSql;
			WriteLog(strLog);

			// 실행되지 못한 구문을 SqlList에 저장
			if (bAddList == false) {
				AddFailedSqlToList(strSql);
				bAddList = true;
			}

			ReconnectToDB();
			throw new SQLException(e1.getMessage());
		}
	}

	/**
	 * Batch Query 실행
	 * @throws SQLException
	 */
	synchronized void ExecBatch() throws SQLException {
		String strLog = "";
		boolean bAddList = false;
		boolean bRollback = false;

		// WriteLog(System.currentTimeMillis()+" ExecBatch "+m_vtBatchSqlList.size());
		// 기존 conn이 Reconnect하기 위해, close되었을 경우 에러 처리로 빠지도록 수정
		// by OWJ 2005.12.13.
		Connection conn = m_conn;
		try {
			try {
				if (m_stBatch != null) {
					ExecQueuedSqlList();

					// 2005. 10. 7. 추가. 정정주.
					// autocommit default=true이므로 false 임시 변경
					if (conn.getAutoCommit() == true) {
						conn.setAutoCommit(false);
					}
					m_stBatch.executeBatch();

					// 2005. 7. 6. 수정
					conn.commit();

					m_nDBReconnectCnt = 0; // Transaction 정상 수행시 DBReconnectCnt초기화 by OWJ 2005.12.13

					// 2005. 10. 7. 추가. 정정주.
					// autocommit default=true이므로 true로 재변경
					if (conn.getAutoCommit() == false) {
						conn.setAutoCommit(true);
					}
					m_vtBatchSqlList.removeAllElements();

					// 2005. 10. 30. 수정. 정정주
					// null인 경우 close하지 않도록 함.
					if (m_stBatch != null) {
						try {
							m_stBatch.close();
							m_stBatch = null;
						} catch (SQLException ex) {
						}
					}
				}
			} catch (BatchUpdateException e_batch) {
				// 2005. 7. 6. 수정
				// 실패한 transaction에 대해 rollback 처리
				conn.rollback();

				// 2005. 10. 7. 추가. 정정주.
				// autocommit default=true이므로 true로 재변경
				if (conn.getAutoCommit() == false) {
					conn.setAutoCommit(true);
				}
				strLog = "SQLException [" + e_batch.getMessage() + "] ExecBatch() executeBatch Failed";
				WriteLog(strLog);

				if (bAddList == false) {
					for (int i = 0; i < m_vtBatchSqlList.size(); i++) {
						AddFailedSqlToList( (String) m_vtBatchSqlList.get(i));
					}

					m_vtBatchSqlList.removeAllElements();
					bAddList = true;
				}
				bRollback = true;

				// 2005. 10. 30. 수정. 정정주
				// null인 경우 close하지 않도록 함.
				if (m_stBatch != null) {
					try {
						m_stBatch.close();
						m_stBatch = null;
					} catch (SQLException ex) {
					}
				}

				// 2005. 8. 9. 추가. 정정주.
				// BatchUpdateException에 대한 throw가 없어 추가함.
				throw new SQLException(e_batch.getMessage());
			} catch (SQLException e) {
				if (bRollback == false) {
					// 2005. 7. 6. 수정
					// 실패한 transaction에 대해 rollback 처리
					conn.rollback();

					// 2005. 10. 7. 추가. 정정주.
					// autocommit default=true이므로 true로 재변경
					if (conn.getAutoCommit() == false) {
						conn.setAutoCommit(true);
					}

					strLog = "SQLException [" + e.getMessage() + "] ExecBatch() executeBatch Failed";
					WriteLog(strLog);

					if (bAddList == false) {
						for (int i = 0; i < m_vtBatchSqlList.size(); i++) {
							AddFailedSqlToList( (String) m_vtBatchSqlList.get(i));
						}

						m_vtBatchSqlList.removeAllElements();
						bAddList = true;
					}

					// 2005. 10. 30. 수정. 정정주
					// null인 경우 close하지 않도록 함.
					if (m_stBatch != null) {
						try {
							m_stBatch.close();
							m_stBatch = null;
						} catch (SQLException ex) {
						}
					}

					ReconnectToDB();
				}
				throw e;
			}
		} catch (Exception e1) {
			if (conn.getAutoCommit() == false) {
				conn.setAutoCommit(true);
			}
			strLog = "Exception [" + e1.getMessage() + "] ExecBatch() Failed";
			WriteLog(strLog);

			if (bAddList == false) {
				for (int i = 0; i < m_vtBatchSqlList.size(); i++) {
					AddFailedSqlToList( (String) m_vtBatchSqlList.get(i));
				}

				m_vtBatchSqlList.removeAllElements();
				bAddList = true;
			}

			// 2005. 10. 30. 수정. 정정주
			// null인 경우 close하지 않도록 함.
			if (m_stBatch != null) {
				try {
					m_stBatch.close();
					m_stBatch = null;
				} catch (SQLException ex) {
				}
			}

			ReconnectToDB();
			throw new SQLException(e1.getMessage());
		}
	}

	synchronized void ExecQueuedSqlList() {
		String strLog = "";

		try {
			Statement st = null;
			try {
				if ((m_conn != null) && (m_conn.isClosed() == false)) {
					try {
						st = m_conn.createStatement();
					} catch (SQLException e) {
					}

					if (m_vtSqlList.size() > 0 && st != null) {
						for (int i = 0; i < m_vtSqlList.size(); i++) {
							try {
								st.execute( (String) m_vtSqlList.get(i));

								if (m_nDBReconnectCnt > 0) {
									m_nDBReconnectCnt = 0; // Transaction 정상 수행시 DBReconnectCnt초기화 by OWJ 2005.12.13
								}
								// 2005. 10. 7. 수정. 정정주.
								// autocommit=true
								// m_conn.commit();
								WriteLog(System.currentTimeMillis() + " ExecQueuedSql [" + String.valueOf(i + 1) + "] : " + (String) m_vtSqlList.get(i));

								m_vtSqlList.remove(i);
								i--;
							} catch (SQLException e3) {
								// 2006. 1. 23. 추가. 정정주
								// SQL List에 있는 구문 실행 실패 시 log 기록
								strLog = "[DBFrame] ExecQueuedSql [" + String.valueOf(i + 1) + "] Failed : " + (String) m_vtSqlList.get(i);
								WriteLog(strLog);
							}
						}
					}
				}
			} catch (SQLException e) {
			} finally {
				if (st != null) {
					st.close();
					st = null;
				}
			}
		} catch (Exception e1) {
		}
	}

	/**
	 * String Vector를 이용하여 구성된 Batch에 대해 동시에 실행
	 * @param lstSqlList Vector
	 */
	synchronized void ExecBatchSql(Vector lstSqlList) throws SQLException {
		WriteLog(System.currentTimeMillis() + " ExecBatchSql " + lstSqlList.size());
		try {
			for (int i = 0; i < lstSqlList.size(); i++) {
				AddBatch((String) lstSqlList.get(i));
			}
			ExecBatch();
		} catch (SQLException e) {
			String strLog = "SQLException [" + e.getMessage() + "] ExecBatchSql() Failed";
			WriteLog(strLog);
			throw e;
		}
	}

	/**
	 * DB Connection fail에 따라 DB Reconnection 시도
	 */
	synchronized void ReconnectToDB() {
		// DB Reconnect Retry Count설정 by OWJ 2005.12.13.
		String strLog = "ReconnectToDB() called";
		if (m_nDBReconnectCnt < 10) {
			strLog += ", 그러나 Retry Count가 " + m_nDBReconnectCnt + " < 10이므로, Reconnect 보류함.";
			m_nDBReconnectCnt++;
			WriteLog(strLog);
			return;
		}
		m_nDBReconnectCnt = 0;
		WriteLog(strLog);

		try {
			// m_conn close조건 수정 by OWJ 2005.12.13.
			if (m_conn != null) {
				if (m_conn.isClosed() == false) {
					m_conn.close();

					strLog = "DB Disconnected..."; // 로그 추가 by OWJ 2005.12.13.
					WriteLog(strLog);

				}
				m_conn = null;
			}
		} catch (SQLException e) {
		}

		if (m_bOnReconnect == false) {
			m_bOnReconnect = true;
			m_bActive = false; // Active 상태 -> false : db access 작업 중지
		}
	}

	/**
	 * DB 연결 정보
	 * @return boolean
	 */
	boolean IsDBConnected() {
		if (m_conn != null) {
			try {
				boolean bClosed = m_conn.isClosed();
				if (bClosed == false) {
					return true;
				}
			} catch (SQLException e) {
			}
		}
		return false;
	}

	String GetCurrTimeStr() {
		Format formatter = new SimpleDateFormat("yyyyMMddHHmmssS");
		Date now = new Date();
		String strCurrTime = formatter.format(now);

		while (strCurrTime.length() > 16) {
			strCurrTime = strCurrTime.substring(0, strCurrTime.length() - 1);
		}

		return strCurrTime;
	}

	String GetCurrDBTimeStr() {
		String strCurrDBTime = "";
		String strSql = "";

		strSql = "SELECT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') FROM DUAL";
		ResultSet rs = null;
		try {
			rs = GetRecord(strSql);
			if ( (rs != null) && (rs.next())) {
				strCurrDBTime = rs.getString(1);
			}
		} catch (SQLException e) {
			String strLog = "[DBAccessManager] GetCurrDBTimeStr - SQLException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				CloseRecord(rs);
			}
		}

		return strCurrDBTime;
	}

	/**
	 * 일정 경과시간 이전의 시간 String
	 * @param lElapsedTime long : 경과 시간
	 * @return String : YYYYMMDDhhmmsscc(16bytes)
	 */
	String GetPrevTimeStr(long lElapsedTime) {
		Date PrevTime = new Date(System.currentTimeMillis() - lElapsedTime);
		Format formatter = new SimpleDateFormat("yyyyMMddHHmmssS");
		String strPrevTime = formatter.format(PrevTime);

		while (strPrevTime.length() > 16) {
			strPrevTime = strPrevTime.substring(0, strPrevTime.length() - 1);
		}

		while (strPrevTime.length() < 16) {
			strPrevTime = strPrevTime + "0";
		}

		return strPrevTime;
	}

	/**
	 * 일정 경과시간 이전의 시간 String
	 * DB 기준 시간으로부터의 경과시간값
	 * @param lElapsedTime long : 경과 시간
	 * @return String : YYYYMMDDhhmmss(14bytes)
	 */
	String GetPrevDBTimeStr(long lElapsedTime) {
		String strPrevTime = "";
		String strElapsedTime = String.valueOf(lElapsedTime) + "/24/60/60";
		String strSql = "SELECT TO_CHAR(SYSDATE-" + strElapsedTime + ", 'YYYYMMDDHH24MISS') FROM DUAL";

		ResultSet rs = null;
		try {
			rs = GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strPrevTime = rs.getString(1);
			}
		} catch (SQLException e) {
			String strLog = "[DBAccessManager] GetPrevDBTimeStr - SQLException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				CloseRecord(rs);
			}
		}

		return strPrevTime;
	}

	synchronized void AccessSql(String strSQL, Vector vtSqlList) {
		if (strSQL != null) {
			m_vtExecSqlList.add(strSQL);
		} else if (vtSqlList != null) {
			if (m_vtExecSqlList.size() > 0) {
				vtSqlList.add((String) m_vtExecSqlList.get(0));
				m_vtExecSqlList.remove(0);
			}
		}
	}

	synchronized public int GetConnection() {
		return m_nConnectedOperation;
	}

	synchronized public void AddConnection() {
		m_nConnectedOperation++;
	}

	synchronized public void DeleteConnection() {
		m_nConnectedOperation--;
	}

	synchronized public Connection getConnection() {
		return m_conn;
	}

}