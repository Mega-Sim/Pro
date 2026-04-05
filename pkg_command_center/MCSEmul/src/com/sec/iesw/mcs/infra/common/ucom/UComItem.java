package com.sec.iesw.mcs.infra.common.ucom;

import SEComEnabler.SEComStructure.SECSID;
import XCom.XSecsItem;

public class UComItem {
	public final static int ITEM_TYPE_XCOM = 1;

	public final static int ITEM_TYPE_SECOM = 2;

	/** Message Type */
	int m_nItemType;

	/** XComMsg °´Ã¼ */
	private XSecsItem m_XComItem;

	private SECSID m_SEComItem;

	/** SEComMsg °´Ã¼ */

	public UComItem(XSecsItem item) {
		m_XComItem = null;
		m_SEComItem = null;

		m_nItemType = ITEM_TYPE_XCOM;
		m_XComItem = item;
	}

	public UComItem(SECSID item) {
		m_XComItem = null;
		m_SEComItem = null;

		m_nItemType = ITEM_TYPE_SECOM;
		m_SEComItem = item;
	}

	public String GetTypeName() {
		String strReturn = "";

		switch (m_nItemType) {
		case ITEM_TYPE_XCOM:
			strReturn = m_XComItem.GetTypeName();
			break;
		case ITEM_TYPE_SECOM:
			strReturn = m_SEComItem.getSECSFormat();
			break;
		}

		return strReturn;
	}

	public int GetU2Item() {
		int nReturn = 0;
		switch (m_nItemType) {
		case ITEM_TYPE_XCOM:
			nReturn = m_XComItem.GetU2Item()[0];
			break;
		case ITEM_TYPE_SECOM:
			nReturn = Integer.parseInt(m_SEComItem.getValue());
			break;
		}

		return nReturn;
	}

	public long GetU4Item() {
		long nReturn = 0;
		switch (m_nItemType) {
		case ITEM_TYPE_XCOM:
			nReturn = m_XComItem.GetU4Item()[0];
			break;
		case ITEM_TYPE_SECOM:
			nReturn = Long.parseLong(m_SEComItem.getValue());
			break;
		}

		return nReturn;
	}

}
