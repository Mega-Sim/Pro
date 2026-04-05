package com.samsung.sem.items;

import com.samsung.sem.SEMConstant.SECSMSG_TYPE;

/**
 * EquipmentConstant Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date 2011. 6. 21.
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

public class EquipmentConstant {
	/*****************************************************************************
	 *  EquipmentConstant Class
	 *  - SetData(), SetValue(), SetValueString()
	 *  - GetData(), GetValue(), GetValueString(), GetECID()
	 *****************************************************************************/
	
	/* variables */
	private int equipmentConstantId;
	private String equipmentConstantName;
//	private String valueType; // 'A','B','BOOLEAN','U2','U4'
	private SECSMSG_TYPE valueType;
	private int numValue;
	private String strValue;
	private int defVal;
	private int min;
	private int max;
	private String unit;

	/**
	 * Constructor of EquipmentConstant class.
	 */
	public EquipmentConstant(){		
		equipmentConstantId = 0;
		numValue = 0;
		strValue = "";
		unit = "";
		min = 0;
		max = 0;
		defVal = 0;
		valueType = SECSMSG_TYPE.A;
	}
	
	/* methods : getter, setter */	
//	public boolean getData(HashMap returnTable){
//		Integer lECID = new Integer(ecId);
//		returnTable.put("ECID", lECID);
//		returnTable.put("ECNAME", ecName);
//		Integer lValue = new Integer(numValue);
//		returnTable.put("VALUE", lValue);
//		Integer lMin = new Integer(min);
//		returnTable.put("MIN", lMin);
//		Integer lMax = new Integer(max);
//		returnTable.put("MAX", lMax);
//		Integer lDefault = new Integer(nDefault);
//		returnTable.put("DEFAULT", lDefault);
//		returnTable.put("UNIT", unit);
//		return true;
//	}
	
	public boolean setData(int equipmentConstantId, String equipmentConstantName, int numValue, int min,
			int max, int defVal, String unit, SECSMSG_TYPE valueType)	{
		this.equipmentConstantId = equipmentConstantId;
		this.equipmentConstantName = equipmentConstantName;
		this.numValue = numValue;
		this.min = min;
		this.max = max;
		this.defVal = defVal;
		this.unit = unit;
		this.valueType = valueType;
		return true;
	}

	public boolean setData(int equipmentConstantId, String equipmentConstantName, String strValue, String unit){
		this.equipmentConstantId = equipmentConstantId;
		this.equipmentConstantName = equipmentConstantName;
		this.strValue = strValue;
		this.unit = unit;
		return true;
	}

	public int getEquipmentConstantId() {
		return equipmentConstantId;
	}

	public void setEquipmentConstantId(int equipmentConstantId) {
		this.equipmentConstantId = equipmentConstantId;
	}

	public String getEquipmentConstantName() {
		return equipmentConstantName;
	}

	public void setEquipmentConstantName(String equipmentConstantName) {
		this.equipmentConstantName = equipmentConstantName;
	}

	public int getNumValue() {
		return numValue;
	}

	public void setNumValue(int numValue) {
		this.numValue = numValue;
	}

	public String getStrValue() {
		return strValue;
	}

	public void setStrValue(String strValue) {
		this.strValue = strValue;
	}

	public int getDefVal() {
		return defVal;
	}

	public void setDefVal(int defVal) {
		this.defVal = defVal;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}
	
	public SECSMSG_TYPE getValueType() {
		return valueType;
	}

	public void setValueType(SECSMSG_TYPE valueType) {
		this.valueType = valueType;
	}
}
