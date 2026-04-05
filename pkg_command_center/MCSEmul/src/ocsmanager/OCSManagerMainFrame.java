package ocsmanager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.sql.*;
import java.util.*;

import com.borland.jbcl.layout.*;
import com.sun.org.apache.bcel.internal.generic.NEW;

import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

import ocsmanager.model.TransferCommand;
import ocsmanager.model.TransferEx4Command;

import java.beans.*;

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

public class OCSManagerMainFrame extends JFrame {
	JPanel contentPane;
	JMenuBar jMenuBar1 = new JMenuBar();
	JMenu jMenuFile = new JMenu();
	JMenuItem jMenuFileExit = new JMenuItem();
	JLabel statusBar = new JLabel();
	XYLayout xYLayout1 = new XYLayout();
	JPanel jBasicInfoPanel = new JPanel();
	XYLayout xYLayout2 = new XYLayout();
	JLabel jLabel1 = new JLabel();
	JTextField jTextField_TSCID = new JTextField();
	JTabbedPane jTabbedPane1 = new JTabbedPane();
	JPanel jInitializePanel = new JPanel();
	JPanel jTrCmdPanel = new JPanel();
	TitledBorder titledBorder1;
	TitledBorder titledBorder2;
	DefaultTableModel TSCModel = new DefaultTableModel();
	DefaultTableModel TrCmdModel = new DefaultTableModel();
	JTable jTrCmdTable = new JTable(TrCmdModel);
	DefaultTableModel CarrierModel = new DefaultTableModel();
	DefaultTableModel AlarmModel = new DefaultTableModel();
	JPanel jPanel1 = new JPanel();
	JTextField jTextField_DBUrl = new JTextField();
	JTextField jTextField_DBConnectionStatus = new JTextField();
	XYLayout xYLayout3 = new XYLayout();
	JMenu jMenu1 = new JMenu();
	JMenu jMenuHelp = new JMenu();
	JMenuItem jMenuHelpAbout = new JMenuItem();
	JMenu jMenuTSCControl = new JMenu();
	JMenuItem jMenuTSCAuto = new JMenuItem();
	JMenuItem jMenuTSCPaused = new JMenuItem();
	JMenu jMenu2 = new JMenu();
	JMenuItem jMenuSyncCarrierInfo = new JMenuItem();
	JMenuItem jMenuSyncTrCmdInfo = new JMenuItem();
	JMenu jMenuDebug = new JMenu();
	JMenuItem jMenuMsgEmulator = new JMenuItem();
	JButton jButton1 = new JButton();
	JButton jButton2 = new JButton();

	OCSManagerMain m_ocsMain = null;

	UDPComm m_UDPComm = null; // 2010.02.03. by IKY

	// Display Table Column 생성 여부
	boolean m_bTrCmdColumnMade = false;
	boolean m_bCarrierColumnMade = false;
	boolean m_bTSCColumnMade = false;
	boolean m_bAlarmColumnMade = false;

	// Timer
	java.util.Timer MainTimer = new java.util.Timer();
	MainTimerProc MainTimerTask = new MainTimerProc(this);
	XYLayout xYLayout4 = new XYLayout();
	JButton btSendMsg = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();
	JTextArea logArea = new JTextArea();
	XYLayout xYLayout5 = new XYLayout();
	JLabel jLabel4 = new JLabel();
	JTextField editCarrierID = new JTextField();
	JLabel jLabel2 = new JLabel();
	JTextField editSource = new JTextField();
	JLabel jLabel3 = new JLabel();
	JTextField editDest = new JTextField();
	JButton btSendCmd = new JButton();
	JComboBox cbMsgList = new JComboBox();
	JButton btAbort = new JButton();
	JButton btCancel = new JButton();
	JTextField edCmdID = new JTextField();
	JLabel jLabel5 = new JLabel();
	JLabel jLabel6 = new JLabel();
	JTextField edPriority = new JTextField();
	JPanel jStagePanel = new JPanel();
	XYLayout xYLayout8 = new XYLayout();
	JLabel jLabel7 = new JLabel();
	JTextField editStageCarrierID = new JTextField();
	JLabel jLabel8 = new JLabel();
	JTextField editStageCmdID = new JTextField();
	JTextField editStageSource = new JTextField();
	JLabel jLabel9 = new JLabel();
	JTextField editStageDest = new JTextField();
	JLabel jLabel10 = new JLabel();
	JTextField editStageED = new JTextField();
	JLabel jLabel11 = new JLabel();
	JTextField editStageNBT = new JTextField();
	JLabel jLabel12 = new JLabel();
	JLabel jLabel13 = new JLabel();
	JTextField editStageWTO = new JTextField();
	JTextField editStagePriority = new JTextField();
	JLabel jLabel14 = new JLabel();
	JButton jSendStageCmd = new JButton();
	JButton jSendStageDeleteCmd = new JButton();
	JPanel jLongrunPanel = new JPanel();
	XYLayout xYLayout6 = new XYLayout();

	// 2009.08.28 by MYM : Transfer History 로 롱런을 돌릴 수 있도록 추가
	LongRun m_Longrun = null;
	JPanel jPanel2 = new JPanel();
	XYLayout xYLayout7 = new XYLayout();
	JTextField jLongrunStageInterval = new JTextField();
	JTextField jLongrunStageEDT = new JTextField();
	JLabel jLabel18 = new JLabel();
	JLabel jLabel17 = new JLabel();
	JLabel jLabel15 = new JLabel();
	JButton jLongrunSetOpt = new JButton();
	JPanel jPanel3 = new JPanel();
	XYLayout xYLayout9 = new XYLayout();
	JButton jLongrunStart = new JButton();
	JButton jLoadTransferHistory = new JButton();
	JButton jLongrunPause = new JButton();
	JCheckBox jLongrunUseStage = new JCheckBox();
	JTextField jLongrunAccel = new JTextField();
	JTextArea jLongrunLog = new JTextArea();
	JScrollPane jScrollPane2 = new JScrollPane();
	JPanel jPanel4 = new JPanel();
	JLabel jLongrunTime = new JLabel();
	JCheckBox jTrForStage = new JCheckBox();
	JCheckBox jAutoDestChange = new JCheckBox();
	XYLayout xYLayout11 = new XYLayout();
	JPanel jScanPanel1 = new JPanel();
	JLabel jLabel19 = new JLabel();
	JTextField editScanCarrierLoc = new JTextField();
	JLabel jLabel20 = new JLabel();
	JTextField editScanCarrierID = new JTextField();
	JLabel jLabel21 = new JLabel();
	JTextField editScanCommandID = new JTextField();
	JLabel jLabel22 = new JLabel();
	JTextField editScanPriority = new JTextField();
	JButton btSendScan = new JButton();
	JPanel jLocalOHTPanel = new JPanel();
	XYLayout xYLayout10 = new XYLayout();
	JPanel jPanel5 = new JPanel();
	JPanel jPanel6 = new JPanel();
	XYLayout xYLayout12 = new XYLayout();
	XYLayout xYLayout13 = new XYLayout();
	JScrollPane jScrollPane3 = new JScrollPane();
	JTextArea jSndTextArea = new JTextArea();
	JComboBox jCBLocalOHTMsg = new JComboBox();
	JButton jBtnSendMsg = new JButton();
	JButton jBtnSendMsgFromFile = new JButton();
	JPanel jUserLongrunPanel = new JPanel();
	XYLayout xYLayout14 = new XYLayout();
	JPanel jPanel7 = new JPanel();
	JButton jBtnLoadJobFile = new JButton();
	XYLayout xYLayout15 = new XYLayout();
	JButton jBtnResume = new JButton();
	JButton jBtnPause = new JButton();
	JScrollPane jScrollPane4 = new JScrollPane();
	JTextArea jUserLongrunText = new JTextArea();
	JCheckBox jLongrunUseAbort = new JCheckBox();
	JCheckBox jLongrunUseCancel = new JCheckBox();
	JTextField jLongrunAbortInterval = new JTextField();
	JTextField jLongrunCancelInterval = new JTextField();

	// 2011.07.25 by KYK : STBC Tab추가
	JPanel jSTBC = new JPanel();
	JButton btSendINSTALL = new JButton();
	JButton btSendREMOVE = new JButton();
	JButton btSendIDREAD = new JButton();
	// add
	JButton btSendIDREADALL = new JButton();

	JLabel jlCarrierLocId = new JLabel();
	JLabel jlCarrierId = new JLabel();
	JTextField editCarrierLocId = new JTextField();
	JTextField editCarrierId = new JTextField();

	/**
	 * 2021.03.30 dahye TRANSFER_EX4
	 */
	JPanel jTransferEx4Panel = new JPanel();
	XYLayout xYLayout16 = new XYLayout();
	// CommandInfo
	JTextField editEx4CmdId = new JTextField();
	JLabel jLblEx4CmdId = new JLabel();
	JTextField editEx4Priority = new JTextField();
	JLabel jLblEx4Priority = new JLabel();
	JTextField editEx4Replace = new JTextField();
	JLabel jLblEx4Replace = new JLabel();
	// TransferInfo
	JTextField editEx4Carrier = new JTextField();
	JLabel jLblEx4Carrier = new JLabel();
	JTextField editEx4Source = new JTextField();
	JLabel jLblEx4Source = new JLabel();
	JTextField editEx4Dest = new JTextField();
	JLabel jLblEx4Dest = new JLabel();
	// DeliveryInfo
	JTextField editEx4DeliveryType = new JTextField();
	JLabel jLblEx4DeliveryType = new JLabel();
	JTextField editEx4ExpectedDeliveryTime = new JTextField();
	JLabel jLblEx4ExpectedDeliveryTime = new JLabel();
	JTextField editEx4DeliveryWaitTimeout = new JTextField();
	JLabel jLblEx4DeliveryWaitTimeout = new JLabel();
	// Button
	JButton jBtnEx4Transfer = new JButton();

