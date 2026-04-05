package ocsmanager;

import java.awt.*;
import javax.swing.*;
import com.borland.jbcl.layout.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

public class MsgEmulatorDlg extends JDialog {
	JPanel panel1 = new JPanel();
	OCSManagerMainFrame m_OwnerFrame = null;
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	BorderLayout borderLayout2 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	DefaultTableModel TableModel = new DefaultTableModel();
	JTable jTable_MyHashtable = new JTable(TableModel);
	JPanel jPanel2 = new JPanel();
	JPanel jPanel3 = new JPanel();
	BorderLayout borderLayout4 = new BorderLayout();
	BorderLayout borderLayout3 = new BorderLayout();
	JPanel jPanel4 = new JPanel();
	JTextArea jTextArea_Msg = new JTextArea();
	BorderLayout borderLayout5 = new BorderLayout();
	JPanel jPanel5 = new JPanel();
	JButton jButton_Parsing = new JButton();
	BorderLayout borderLayout6 = new BorderLayout();
	BorderLayout borderLayout7 = new BorderLayout();
	JPanel jPanel6 = new JPanel();
	BorderLayout borderLayout8 = new BorderLayout();
	JButton jButton_CallProc = new JButton();
	JCheckBox jCheckBox_SEMIFDlg = new JCheckBox();

