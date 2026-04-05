package com.samsung.sem.ucominterface;

import java.util.*;

import SEComEnabler.SEComStructure.SECSID;
import SEComEnabler.SEComStructure.SXTransaction;
import SEComEnabler.SEComStructure.SX.SECSFormat;

/**
 * UComMsg Class, OCS 3.0 for Unified FAB
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

public class UComMsg {
	protected final String LEVEL_SEPERATOR = "  ";

	/** Message Type */
	int msgType;

	/** SEComMsg °´Ã¼  */
	private SXTransaction secomMsg;

	/**
	 * Constructor of UComMsg class.
	 */
	public UComMsg(SXTransaction smsg) {
		secomMsg = smsg;
	}

	public SXTransaction getSEComMsg() {
		return secomMsg;
	}

	public int getStream() {
		return secomMsg.getStream();
	}

	public int getFunction() {
		return secomMsg.getFunction();
	}

	public long getSysbytes(){
		return getSystemBytes();
	}

	public long getSystemBytes() {
		return secomMsg.getSystemBytes();
	}

	// Value Get/set ÇÔ¼öµé
	public void setAsciiItem(String strItem) {
		secomMsg.writeNode(SECSFormat.A, strItem.length(), "", strItem, null);
	}

	public String getAsciiItem() {
		String strReturn = "";
		SECSID id = secomMsg.readNode();
		if (id != null) {
			strReturn = id.getValue();
		}			
		
		if (strReturn == null) {
			return "";
		} else {
			return strReturn; 
		}
	}

	public void setBinaryItem(int iItem) {
		secomMsg.writeNode(SECSFormat.B, 1, "", Integer.toString(iItem), null);
	}

	public int getBinaryItem() {
		int iReturn = 0;
		SECSID id = secomMsg.readNode();

		if (id != null) {
			iReturn = Integer.parseInt(id.getValue());
		}
		return iReturn;
	}

	public void setBoolItem(boolean bItem) {
		secomMsg.writeNode(SECSFormat.BOOLEAN, 1, "", Boolean.toString(bItem), null);
	}

	public boolean getBoolItem() {
		boolean bReturn = false;
		int nValue;
		SECSID id = secomMsg.readNode();

		if (id != null) {
			nValue = Integer.parseInt(id.getValue());
			bReturn = (nValue != 0) ? true : false;
		}
		return bReturn;
	}

	public void setListItem(int itemCnt) {
		secomMsg.writeNode(SECSFormat.L, itemCnt, null, null, null);
	}

	public int getListItem() {
		int nReturn = 0;
		SECSID id = secomMsg.readNode();

		if (id != null) {
			nReturn = id.getLength();
		}
		return nReturn;
	}

	public void setU1Item(int item) {
		secomMsg.writeNode(SECSFormat.U1, 1, "", Integer.toString(item), null);
	}

	public int getU1Item() {
		int nReturn = 0;
		SECSID id = secomMsg.readNode();
		if (id != null) {
			nReturn = Integer.parseInt(id.getValue());
		}
		return nReturn;
	}

	public void setU2Item(int nItem) {
		secomMsg.writeNode(SECSFormat.U2, 1, "", Integer.toString(nItem), null);
	}

	public int getU2Item() {
		int nReturn = 0;
		SECSID id = secomMsg.readNode();

		if (id != null) {
			nReturn = Integer.parseInt(id.getValue());
		}
		return nReturn;
	}

	public UComItem getCurrentItemAndMoveNext()	{
		UComItem item = null;
		SECSID secomitem = null;
		secomitem = secomMsg.readNode();

		if (secomitem != null) {
			item = new UComItem(secomitem);
		}
		return item;
	}

	public boolean getWbit(){
		boolean bReturn = false;
		bReturn = secomMsg.getWait();
		return bReturn;
	}

	public void setU4Item(long nItem) {
		secomMsg.writeNode(SECSFormat.U4, 1, "", Long.toString(nItem), null);
	}

	public long getU4Item() {
		long nReturn = 0;
		SECSID id = secomMsg.readNode();

		if (id != null) {
			nReturn = Long.parseLong(id.getValue());
		}
		return nReturn;
	}

	public boolean isEOF() {
		boolean bReturn = false;
		bReturn = ((secomMsg.hasNext()) == false);
		return bReturn;
	}

	public String toString() {
		return convertSEComMsgToString(secomMsg);
	}