	//Construct the frame
	public OCSManagerMainFrame() {
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Component initialization
	private void jbInit() throws Exception {
		contentPane = (JPanel) this.getContentPane();
		titledBorder1 = new TitledBorder("");
		titledBorder2 = new TitledBorder("");
		contentPane.setLayout(xYLayout1);
		this.setSize(new Dimension(560, 455));
		this.setResizable(false);
		//    this.setTitle("OCS Manager [2015.01.30]"); 밑에서 처리
		statusBar.setText(" ");
		jMenuFile.setText("System");
		jMenuFileExit.setText("Exit");
		jMenuFileExit.addActionListener(new OCSManagerMainFrame_jMenuFileExit_ActionAdapter(this));
		jBasicInfoPanel.setBackground(SystemColor.activeCaptionBorder);
		jBasicInfoPanel.setAlignmentY((float) 0.5);
		jBasicInfoPanel.setBorder(BorderFactory.createEtchedBorder());
		jBasicInfoPanel.setLayout(xYLayout2);
		jLabel1.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 14));
		jLabel1.setForeground(Color.black);
		jLabel1.setText("TYPE : ");
		jTextField_TSCID.setBackground(Color.white);
		jTextField_TSCID.setFont(new java.awt.Font("Dialog", 1, 14));
		jTextField_TSCID.setForeground(Color.blue);
		jTextField_TSCID.setBorder(BorderFactory.createEtchedBorder());
		jTextField_TSCID.setEditable(false);
		jTextField_TSCID.setText("");
		jTabbedPane1.setFont(new java.awt.Font("MS Sans Serif", 0, 11));
		jTabbedPane1.setForeground(Color.black);
		jTabbedPane1.setAlignmentY((float) 0.5);
		jTabbedPane1.setBorder(BorderFactory.createEtchedBorder());
		jTabbedPane1.setDebugGraphicsOptions(0);
		jTabbedPane1.setMaximumSize(new Dimension(32767, 32767));
		jTabbedPane1.setMinimumSize(new Dimension(344, 364));
		jTabbedPane1.setPreferredSize(new Dimension(344, 364));
		jTabbedPane1.setVerifyInputWhenFocusTarget(true);
		jInitializePanel.setLayout(xYLayout4);
		jTrCmdPanel.setLayout(xYLayout5);
		jPanel1.setBorder(BorderFactory.createEtchedBorder());
		jPanel1.setLayout(xYLayout3);
		jTextField_DBUrl.setBackground(SystemColor.activeCaptionBorder);
		jTextField_DBUrl.setBorder(BorderFactory.createEtchedBorder());
		//    jTextField_DBUrl.setText("DB URL : ");
		//    jTextField_DBConnectionStatus.setBackground(new Color(255, 125, 125));
		jTextField_DBConnectionStatus.setBackground(SystemColor.activeCaptionBorder);
		jTextField_DBConnectionStatus.setBorder(BorderFactory.createEtchedBorder());
		jTextField_DBConnectionStatus.setCaretColor(Color.black);
		//    jTextField_DBConnectionStatus.setText("NOT CONNECTED");
		jTextField_DBConnectionStatus.setHorizontalAlignment(SwingConstants.CENTER);
		jMenu1.setText("Operation");
		jMenuHelp.setText("Help");
		jMenuHelpAbout.setText("About");
		jMenuHelpAbout.addActionListener(new OCSManagerMainFrame_jMenuHelpAbout_actionAdapter(this));
		jMenuTSCControl.setText("TSC Control");
		jMenuTSCAuto.setText("AUTO");
		jMenuTSCAuto.addActionListener(new OCSManagerMainFrame_jMenuTSCAuto_actionAdapter(this));
		jMenuTSCPaused.setText("PAUSED");
		jMenuTSCPaused.addActionListener(new OCSManagerMainFrame_jMenuTSCPaused_actionAdapter(this));
		jMenu2.setText("Sync RealTime Data");
		jMenuSyncCarrierInfo.setText("Carrier Info");
		jMenuSyncTrCmdInfo.setText("Tr. Cmd Info");
		jMenuDebug.setText("Debug");
		jMenuBar1.setAlignmentX((float) 0.5);
		jMenuMsgEmulator.setText("Message Emulator...");
		jMenuMsgEmulator.addActionListener(new OCSManagerMainFrame_jMenuMsgEmulator_actionAdapter(this));
		jButton1.setEnabled(true);
		jButton1.setText("Cancel");
		jButton1.addActionListener(new OCSManagerMainFrame_jButton1_actionAdapter(this));
		jButton2.setText("Abort");
		jButton2.addActionListener(new OCSManagerMainFrame_jButton2_actionAdapter(this));
		btSendMsg.setText("Send Msg");
		btSendMsg.addActionListener(new OCSManagerMainFrame_btSendMsg_actionAdapter(this));
		logArea.setText("");
		jLabel4.setText("Dest");
		editCarrierID.setText("AAA");
		jLabel2.setText("Carrier");
		jLabel3.setText("Source");
		btSendCmd.setToolTipText("");
		btSendCmd.setActionCommand("Send Cmd");
		btSendCmd.setText("TRANSFER");
		btSendCmd.addActionListener(new OCSManagerMainFrame_btSendCmd_actionAdapter(this));
		btAbort.setText("ABORT");
		btAbort.addActionListener(new OCSManagerMainFrame_btAbort_actionAdapter(this));
		btCancel.setText("CANCEL");
		btCancel.addActionListener(new OCSManagerMainFrame_btCancel_actionAdapter(this));
		edCmdID.setText("Command001");
		jLabel5.setText("CommandID");
		jLabel6.setText("Priority");
		edPriority.setText("10");
		editSource.setText("");
		jStagePanel.setLayout(xYLayout8);
		jLabel7.setText("Dest");
		editStageCarrierID.setText("Carrier001");
		jLabel8.setText("Carrier");
		editStageCmdID.setText("StageCmd001");
		editStageSource.setText("");
		jLabel9.setText("Source");
		jLabel10.setText("StageID");
		editStageED.setText("60");
		jLabel11.setText("ExpectedDuration");
		editStageNBT.setText("20");
		jLabel12.setText("NoBlockingTime");
		jLabel13.setText("WaitTimeOut");
		editStageWTO.setText("20");
		editStagePriority.setText("10");
		jLabel14.setText("Priority");
		jSendStageCmd.setText("STAGE");
		jSendStageCmd.addActionListener(new OCSManagerMainFrame_jSendStageCmd_actionAdapter(this));
		jSendStageDeleteCmd.setText("STAGEDELETE");
		jSendStageDeleteCmd.addActionListener(new OCSManagerMainFrame_jSendStageDeleteCmd_actionAdapter(this));
		jLongrunPanel.setLayout(xYLayout6);
		jPanel2.setBorder(BorderFactory.createEtchedBorder());
		jPanel2.setDebugGraphicsOptions(0);
		jPanel2.setLayout(xYLayout7);
		jLongrunStageInterval.setText("1");
		jLongrunStageEDT.setText("80");
		jLabel18.setText("분");
		jLabel17.setText("배속");
		jLabel15.setText("명령 가속 비율");
		jLabel15.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		jLongrunSetOpt.setText("적 용");
		jLongrunSetOpt.addActionListener(new OCSManagerMainFrame_jLongrunSetOpt_actionAdapter(this));
		jPanel3.setBorder(BorderFactory.createEtchedBorder());
		jPanel3.setLayout(xYLayout9);
		jLongrunStart.setText("Start");
		jLongrunStart.addActionListener(new OCSManagerMainFrame_jLongrunStart_actionAdapter(this));
		jLoadTransferHistory.setText("Load TrHistory");
		jLoadTransferHistory.addActionListener(new OCSManagerMainFrame_jLoadTransferHistory_actionAdapter(this));
		jLongrunPause.setText("Pause");
		jLongrunPause.addActionListener(new OCSManagerMainFrame_jLongrunPause_actionAdapter(this));
		jLongrunUseStage.setBorder(titledBorder2);
		jLongrunUseStage.setToolTipText("");
		jLongrunUseStage.setText("Stage");
		jLongrunUseStage.addActionListener(new OCSManagerMainFrame_jLongrunUseStage_actionAdapter(this));
		jLongrunAccel.setText("1");
		jLongrunLog.setBorder(null);
		jLongrunLog.setText("");
		jPanel4.setBorder(BorderFactory.createEtchedBorder());
		jTrForStage.setText("for Stage");
		jAutoDestChange.setText("Auto DestChange");
		jScanPanel1.setLayout(xYLayout11);
		jLabel19.setText("CarrierLoc : ");
		editScanCarrierLoc.setText("");
		jLabel20.setText("CarrierID :");
		editScanCarrierID.setText("");
		jLabel21.setText("CommandID :");
		editScanCommandID.setText("");
		jLabel22.setText("Priority :");
		editScanPriority.setText("");
		btSendScan.setText("Scan");
		btSendScan.addActionListener(new OCSManagerMainFrame_btSendScan_actionAdapter(this));
		jLocalOHTPanel.setLayout(xYLayout10);
		jPanel5.setBorder(BorderFactory.createEtchedBorder());
		jPanel5.setLayout(xYLayout13);
		jPanel6.setBorder(BorderFactory.createEtchedBorder());
		jPanel6.setLayout(xYLayout12);
		jSndTextArea.setText("");
		jBtnSendMsg.setText("Send");
		jBtnSendMsg.addActionListener(new OCSManagerMainFrame_jBtnSendMsg_actionAdapter(this));
		jBtnSendMsgFromFile.setActionCommand("jBtnSendMsgFromFile");
		jBtnSendMsgFromFile.setText("Send Message From File");
		jBtnSendMsgFromFile.addActionListener(new OCSManagerMainFrame_jBtnSendMsgFromFile_actionAdapter(this));
		jUserLongrunPanel.setLayout(xYLayout14);
		jPanel7.setBorder(BorderFactory.createEtchedBorder());
		jPanel7.setLayout(xYLayout15);
		jBtnLoadJobFile.setText("Load JobFile");
		jBtnLoadJobFile.addActionListener(new OCSManagerMainFrame_jBtnLoadJobFile_actionAdapter(this));
		jBtnResume.setText("Resume");
		jBtnResume.addActionListener(new OCSManagerMainFrame_jBtnResume_actionAdapter(this));
		jBtnPause.setToolTipText("");
		jBtnPause.setText("Pause");
		jBtnPause.addActionListener(new OCSManagerMainFrame_jBtnPause_actionAdapter(this));
		jUserLongrunText.setText("");
		jLongrunUseAbort.addActionListener(new OCSManagerMainFrame_jLongrunUseAbort_actionAdapter(this));
		jLongrunUseAbort.setText("Abort");
		jLongrunUseAbort.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		jLongrunUseAbort.setToolTipText("");
		jLongrunUseAbort.setBorder(titledBorder2);
		jLongrunUseCancel.setBorder(titledBorder2);
		jLongrunUseCancel.setToolTipText("");
		jLongrunUseCancel.setSelected(false);
		jLongrunUseCancel.setText("Cancel");
		jLongrunUseCancel.addActionListener(new OCSManagerMainFrame_jLongrunUseCancel_actionAdapter(this));
		jLongrunUseCancel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		jLongrunAbortInterval.setText("1");
		jLongrunCancelInterval.setText("1");
		jPanel2.add(jLongrunAccel, new XYConstraints(96, 3, 30, -1));
		jPanel2.add(jLongrunUseAbort, new XYConstraints(9, 59, 76, 23));
		jPanel2.add(jLongrunUseCancel, new XYConstraints(9, 83, 78, 23));
		jPanel2.add(jLongrunCancelInterval, new XYConstraints(96, 83, 30, -1));
		jPanel2.add(jLongrunAbortInterval, new XYConstraints(96, 59, 30, -1));
		jPanel2.add(jLongrunStageInterval, new XYConstraints(96, 34, 30, -1));
		jPanel2.add(jLongrunStageEDT, new XYConstraints(130, 34, 30, -1));
		jPanel2.add(jLabel15, new XYConstraints(9, 3, 92, 20));
		jPanel2.add(jLongrunSetOpt, new XYConstraints(107, 106, 78, 23));
		jPanel2.add(jLabel18, new XYConstraints(141, 62, 25, 18));
		jPanel2.add(jLabel17, new XYConstraints(137, 3, 33, 19));
		jPanel2.add(jLongrunUseStage, new XYConstraints(9, 34, 76, 23));
		jLongrunPanel.add(jPanel3, new XYConstraints(203, 48, 130, 134));
		jLongrunPanel.add(jLongrunLog, new XYConstraints(12, 191, 321, 122));
		jLongrunPanel.add(jScrollPane2, new XYConstraints(11, 189, 323, 126));
		jLongrunPanel.add(jPanel2, new XYConstraints(10, 48, 190, 135));
		jMenuFile.add(jMenuFileExit);
		jMenuBar1.add(jMenuFile);
		jMenuBar1.add(jMenu1);
		jMenuBar1.add(jMenuHelp);
		jMenuBar1.add(jMenuDebug);
		this.setJMenuBar(jMenuBar1);
		contentPane.add(statusBar, new XYConstraints(0, 0, 576, -1));
		jBasicInfoPanel.add(jLabel1, new XYConstraints(9, 6, -1, -1));
		jBasicInfoPanel.add(jTextField_TSCID, new XYConstraints(63, 3, 108, -1));
		contentPane.add(jButton1, new XYConstraints(6, 480, -1, -1));
		contentPane.add(jButton2, new XYConstraints(77, 480, -1, -1));
		contentPane.add(jPanel1, new XYConstraints(7, 418, 735, 26));
		jPanel1.add(jTextField_DBUrl, new XYConstraints(0, 0, 600, 22));
		jPanel1.add(jTextField_DBConnectionStatus, new XYConstraints(601, 0, 129, 22));
		jTabbedPane1.add(jInitializePanel, "Initialize");
		jTabbedPane1.add(jTrCmdPanel, "Transfer");
		contentPane.add(jScrollPane1, new XYConstraints(15, 52, 358, 351));
		jScrollPane1.getViewport().add(logArea, null);
		contentPane.add(jBasicInfoPanel, new XYConstraints(7, 11, 735, 30));
		jMenu1.add(jMenuTSCControl);
		jMenu1.add(jMenu2);
		jMenuHelp.add(jMenuHelpAbout);
		jMenuTSCControl.add(jMenuTSCAuto);
		jMenuTSCControl.add(jMenuTSCPaused);
		jMenu2.add(jMenuSyncCarrierInfo);
		jMenu2.add(jMenuSyncTrCmdInfo);
		jMenuDebug.add(jMenuMsgEmulator);
		contentPane.add(jTabbedPane1, new XYConstraints(383, 44, 355, 369));
		jTrCmdPanel.add(editSource, new XYConstraints(100, 14, 168, -1));
		jTrCmdPanel.add(jLabel3, new XYConstraints(39, 18, 48, -1));
		jTrCmdPanel.add(jLabel4, new XYConstraints(39, 44, -1, -1));
		jTrCmdPanel.add(editDest, new XYConstraints(100, 41, 169, 19));
		jTrCmdPanel.add(editCarrierID, new XYConstraints(100, 70, 168, 18));
		jInitializePanel.add(btSendMsg, new XYConstraints(233, 18, -1, -1));
		jInitializePanel.add(cbMsgList, new XYConstraints(16, 21, 200, -1));
		jTrCmdPanel.add(edCmdID, new XYConstraints(100, 97, 166, 18));
		jTrCmdPanel.add(jLabel5, new XYConstraints(36, 100, -1, -1));
		jTrCmdPanel.add(btAbort, new XYConstraints(184, 256, 87, -1));
		jTrCmdPanel.add(btSendCmd, new XYConstraints(184, 185, -1, -1));
		jTrCmdPanel.add(btCancel, new XYConstraints(184, 220, 86, -1));
		jTrCmdPanel.add(jLabel6, new XYConstraints(40, 126, -1, -1));
		jTrCmdPanel.add(edPriority, new XYConstraints(100, 123, 166, 18));
		jTrCmdPanel.add(jTrForStage, new XYConstraints(50, 187, 105, 22));
		jTrCmdPanel.add(jAutoDestChange, new XYConstraints(50, 210, 135, 22));
		jTabbedPane1.add(jStagePanel, "Stage");
		jStagePanel.add(jLabel9, new XYConstraints(20, 32, 48, -1));
		jStagePanel.add(jLabel7, new XYConstraints(20, 59, -1, -1));
		jStagePanel.add(jLabel8, new XYConstraints(20, 86, -1, -1));
		jStagePanel.add(jLabel10, new XYConstraints(20, 114, -1, -1));
		jStagePanel.add(editStageCmdID, new XYConstraints(130, 115, 150, 18));
		jStagePanel.add(editStageCarrierID, new XYConstraints(130, 87, 150, 18));
		jStagePanel.add(editStageSource, new XYConstraints(130, 31, 150, -1));
		jStagePanel.add(editStageDest, new XYConstraints(130, 58, 150, 19));
		jStagePanel.add(jLabel12, new XYConstraints(20, 196, -1, -1));
		jStagePanel.add(jLabel13, new XYConstraints(20, 223, -1, -1));
		jStagePanel.add(editStageWTO, new XYConstraints(130, 221, 150, 18));
		jStagePanel.add(jLabel11, new XYConstraints(20, 168, -1, -1));
		jStagePanel.add(editStageED, new XYConstraints(130, 166, 150, 18));
		jStagePanel.add(editStageNBT, new XYConstraints(130, 194, 150, 18));
		jStagePanel.add(editStagePriority, new XYConstraints(130, 140, 150, 18));
		jStagePanel.add(jLabel14, new XYConstraints(20, 139, -1, -1));
		jStagePanel.add(jSendStageCmd, new XYConstraints(180, 250, 120, 26));
		jStagePanel.add(jSendStageDeleteCmd, new XYConstraints(180, 280, 120, 28));
		jPanel3.add(jLoadTransferHistory, new XYConstraints(4, 8, 117, -1));
		jPanel3.add(jLongrunStart, new XYConstraints(4, 42, 117, 26));
		jPanel3.add(jLongrunPause, new XYConstraints(4, 72, 117, -1));
		jLongrunPanel.add(jPanel4, new XYConstraints(10, 11, 323, 27));
		jPanel4.add(jLongrunTime, null);
		jTabbedPane1.add(jScanPanel1, "Scan");
		jScanPanel1.add(jLabel19, new XYConstraints(26, 46, -1, -1));
		jScanPanel1.add(editScanCarrierLoc, new XYConstraints(110, 45, 206, -1));
		jScrollPane2.getViewport().add(jLongrunLog, null);
		jTrCmdPanel.add(jLabel2, new XYConstraints(36, 70, -1, -1));
		jScanPanel1.add(jLabel20, new XYConstraints(26, 91, -1, -1));
		jScanPanel1.add(editScanCarrierID, new XYConstraints(110, 90, 206, -1));
		jScanPanel1.add(jLabel21, new XYConstraints(26, 133, -1, -1));
		jScanPanel1.add(editScanCommandID, new XYConstraints(110, 132, 206, -1));
		jScanPanel1.add(jLabel22, new XYConstraints(26, 176, -1, -1));
		jScanPanel1.add(editScanPriority, new XYConstraints(110, 173, 66, -1));
		jScanPanel1.add(btSendScan, new XYConstraints(202, 218, 97, -1));
		jTabbedPane1.add(jLocalOHTPanel, "LocalOHT");
		jLocalOHTPanel.add(jPanel5, new XYConstraints(8, 8, 333, 152));
		jLocalOHTPanel.add(jPanel6, new XYConstraints(8, 221, 334, 108));
		jPanel6.add(jScrollPane3, new XYConstraints(4, 5, 319, 64));
		jPanel6.add(jCBLocalOHTMsg, new XYConstraints(4, 78, 234, -1));
		jPanel6.add(jBtnSendMsg, new XYConstraints(242, 76, 82, -1));
		jScrollPane3.getViewport().add(jSndTextArea, null);
		jLocalOHTPanel.add(jBtnSendMsgFromFile, new XYConstraints(127, 185, 211, 21));
		jPanel7.add(jBtnLoadJobFile, new XYConstraints(14, 6, 124, 28));
		jPanel7.add(jBtnResume, new XYConstraints(200, 5, 100, 28));
		jPanel7.add(jBtnPause, new XYConstraints(200, 39, 100, 29));
		jTabbedPane1.add(jLongrunPanel, "Longrun");
		jUserLongrunPanel.add(jScrollPane4, new XYConstraints(9, 97, 327, 218));
		jScrollPane4.getViewport().add(jUserLongrunText, null);
		jUserLongrunPanel.add(jPanel7, new XYConstraints(9, 10, 329, 84));
		jTabbedPane1.add(jUserLongrunPanel, "UserLongrun");
		jCBLocalOHTMsg.addActionListener(new OCSManagerMainFrame_jCBLocalOHTMsg_actionAdapter(this));