	public MsgEmulatorDlg(Frame frame, String title, boolean modal) {
		super(frame, title, modal);
		m_OwnerFrame = (OCSManagerMainFrame) frame;
		try {
			jbInit();
			pack();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public MsgEmulatorDlg(Frame frame) {
		this(frame, "", false);
	}

	private void jbInit() throws Exception {
		panel1.setLayout(borderLayout1);
		panel1.setBorder(BorderFactory.createEtchedBorder());
		this.getContentPane().setLayout(borderLayout2);
		jPanel1.setBorder(BorderFactory.createEtchedBorder());
		jPanel1.setDebugGraphicsOptions(0);
		jPanel1.setMinimumSize(new Dimension(10, 50));
		jPanel1.setPreferredSize(new Dimension(10, 25));
		jPanel1.setLayout(borderLayout7);
		this.setTitle("Message Emulator");
		jPanel2.setBorder(BorderFactory.createEtchedBorder());
		jPanel2.setPreferredSize(new Dimension(10, 100));
		jPanel2.setLayout(borderLayout3);
		jPanel3.setPreferredSize(new Dimension(10, 70));
		jPanel3.setLayout(borderLayout4);
		jPanel4.setBorder(BorderFactory.createEtchedBorder());
		jPanel4.setMinimumSize(new Dimension(0, 0));
		jPanel4.setPreferredSize(new Dimension(10, 25));
		jPanel4.setLayout(borderLayout5);
		jPanel5.setMinimumSize(new Dimension(100, 10));
		jPanel5.setPreferredSize(new Dimension(100, 10));
		jPanel5.setLayout(borderLayout6);
		jButton_Parsing.setToolTipText("");
		jButton_Parsing.setText("Parse Msg");
		jButton_Parsing.addActionListener(new MsgEmulatorDlg_jButton_Parsing_actionAdapter(this));
		jPanel6.setPreferredSize(new Dimension(100, 10));
		jPanel6.setLayout(borderLayout8);
		jButton_CallProc.setText("Call Proc");
		jButton_CallProc.addActionListener(new MsgEmulatorDlg_jButton_CallProc_actionAdapter(this));
		jTextArea_Msg.setLineWrap(true);
		jCheckBox_SEMIFDlg.setText("SEMIFDlg.CallProc »£√‚");
		getContentPane().add(panel1, BorderLayout.CENTER);
		panel1.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(jTable_MyHashtable, null);
		this.getContentPane().add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(jPanel6, BorderLayout.EAST);
		jPanel6.add(jButton_CallProc, BorderLayout.CENTER);
		jPanel1.add(jCheckBox_SEMIFDlg, BorderLayout.WEST);
		this.getContentPane().add(jPanel2, BorderLayout.NORTH);
		jPanel2.add(jPanel3, BorderLayout.NORTH);

		InitTable();
		jPanel2.add(jPanel4, BorderLayout.SOUTH);
		jPanel4.add(jPanel5, BorderLayout.EAST);
		jPanel5.add(jButton_Parsing, BorderLayout.CENTER);
		jPanel3.add(jTextArea_Msg, BorderLayout.CENTER);
	}

	void InitTable() {
		TableModel.addColumn("Name");
		TableModel.addColumn("Type");
		TableModel.addColumn("Value");

		int i;
		for (i = 0; i < 20; i++) {
			TableModel.addRow(new Object[] { "", "", "" });
		}
	}

	void jButton_CallProc_actionPerformed(ActionEvent e) {
		int nRowCount = TableModel.getRowCount();
		int i;
		if (nRowCount > 0) {
			MyHashtable msg = new MyHashtable();
			for (i = 0; i < nRowCount; i++) {
				String strName = (String) TableModel.getValueAt(i, 0);
				if (strName.equals("") == true)
					break;
				String strType = (String) TableModel.getValueAt(i, 1);
				if (strType.equals("String")) {
					msg.put(strName, (String) TableModel.getValueAt(i, 2));
				} else if (strType.equals("Integer")) {
					msg.put(strName, new Integer((String) TableModel.getValueAt(i, 2)));
				}
			}

			if (jCheckBox_SEMIFDlg.isSelected() == false)
				m_OwnerFrame.m_ocsMain.CallProc(msg);
			else
				m_OwnerFrame.m_ocsMain.semIF.CallProc(msg);
		}
	}

	void jButton_Parsing_actionPerformed(ActionEvent e) {
		for (int i = 0; i < TableModel.getRowCount(); i++) {
			TableModel.removeRow(i);
		}
		TableModel.setRowCount(0);

		String strMsg = jTextArea_Msg.getText();
		String strVal = "";
		int nPos1 = strMsg.indexOf("[");
		int nPos2 = strMsg.indexOf("]");
		if ((nPos1 > -1) && (nPos2 > -1)) {
			strVal = strMsg.substring(nPos1 + 1, nPos2);
			TableModel.addRow(new Object[] { "MessageName", "String", strVal });

			strMsg = strMsg.substring(nPos2 + 1);
			nPos1 = strMsg.indexOf(":");
			while (nPos1 > -1) {
				nPos2 = strMsg.indexOf(",");
				strVal = strMsg.substring(1, nPos1);
				TableModel.addRow(new Object[] { strVal, "", "" });
				int nRowIndex = TableModel.getRowCount() - 1;

				strVal = strMsg.substring(nPos1 + 1, nPos2);
				try {
					int nTemp = Integer.parseInt(strVal);
					if (strVal.length() < 14) {
						TableModel.setValueAt("Integer", nRowIndex, 1);
					} else {
						TableModel.setValueAt("String", nRowIndex, 1);
					}
					TableModel.setValueAt(strVal, nRowIndex, 2);
				} catch (Exception e1) {
					TableModel.setValueAt("String", nRowIndex, 1);
					TableModel.setValueAt(strVal, nRowIndex, 2);
				}

				nPos2 = strMsg.indexOf(",");
				strMsg = strMsg.substring(nPos2 + 1);
				nPos1 = strMsg.indexOf(":");
			}
		}
	}
}

class MsgEmulatorDlg_jButton_Parsing_actionAdapter implements java.awt.event.ActionListener {
	MsgEmulatorDlg adaptee;

	MsgEmulatorDlg_jButton_Parsing_actionAdapter(MsgEmulatorDlg adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButton_Parsing_actionPerformed(e);
	}
}

class MsgEmulatorDlg_jButton_CallProc_actionAdapter implements java.awt.event.ActionListener {
	MsgEmulatorDlg adaptee;

	MsgEmulatorDlg_jButton_CallProc_actionAdapter(MsgEmulatorDlg adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButton_CallProc_actionPerformed(e);
	}
}
