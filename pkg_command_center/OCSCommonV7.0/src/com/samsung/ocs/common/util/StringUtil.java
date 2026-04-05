package com.samsung.ocs.common.util;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * StringUtil Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2011. 6. 21.
 * @version 3.0
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class StringUtil {
	private static Format timeFormat = null;
	static {
		timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}
	
	/**
	 * 
	 * @param rs
	 * @param col
	 * @return
	 * @exception SQLException
	 */
    public static Date getDate(ResultSet rs, String col) throws SQLException {
        return new Date(rs.getDate(col).getTime() + rs.getTime(col).getTime());
    }
    
    /**
     * 
     * @param dateString
     * @param format
     * @return
     * @exception ParseException
     */
    public static Date getDate(String dateString, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(dateString);
    }
	
    /**
     * 
     * @return
     */
    public static String getCurrTimeString() {
    	if (timeFormat != null) {
			return timeFormat.format(new Date());
    	} else {
    		return "";
    	}
	}
    
    /**
     * 
     * @param b
     * @return
     */
    public static String encodeBase64(byte[] b) {
        if (b == null) {
            return null;
        }
        BASE64Encoder be = new BASE64Encoder();
        return be.encode(b);
    }
    
    /**
     * 
     * @param src
     * @return
     * @exception IOException
     */
    public static byte[] decodeBASE64(String src) throws IOException {
    	if (src == null){
    		return null;
    	}
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(src);
    }
    
    /**
     * 
     * @param byte1
     * @param byte2
     * @param length
     * @return
     */
    public static boolean compareBytes(byte[] byte1, byte[] byte2, int length) {
    	if (byte1 == null || byte2 == null) {
    		return false;
    	}
    	for (int i = 0; i < length ; i++) {
    		if (byte1[i] != byte2[i]) {
    			return false;
    		}
    	}
    	return true;
    }
}