		// 2011.07.25 by KYK : STBC Tab추가
		jSTBC.setLayout(xYLayout11);
		jTabbedPane1.add(jSTBC, "STBC");

		jlCarrierLocId.setText("CARRIERLOCID");
		jlCarrierId.setText("CARRIERID");
		editCarrierLocId.setText("");
		editCarrierId.setText("");
		btSendINSTALL.setText("INSTALL");
		btSendREMOVE.setText("REMOVE");
		btSendIDREAD.setText("IDREAD");
		btSendIDREADALL.setText("IDREADLALL");

		jSTBC.add(jlCarrierLocId, new XYConstraints(15, 46, -1, -1));
		jSTBC.add(editCarrierLocId, new XYConstraints(110, 45, 206, -1));
		jSTBC.add(jlCarrierId, new XYConstraints(15, 91, -1, -1));
		jSTBC.add(editCarrierId, new XYConstraints(110, 90, 206, -1));
		jSTBC.add(btSendINSTALL, new XYConstraints(202, 180, 105, -1));
		jSTBC.add(btSendREMOVE, new XYConstraints(202, 210, 105, -1));
		jSTBC.add(btSendIDREAD, new XYConstraints(202, 240, 105, -1));
		jSTBC.add(btSendIDREADALL, new XYConstraints(202, 270, 105, -1));

