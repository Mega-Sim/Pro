package com.sec.iesw.mcs.infra.common.ucom.test;

import com.sec.iesw.mcs.infra.common.ucom.IUComEventListener;
import com.sec.iesw.mcs.infra.common.ucom.UCom;
import com.sec.iesw.mcs.infra.common.ucom.UComMsg;

public class UComTestEquip implements IUComEventListener {

	UCom m_UComEquip;

	UComTestEquip() {
		m_UComEquip = new UCom(UCom.COM_TYPE_SECOM, this);
	}

	void startService() {
		m_UComEquip.setCommCfgFile("TestEquip.xml");
		m_UComEquip.startService();
	}

	public void OnSECSReceived(UComMsg umsg) {
		String strMssageName;
		strMssageName = "S" + umsg.GetStream() + "F" + umsg.GetFunc();

		WriteLog("[HOST->EQUIP]" + strMssageName);

		//WriteLog( umsg.toString() );

		if (strMssageName.equals("S1F1")) {

			UComMsg msg = m_UComEquip.MakeReplyMsg(1, 2, umsg.GetSystemBytes());
			msg.SetListItem(2);
			msg.SetAsciiItem("ABC");
			msg.SetAsciiItem("DEF");

			//UComMsg msg = m_UComEquip.MakeReplyMsg( 1,1, umsg.GetSystemBytes() );

			m_UComEquip.Send(msg, false);
		}
	}

	public void OnSECSConnected() {
		WriteLog("OnSECSConnected called");
	}

	public void OnSECSDisConnected() {
		WriteLog("OnSECSDisConnected called");
	}

	public void OnSECST3TimeOut() {
		WriteLog("OnSECST3TimeOut called");
	}

	private void WriteLog(String msg) {
		System.out.println("EQUIP: " + msg);
	}
}
