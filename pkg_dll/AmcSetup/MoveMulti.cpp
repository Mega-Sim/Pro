// MoveMulti.cpp : implementation file
//

#include "stdafx.h"
#include "amcsetup.h"
#include "MoveMulti.h"

#include "MyPropertySheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


#define		MOVE_ACCEL		(1)
/////////////////////////////////////////////////////////////////////////////
// CMoveMulti property page

IMPLEMENT_DYNCREATE(CMoveMulti, CPropertyPage)

CMoveMulti::CMoveMulti() : CPropertyPage(CMoveMulti::IDD)
{
	//{{AFX_DATA_INIT(CMoveMulti)
	m_bAction1 = TRUE;
	m_bAction2 = TRUE;
	m_bAction3 = FALSE;
	m_bAction4 = FALSE;
	m_nPos1 = 8192 * 10;
	m_nPos2 = 8192 * 20;
	m_nPos3 = 8192 * 30;
	m_nPos4 = 8192 * 40;
	m_nMaxVelocity = 8192* 20;
	m_bSCurve = FALSE;
	m_nPauseAxis = 3;
	m_nAccMsec = 1000;
	m_bUseStMove = FALSE;
	m_nSeqVel = 409600;
	m_nMaxVelocity = 100;
	m_nSeqAcc = 500;
	//}}AFX_DATA_INIT

	m_hStopped = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hQuit = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hQuitRepeat = CreateEvent(NULL, TRUE, FALSE, NULL);
}

CMoveMulti::~CMoveMulti()
{
	SetEvent(m_hQuitRepeat);
	SetEvent(m_hQuit);
	WaitForSingleObject(m_hQuit, 500);

	CloseHandle(m_hQuitRepeat);
	CloseHandle(m_hQuit);
	CloseHandle(m_hStopped);
}

void CMoveMulti::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CMoveMulti)
	DDX_Check(pDX, IDC_CHECK_AX1, m_bAction1);
	DDX_Check(pDX, IDC_CHECK_AX2, m_bAction2);
	DDX_Check(pDX, IDC_CHECK_AX3, m_bAction3);
	DDX_Check(pDX, IDC_CHECK_AX4, m_bAction4);
	DDX_Text(pDX, IDC_EDIT_AX1_POS, m_nPos1);
	DDX_Text(pDX, IDC_EDIT_AX2_POS, m_nPos2);
	DDX_Text(pDX, IDC_EDIT_AX3_POS, m_nPos3);
	DDX_Text(pDX, IDC_EDIT_AX4_POS, m_nPos4);
	DDX_Text(pDX, IDC_EDIT_MAX_SPEED, m_nMaxVelocity);
	DDV_MinMaxInt(pDX, m_nMaxVelocity, 1, 100);
	DDX_Check(pDX, IDC_CHECK_SCURVE, m_bSCurve);
	DDX_Text(pDX, IDC_EDIT_PAUSE_AXIS, m_nPauseAxis);
	DDV_MinMaxInt(pDX, m_nPauseAxis, 0, 3);
	DDX_Text(pDX, IDC_EDIT_ACC_MSEC, m_nAccMsec);
	DDV_MinMaxUInt(pDX, m_nAccMsec, 10, 1000);
	DDX_Check(pDX, IDC_CHECK_USE_STMOVE, m_bUseStMove);
	DDX_Text(pDX, IDC_EDIT_SEQ_VEL, m_nSeqVel);
	DDX_Text(pDX, IDC_EDIT_SEQ_ACC, m_nSeqAcc);
	DDV_MinMaxUInt(pDX, m_nSeqAcc, 1, 1000);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CMoveMulti, CPropertyPage)
	//{{AFX_MSG_MAP(CMoveMulti)
	ON_BN_CLICKED(IDC_BUTTON_POSITION_CLEAR, OnButtonPositionClear)
	ON_BN_CLICKED(IDC_BUTTON_ACTION_ALL, OnButtonActionAll)
	ON_BN_CLICKED(IDC_BUTTON_STOP_ALL, OnButtonStopAll)
	ON_BN_CLICKED(IDC_BUTTON_CLEAR_STOP_ALL, OnButtonClearStopAll)
	ON_BN_CLICKED(IDC_BUTTON_FRAMES_CLEAR_ALL, OnButtonFramesClearAll)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_ON_ALL, OnButtonAxisOnAll)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_OFF_ALL, OnButtonAxisOffAll)
	ON_BN_CLICKED(IDC_BUTTON_REPEAT, OnButtonRepeat)
	ON_BN_CLICKED(IDC_BUTTON_REPEAT_STOP, OnButtonRepeatStop)
	ON_BN_CLICKED(IDC_BUTTON_ACTION_HOME, OnButtonActionHome)
	ON_BN_CLICKED(IDC_BUTTON_PAUSE, OnButtonPause)
	ON_BN_CLICKED(IDC_BUTTON_CLEAR_STOP, OnButtonClearStop)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CMoveMulti message handlers