		btSendINSTALL.addActionListener(new OCSManagerMainFrame_btSendINSTALL_actionAdapter(this));
		btSendREMOVE.addActionListener(new OCSManagerMainFrame_btSendREMOVE_actionAdapter(this));
		btSendIDREAD.addActionListener(new OCSManagerMainFrame_btSendIDREAD_actionAdapter(this));
		btSendIDREADALL.addActionListener(new OCSManagerMainFrame_btSendIDREADALL_actionAdapter(this));

		// 2021.03.30 dahye TRANSFER_EX4
		jTabbedPane1.add(jTransferEx4Panel, "Transfer_EX4");
		jTransferEx4Panel.setLayout(xYLayout16);
		jLblEx4CmdId.setText("CmdId");
		editEx4CmdId.setText("PremoveCmd001");
		jLblEx4Priority.setText("Priority");
		editEx4Priority.setText("10");
		jLblEx4Replace.setText("Replace");
		editEx4Replace.setText("0");
		jLblEx4Carrier.setText("Carrier");
		editEx4Carrier.setText("Carrier001");
		jLblEx4Source.setText("Source");
		editEx4Source.setText("");
		jLblEx4Dest.setText("Dest");
		editEx4Dest.setText("");
		jLblEx4DeliveryType.setText("DT");
		editEx4DeliveryType.setText("PREMOVE");
		jLblEx4ExpectedDeliveryTime.setText("ED");
		editEx4ExpectedDeliveryTime.setText("0");
		jLblEx4DeliveryWaitTimeout.setText("DW");
		editEx4DeliveryWaitTimeout.setText("0");
		jBtnEx4Transfer.setToolTipText("SEND TR_EX4");
		jBtnEx4Transfer.setActionCommand("Send Cmd"); //?
		jBtnEx4Transfer.setText("TRANSFER_EX4");
		jBtnEx4Transfer.addActionListener(new OCSManagerMainFrame_btTrEx4SendCmd_actionAdapter(this));
		//
		jTransferEx4Panel.add(jLblEx4CmdId, new XYConstraints(39, 18, 48, -1));
		jTransferEx4Panel.add(editEx4CmdId, new XYConstraints(100, 14, 168, -1));
		jTransferEx4Panel.add(jLblEx4Priority, new XYConstraints(39, 48, 48, -1));
		jTransferEx4Panel.add(editEx4Priority, new XYConstraints(100, 44, 168, -1));
		jTransferEx4Panel.add(jLblEx4Replace, new XYConstraints(39, 78, 48, -1));
		jTransferEx4Panel.add(editEx4Replace, new XYConstraints(100, 74, 168, -1));
		jTransferEx4Panel.add(jLblEx4Carrier, new XYConstraints(39, 108, 48, -1));
		jTransferEx4Panel.add(editEx4Carrier, new XYConstraints(100, 104, 168, -1));
		jTransferEx4Panel.add(jLblEx4Source, new XYConstraints(39, 138, 48, -1));
		jTransferEx4Panel.add(editEx4Source, new XYConstraints(100, 134, 168, -1));
		jTransferEx4Panel.add(jLblEx4Dest, new XYConstraints(39, 168, 48, -1));
		jTransferEx4Panel.add(editEx4Dest, new XYConstraints(100, 164, 168, -1));
		jTransferEx4Panel.add(jLblEx4DeliveryType, new XYConstraints(39, 198, 48, -1));
		jTransferEx4Panel.add(editEx4DeliveryType, new XYConstraints(100, 194, 168, -1));
		jTransferEx4Panel.add(jLblEx4ExpectedDeliveryTime, new XYConstraints(39, 228, 48, -1));
		jTransferEx4Panel.add(editEx4ExpectedDeliveryTime, new XYConstraints(100, 224, 168, -1));
		jTransferEx4Panel.add(jLblEx4DeliveryWaitTimeout, new XYConstraints(39, 258, 48, -1));
		jTransferEx4Panel.add(editEx4DeliveryWaitTimeout, new XYConstraints(100, 254, 168, -1));
		jTransferEx4Panel.add(jBtnEx4Transfer, new XYConstraints(184, 285, -1, -1));

		this.setSize(new Dimension(755, 510));

