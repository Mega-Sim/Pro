package com.sec.iesw.mcs.infra.common.ucom;

import java.util.Iterator;

import SEComEnabler.SEComStructure.SECSID;
import SEComEnabler.SEComStructure.SXTransaction;
import SEComEnabler.SEComStructure.SX.SECSFormat;
import XCom.XSecsItem;
import XCom.XSecsMsg;

/**
 * 모듈별 SECS Message를 처리하기 위한 SECS Message함수
 * <p>
 * XCom, SECom 등 각 모듈별로 있는 SECS메세지 클래스를 통합한 클래스이다.
 * 
 * @author Hyung Doo, Yoon
 * @version 1.0
 * @since 1.4
 * @see UCom, UComMsg
 * 
 */

public class UComMsg {
	public final static int MESSAGE_TYPE_XCOM = 1;

	public final static int MESSAGE_TYPE_SECOM = 2;

	final protected String LEVEL_SEPERATOR = "  ";

	int m_nMsgType;

	/** Message Type */
	private XSecsMsg m_XComMsg;

	/** XComMsg 객체 */
	private SXTransaction m_SEComMsg;

	/** SEComMsg 객체 */

	public UComMsg(XSecsMsg xmsg) {
		m_XComMsg = null;
		m_SEComMsg = null;

		m_nMsgType = MESSAGE_TYPE_XCOM;
		m_XComMsg = xmsg;
	}

	public UComMsg(SXTransaction smsg) {
		m_XComMsg = null;
		m_SEComMsg = null;

		m_nMsgType = MESSAGE_TYPE_SECOM;
		m_SEComMsg = smsg;
	}

	public XSecsMsg getXComMsg() {
		return m_XComMsg;
	}

	public SXTransaction getSEComMsg() {
		return m_SEComMsg;
	}