BOOL CMoveMulti::OnKillActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	SetEvent(m_hQuit);
	return CPropertyPage::OnKillActive();
}

UINT _Mon(LPVOID lpv)
{
	CMoveMulti *pMulti = (CMoveMulti *) lpv;
	pMulti->Moni();
	AfxEndThread(0);

	return 0;
}

BOOL CMoveMulti::OnSetActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen) 
		AfxBeginThread(_Mon, this);	
	return CPropertyPage::OnSetActive();
}

void CMoveMulti::Moni()
{
	CString cStr;
	ResetEvent(m_hQuit);

	while (WaitForSingleObject(m_hQuit, 20) == WAIT_TIMEOUT)
	{
#ifndef MDF_FUNC	
		// Axis Statue
		cStr = (in_sequence(0) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX1_INSEQUENCE)->SetWindowText(cStr);
		cStr = (in_sequence(1) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX2_INSEQUENCE)->SetWindowText(cStr);
		cStr = (in_sequence(2) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX3_INSEQUENCE)->SetWindowText(cStr);
		cStr = (in_sequence(3) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX4_INSEQUENCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////
		cStr = (in_motion(0) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX1_INMOTION)->SetWindowText(cStr);
		cStr = (in_motion(1) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX2_INMOTION)->SetWindowText(cStr);
		cStr = (in_motion(2) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX3_INMOTION)->SetWindowText(cStr);
		cStr = (in_motion(3) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX4_INMOTION)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////
		cStr	= (in_position(0) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX1_INPOSITION)->SetWindowText(cStr);
		cStr	= (in_position(1) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX2_INPOSITION)->SetWindowText(cStr);
		cStr	= (in_position(2) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX3_INPOSITION)->SetWindowText(cStr);
		cStr	= (in_position(3) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX4_INPOSITION)->SetWindowText(cStr);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
		int chk_errss;
		cStr = (in_sequence(0,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX1_INSEQUENCE)->SetWindowText(cStr);

		cStr = (in_sequence(1,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX2_INSEQUENCE)->SetWindowText(cStr);

		cStr = (in_sequence(2,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX3_INSEQUENCE)->SetWindowText(cStr);

		cStr = (in_sequence(3,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX4_INSEQUENCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////
		cStr = (in_motion(0,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX1_INMOTION)->SetWindowText(cStr);

		cStr = (in_motion(1,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX2_INMOTION)->SetWindowText(cStr);

		cStr = (in_motion(2,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX3_INMOTION)->SetWindowText(cStr);

		cStr = (in_motion(3,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX4_INMOTION)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////
		cStr	= (in_position(0,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX1_INPOSITION)->SetWindowText(cStr);

		cStr	= (in_position(1,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX2_INPOSITION)->SetWindowText(cStr);

		cStr	= (in_position(2,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX3_INPOSITION)->SetWindowText(cStr);

		cStr	= (in_position(3,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AX4_INPOSITION)->SetWindowText(cStr);
#endif




#ifndef MDF_FUNC
		cStr.Format("%d", frames_left(0)); 
		GetDlgItem(IDC_EDIT_AX1_FRAMESLEFT)->SetWindowText(cStr);
		cStr.Format("%d", frames_left(1)); 
		GetDlgItem(IDC_EDIT_AX2_FRAMESLEFT)->SetWindowText(cStr);
		cStr.Format("%d", frames_left(2)); 
		GetDlgItem(IDC_EDIT_AX3_FRAMESLEFT)->SetWindowText(cStr);
		cStr.Format("%d", frames_left(3)); 
		GetDlgItem(IDC_EDIT_AX4_FRAMESLEFT)->SetWindowText(cStr);
////////////////////////////////////////////////////////////////////		
		cStr = (axis_done(0) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX1_AXISDONE)->SetWindowText(cStr);
		cStr = (axis_done(1) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX2_AXISDONE)->SetWindowText(cStr);
		cStr = (axis_done(2) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX3_AXISDONE)->SetWindowText(cStr);
		cStr = (axis_done(3) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX4_AXISDONE)->SetWindowText(cStr);
////////////////////////////////////////////////////////////////////
		cStr.Format("0x%04X", 0xffff & axis_source(0));
		GetDlgItem(IDC_EDIT_AX1_AXISSOURCE)->SetWindowText(cStr);
		cStr.Format("0x%04X", 0xffff & axis_source(1));
		GetDlgItem(IDC_EDIT_AX2_AXISSOURCE)->SetWindowText(cStr);
		cStr.Format("0x%04X", 0xffff & axis_source(2));
		GetDlgItem(IDC_EDIT_AX3_AXISSOURCE)->SetWindowText(cStr);
		cStr.Format("0x%04X", 0xffff & axis_source(3));
		GetDlgItem(IDC_EDIT_AX4_AXISSOURCE)->SetWindowText(cStr);
////////////////////////////////////////////////////////////////////
		cStr.Format("0x%02X", axis_state(0));
		GetDlgItem(IDC_EDIT_AX1_STATE)->SetWindowText(cStr);
		cStr.Format("0x%02X", axis_state(1));
		GetDlgItem(IDC_EDIT_AX2_STATE)->SetWindowText(cStr);
		cStr.Format("0x%02X", axis_state(2));
		GetDlgItem(IDC_EDIT_AX3_STATE)->SetWindowText(cStr);
		cStr.Format("0x%02X", axis_state(3));
		GetDlgItem(IDC_EDIT_AX4_STATE)->SetWindowText(cStr);

#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
		int tmp_as_v, tmp_as_e;
		int loop_i=0;
////////////////////////////////////////////////////////////////////

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=frames_left(0 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("%d", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX1_FRAMESLEFT)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=frames_left(1 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("%d", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX2_FRAMESLEFT)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=frames_left(2 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("%d", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX3_FRAMESLEFT)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=frames_left(3 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("%d", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX4_FRAMESLEFT)->SetWindowText(cStr);

////////////////////////////////////////////////////////////////////		
		cStr = (axis_done(0) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX1_AXISDONE)->SetWindowText(cStr);
		cStr = (axis_done(1) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX2_AXISDONE)->SetWindowText(cStr);
		cStr = (axis_done(2) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX3_AXISDONE)->SetWindowText(cStr);
		cStr = (axis_done(3) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AX4_AXISDONE)->SetWindowText(cStr);
////////////////////////////////////////////////////////////////////
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= 0xffff & axis_source(0 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%04X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX1_AXISSOURCE)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= 0xffff & axis_source(1 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%04X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX2_AXISSOURCE)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= 0xffff & axis_source(2 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%04X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX3_AXISSOURCE)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= 0xffff & axis_source(3 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%04X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX4_AXISSOURCE)->SetWindowText(cStr);
////////////////////////////////////////////////////////////////////
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=axis_state(0 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%02X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX1_STATE)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=axis_state(1 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%02X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX2_STATE)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=axis_state(2 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%02X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX3_STATE)->SetWindowText(cStr);

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=axis_state(3 ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%02X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AX4_STATE)->SetWindowText(cStr);
#endif

		double dPos;
		// Motion Status
		get_command(0, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX1_COMMAND)->SetWindowText(cStr);
		get_command(1, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX2_COMMAND)->SetWindowText(cStr);
		get_command(2, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX3_COMMAND)->SetWindowText(cStr);
		get_command(3, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX4_COMMAND)->SetWindowText(cStr);

		get_counter(0, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX1_ACTUAL)->SetWindowText(cStr);
		get_counter(1, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX2_ACTUAL)->SetWindowText(cStr);
		get_counter(2, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX3_ACTUAL)->SetWindowText(cStr);
		get_counter(3, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX4_ACTUAL)->SetWindowText(cStr);

		get_error(0, &dPos);		cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX1_ERROR)->SetWindowText(cStr);
		get_error(1, &dPos);		cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX2_ERROR)->SetWindowText(cStr);
		get_error(2, &dPos);		cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX3_ERROR)->SetWindowText(cStr);
		get_error(3, &dPos);		cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX4_ERROR)->SetWindowText(cStr);
#if 0
		av = get_com_velocity(m_nAxis);	cStr.Format("%d", av);
		GetDlgItem(IDC_EDIT_MS_VELOCITY)->SetWindowText(cStr);
#endif
		get_position(0, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX1_ENCODER)->SetWindowText(cStr);
		get_position(1, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX2_ENCODER)->SetWindowText(cStr);
		get_position(2, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX3_ENCODER)->SetWindowText(cStr);
		get_position(3, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_AX4_ENCODER)->SetWindowText(cStr);
#if 0
		av = get_act_velocity(m_nAxis); cStr.Format("%.3f", av);// / 60.);
		GetDlgItem(IDC_EDIT_MS_RPM)->SetWindowText(cStr);
#endif

		cStr.Format("%d", GetPrivateProfileInt("AXIS_1", "VEL_LIMIT", 100000, GetParamPath()));
		GetDlgItem(IDC_EDIT_AX1_VEL)->SetWindowText(cStr);
		cStr.Format("%d", GetPrivateProfileInt("AXIS_1", "ACC_LIMIT", 100000, GetParamPath()));
		GetDlgItem(IDC_EDIT_AX1_ACC)->SetWindowText(cStr);
		cStr.Format("%d", GetPrivateProfileInt("AXIS_2", "VEL_LIMIT", 100000, GetParamPath()));
		GetDlgItem(IDC_EDIT_AX2_VEL)->SetWindowText(cStr);
		cStr.Format("%d", GetPrivateProfileInt("AXIS_2", "ACC_LIMIT", 100000, GetParamPath()));
		GetDlgItem(IDC_EDIT_AX2_ACC)->SetWindowText(cStr);
		cStr.Format("%d", GetPrivateProfileInt("AXIS_3", "VEL_LIMIT", 100000, GetParamPath()));
		GetDlgItem(IDC_EDIT_AX3_VEL)->SetWindowText(cStr);
		cStr.Format("%d", GetPrivateProfileInt("AXIS_3", "ACC_LIMIT", 100000, GetParamPath()));
		GetDlgItem(IDC_EDIT_AX3_ACC)->SetWindowText(cStr);
		cStr.Format("%d", GetPrivateProfileInt("AXIS_4", "VEL_LIMIT", 100000, GetParamPath()));
		GetDlgItem(IDC_EDIT_AX4_VEL)->SetWindowText(cStr);
		cStr.Format("%d", GetPrivateProfileInt("AXIS_4", "ACC_LIMIT", 100000, GetParamPath()));
		GetDlgItem(IDC_EDIT_AX4_ACC)->SetWindowText(cStr);
	}

	SetEvent(m_hStopped);
}

char *CMoveMulti::GetParamPath()
{
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;

	sprintf(m_strPath, "%s%s", pSheet->m_szAbsPath, AMC_PARAMETER_FILENAME);
	return m_strPath;
}


void CMoveMulti::OnButtonPositionClear() 
{
	// TODO: Add your control notification handler code here
	set_position(0, 0);
	set_position(1, 0);
	set_position(2, 0);
	set_position(3, 0);
}

void CMoveMulti::OnButtonActionAll() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetOptions(m_nPos1, m_nPos2, m_nPos3, m_nPos4);

	DoAction();
}

void CMoveMulti::OnButtonStopAll() 
{
	// TODO: Add your control notification handler code here
	set_stop(0);
	set_stop(1);
	set_stop(2);
	set_stop(3);
}

void CMoveMulti::OnButtonClearStopAll() 
{
	// TODO: Add your control notification handler code here
	clear_stop(0);
	clear_stop(1);
	clear_stop(2);
	clear_stop(3);
}

void CMoveMulti::OnButtonFramesClearAll() 
{
	// TODO: Add your control notification handler code here
	frames_clear(0);
	frames_clear(1);
	frames_clear(2);
	frames_clear(3);
}

void CMoveMulti::OnButtonAxisOnAll() 
{
	// TODO: Add your control notification handler code here
	set_amp_enable(0, 1);
	set_amp_enable(1, 1);
	set_amp_enable(2, 1);
	set_amp_enable(3, 1);
}

void CMoveMulti::OnButtonAxisOffAll() 
{
	// TODO: Add your control notification handler code here
	set_amp_enable(0, 0);
	set_amp_enable(1, 0);
	set_amp_enable(2, 0);
	set_amp_enable(3, 0);
}


void CMoveMulti::SetOptions(int p1, int p2, int p3, int p4)
{
	m_nActAxisCount = 0;

	if (m_bAction1) 
	{
		m_AxisOn[m_nActAxisCount] = 0;
		m_Pos[m_nActAxisCount] = p1;
		m_nActAxisCount ++; 
	}
	if (m_bAction2) 
	{ 
		m_AxisOn[m_nActAxisCount] = 1; 
		m_Pos[m_nActAxisCount] = p2;
		m_nActAxisCount ++; 
	}
	if (m_bAction3) 
	{ 
		m_AxisOn[m_nActAxisCount] = 2; 
		m_Pos[m_nActAxisCount] = p3;
		m_nActAxisCount ++; 
	}
	if (m_bAction4) 
	{ 
		m_AxisOn[m_nActAxisCount] = 3; 
		m_Pos[m_nActAxisCount] = p4;
		m_nActAxisCount ++; 
	}

	map_axes(m_nActAxisCount, m_AxisOn);
	set_move_speed(m_nMaxVelocity);
	set_move_accel(m_nAccMsec);
}

void CMoveMulti::OnButtonActionHome() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	SetOptions(0, 0, 0, 0);
	DoAction();
}

UINT CMoveMulti::GetVelocity(int axis)
{
	CString cStr;
	if (axis == 1) GetDlgItemText(IDC_EDIT_AX1_VEL, cStr);
	else if (axis == 2) GetDlgItemText(IDC_EDIT_AX2_VEL, cStr);
	else if (axis == 3) GetDlgItemText(IDC_EDIT_AX3_VEL, cStr);
	else GetDlgItemText( IDC_EDIT_AX4_VEL, cStr);

	return atoi((char *) (LPCTSTR) cStr);
}

UINT CMoveMulti::GetAcc(int axis)
{
	CString cStr;
	if (axis == 1) GetDlgItemText(IDC_EDIT_AX1_ACC, cStr);
	else if (axis == 2) GetDlgItemText(IDC_EDIT_AX2_ACC, cStr);
	else if (axis == 3) GetDlgItemText(IDC_EDIT_AX3_ACC, cStr);
	else GetDlgItemText(IDC_EDIT_AX4_ACC, cStr);

	return atoi((char *) (LPCTSTR) cStr);
}

void CMoveMulti::DoAction()
{
	if (m_bUseStMove)
	{
		if (m_bSCurve)
		{
			for (int i = 0; i < m_nActAxisCount; i ++)
			{
				start_s_move(m_AxisOn[i], m_Pos[i], m_nSeqVel, m_nSeqAcc);
			}
		} else {
			for (int i = 0; i < m_nActAxisCount; i ++)
			{
				start_move(m_AxisOn[i], m_Pos[i], m_nSeqVel, m_nSeqAcc);
			}
		}
	}
	else if (m_bSCurve)
	{
		if (m_nActAxisCount == 1) 
		{
			UINT uiVel = GetVelocity(m_AxisOn[0]);
			UINT uiAcc = GetAcc(m_AxisOn[0]);
			start_s_move(m_AxisOn[0], m_Pos[0], uiVel, uiAcc);
		}
		else if (m_nActAxisCount == 2) smove_2(m_Pos[0], m_Pos[1]);
		else if (m_nActAxisCount == 3) smove_3(m_Pos[0], m_Pos[1], m_Pos[2]);
		else if (m_nActAxisCount == 4) smove_4(m_Pos[0], m_Pos[1], m_Pos[2], m_Pos[3]);
	} else {
		if (m_nActAxisCount == 1) 
		{
			UINT uiVel = GetVelocity(m_AxisOn[0]);
			UINT uiAcc = GetAcc(m_AxisOn[0]);
			start_move(m_AxisOn[0], m_Pos[0], uiVel, uiAcc);
		}
		else if (m_nActAxisCount == 2) move_2(m_Pos[0], m_Pos[1]);
		else if (m_nActAxisCount == 3) move_3(m_Pos[0], m_Pos[1], m_Pos[2]);
		else if (m_nActAxisCount == 4) move_4(m_Pos[0], m_Pos[1], m_Pos[2], m_Pos[3]);
	}
}

BOOL CMoveMulti::CheckAllDone()
{
#ifndef MDF_FUNC	
	if (m_nActAxisCount == 1) 
		return axis_done(m_AxisOn[0]) && (axis_state(m_AxisOn[0]) == 0);
	else if (m_nActAxisCount == 2) 
		return (axis_done(m_AxisOn[0]) && axis_done(m_AxisOn[1])) && 
			(axis_state(m_AxisOn[0]) == 0) && (axis_state(m_AxisOn[1]) == 0);
	else if (m_nActAxisCount == 3) 
		return (axis_done(m_AxisOn[0]) && axis_done(m_AxisOn[1])
			&& axis_done(m_AxisOn[2])) &&
			(axis_state(m_AxisOn[0]) == 0) && (axis_state(m_AxisOn[1]) == 0) &&
			(axis_state(m_AxisOn[2]) == 0);
	else if (m_nActAxisCount == 4) 
		return (axis_done(m_AxisOn[0]) && axis_done(m_AxisOn[1])
			&& axis_done(m_AxisOn[2]) && axis_done(m_AxisOn[3])) &&
			(axis_state(m_AxisOn[0]) == 0) && (axis_state(m_AxisOn[1]) == 0) &&
			(axis_state(m_AxisOn[2]) == 0) && (axis_state(m_AxisOn[3]) == 0);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_as_v,tmp_as_v1,tmp_as_v2,tmp_as_v3, tmp_as_e;
	int loop_i=0;

	if (m_nActAxisCount == 1)
	{
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= axis_state(m_AxisOn[0], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}	
		return axis_done(m_AxisOn[0]) && (tmp_as_v == 0);
	}
	else if (m_nActAxisCount == 2)
	{
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= axis_state(m_AxisOn[0], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}	

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v1= axis_state(m_AxisOn[1], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}	

		return (axis_done(m_AxisOn[0]) && axis_done(m_AxisOn[1])) && 
			(tmp_as_v == 0) && (tmp_as_v1 == 0);
	}
	else if (m_nActAxisCount == 3) 
	{
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= axis_state(m_AxisOn[0], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v1= axis_state(m_AxisOn[1], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v2= axis_state(m_AxisOn[2], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		return (axis_done(m_AxisOn[0]) && axis_done(m_AxisOn[1])
			&& axis_done(m_AxisOn[2])) &&
			(tmp_as_v == 0) && (tmp_as_v1 == 0) &&
			(tmp_as_v2 == 0);
	}
	else if (m_nActAxisCount == 4) 
	{
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= axis_state(m_AxisOn[0], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v1= axis_state(m_AxisOn[1], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v2= axis_state(m_AxisOn[2], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v3= axis_state(m_AxisOn[3], &tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		return (axis_done(m_AxisOn[0]) && axis_done(m_AxisOn[1])
			&& axis_done(m_AxisOn[2]) && axis_done(m_AxisOn[3])) &&
			(tmp_as_v == 0) && (tmp_as_v1 == 0) &&
			(tmp_as_v2 == 0) && (tmp_as_v3 == 0);
	}
#endif


	return FALSE;
}



int nPauseAxis = 0;
void CMoveMulti::OnButtonPause() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	set_stop(m_nPauseAxis);
	nPauseAxis = m_nPauseAxis;
	GetDlgItem(IDC_BUTTON_CLEAR_STOP)->EnableWindow(TRUE);
}

void CMoveMulti::OnButtonClearStop() 
{
	// TODO: Add your control notification handler code here
	clear_stop(nPauseAxis);
	GetDlgItem(IDC_BUTTON_CLEAR_STOP)->EnableWindow(FALSE);
	GetDlgItem(IDC_BUTTON_PAUSE)->EnableWindow(TRUE);
}


UINT _Repeat(LPVOID lpv)
{
	CMoveMulti *pMult = (CMoveMulti *) lpv;
	pMult->Repeat();
	return 0;
}
void CMoveMulti::OnButtonRepeat() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	SetOptions(m_nPos1, m_nPos2, m_nPos3, m_nPos4);
	m_nRepeatCmd = 1;
}

void CMoveMulti::OnButtonRepeatStop() 
{
	// TODO: Add your control notification handler code here
	m_nRepeatCmd = 0;
	Sleep(10);
	m_nRepeatCmd = 0;
}

BOOL CMoveMulti::OnInitDialog() 
{
	CPropertyPage::OnInitDialog();
	
	// TODO: Add extra initialization here
	AfxBeginThread(_Repeat, this);

	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}

void CMoveMulti::Repeat()
{
//	int Pos[4];//2011.10.10

	m_nActAxisCount = 0;
	m_nRepeatCmd = 0;
	ResetEvent(m_hQuitRepeat);

	int state = -1;
	while (::WaitForSingleObject(m_hQuitRepeat, 200) != WAIT_OBJECT_0)
	{
		if (m_nRepeatCmd == 0) { state = -1; continue;}

		switch (state)
		{
		case -1:
			state = 0;
			break;

		case 0:		// wait for action done
			if (CheckAllDone()) 
				state = 1;
			break;

		case 1:
			if (m_nRepeatCmd == 1) 
			{
				SetOptions(m_nPos1, m_nPos2, m_nPos3, m_nPos4);
				DoAction();
				m_nRepeatCmd = 2;
			} else {
				SetOptions(0, 0, 0, 0);
				DoAction();
				m_nRepeatCmd = 1;
			}
			state = 0;
			break;
		}
	}
}