		Initialize();
		//    jTabbedPane1.setSelectedComponent(jLongrunPanel);
		jTabbedPane1.setSelectedComponent(jSTBC);
		RegisterSampleMsgToComboBox(); // 2010.02.03. by IKY : Local OHT 기능
		UDPCommInitialize(); // 2010.02.03. by IKY : UDP 통신 초기화
	}

	/**
	 * 프로그램 실행 종료.
	 */
	void TerminateProgram() {
		String strLog = this.getTitle() + " 종료 ----------------------";
		m_ocsMain.WriteLog(strLog);
		m_ocsMain.WriteLog("");

		System.exit(0);
	}

	/**
	 * 초기화. DB Frame 및 STKSEM I/F 생성
	 */
	void Initialize() {
		m_ocsMain = new OCSManagerMain(this);
		setTitle("MCSEmulator " + m_ocsMain.m_strVersionID);
		updateDBStatusBar();

		// Main Operation Timer Thread 생성 및 실행
		// 주기적인 Timer 동작개시
		MainTimer.schedule(MainTimerTask, 0, 1000);

		SetMsgList();

		// 2009.08.28 by MYM : LongRun 객체 생성
		m_Longrun = new LongRun(m_ocsMain, m_ocsMain.Util);
	}

	void SetMsgList() {
		cbMsgList.addItem("S1F13");
		cbMsgList.addItem("S1F17");
		cbMsgList.addItem("S2F31");
		cbMsgList.addItem("S2F15");
		cbMsgList.addItem("S2F37Disable(All)");
		cbMsgList.addItem("S2F33");
		cbMsgList.addItem("S2F35");
		cbMsgList.addItem("S2F37");
		cbMsgList.addItem("S2F33Delete");
		cbMsgList.addItem("S2F41(PAUSE)");
		cbMsgList.addItem("S2F41(RESUME)");

	}

	/**
	 * MainTimerProc : MainTimerTask 수행
	 */
	class MainTimerProc extends TimerTask {
		OCSManagerMainFrame theClass;

		public MainTimerProc(OCSManagerMainFrame instance) {
			theClass = instance;
		}

		public void run() {
			try {
				// 화면 Display
				UpdateDisplay();

				// StatusBar 갱신
				//        UpdateStatusBar();
				updateDBStatusBar();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 화면 Display 갱신
	 */
	void UpdateDisplay() {
		// TSC 정보 --------------
		if (jTextField_TSCID.getText().equals(m_ocsMain.m_strTSCID) == false) {
			jTextField_TSCID.setText(m_ocsMain.tscType);
		}

		// TSC 상태 정보 -------------
		UpdateDisplay_TSC();

		// 반송명령 정보
		UpdateDisplay_TrCmd();

		// Carrier 정보
		UpdateDisplay_Carrier();

		// Alarm 정보
		UpdateDisplay_Alarm();
	}

	void UpdateStatusBar() {
		// DB 연결정보 ------------
		//    String strDBUrl = "DB URL : " + m_ocsMain.m_dbFrame.m_strUrl;
		String dbUserName = "DB USERNAME:" + m_ocsMain.m_dbFrame.m_strUser;
		if (jTextField_DBUrl.getText().equals(dbUserName) == false) {
			jTextField_DBUrl.setText(dbUserName);
		}

		String strDBConnectionStatus = "DB_NOT CONNECTED";
		if ((m_ocsMain.m_dbFrame != null) && m_ocsMain.m_dbFrame.IsDBConnected()) {
			strDBConnectionStatus = "DB_CONNECTED";
		}
		if (jTextField_DBConnectionStatus.getText().equals(strDBConnectionStatus) == false) {
			jTextField_DBConnectionStatus.setText(strDBConnectionStatus);
			if (strDBConnectionStatus.equals("DB_CONNECTED")) {
				jTextField_DBConnectionStatus.setBackground(new Color(125, 255, 125));
			} else {
				jTextField_DBConnectionStatus.setBackground(new Color(255, 125, 125));
			}
		}
	}

	// 2015.01.30 by KYK
	void updateDBStatusBar() {
		if ("STBC".equals(m_ocsMain.tscType)) {
			String dbUserName = "OCSDB(STBC) : " + m_ocsMain.m_dbFrame.m_strUser;
			if (jTextField_DBUrl.getText().equals(dbUserName) == false) {
				jTextField_DBUrl.setText(dbUserName);
			}
			String dbStatus = "DB_NOT CONNECTED";
			if (m_ocsMain.semIF != null && m_ocsMain.semIF.isDBConnected()) {
				dbStatus = "DB_CONNECTED";
			}
			if (jTextField_DBConnectionStatus.getText().equals(dbStatus) == false) {
				jTextField_DBConnectionStatus.setText(dbStatus);
				if ("DB_CONNECTED".equals(dbStatus)) {
					jTextField_DBConnectionStatus.setBackground(new Color(125, 255, 125));
				} else {
					jTextField_DBConnectionStatus.setBackground(new Color(255, 125, 125));
				}
			}
		} else {
			String dbStatus = "DB_NO_USE";
			if (jTextField_DBConnectionStatus.getText().equals(dbStatus) == false) {
				jTextField_DBConnectionStatus.setText("DB_NO_USE");
				jTextField_DBConnectionStatus.setBackground(new Color(255, 255, 0));
			}
		}
	}

	void UpdateDisplay_TSC() {
		String strSql;
		ResultSet rs = null;
		int Count;

		if (m_bTSCColumnMade == false) {
			TSCModel.addColumn("HSMSStatus");
			TSCModel.addColumn("CommunicationStatus");
			TSCModel.addColumn("ControlStatus");
			TSCModel.addColumn("TSCStatus");
			TSCModel.addColumn("DataUpdatedTime");
			m_bTSCColumnMade = true;
		}
		// DB로부터 Carrier를 조회
		try {
			strSql = "SELECT * FROM TSC WHERE TSCID='" + m_ocsMain.m_strTSCID + "'";
			rs = m_ocsMain.m_dbFrame.GetRecord(strSql);
			Count = 0;
			while ((rs != null) && (rs.next())) {
				if (Count >= TSCModel.getRowCount()) {
					TSCModel.addRow(new Object[] { "", "", "", "", "" });
				}
				TSCModel.setValueAt(m_ocsMain.semIF.m_strHSMSState, Count, 0);
				TSCModel.setValueAt(rs.getString("CommunicationStatus"), Count, 1);
				TSCModel.setValueAt(rs.getString("ControlStatus"), Count, 2);
				TSCModel.setValueAt(rs.getString("TSCStatus"), Count, 3);
				TSCModel.setValueAt(rs.getString("DataUpdatedTime"), Count, 4);
				Count++;
			}

			while (TSCModel.getRowCount() > Count) {
				TSCModel.removeRow(TSCModel.getRowCount() - 1);
			}
		} catch (SQLException e) {
			String strLog = "UpdateDisplay_TSC - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_ocsMain.m_dbFrame.CloseRecord(rs);
			}
		}
	}

	void UpdateDisplay_Alarm() {
		String strSql;
		ResultSet rs = null;
		int Count;

		if (m_bAlarmColumnMade == false) {
			AlarmModel.addColumn("AlarmID");
			AlarmModel.addColumn("AlarmText");
			AlarmModel.addColumn("AlarmSetTime");
			AlarmModel.addColumn("ErrorID");
			AlarmModel.addColumn("UnitID");
			AlarmModel.addColumn("CarrierLoc");
			m_bAlarmColumnMade = true;
		}
		// DB로부터 Carrier를 조회
		try {
			strSql = "SELECT * FROM Alarm WHERE TSC='" + m_ocsMain.m_strTSCID + "'";
			rs = m_ocsMain.m_dbFrame.GetRecord(strSql);
			Count = 0;
			while ((rs != null) && (rs.next())) {
				if (Count >= AlarmModel.getRowCount()) {
					AlarmModel.addRow(new Object[] { "", "", "", "", "", "" });
				}
				AlarmModel.setValueAt(String.valueOf(rs.getInt("AlarmID")), Count, 0);
				AlarmModel.setValueAt(rs.getString("AlarmText"), Count, 1);
				AlarmModel.setValueAt(rs.getString("AlarmSetTime"), Count, 2);
				AlarmModel.setValueAt(rs.getString("ErrorID"), Count, 3);
				AlarmModel.setValueAt(rs.getString("UnitID"), Count, 4);
				AlarmModel.setValueAt(rs.getString("CarrierLoc"), Count, 5);
				Count++;
			}

			while (AlarmModel.getRowCount() > Count) {
				AlarmModel.removeRow(AlarmModel.getRowCount() - 1);
			}
		} catch (SQLException e) {
			String strLog = "UpdateDisplay_Alarm - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_ocsMain.m_dbFrame.CloseRecord(rs);
			}
		}
	}

	void UpdateDisplay_TrCmd() {
		String strSql;
		ResultSet rs = null;
		int Count;

		if (m_bTrCmdColumnMade == false) {
			TrCmdModel.addColumn("MicroTrCmdID");
			TrCmdModel.addColumn("CarrierID");
			TrCmdModel.addColumn("SourceLoc");
			TrCmdModel.addColumn("DestLoc");
			TrCmdModel.addColumn("VehicleID");
			TrCmdModel.addColumn("Status");
			TrCmdModel.addColumn("StatusChangedTime");
			TrCmdModel.addColumn("InstallTime");
			m_bTrCmdColumnMade = true;
		}
		// DB로부터 Carrier를 조회
		try {
			strSql = "SELECT * FROM MicroTrCmd WHERE TSC='" + m_ocsMain.m_strTSCID + "'";
			strSql += " AND (Status<>'TransferCompleted' AND Status<>'TransferAbortCompleted' AND Status<>'TransferCancelCompleted' AND Status<>'NONE')";
			rs = m_ocsMain.m_dbFrame.GetRecord(strSql);
			Count = 0;
			while ((rs != null) && (rs.next())) {
				if (Count >= TrCmdModel.getRowCount()) {
					TrCmdModel.addRow(new Object[] { "", "", "", "", "", "", "" });
				}
				TrCmdModel.setValueAt(rs.getString("MicroTrCmdID"), Count, 0);
				TrCmdModel.setValueAt(rs.getString("CarrierID"), Count, 1);
				TrCmdModel.setValueAt(rs.getString("SourceLoc"), Count, 2);
				TrCmdModel.setValueAt(rs.getString("DestLoc"), Count, 3);
				TrCmdModel.setValueAt(rs.getString("Vehicle"), Count, 4);
				TrCmdModel.setValueAt(rs.getString("Status"), Count, 5);
				TrCmdModel.setValueAt(rs.getString("StatusChangedTime"), Count, 6);
				TrCmdModel.setValueAt(rs.getString("InstallTime"), Count, 7);
				Count++;
			}

			while (TrCmdModel.getRowCount() > Count) {
				TrCmdModel.removeRow(TrCmdModel.getRowCount() - 1);
			}
		} catch (SQLException e) {
			String strLog = "UpdateDisplay_TrCmd - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_ocsMain.m_dbFrame.CloseRecord(rs);
			}
		}
	}

	void UpdateDisplay_Carrier() {
		String strSql;
		ResultSet rs = null;
		int Count;

		if (m_bCarrierColumnMade == false) {
			CarrierModel.addColumn("PodID");
			CarrierModel.addColumn("CurrLoc");
			CarrierModel.addColumn("Status");
			CarrierModel.addColumn("StatusChangedTime");
			CarrierModel.addColumn("TimeoutStatus");
			CarrierModel.addColumn("TimeoutStartTime");
			CarrierModel.addColumn("MoveRequested");
			m_bCarrierColumnMade = true;
		}
		// DB로부터 Carrier를 조회
		try {
			strSql = "SELECT * FROM Carrier WHERE CurrLoc IN (SELECT CarrierLocID FROM CarrierLoc";
			strSql += " WHERE (Owner IN (SELECT VehicleID FROM Vehicle WHERE TSC='" + m_ocsMain.m_strTSCID + "'))";
			strSql += " OR (CarrierLocID IN (SELECT SourceLoc FROM MicroTrCmd WHERE TSC='" + m_ocsMain.m_strTSCID + "' AND Status<>'NONE')))";

			rs = m_ocsMain.m_dbFrame.GetRecord(strSql);
			Count = 0;
			while ((rs != null) && (rs.next())) {
				if (Count >= CarrierModel.getRowCount()) {
					CarrierModel.addRow(new Object[] { "", "", "", "", "", "", "" });
				}
				CarrierModel.setValueAt(rs.getString("CarrierID"), Count, 0);
				CarrierModel.setValueAt(rs.getString("CurrLoc"), Count, 1);
				CarrierModel.setValueAt(rs.getString("Status"), Count, 2);
				CarrierModel.setValueAt(rs.getString("StatusChangedTime"), Count, 3);
				CarrierModel.setValueAt(rs.getString("TimeoutStatus"), Count, 4);
				CarrierModel.setValueAt(rs.getString("TimeoutStartTime"), Count, 5);
				CarrierModel.setValueAt(String.valueOf(rs.getInt("MoveRequestedFlag")), Count, 6);
				Count++;
			}

			while (CarrierModel.getRowCount() > Count) {
				CarrierModel.removeRow(CarrierModel.getRowCount() - 1);
			}
		} catch (SQLException e) {
			String strLog = "UpdateDisplay_Carrier - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_ocsMain.m_dbFrame.CloseRecord(rs);
			}
		}
	}

	//File | Exit action performed
	public void jMenuFileExit_actionPerformed(ActionEvent e) {
		TerminateProgram();
	}

	//Help | About action performed
	public void jMenuHelpAbout_actionPerformed(ActionEvent e) {
		OCSManagerMainFrame_AboutBox dlg = new OCSManagerMainFrame_AboutBox(this);
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
		dlg.setModal(true);
		dlg.pack();
		dlg.show();
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			jMenuFileExit_actionPerformed(null);
		}
	}

	void jMenuTSCAuto_actionPerformed(ActionEvent e) {
		MyHashtable tscInfo = new MyHashtable();
		m_ocsMain.GetTSCInfo(tscInfo);

		if (tscInfo.toString("ControlStatus", 0).equals("OFFLINE")) {
			m_ocsMain.ReqTSCControlStatusChange("ONLINE");
		} else {
			m_ocsMain.ReqTSCStatusChange("AUTO");
		}
	}

	void jMenuTSCPaused_actionPerformed(ActionEvent e) {
		m_ocsMain.ReqTSCStatusChange("PAUSE");
	}

	void jMenuMsgEmulator_actionPerformed(ActionEvent e) {
		MsgEmulatorDlg dlg = new MsgEmulatorDlg(this);
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
		dlg.setModal(false);
		dlg.pack();
		dlg.show();
	}

	void jButton1_actionPerformed(ActionEvent e) {
		//MyHashtable trCmdInfo = new MyHashtable();
		//m_ocsMain.GetTrCmdInfo(trCmdInfo);
		//m_ocsMain.SendCancel(trCmdInfo);
	}

	void jButton2_actionPerformed(ActionEvent e) {
		//MyHashtable trCmdInfo = new MyHashtable();
		//m_ocsMain.GetTrCmdInfo(trCmdInfo);
		//m_ocsMain.SendAbort(trCmdInfo);
	}

	void btSendCmd_actionPerformed(ActionEvent e) {
		MyHashtable htCommandInfo = new MyHashtable();
		htCommandInfo.put("TSC", "TOCS411");
		htCommandInfo.put("MicroTrCmdID", edCmdID.getText());
		htCommandInfo.put("MicroTrCmdType", "TRANSFER");
		htCommandInfo.put("CarrierID", editCarrierID.getText());
		htCommandInfo.put("CarrierLocID", "");
		htCommandInfo.put("Source", editSource.getText());
		htCommandInfo.put("Dest", editDest.getText());
		htCommandInfo.put("Priority", new Integer(edPriority.getText()));
		htCommandInfo.put("Replace", new Integer(0));
		htCommandInfo.put("EmptyCarrier", new Integer(0));
		htCommandInfo.put("LotID", "AAA");
		htCommandInfo.put("ErrorID", "");
		htCommandInfo.put("FLOORNUMBER", new Integer(0));

		// 2009.09.10 by MYM : Stage에 대한 Transfer 명령인 경우
		if (jTrForStage.isSelected() == true) {
			m_ocsMain.semIF.SendMicroTCForStage(htCommandInfo);
			return;
		}

		// 2012.01.25 by LWG [Cancel/Abort 내리기]
		TransferCommand trcmd = new TransferCommand();
		trcmd.setTrCmdID(edCmdID.getText());
		trcmd.setPriority(new Integer(edPriority.getText()).intValue());
		trcmd.setSourceLoc(editSource.getText());
		trcmd.setDestLoc(editDest.getText());
		trcmd.setRemoteCmd("TRANSFER");
		trcmd.setCarrierID(editCarrierID.getText());

		m_ocsMain.getLongRunManager().transfer(trcmd);
		m_ocsMain.semIF.SendMicroTC(htCommandInfo);
	}

	void DisplayLog(String strMsg) {
		if (logArea.getLineCount() > 1000) {
			logArea.setText("");
			logArea.append(strMsg + "\n");
		} else {
			logArea.append(strMsg + "\n");
		}

		// 2009.08.28 by MYM : Stage 정보 추가
		if (jLongrunLog.getLineCount() > 1000) {
			jLongrunLog.setText("");
		} else {
			if (m_Longrun != null && m_Longrun.IsRunThread() == true) {
				long lTime = (long) m_Longrun.m_dThreadClock;
				String strTime = (lTime / 3600) + "시 ";
				if ((lTime / 3600) > 0)
					lTime = (lTime % 3600);
				strTime += (lTime / 60) + "분 ";
				if ((lTime / 60) > 0)
					lTime = (lTime % 60);
				strTime += lTime + "초";
				jLongrunTime.setText(strTime + " 경과 (" + m_Longrun.m_nIndex + "/" + m_Longrun.m_vtTransferCmdList.size() + ") \n");
			}
		}
	}

	void btSendMsg_actionPerformed(ActionEvent e) {
		m_ocsMain.semIF.SendMsg((String) cbMsgList.getSelectedItem());
	}

	void btCancel_actionPerformed(ActionEvent e) {
		MyHashtable pRCMD = new MyHashtable();
		pRCMD.put("RCMD", "CANCEL");
		pRCMD.put("COMMANDID", edCmdID.getText());
		m_ocsMain.semIF.SendS2F41(pRCMD);
	}

	void btAbort_actionPerformed(ActionEvent e) {
		MyHashtable pRCMD = new MyHashtable();
		pRCMD.put("RCMD", "ABORT");
		pRCMD.put("COMMANDID", edCmdID.getText());
		m_ocsMain.semIF.SendS2F41(pRCMD);

	}

	// 2009.07.21 by MYM : Stage 명령 전송
	void btSendStageCmd_actionPerformed(ActionEvent e) {
		MyHashtable htCommandInfo = new MyHashtable();
		htCommandInfo.put("TSC", "TOCS411");
		htCommandInfo.put("MicroTrCmdID", editStageCmdID.getText());
		htCommandInfo.put("MicroTrCmdType", "STAGE");
		htCommandInfo.put("CarrierID", editStageCarrierID.getText());
		htCommandInfo.put("CarrierLocID", "");
		htCommandInfo.put("Source", editStageSource.getText());
		htCommandInfo.put("Dest", editStageDest.getText());
		htCommandInfo.put("Priority", new Integer(editStagePriority.getText()));
		htCommandInfo.put("Replace", new Integer(0));
		htCommandInfo.put("EmptyCarrier", new Integer(0));
		htCommandInfo.put("LotID", "AAA");
		htCommandInfo.put("ErrorID", "");
		htCommandInfo.put("FLOORNUMBER", new Integer(0));
		htCommandInfo.put("EXPECTEDDURATION", new Integer(editStageED.getText()));
		htCommandInfo.put("NOBLOCKINGTIME", new Integer(editStageNBT.getText()));
		htCommandInfo.put("WAITTIMEOUT", new Integer(editStageWTO.getText()));

		m_ocsMain.semIF.SendMicroTC(htCommandInfo);
	}

	// 2009.07.21 by MYM : Stage Delete 명령 전송
	void btStageDelete_actionPerformed(ActionEvent e) {
		MyHashtable pRCMD = new MyHashtable();
		pRCMD.put("RCMD", "STAGEDELETE");
		pRCMD.put("COMMANDID", editStageCmdID.getText());
		m_ocsMain.semIF.SendS2F41(pRCMD);
	}

	// 2009.07.21 by MYM : Stage 명령 전송
	void jSendStageCmd_actionPerformed(ActionEvent e) {
		MyHashtable htCommandInfo = new MyHashtable();
		htCommandInfo.put("TSC", "TOCS411");
		htCommandInfo.put("MicroTrCmdID", editStageCmdID.getText());
		htCommandInfo.put("MicroTrCmdType", "STAGE");
		htCommandInfo.put("CarrierID", editStageCarrierID.getText());
		htCommandInfo.put("CarrierLocID", "");
		htCommandInfo.put("Source", editStageSource.getText());
		htCommandInfo.put("Dest", editStageDest.getText());
		htCommandInfo.put("Priority", new Integer(editStagePriority.getText()));
		htCommandInfo.put("Replace", new Integer(0));
		htCommandInfo.put("EmptyCarrier", new Integer(0));
		htCommandInfo.put("LotID", "AAA");
		htCommandInfo.put("ErrorID", "");
		htCommandInfo.put("FLOORNUMBER", new Integer(0));
		htCommandInfo.put("EXPECTEDDURATION", new Integer(editStageED.getText()));
		htCommandInfo.put("NOBLOCKINGTIME", new Integer(editStageNBT.getText()));
		htCommandInfo.put("WAITTIMEOUT", new Integer(editStageWTO.getText()));

		m_ocsMain.semIF.SendMicroTC(htCommandInfo);
	}

	// 2009.07.21 by MYM : Stage Delete 명령 전송
	void jSendStageDeleteCmd_actionPerformed(ActionEvent e) {
		MyHashtable pRCMD = new MyHashtable();
		pRCMD.put("RCMD", "STAGEDELETE");
		pRCMD.put("COMMANDID", editStageCmdID.getText());
		m_ocsMain.semIF.SendS2F41(pRCMD);
	}

	void jLoadTransferHistory_actionPerformed(ActionEvent e) {
		// 2009.08.28 by MYM : LongRun을 위한 Transfer.xls 파일 로드
		FileDialog fileDialog = new FileDialog(this);
		fileDialog.setMode(FileDialog.LOAD);
		fileDialog.setTitle("Open Transfer History");
		fileDialog.show();

		String file = fileDialog.getFile();
		if (file == null) {
			return;
		}
		String directory = fileDialog.getDirectory();
		String strFileName = directory + file;

		m_Longrun.LoadTransferCmdFromExcel(strFileName);
		m_Longrun.LongRunPause();
		jLongrunStart.setEnabled(true);
		jLongrunPause.setText("Pause");
		jLongrunSetOpt_actionPerformed(e);
	}

	void jLongrunStart_actionPerformed(ActionEvent e) {
		// 2009.08.28 by MYM : LongRun을 시작한다.
		if (m_Longrun.IsRunThread() == true) {
			jLongrunLog.append("LongRun Already Started... \n");
			return;
		}

		if (m_Longrun.LongRunStart() == false) {
			jLongrunLog.append("[LongRun Start Fail] Please, Load Transfer History... \n");
			return;
		}

		jLongrunStart.setEnabled(false);
		jLoadTransferHistory.setEnabled(false);
		jLongrunLog.append("LongRun Started... \n");
	}

	void jLongrunPause_actionPerformed(ActionEvent e) {
		// 2009.08.28 by MYM : LongRun을 Pause or Resume한다.
		if (m_Longrun.IsRunThread() == true && jLongrunPause.getText().equals("Pause")) {
			m_Longrun.LongRunPause();
			jLongrunPause.setText("Resume");
			jLongrunLog.append("LongRun Paused... \n");
			jLoadTransferHistory.setEnabled(true);
		} else if (jLongrunPause.getText().equals("Resume")) {
			if (m_Longrun.LongRunStart() == true) {
				jLongrunPause.setText("Pause");
				jLongrunLog.append("LongRun Started... \n");
				jLoadTransferHistory.setEnabled(false);
			}
		} else {
			jLongrunLog.append("LongRun State is wrong. \n");
		}
	}

	void jLongrunUseStage_actionPerformed(ActionEvent e) {
		// 2009.08.28 by MYM : LongRun을 Pause or Resume한다.
		m_Longrun.SetUseStageCmd(jLongrunUseStage.isSelected());
		jLongrunLog.append("Stage 사용유무 :" + jLongrunUseStage.isSelected() + " \n");
	}

	// 2012.01.25 by LWG [Cancel/Abort 내리기]
	void jLongrunUseCancel_actionPerformed(ActionEvent e) {
		m_Longrun.setUseCancelCmd(jLongrunUseCancel.isSelected());
		jLongrunLog.append("Cancel 사용유무 :" + jLongrunUseCancel.isSelected() + " \n");
	}

	void jLongrunUseAbort_actionPerformed(ActionEvent e) {
		m_Longrun.setUseAbortCmd(jLongrunUseAbort.isSelected());
		jLongrunLog.append("Abort 사용유무 :" + jLongrunUseAbort.isSelected() + " \n");
	}

	void jLongrunSetOpt_actionPerformed(ActionEvent e) {
		// 2009.08.28 by MYM : 반송명령 생성 가속 비율 및 Stage 명령 생성 주기를 설정한다.
		// 2012.01.25 by LWG [Cancel/Abort 내리기]
		double nAccel, nStageInterval, cancelInterval, abortInterval;
		int stageExpectedDuration;
		try {
			nAccel = Double.parseDouble(jLongrunAccel.getText());
			nStageInterval = Double.parseDouble(jLongrunStageInterval.getText());
			stageExpectedDuration = Integer.parseInt(jLongrunStageEDT.getText());
			cancelInterval = Double.parseDouble(jLongrunCancelInterval.getText());
			abortInterval = Double.parseDouble(jLongrunAbortInterval.getText());
		} catch (Exception exception) {
			nAccel = 1;
			nStageInterval = 10;
			cancelInterval = 10;
			abortInterval = 10;
			stageExpectedDuration = 80;
		}
		nStageInterval = 60 * nStageInterval;
		cancelInterval = 60 * cancelInterval;
		abortInterval = 60 * abortInterval;

		m_Longrun.SetThreadInterval(nAccel);
		m_Longrun.SetStageInterval((int) nStageInterval);
		m_Longrun.SetStageEDT((int) stageExpectedDuration);
		m_Longrun.setAbortInterval((int) abortInterval);
		m_Longrun.setCancelInterval((int) cancelInterval);

		jLongrunLog.append("반송명령 생성 가속 비율 :" + jLongrunAccel.getText() + " 배속 \n");
		jLongrunLog.append("Stage 발생주기 :" + jLongrunStageInterval.getText() + " 분" + ", EDT:" + jLongrunStageEDT.getText() + "초 \n");
		jLongrunLog.append("Cancel 발생주기 :" + jLongrunCancelInterval.getText() + " 분 \n");
		jLongrunLog.append("Abort 발생주기 :" + jLongrunAbortInterval.getText() + " 분 \n");
	}

	// 2009.10.12. by IKY : Scan 명령 전송 함수
	void btSendScan_actionPerformed(ActionEvent e) {
		MyHashtable htCommandInfo = new MyHashtable();
		htCommandInfo.put("TSC", "TOCS411");
		htCommandInfo.put("MicroTrCmdID", editScanCommandID.getText());
		htCommandInfo.put("MicroTrCmdType", "SCAN");
		htCommandInfo.put("CarrierID", editScanCarrierID.getText());
		htCommandInfo.put("CarrierLocID", editScanCarrierLoc.getText());
		htCommandInfo.put("Priority", new Integer(editScanPriority.getText()));

		m_ocsMain.semIF.SendMicroTC(htCommandInfo);
	}

	// 2010.02.03. by IKY : (Local OHT 기능) UDPComm 인스턴스 생성 및 SocketThread 시작
	void UDPCommInitialize() {
		m_UDPComm = new UDPComm(this);
		m_UDPComm.UDPCommStart();
	}

	// 2010.02.03. by IKY : Local OHT Sample Msg를 ComboBox에 등록함
	void RegisterSampleMsgToComboBox() {
		String strSampleMsg = "";

		// Local OHT 기능 사용 유무 Msg 등록
		strSampleMsg = "ULR HDR=(OCS,iFACS) IP=12.55.55.55 PORT=1111 USE=Y";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 0);
		strSampleMsg = "DGR HDR=(OCS,iFACS) GROUPID=G1 EQPID=(TATA04,TATA05,TATA06) MINVHL=1 MAXVHL=3 TYPE=ADD";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 1);
		strSampleMsg = "DGR HDR=(OCS,iFACS) GROUPID=G2 EQPID=(TABA03,TABA04) MINVHL=1 MAXVHL=3 TYPE=ADD";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 2);
		strSampleMsg = "SVR HDR=(OCS,iFACS) GROUPID=G1 SETVHL=3 DISTANCE=80 EXPIREDTIME=50";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 3);
		strSampleMsg = "SVR HDR=(OCS,iFACS) GROUPID=G2 SETVHL=3 DISTANCE=80 EXPIREDTIME=50";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 4);
		strSampleMsg = "GVR HDR=(OCS,iFACS) GROUPID=G1";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 5);
		strSampleMsg = "GVR HDR=(OCS,iFACS) GROUPID=G2";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 6);
		strSampleMsg = "DGR HDR=(OCS,iFACS) GROUPID=G1 EQPID= MINVHL= MAXVHL= TYPE=DEL";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 7);
		strSampleMsg = "DGR HDR=(OCS,iFACS) GROUPID=G2 EQPID= MINVHL= MAXVHL= TYPE=DEL";
		jCBLocalOHTMsg.insertItemAt(strSampleMsg, 8);
	}

	void jCBLocalOHTMsg_actionPerformed(ActionEvent e) {
		jSndTextArea.setText("");
		String strSelectedMsg = (String) jCBLocalOHTMsg.getSelectedItem();
		jSndTextArea.append(strSelectedMsg);
	}

	void jBtnSendMsg_actionPerformed(ActionEvent e) {
		String strSendMsg = jSndTextArea.getText();
		if (strSendMsg.equals("") == false)
			m_UDPComm.SendMsg(strSendMsg);
	}

	void jBtnSendMsgFromFile_actionPerformed(ActionEvent e) {
		// 2010.06.25 by MYM
		FileDialog fileDialog = new FileDialog(this);
		fileDialog.setMode(FileDialog.LOAD);
		fileDialog.setTitle("Load Data...");
		fileDialog.show();

		String file = fileDialog.getFile();
		if (file == null) {
			return;
		}
		String directory = fileDialog.getDirectory();
		String strFileName = directory + file;

		java.io.File f;
		java.io.RandomAccessFile raf;

		String sLine = "";

		try {
			f = new java.io.File(strFileName.toString());
			raf = new java.io.RandomAccessFile(f, "r");

			while ((sLine = raf.readLine()) != null) {
				if (sLine.toUpperCase().indexOf("ULR") == 0 || sLine.toUpperCase().indexOf("DGR") == 0 || sLine.toUpperCase().indexOf("SVR") == 0) {
					sLine.trim();
					if (sLine.equals("") == false)
						m_UDPComm.SendMsg(sLine);
				} else if (sLine.toUpperCase().indexOf("SLEEP") == 0) {
					try {
						sLine = sLine.substring(sLine.indexOf("SLEEP") + 6);
						sLine.trim();
						int nDelayTime = Integer.parseInt(sLine);
						Thread.sleep(nDelayTime);
					} catch (Exception ee) {
						String strLog = ee.toString();
					}
				}
			}
			raf.close();
		} catch (Exception exception) {
			String strLog = exception.toString();
		}
		statusBar.setText("");
	}

	void jBtnResume_actionPerformed(ActionEvent e) {
		FileLongRunManager fileLongRunManager = m_ocsMain.getFileLongRunManager();
		if (fileLongRunManager != null) {
			fileLongRunManager.start();
		}
	}

	void jBtnPause_actionPerformed(ActionEvent e) {
		FileLongRunManager fileLongRunManager = m_ocsMain.getFileLongRunManager();
		if (fileLongRunManager != null) {
			fileLongRunManager.pause();
		}
	}

	void jBtnLoadJobFile_actionPerformed(ActionEvent e) {
		//2011.09.07 by LWG : [롱런을 읽어서 반송 내리기]
		FileDialog fileDialog = new FileDialog(this);
		fileDialog.setMode(FileDialog.LOAD);
		fileDialog.setTitle("Open User-Defined-JobFile");
		fileDialog.show();

		String file = fileDialog.getFile();
		if (file == null) {
			return;
		}
		String directory = fileDialog.getDirectory();
		String strFileName = directory + file;

		FileLongRunManager fileLongRunManager = m_ocsMain.getFileLongRunManager();
		if (fileLongRunManager != null) {
			fileLongRunManager.initFileLongRunManager(strFileName);
		}
	}

	// 2011.07.25 by KYK 
	public void btSendINSTALL_actionPerformed(ActionEvent e) {
		String carrierLocId = editCarrierLocId.getText();
		String carrierId = editCarrierId.getText();
		m_ocsMain.semIF.sendS2F41("INSTALL", carrierId, carrierLocId);
	}

	public void btSendREMOVE_actionPerformed(ActionEvent e) {
		String carrierId = editCarrierId.getText();
		m_ocsMain.semIF.sendS2F41("REMOVE", carrierId, "");
	}

	public void btSendIDREAD_actionPerformed(ActionEvent e) {
		String carrierLocId = editCarrierLocId.getText();
		String carrierId = editCarrierId.getText();
		m_ocsMain.semIF.sendS2F41("IDREAD", carrierId, carrierLocId);
	}

	public void btSendIDREADALL_actionPerformed(ActionEvent e) {
		//	  m_ocsMain.stbSem.sendS2F49("IDREADLIST");
		m_ocsMain.semIF.sendS2F49("IDREADLIST", false);
	}

	/**
	 * 2021.03.30 dahye TRANSFER_EX4
	 */
	void btTrEx4SendCmd_actionPerformed(ActionEvent e) {
		MyHashtable htCommandInfo = new MyHashtable();
		htCommandInfo.put("TSC", "TOCS411");
		htCommandInfo.put("MicroTrCmdID", editEx4CmdId.getText());
		htCommandInfo.put("MicroTrCmdType", "TRANSFER_EX4");
		htCommandInfo.put("CarrierID", editEx4Carrier.getText());
		htCommandInfo.put("CarrierLocID", "");
		htCommandInfo.put("Source", editEx4Source.getText());
		htCommandInfo.put("Dest", editEx4Dest.getText());
		htCommandInfo.put("Priority", new Integer(editEx4Priority.getText()));
		htCommandInfo.put("Replace", new Integer(editEx4Replace.getText()));
		htCommandInfo.put("EmptyCarrier", new Integer(0));
		htCommandInfo.put("LotID", "AAA");
		htCommandInfo.put("ErrorID", "");
		htCommandInfo.put("FLOORNUMBER", new Integer(0));
		htCommandInfo.put("DELIVERYTYPE", editEx4DeliveryType.getText());
		htCommandInfo.put("EXPECTEDDELIVERYTIME", new Integer(editEx4ExpectedDeliveryTime.getText()));
		htCommandInfo.put("DELIVERYWAITTIMEOUT", new Integer(editEx4DeliveryWaitTimeout.getText()));

		//	    // For Longrun?
		//	    TransferEx4Command trcmd = new TransferEx4Command();
		//	    trcmd.setTrCmdID(editEx4CmdId.getText());
		//	    trcmd.setPriority(new Integer(editEx4Priority.getText()).intValue());
		//	    trcmd.setReplace(new Integer(editEx4Replace.getText()).intValue());
		//	    trcmd.setCarrierID(editEx4Carrier.getText());
		//	    trcmd.setSourceLoc(editEx4Source.getText());
		//	    trcmd.setDestLoc(editEx4Dest.getText());
		//	    trcmd.setDeliveryType(editEx4DeliveryType.getText());
		//	    trcmd.setExpectedDeliveryTime(new Integer(editEx4ExpectedDeliveryTime.getText()).intValue());
		//	    trcmd.setDeliveryWaitTimeout(new Integer(editEx4DeliveryWaitTimeout.getText()).intValue());

		m_ocsMain.semIF.SendMicroTC(htCommandInfo);
	}

}

