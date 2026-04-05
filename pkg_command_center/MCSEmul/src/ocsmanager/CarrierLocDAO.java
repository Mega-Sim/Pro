package ocsmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class CarrierLocDAO extends DBAccessFrame2 {

	HashMap<String, CarrierLoc> carrierLocMap = new HashMap<String, CarrierLoc>();

	public CarrierLocDAO() {
		super();
		uploadCarrierLocFromDB();
	}

	private static String SELECT_SQL = "SELECT * FROM STBCARRIERLOC WHERE CARRIERID IS NOT NULL";

	private boolean uploadCarrierLocFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;

		conn = getConnection();
		try {
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			if (rs != null) {
				String carrierLocId = null;
				CarrierLoc carrierLoc = null;
				while (rs.next()) {
					carrierLocId = makeString(rs.getString("CARRIERLOCID"));
					carrierLoc = carrierLocMap.get(carrierLocId);
					if (carrierLoc == null) {
						carrierLoc = new CarrierLoc();
					}
					carrierLoc.setCarrierLocId(carrierLocId);
					carrierLoc.setCarrierId(makeString(rs.getString("CARRIERID")));
					carrierLocMap.put(carrierLocId, carrierLoc);
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
		} catch (Exception ie) {
			result = false;
			ie.printStackTrace();
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception ignore2) {
				}
			}
		}
		return result;
	}

	public HashMap<String, CarrierLoc> getCarrierLocMap() {
		return carrierLocMap;
	}

}