//	private String convertUComMsgToString(XSecsMsg msg) {
//
//		StringBuffer sb = new StringBuffer();
//		String msgName = "S" + msg.GetStream() + "F" + msg.GetFunc();
//		sb.append(msgName + " Message Contents\n");
//		XSecsItem item = msg.GetFirstItem();
//		while (item != null) {
//			switch (item.GetType()) {
//			case XSecsItem.SECS2_ITEM_LIST:
//				traceUComSecsList(sb, msgName, msg, item, 0);
//				break;
//			default:
//				traceUComSecsItem(sb, msgName, item, 0);
//			}
//			item = msg.GetNextItem();
//		}
//		msg.MoveFirst();
//		return sb.toString();
//	}
//
//	private void traceUComSecsList(StringBuffer sb, String msgName,XSecsMsg msg, XSecsItem item, int nLevel) {
//
//		String span = "";
//		for (int i = 0; i < nLevel; i++)
//			span += LEVEL_SEPERATOR;
//
//		if (item.GetType() != XSecsItem.SECS2_ITEM_LIST)
//			return;
//
//		sb.append("[" + span + item.GetTypeName() + "," + item.GetListItem() + "]\n");
//		XSecsItem tempItem = null;
//		for (int i = 0; i < item.GetListItem(); i++) {
//			tempItem = msg.GetNextItem();
//
//			if (tempItem == null)
//				break;
//
//			switch (tempItem.GetType()) {
//			case XSecsItem.SECS2_ITEM_LIST:
//				traceUComSecsList(sb, msgName, msg, tempItem, nLevel + 1);
//				break;
//			default:
//				traceUComSecsItem(sb, msgName, tempItem, nLevel + 1);
//			}
//		}
//	}
//
//	private void traceUComSecsItem(StringBuffer sb, String msgName,
//			XSecsItem item, int nLevel) {
//		String span = "";
//
//		for (int i = 0; i < nLevel; i++)
//			span += LEVEL_SEPERATOR;
//
//		switch (item.GetType()) {
//		case XSecsItem.SECS2_ITEM_U4:
//			// 2011.01.07 by PMM
//			//sb.append(span + "[" + item.GetTypeName() + "]:" + item.GetU4Item()[0] + "\n");
//			long[] lArray = item.GetU4Item();
//			if(lArray != null) sb.append(span + "[" + item.GetTypeName() + "]:" + lArray[0] + "\n");
//			else sb.append(span + "[" + item.GetTypeName() + "]:\n");
//
//			break;
//		case XSecsItem.SECS2_ITEM_U2:
//			// 2011.01.07 by PMM
//			//sb.append(span + "[" + item.GetTypeName() + "]:" + item.GetU2Item()[0] + "\n");
//			int[] iArray = item.GetU2Item();
//			if(iArray != null) sb.append(span + "[" + item.GetTypeName() + "]:" + iArray[0] + "\n");
//			else sb.append(span + "[" + item.GetTypeName() + "]:\n");
//
//			break;
//		case XSecsItem.SECS2_ITEM_U1:
//			// 2011.01.07 by PMM
//			//sb.append(span + "[" + item.GetTypeName() + "]:" + item.GetU1Item()[0] + "\n");
//			byte[] bArray = item.GetU1Item();
//			if(bArray != null) sb.append(span + "[" + item.GetTypeName() + "]:" + bArray[0] + "\n");
//			else sb.append(span + "[" + item.GetTypeName() + "]:\n");
//
//			break;
//		case XSecsItem.SECS2_ITEM_ASCII:
//			sb.append(span + "[" + item.GetTypeName() + "]:" + item.GetAsciiItem() + "\n");
//			break;
//		default:
//			sb.append(span + item.GetTypeName());
//		}
//	}

	/**
	 * Convert SEComMsg to String
	 */
	String convertSEComMsgToString(SXTransaction trx) {
		StringBuffer sb = new StringBuffer();
		String msgName = "S" + trx.getStream() + "F" + trx.getFunction();
		sb.append(msgName + " Message Contents\n");

		List list = trx.getSECSIDs();

		for (Iterator iter = list.iterator(); iter.hasNext();) {
			SECSID secsid = (SECSID) iter.next();

			for (int i = 0; i < secsid.getLevel(); i++) {
				sb.append(LEVEL_SEPERATOR);
			}

			// 2011.11.11. by PMM
//			if (secsid.getSECSFormat() == SECSFormat.L) {
//				sb.append("[" + secsid.getSECSFormat() + "," + secsid.getLength() + "]\n");
//			}  else {
//				sb.append("[" + secsid.getSECSFormat() + "]:" + secsid.getValue() + "\n");
//			}
			if (secsid.getSECSFormat() == SECSFormat.L) {
				sb.append("[").append(secsid.getSECSFormat()).append(",").append(secsid.getLength()).append("]\n");
			}  else {
				sb.append("[").append(secsid.getSECSFormat()).append("]:").append(secsid.getValue()).append("\n");
			}
		}
		return sb.toString();
	}
}