class OCSManagerMainFrame_jMenuFileExit_ActionAdapter implements ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jMenuFileExit_ActionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuFileExit_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jMenuHelpAbout_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jMenuHelpAbout_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuHelpAbout_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jMenuTSCAuto_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jMenuTSCAuto_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuTSCAuto_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jMenuTSCPaused_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jMenuTSCPaused_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuTSCPaused_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jMenuMsgEmulator_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jMenuMsgEmulator_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuMsgEmulator_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jButton1_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jButton1_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButton1_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jButton2_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jButton2_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButton2_actionPerformed(e);
	}
}

class OCSManagerMainFrame_btSendCmd_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btSendCmd_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btSendCmd_actionPerformed(e);
	}
}

class OCSManagerMainFrame_btSendMsg_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btSendMsg_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btSendMsg_actionPerformed(e);
	}
}

class OCSManagerMainFrame_btCancel_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btCancel_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btCancel_actionPerformed(e);
	}
}

class OCSManagerMainFrame_btAbort_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btAbort_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btAbort_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jSendStageCmd_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jSendStageCmd_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jSendStageCmd_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jSendStageDeleteCmd_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jSendStageDeleteCmd_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jSendStageDeleteCmd_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jLongrunSetOpt_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jLongrunSetOpt_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jLongrunSetOpt_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jLongrunPause_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jLongrunPause_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jLongrunPause_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jLoadTransferHistory_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jLoadTransferHistory_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jLoadTransferHistory_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jLongrunStart_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jLongrunStart_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jLongrunStart_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jLongrunUseStage_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jLongrunUseStage_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jLongrunUseStage_actionPerformed(e);
	}
}