	public int GetStream() {
		int nReturn = 0;

		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			nReturn = m_XComMsg.GetStream();
			break;
		case MESSAGE_TYPE_SECOM:
			nReturn = m_SEComMsg.getStream();
			break;
		}
		return nReturn;
	}

	public int GetFunc() {
		int nReturn = 0;

		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			nReturn = m_XComMsg.GetFunc();
			break;
		case MESSAGE_TYPE_SECOM:
			nReturn = m_SEComMsg.getFunction();
			break;
		}
		return nReturn;

	}

	public long GetSysbytes() {
		return GetSystemBytes();
	}

	public long GetSystemBytes() {
		long nReturn = 0;

		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			nReturn = m_XComMsg.GetSysbytes();
			break;
		case MESSAGE_TYPE_SECOM:
			nReturn = m_SEComMsg.getSystemBytes();
			break;
		}
		return nReturn;

	}

	// Value Get/Set 함수들

	public void SetAsciiItem(String strItem) {
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			m_XComMsg.SetAsciiItem(strItem);
			break;
		case MESSAGE_TYPE_SECOM:
			m_SEComMsg.writeNode(SECSFormat.A, strItem.length(), "", strItem, null);
			break;
		}
	}

	public String GetAsciiItem() {
		String strReturn = "";

		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			strReturn = m_XComMsg.GetAsciiItem();
			break;
		case MESSAGE_TYPE_SECOM:
			strReturn = m_SEComMsg.readNode().getValue();
			break;
		}

		return strReturn;
	}

	public void SetBinaryItem(int iItem) {
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			m_XComMsg.SetBinaryItem(iItem);
			break;
		case MESSAGE_TYPE_SECOM:
			// TODO : 아래 항목을 확인하여야 함
			// m_SEComMsg.writeNode(SECSFormat.B, 1, Integer.toString(nItem), null, null);
			m_SEComMsg.writeNode(SECSFormat.B, 1, "", Integer.toString(iItem), null);
			break;
		}
	}

	public int GetBinaryItem() {
		int iReturn = 0;
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			iReturn = m_XComMsg.GetBinaryItem()[0];
			break;
		case MESSAGE_TYPE_SECOM:
			SECSID id = m_SEComMsg.readNode();
			iReturn = Integer.parseInt(id.getValue());
			break;
		}

		return iReturn;
	}

	public void SetBoolItem(boolean bItem) {
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			m_XComMsg.SetBoolItem(bItem);
			break;
		case MESSAGE_TYPE_SECOM:
			m_SEComMsg.writeNode(SECSFormat.BOOLEAN, 1, "", Boolean.toString(bItem), null);
			break;
		}
	}

	public boolean GetBoolItem() {
		boolean bReturn = false;
		int nValue;
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			bReturn = m_XComMsg.GetBoolItem()[0];
			break;
		case MESSAGE_TYPE_SECOM:
			nValue = Integer.parseInt(m_SEComMsg.readNode().getValue());
			bReturn = (nValue != 0) ? true : false;
			break;
		}

		return bReturn;
	}

	public void SetListItem(int nItemCnt) {
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			m_XComMsg.SetListItem(nItemCnt);
			break;
		case MESSAGE_TYPE_SECOM:
			m_SEComMsg.writeNode(SECSFormat.L, nItemCnt, null, null, null);
			break;
		}
	}

	public int GetListItem() {
		int nReturn = 0;
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			nReturn = m_XComMsg.GetListItem();
			break;
		case MESSAGE_TYPE_SECOM:
			nReturn = m_SEComMsg.readNode().getLength();
			break;
		}

		return nReturn;
	}

	public void SetU1Item(int nItem) {
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			m_XComMsg.SetU1Item(nItem);
			break;
		case MESSAGE_TYPE_SECOM:
			m_SEComMsg.writeNode(SECSFormat.U1, 1, "", Integer.toString(nItem), null);
			break;
		}
	}

	public int GetU1Item() {
		int nReturn = 0;
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			nReturn = m_XComMsg.GetU1Item()[0];
			break;
		case MESSAGE_TYPE_SECOM:
			nReturn = Integer.parseInt(m_SEComMsg.readNode().getValue());
			break;
		}

		return nReturn;
	}

	public void SetU2Item(int nItem) {
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			m_XComMsg.SetU2Item(nItem);
			break;
		case MESSAGE_TYPE_SECOM:
			m_SEComMsg.writeNode(SECSFormat.U2, 1, "", Integer.toString(nItem), null);
			break;
		}
	}

	public int GetU2Item() {
		int nReturn = 0;
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			nReturn = m_XComMsg.GetU2Item()[0];
			break;
		case MESSAGE_TYPE_SECOM:
			nReturn = Integer.parseInt(m_SEComMsg.readNode().getValue());
			break;
		}

		return nReturn;
	}

	public UComItem GetCurrentItemAndMoveNext() {
		UComItem item = null;
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			XSecsItem xcomitem = null;

			xcomitem = m_XComMsg.GetCurrentItem();
			m_XComMsg.MoveNext();
			if (xcomitem != null)
				item = new UComItem(xcomitem);
			break;
		case MESSAGE_TYPE_SECOM:
			SECSID secomitem = null;

			secomitem = m_SEComMsg.readNode();

			if (secomitem != null)
				item = new UComItem(secomitem);
			break;
		}

		return item;
	}

	public boolean GetWbit() {
		boolean bReturn = false;
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			bReturn = m_XComMsg.GetWbit();
			break;
		case MESSAGE_TYPE_SECOM:
			bReturn = m_SEComMsg.getWait();
			break;
		}

		return bReturn;

	}

	public void SetU4Item(long nItem) {
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			m_XComMsg.SetU4Item(nItem);
			break;
		case MESSAGE_TYPE_SECOM:
			m_SEComMsg.writeNode(SECSFormat.U4, 1, "", Long.toString(nItem), null);
			break;
		}
	}

	public long GetU4Item() {
		long nReturn = 0;
		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			nReturn = m_XComMsg.GetU4Item()[0];
			break;
		case MESSAGE_TYPE_SECOM:
			nReturn = Long.parseLong(m_SEComMsg.readNode().getValue());
			break;
		}

		return nReturn;
	}

	public boolean IsEOF() {
		boolean bReturn = false;

		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			bReturn = m_XComMsg.IsEOF();
			break;
		case MESSAGE_TYPE_SECOM:
			bReturn = !(m_SEComMsg.hasNext());
			break;
		}

		return bReturn;
	}

	public String toString() {
		String strRet = "";

		switch (m_nMsgType) {
		case MESSAGE_TYPE_XCOM:
			strRet = XComMssageToString(m_XComMsg);
			break;
		case MESSAGE_TYPE_SECOM:
			strRet = SEComMessageToString(m_SEComMsg);
			break;
		}

		return strRet;
	}

	String XComMssageToString(XSecsMsg msg) {
		StringBuffer sb = new StringBuffer();

		String strMsgName = "S" + msg.GetStream() + "F" + msg.GetFunc();

		sb.append(strMsgName + " Message Contents\n");

		XSecsItem item = msg.GetFirstItem();
		while (item != null) {
			switch (item.GetType()) {
			case XSecsItem.SECS2_ITEM_LIST:
				traceXComSecsList(sb, strMsgName, msg, item, 0);
				break;
			default:
				traceXComSecsItem(sb, strMsgName, item, 0);
			}

			item = msg.GetNextItem();
		}
		msg.MoveFirst();

		return sb.toString();
	}

	private void traceXComSecsList(StringBuffer sb, String strMsgName, XSecsMsg msg, XSecsItem item, int nLevel) {
		String span = "";

		for (int i = 0; i < nLevel; i++)
			span += LEVEL_SEPERATOR;

		if (item.GetType() != XSecsItem.SECS2_ITEM_LIST)
			return;

		sb.append("[" + span + item.GetTypeName() + "," + item.GetListItem() + "]\n");

		XSecsItem tempItem = null;
		for (int i = 0; i < item.GetListItem(); i++) {
			tempItem = msg.GetNextItem();

			if (tempItem == null)
				break;

			switch (tempItem.GetType()) {
			case XSecsItem.SECS2_ITEM_LIST:
				traceXComSecsList(sb, strMsgName, msg, tempItem, nLevel + 1);
				break;
			default:
				traceXComSecsItem(sb, strMsgName, tempItem, nLevel + 1);
			}
		}
	}

	private void traceXComSecsItem(StringBuffer sb, String strMsgName, XSecsItem item, int nLevel) {
		String span = "";

		for (int i = 0; i < nLevel; i++)
			span += LEVEL_SEPERATOR;

		switch (item.GetType()) {
		case XSecsItem.SECS2_ITEM_U4:
			sb.append(span + "[" + item.GetTypeName() + "]:" + item.GetU4Item()[0] + "\n");
			break;
		case XSecsItem.SECS2_ITEM_U2:
			sb.append(span + "[" + item.GetTypeName() + "]:" + item.GetU2Item()[0] + "\n");
			break;
		case XSecsItem.SECS2_ITEM_U1:
			sb.append(span + "[" + item.GetTypeName() + "]:" + item.GetU1Item()[0] + "\n");
			break;
		case XSecsItem.SECS2_ITEM_ASCII:
			sb.append(span + "[" + item.GetTypeName() + "]:" + item.GetAsciiItem() + "\n");
			break;
		default:
			sb.append(span + item.GetTypeName());
		}
	}

	String SEComMessageToString(SXTransaction trx) {
		StringBuffer sb = new StringBuffer();

		String strMsgName = "S" + trx.getStream() + "F" + trx.getFunction();

		sb.append(strMsgName + " Message Contents\n");

		java.util.List list = trx.getSECSIDs();

		for (Iterator iter = list.iterator(); iter.hasNext();) {
			SECSID secsid = (SECSID) iter.next();

			for (int i = 0; i < secsid.getLevel(); i++)
				sb.append(LEVEL_SEPERATOR);

			if (secsid.getSECSFormat() == SECSFormat.L)
				sb.append("[" + secsid.getSECSFormat() + "," + secsid.getLength() + "]\n");
			else
				sb.append("[" + secsid.getSECSFormat() + "]:" + secsid.getValue() + "\n");
		}

		return sb.toString();
	}

	/**/

}