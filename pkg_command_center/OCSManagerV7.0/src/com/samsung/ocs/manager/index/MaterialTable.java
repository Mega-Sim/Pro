package com.samsung.ocs.manager.index;

import java.util.ArrayList;

/**
 * MaterialTable Class, OCS 3.0 for Unified FAB
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

public class MaterialTable {
	private static MaterialTable materialTable = null;
	private ArrayList<String> materialList = null;
	
	/**
	 * Constructor of MaterialTable class.
	 */
	private MaterialTable() {
		materialList = new ArrayList<String>();
	}
	
	/**
	 * Constructor of MaterialTable class. (Singleton)
	 */
	public static synchronized MaterialTable getInstance() {
		if (materialTable == null) {
			materialTable = new MaterialTable();
		}
		return materialTable;
	}
	
	public int getMaterialIndex(String material) {
		try {
			if (materialList.contains(material) == false) {
				materialList.add(material);
			}
			return materialList.indexOf(material);
		} catch (Exception e) {
		}
		return 9999;
	}
}