//2012.01.25 by LWG [Cancel/Abort 내리기]
class OCSManagerMainFrame_jLongrunUseAbort_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jLongrunUseAbort_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jLongrunUseAbort_actionPerformed(e);
	}
}

//2012.01.25 by LWG [Cancel/Abort 내리기]
class OCSManagerMainFrame_jLongrunUseCancel_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jLongrunUseCancel_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jLongrunUseCancel_actionPerformed(e);
	}
}

class OCSManagerMainFrame_btSendScan_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btSendScan_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btSendScan_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jCBLocalOHTMsg_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jCBLocalOHTMsg_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jCBLocalOHTMsg_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jBtnSendMsg_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jBtnSendMsg_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBtnSendMsg_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jBtnSendMsgFromFile_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jBtnSendMsgFromFile_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBtnSendMsgFromFile_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jBtnResume_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jBtnResume_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBtnResume_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jBtnPause_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jBtnPause_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBtnPause_actionPerformed(e);
	}
}

class OCSManagerMainFrame_jBtnLoadJobFile_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_jBtnLoadJobFile_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBtnLoadJobFile_actionPerformed(e);
	}
}

//2011.07.25 by KYK : STBC Tab추가
class OCSManagerMainFrame_btSendINSTALL_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btSendINSTALL_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btSendINSTALL_actionPerformed(e);
	}
}

class OCSManagerMainFrame_btSendREMOVE_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btSendREMOVE_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btSendREMOVE_actionPerformed(e);
	}
}

class OCSManagerMainFrame_btSendIDREAD_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btSendIDREAD_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btSendIDREAD_actionPerformed(e);
	}
}

class OCSManagerMainFrame_btSendIDREADALL_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btSendIDREADALL_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btSendIDREADALL_actionPerformed(e);
	}
}

/**
 * 2021.03.30 dahye TRANSFER_EX4
 */
class OCSManagerMainFrame_btTrEx4SendCmd_actionAdapter implements java.awt.event.ActionListener {
	OCSManagerMainFrame adaptee;

	OCSManagerMainFrame_btTrEx4SendCmd_actionAdapter(OCSManagerMainFrame adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.btTrEx4SendCmd_actionPerformed(e);
	}
}