// ActionPage.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "ActionPage.h"

#include "MyPropertySheet.h"

#include "ParamTuning.h"
#include "SoftLimits.h"
#include "LimitSwitch.h"
#include "AxisConfig.h"

#include "console.h"



CConsole *pConsole = NULL;
BOOL bDbgMsgWnd = FALSE;

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CActionPage property page

IMPLEMENT_DYNCREATE(CActionPage, CPropertyPage)

CActionPage::CActionPage() : CPropertyPage(CActionPage::IDD)
{
	//{{AFX_DATA_INIT(CActionPage)
	m_bAmpEnable = FALSE;
	m_bAmpFault = FALSE;
	m_bHome = FALSE;
	m_bNeg = FALSE;
	m_bPos = FALSE;
	m_bSCurve = FALSE;
	m_uiAxis = 0;
	m_uiAccel = 500;
	m_uiDecel = 500;
	m_uiDelay = 5000;
	m_uiVel = 8192*10;
	m_uiJogPS = 0;
	m_uiJogRM = 0;
	m_bTrapezoidal = TRUE;
	m_bRelative = FALSE;
	m_iPos1 = 0;
	m_iPos2 = 8192*50;
	m_iEncoderPosition = 0;
	m_bPos2Rnd = FALSE;
	//}}AFX_DATA_INIT

	m_hShowStatusQuit = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hShowStatusStopped = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hMotionActionQuit = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hMotionActionStopped = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_nRptCmd = 0;

	m_nOneCycleDir = 0;	// 0:goto pos2, 1:goto pos1

	pConsole = NULL;
//	pConsole = new CConsole;
//	pConsole->InitInstance("AMC setup");
}

CActionPage::~CActionPage()
{
	if (pConsole) delete pConsole;

	SetEvent(m_hShowStatusQuit);
	SetEvent(m_hMotionActionQuit);
	::WaitForSingleObject(m_hShowStatusStopped, 500);
	::WaitForSingleObject(m_hMotionActionStopped, 500);

	CloseHandle(m_hShowStatusQuit);
	CloseHandle(m_hShowStatusStopped);
	CloseHandle(m_hMotionActionQuit);
	CloseHandle(m_hMotionActionStopped);
}

void CActionPage::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CActionPage)
	DDX_Check(pDX, IDC_CHECK_AMP_ENABLE, m_bAmpEnable);
	DDX_Check(pDX, IDC_CHECK_AMP_FAULT, m_bAmpFault);
	DDX_Check(pDX, IDC_CHECK_HOME, m_bHome);
	DDX_Check(pDX, IDC_CHECK_NEG_MINUS, m_bNeg);
	DDX_Check(pDX, IDC_CHECK_POS_PLUS, m_bPos);
	DDX_Check(pDX, IDC_CHECK_SCURVE, m_bSCurve);
	DDX_Text(pDX, IDC_EDIT_CMD_AXIS, m_uiAxis);
	DDX_Text(pDX, IDC_EDIT_CMD_ACCEL, m_uiAccel);
	DDX_Text(pDX, IDC_EDIT_CMD_DECEL, m_uiDecel);
	DDX_Text(pDX, IDC_EDIT_CMD_DELAY, m_uiDelay);
	DDX_Text(pDX, IDC_EDIT_CMD_VEL, m_uiVel);
	DDX_Text(pDX, IDC_EDIT_JOG_SPEED_PS, m_uiJogPS);
	DDX_Text(pDX, IDC_EDIT_JOG_SPEED_RM, m_uiJogRM);
	DDX_Check(pDX, IDC_CHECK_TRAPEZOID, m_bTrapezoidal);
	DDX_Check(pDX, IDC_CHECK_RELATIVE_MOVE, m_bRelative);
	DDX_Text(pDX, IDC_EDIT_CMD_POS1, m_iPos1);
	DDX_Text(pDX, IDC_EDIT_CMD_POS2, m_iPos2);
	DDX_Text(pDX, IDC_EDIT_ENCODER_POSITION, m_iEncoderPosition);
	DDX_Check(pDX, IDC_CHECK_POS2RND, m_bPos2Rnd);
	//}}AFX_DATA_MAP
	DDX_Control(pDX, IDC_BUTTON_JOG_POS, m_JogPos);
	DDX_Control(pDX, IDC_BUTTON_JOG_NEG, m_JogNeg);
}


BEGIN_MESSAGE_MAP(CActionPage, CPropertyPage)
	//{{AFX_MSG_MAP(CActionPage)
	ON_BN_CLICKED(IDC_BUTTON_TUNNING, OnButtonTunning)
	ON_BN_CLICKED(IDC_BUTTON_SW_LIMIT, OnButtonSwLimit)
	ON_BN_CLICKED(IDC_BUTTON_LIMIT_SWITCH, OnButtonLimitSwitch)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_CFG, OnButtonAxisCfg)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_PREV, OnButtonAxisPrev)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_NEXT, OnButtonAxisNext)
	ON_WM_TIMER()
	ON_BN_CLICKED(IDC_CHECK_TRAPEZOID, OnCheckTrapezoid)
	ON_BN_CLICKED(IDC_CHECK_SCURVE, OnCheckScurve)
	ON_BN_CLICKED(IDC_BUTTON_CYCLE_POS, OnButtonCyclePos)
	ON_BN_CLICKED(IDC_BUTTON_REPEAT, OnButtonRepeat)
	ON_BN_CLICKED(IDC_BUTTON_REPEAT_STOP, OnButtonRepeatStop)
	ON_BN_CLICKED(IDC_BUTTON_ESTOP, OnButtonEstop)
	ON_BN_CLICKED(IDC_BUTTON_STOP, OnButtonStop)
	ON_BN_CLICKED(IDC_BUTTON_RELOAD, OnButtonReload)
	ON_BN_CLICKED(IDC_BUTTON_PAUSE, OnButtonPause)
	ON_BN_CLICKED(IDC_BUTTON_AMP_ON, OnButtonAmpOn)
	ON_BN_CLICKED(IDC_BUTTON_AMP_OFF, OnButtonAmpOff)
	ON_BN_CLICKED(IDC_BUTTON_RESET, OnButtonReset)
	ON_BN_CLICKED(IDC_BUTTON_PAUSE_CLEAR, OnButtonPauseClear)
	ON_BN_CLICKED(IDC_BUTTON_CLEAR_STOP, OnButtonClearStop)
	ON_BN_CLICKED(IDC_BUTTON_CLEARALL, OnButtonClearall)
	ON_BN_CLICKED(IDC_BUTTON_FRAME_CLEAR, OnButtonFrameClear)
	ON_BN_CLICKED(IDC_BUTTON_GET_POSITION, OnButtonGetPosition)
	ON_BN_CLICKED(IDC_BUTTON_SET_POSITION, OnButtonSetPosition)
	ON_BN_CLICKED(IDC_BUTTON_CYCLE_POS2, OnButtonCyclePos2)
	ON_BN_CLICKED(IDC_BUTTON_DEBUG, OnButtonDebug)
	ON_BN_CLICKED(IDC_BUTTON_TQ_LIMIT_SET, OnButtonTqLimitSet)
	ON_BN_CLICKED(IDC_BUTTON_TQ_LIMIT_RESET, OnButtonTqLimitReset)
	//}}AFX_MSG_MAP
	ON_MESSAGE(WM_USER+10, OnJogPos)
	ON_MESSAGE(WM_USER+11, OnJogNeg)
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CActionPage message handlers

void CActionPage::OnButtonTunning() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	CParamTuning cTuning;
	cTuning.SetSheetPtr(m_lpSheetPtr);
	cTuning.SetAxis(this->m_uiAxis);
	cTuning.DoModal();
}

void CActionPage::OnButtonSwLimit() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	CSoftLimits cLimit;
	cLimit.SetSheetPtr(m_lpSheetPtr);
	cLimit.SetAxis(this->m_uiAxis);
	cLimit.DoModal();
}

void CActionPage::OnButtonLimitSwitch() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	CLimitSwitch cSwitch;
	cSwitch.SetSheetPtr(m_lpSheetPtr);
	cSwitch.SetAxis(this->m_uiAxis);
	cSwitch.DoModal();
}

void CActionPage::OnButtonAxisCfg() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	CAxisConfig cConfig;
	cConfig.SetSheetPtr(m_lpSheetPtr);
	cConfig.SetAxis(this->m_uiAxis);
	cConfig.DoModal();
}

BOOL CActionPage::OnInitDialog() 
{
	CPropertyPage::OnInitDialog();
	
	// TODO: Add extra initialization here
	SetInitialButtonState();
	SetAxisConfigurations(m_uiAxis);

	m_JogPos.SetWnd(this->m_hWnd, WM_USER+10);
	m_JogNeg.SetWnd(this->m_hWnd, WM_USER+11);

	SetTimer(0, 500, NULL);
	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}

void CActionPage::OnButtonAxisPrev() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int i = m_uiAxis;
	if (--i < 0) i = 0;
	m_uiAxis = (UINT) i;

	UpdateData(FALSE);

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	pSheet->SetAxisNum(m_uiAxis);

	SetAxisConfigurations(m_uiAxis);
}

void CActionPage::OnButtonAxisNext() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if (++m_uiAxis >= JPC_AXIS) m_uiAxis = JPC_AXIS-1;
	UpdateData(FALSE);

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	pSheet->SetAxisNum(m_uiAxis);

	SetAxisConfigurations(m_uiAxis);
}

void CActionPage::SetAxisConfigurations(int ax) 
{
	m_nAxis = ax;
}

BOOL CActionPage::OnKillActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	SetEvent(m_hShowStatusQuit);
	SetEvent(m_hMotionActionQuit);
	::WaitForSingleObject(m_hShowStatusStopped, 500);

	return CPropertyPage::OnKillActive();
}


void CActionPage::SetInitialButtonState()
{
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen == FALSE)
	{
		GetDlgItem(IDC_BUTTON_CYCLE_POS)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_REPEAT)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_REPEAT_STOP)->EnableWindow(FALSE);

		GetDlgItem(IDC_BUTTON_ESTOP)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_STOP)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_PAUSE)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_CLEAR)->EnableWindow(FALSE);

		GetDlgItem(IDC_BUTTON_AXIS_PREV)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_AXIS_NEXT)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_AMP_ON)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_AMP_OFF)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_RESET)->EnableWindow(FALSE);

		GetDlgItem(IDC_BUTTON_JOG_POS)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_JOG_NEG)->EnableWindow(FALSE);

		AfxMessageBox("장치가 열려있지 않아 동작을 실행 할 수 없습니다");
		return;
	}
	GetDlgItem(IDC_BUTTON_CYCLE_POS)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_REPEAT)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_REPEAT_STOP)->EnableWindow(TRUE);

	GetDlgItem(IDC_BUTTON_ESTOP)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_STOP)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_PAUSE)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_CLEAR)->EnableWindow(TRUE);

	GetDlgItem(IDC_BUTTON_AXIS_PREV)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_AXIS_NEXT)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_AMP_ON)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_AMP_OFF)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_RESET)->EnableWindow(TRUE);

	GetDlgItem(IDC_BUTTON_JOG_POS)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_JOG_NEG)->EnableWindow(TRUE);

	GetDlgItem(IDC_BUTTON_CYCLE_POS)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_CYCLE_POS2)->EnableWindow(FALSE);
	
}



static UINT _ShowStatus(LPVOID lpv)
{
	CActionPage *pPage = (CActionPage *) lpv;
	pPage->ShowStatus();
	AfxEndThread(0);

	return 0;
}


BOOL CActionPage::OnSetActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	// 상태를 표시하는 쓰레드를 실행한다.

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen == FALSE)
	{
		AfxMessageBox("장치가 열려있지 않아 동작을 실행 할 수 없습니다");
	}else {
		SetTimer(0, 500, NULL);
	}

	m_uiAxis = pSheet->GetAxisNum();

	SetAxisConfigurations(m_uiAxis);

	return CPropertyPage::OnSetActive();
}

void CActionPage::ShowStatus()
{
	CString cStr;
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;

	ResetEvent(m_hShowStatusQuit);
	ResetEvent(m_hShowStatusStopped);

	while (::WaitForSingleObject(m_hShowStatusQuit, 20) != WAIT_OBJECT_0)
	{
		if (pSheet->m_bDevOpen == FALSE) continue;

		int av;
		// Axis Statue
#ifndef MDF_FUNC	
		cStr = (in_sequence(m_nAxis) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AS_INSEQUENCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////
		cStr = (in_motion(m_nAxis) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AS_INMOTION)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////
		cStr	= (in_position(m_nAxis) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AS_INPOSITION)->SetWindowText(cStr);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
		int chk_errss, chk_errss1;
/////////////////////////////////////////////////////////////////////////////////////////
		cStr = (in_sequence(m_nAxis,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AS_INSEQUENCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////
		cStr = (in_motion(m_nAxis,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AS_INMOTION)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////
		cStr = (in_position(m_nAxis,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_AS_INPOSITION)->SetWindowText(cStr);
#endif

#ifndef MDF_FUNC
		cStr.Format("%d", frames_left(m_nAxis)); 
		GetDlgItem(IDC_EDIT_AS_FRAMES_LEFT)->SetWindowText(cStr);	
/////////////////////////////////////////////////////////////////////////////////////////		
		cStr = (axis_done(m_nAxis) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AS_AXISDONE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////
		cStr.Format("0x%04X", 0xffff & axis_source(m_nAxis));
		GetDlgItem(IDC_EDIT_AS_AXIS_SOURCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////
		cStr.Format("0x%02X", axis_state(m_nAxis));
		GetDlgItem(IDC_EDIT_AS_AXIS_STATE)->SetWindowText(cStr);	
		
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
		int tmp_as_v, tmp_as_e;
		int loop_i=0;
/////////////////////////////////////////////////////////////////////////////////////////		
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=frames_left(m_nAxis ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		cStr.Format("%d", tmp_as_v);
		GetDlgItem(IDC_EDIT_AS_FRAMES_LEFT)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////	
		cStr = (axis_done(m_nAxis) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AS_AXISDONE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v= 0xffff & axis_source(m_nAxis ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}		

		cStr.Format("0x%04X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AS_AXIS_SOURCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////////////
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=axis_state(m_nAxis ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		cStr.Format("0x%02X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AS_AXIS_STATE)->SetWindowText(cStr);
#endif

		double dPos;
		// Motion Status
		get_command(m_nAxis, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_MS_POS)->SetWindowText(cStr);

		get_counter(m_nAxis, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_MS_ACTUAL_POS)->SetWindowText(cStr);

		get_error(m_nAxis, &dPos);		cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_MS_ERROR_VALUE)->SetWindowText(cStr);


#ifndef MDF_FUNC	
		av = get_com_velocity(m_nAxis);	cStr.Format("%d", av);
		GetDlgItem(IDC_EDIT_MS_VELOCITY)->SetWindowText(cStr);
////////////////////////////////////////////////////////////////////////////
		av = get_act_velocity(m_nAxis);	cStr.Format("%d", av);
		GetDlgItem(IDC_EDIT_MS_ACTVELOCITY)->SetWindowText(cStr);
////////////////////////////////////////////////////////////////////////////
		int av1 = get_com_velocity_rpm(m_nAxis); 
		av = get_act_velocity_rpm(m_nAxis); cStr.Format("%d,%d", av1, av);// / 60.);
		GetDlgItem(IDC_EDIT_MS_RPM)->SetWindowText(cStr);

#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5유형 사용자open함수 원형 변경으로 인해 수정
		av = get_com_velocity(m_nAxis,&chk_errss);
		if(chk_errss ==MMC_OK)
		{
			cStr.Format("%d", av);
			GetDlgItem(IDC_EDIT_MS_VELOCITY)->SetWindowText(cStr);
		}
////////////////////////////////////////////////////////////////////////////
		av = get_act_velocity(m_nAxis,&chk_errss);
		if(chk_errss ==MMC_OK)
		{
			cStr.Format("%d", av);
			GetDlgItem(IDC_EDIT_MS_ACTVELOCITY)->SetWindowText(cStr);
		}
////////////////////////////////////////////////////////////////////////////

		int av1 = get_com_velocity_rpm(m_nAxis,&chk_errss);
		av = get_act_velocity_rpm(m_nAxis,&chk_errss1);

		if((chk_errss ==MMC_OK) && (chk_errss1 ==MMC_OK))
		{		
			cStr.Format("%d,%d", av1, av);// / 60.);
			GetDlgItem(IDC_EDIT_MS_RPM)->SetWindowText(cStr);
		}

#endif
		get_position(m_nAxis, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_MS_ENCODER_POS)->SetWindowText(cStr);

/*
		m_sASInSequence	= (in_sequence(m_nAxis) == TRUE) ? _T("YES") : _T("NO");
		m_sASInMotion	= (in_motion(m_nAxis) == TRUE) ? _T("YES") : _T("NO");
		m_sASInPosition	= (in_position(m_nAxis) == TRUE) ? _T("YES") : _T("NO");
		m_nASFramesLeft = frames_left(m_nAxis);
		m_sASAxisDone	= (axis_done(m_nAxis) == TRUE)  ? _T("YES") : _T("NO");
		m_sASAxisSource.Format("%04X", 0xffff & axis_source(m_nAxis));
		m_sASAxisState.Format("%02X", axis_state(m_nAxis));

		int av;
		double dPos;
		// Motion Status
		get_command(m_nAxis, &dPos);		m_fMSPos = dPos;
		get_counter(m_nAxis, &dPos);		m_fMSActualPos = dPos;
		get_error(m_nAxis, &dPos);			m_fMSErrorValue = dPos;
		av = get_com_velocity(m_nAxis);		m_fMSVelocity = av;
		get_position(m_nAxis, &dPos);		m_fMSEncoderPos = dPos;

		av = get_act_velocity(m_nAxis);
		m_sMSRpm.Format("%d", av * 60);
*/

		// Sensor & Switch status
		CButton *pBtn;

#ifndef MDF_FUNC
		pBtn = (CButton *) GetDlgItem(IDC_CHECK_HOME);		pBtn->SetCheck(home_switch(m_nAxis));
		pBtn = (CButton *) GetDlgItem(IDC_CHECK_POS_PLUS);	pBtn->SetCheck(pos_switch(m_nAxis));
		pBtn = (CButton *) GetDlgItem(IDC_CHECK_NEG_MINUS);	pBtn->SetCheck(neg_switch(m_nAxis));
//		pBtn = (CButton *) GetDlgItem(IDC_CHECK_NEG_MINUS);	pBtn->SetCheck(neg_switch(m_nAxis)); // 2.5.25v2.8.07통합 버젼 120120 syk,코드가 2개라서 주석처리함
		pBtn = (CButton *) GetDlgItem(IDC_CHECK_AMP_FAULT);	pBtn->SetCheck(amp_fault_switch(m_nAxis));		
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
		int select_tf, chk_err_cs;//tf -> truefalse

		pBtn = (CButton *) GetDlgItem(IDC_CHECK_HOME);
		select_tf = home_switch(m_nAxis,&chk_err_cs);
		if(chk_err_cs==MMC_OK)	pBtn->SetCheck(select_tf);
////////////////////////////////////////////////////////////////////////////////////////////////////
		pBtn = (CButton *) GetDlgItem(IDC_CHECK_POS_PLUS);
		select_tf = pos_switch(m_nAxis,&chk_err_cs);
		if(chk_err_cs==MMC_OK)	pBtn->SetCheck(select_tf);
////////////////////////////////////////////////////////////////////////////////////////////////////
		pBtn = (CButton *) GetDlgItem(IDC_CHECK_NEG_MINUS);
		select_tf = neg_switch(m_nAxis,&chk_err_cs);
		if(chk_err_cs==MMC_OK)	pBtn->SetCheck(select_tf);
////////////////////////////////////////////////////////////////////////////////////////////////////
//		pBtn = (CButton *) GetDlgItem(IDC_CHECK_NEG_MINUS);	pBtn->SetCheck(neg_switch(m_nAxis));// 2.5.25v2.8.07통합 버젼 120120 syk,코드가 2개라서 주석처리함
////////////////////////////////////////////////////////////////////////////////////////////////////
		pBtn = (CButton *) GetDlgItem(IDC_CHECK_AMP_FAULT);
		select_tf = amp_fault_switch(m_nAxis,&chk_err_cs);
		if(chk_err_cs==MMC_OK)	pBtn->SetCheck(select_tf);
#endif

		get_amp_enable(m_nAxis, &av);
		pBtn = (CButton *) GetDlgItem(IDC_CHECK_AMP_ENABLE); pBtn->SetCheck(av);

	}

	SetEvent(m_hShowStatusStopped);
}

void CActionPage::OnButtonGotoPos() 
{
	// TODO: Add your control notification handler code here
	
}

static UINT _MotionAction(LPVOID lpv)
{
	CActionPage *pPage = (CActionPage *) lpv;
	pPage->MotionAction();
	AfxEndThread(0);

	return 0;
}

void CActionPage::OnTimer(UINT nIDEvent) 
{
	// TODO: Add your message handler code here and/or call default
	if (nIDEvent == 0)
	{
		KillTimer(0);
		AfxBeginThread(_ShowStatus, this);
		AfxBeginThread(_MotionAction, this);
	}
	CPropertyPage::OnTimer(nIDEvent);
}

void CActionPage::OnCheckTrapezoid() 
{
	// TODO: Add your control notification handler code here
	SetCurveMode(0);
}

void CActionPage::OnCheckScurve() 
{
	// TODO: Add your control notification handler code here
	SetCurveMode(1);
}

void CActionPage::SetCurveMode(int ofs)
{
	m_bTrapezoidal = (ofs == 0) ? 1 : 0;
	m_bSCurve = (ofs == 1) ? 1 : 0;
	UpdateData(FALSE);
}

void CActionPage::OnButtonCyclePos() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	GetDlgItem(IDC_BUTTON_CYCLE_POS)->EnableWindow(FALSE);
	GetDlgItem(IDC_BUTTON_CYCLE_POS2)->EnableWindow(TRUE);
	
	m_nOneCycleDir = 0;
	RepeatAction(1);	// 1회 반복
}

void CActionPage::OnButtonCyclePos2() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	GetDlgItem(IDC_BUTTON_CYCLE_POS)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_CYCLE_POS2)->EnableWindow(FALSE);

	m_nOneCycleDir = 1;
	RepeatAction(1);	// 1회 반복
}

void CActionPage::OnButtonRepeat() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	RepeatAction(2);	// 무한히 반복
}

void CActionPage::OnButtonRepeatStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	RepeatAction(0);	// stop
}

void CActionPage::OnButtonEstop() 
{
	// TODO: Add your control notification handler code here
	set_e_stop(m_uiAxis);
}

void CActionPage::OnButtonStop() 
{
	// TODO: Add your control notification handler code here
	set_stop(m_uiAxis);
}

void CActionPage::OnButtonReload() 
{
	// TODO: Add your control notification handler code here
	reload_servopack_all();
}

void CActionPage::OnButtonPause() 
{
	// TODO: Add your control notification handler code here
	set_sw_pause(m_uiAxis, 1);
}

void CActionPage::OnButtonPauseClear() 
{
	// TODO: Add your control notification handler code here
	set_sw_pause(m_uiAxis, 0);
}

void CActionPage::OnButtonAmpOn() 
{
	// TODO: Add your control notification handler code here
	set_amp_enable(m_uiAxis, 1);
}

void CActionPage::OnButtonAmpOff() 
{
	// TODO: Add your control notification handler code here
	set_amp_enable(m_uiAxis, 0);
}

void CActionPage::OnButtonReset() 
{
	// TODO: Add your control notification handler code here
	amp_fault_reset(m_uiAxis);
	Sleep(500);
	amp_fault_set(m_uiAxis);
}


void CActionPage::OnButtonClearStop() 
{
	// TODO: Add your control notification handler code here
	clear_stop(m_uiAxis);
}

void CActionPage::RepeatAction(int ncmd)
{
	m_nRptCmd = ncmd;
}

void CActionPage::putinfo()
{
	char str[100];
	double dpos;

#ifndef MDF_FUNC
	for (int i = 0; i < 1; i ++)
	{
		get_counter(m_uiAxis, &dpos);
		sprintf(str, "in_seq=%d, in_mot=%d, mot_done=%d, inpos=%d, actpos=%6.f\n", 
			in_sequence(m_uiAxis), in_motion(m_uiAxis), in_position(m_uiAxis), motion_done(m_uiAxis), dpos);
		if (bDbgMsgWnd)
		{
			if (pConsole) pConsole->PutsConsole(str);
		}
	}	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_is_v, tmp_im_v, tmp_ip_v, tmp_md_v;
	int tmp_err;
	int loop_i=0;	
	
	get_counter(m_uiAxis, &dpos);
	tmp_md_v =motion_done(m_uiAxis);

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_is_v=in_sequence(m_uiAxis, &tmp_err);
		if(tmp_err==MMC_OK) break;
	}

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_im_v=in_motion(m_uiAxis, &tmp_err);
		if(tmp_err==MMC_OK) break;
	}

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_ip_v=in_position(m_uiAxis, &tmp_err);
		if(tmp_err==MMC_OK) break;
	}

	sprintf(str, "in_seq=%d, in_mot=%d, mot_done=%d, inpos=%d, actpos=%6.f\n", 
		tmp_is_v, tmp_im_v, tmp_ip_v, tmp_md_v , dpos);

	if (bDbgMsgWnd)
	{
		if (pConsole) pConsole->PutsConsole(str);
	}
	
#endif

}

void CActionPage::MotionAction()
{
	char str[300];
	ResetEvent(m_hMotionActionQuit);
	ResetEvent(m_hMotionActionStopped);

	while (::WaitForSingleObject(m_hMotionActionQuit, 100) != WAIT_OBJECT_0)
	{
		if (m_nRptCmd < 1) continue;

		if (m_nRptCmd == 1)
		{
			int MoveMode;

			if (m_bRelative == FALSE) 
				MoveMode = (m_bTrapezoidal ? TRAPEZOID : S_CURVE);
			else 
				MoveMode = (m_bTrapezoidal ? T_RELATIVE : S_RELATIVE);

			if (m_nOneCycleDir == 0)
			{
				if (PTP_Move(m_uiAxis, m_iPos2, m_uiVel, m_uiAccel, m_uiDecel, MoveMode) != MMC_OK)
				{
					char *pmsg = _error_message(get_local_error());
					sprintf(str, "위치2로의 이동명령에서 에러가 있습니다\r\n'%s'", pmsg);
					AfxMessageBox(str);
				} else {
					// same as "wait_for_done(ax)" buf check thread quit flag
					while (!axis_done(m_uiAxis))
					{
						if (::WaitForSingleObject(m_hMotionActionQuit, 100) == WAIT_OBJECT_0)
							break;
					}
				}
			} else {

				if (PTP_Move(m_uiAxis, m_iPos1, m_uiVel, m_uiAccel, m_uiDecel, MoveMode) != MMC_OK)
				{
					char *pmsg = _error_message(get_local_error());
					sprintf(str, "위치1로의 이동명령에서 에러가 있습니다\r\n'%s'", pmsg);
					AfxMessageBox(str);
				} else {
					// same as "wait_for_done(ax)" buf check thread quit flag
					while (!axis_done(m_uiAxis))
					{
						if (::WaitForSingleObject(m_hMotionActionQuit, 100) == WAIT_OBJECT_0) 
							break;
					}
				}
			}
			m_nRptCmd = 0;
		} else {
			int MoveMode;
			if (m_bRelative == FALSE) 
				MoveMode = (m_bTrapezoidal ? TRAPEZOID : S_CURVE);
			else 
				MoveMode = (m_bTrapezoidal ? T_RELATIVE : S_RELATIVE);

			int inPosition;
			inPosition	= GetPrivateProfileInt(GetSection(m_uiAxis), "IN_POSITION", 100000, GetParamPath());

			int iPos2 = m_iPos2;
			if (m_bPos2Rnd == TRUE) iPos2 = (100 + (rand() * 1000) % m_iPos2);
			if (PTP_Move(m_uiAxis, iPos2, m_uiVel, m_uiAccel, m_uiDecel, MoveMode) != MMC_OK) 
			{
				char *pmsg = _error_message(get_local_error());
				sprintf(str, "위치1로의 이동명령에서 에러가 있습니다\r\n'%s'", pmsg);
				AfxMessageBox(str);
				m_nRptCmd = 0;
			}
			else 
			{
				// same as "wait_for_done(ax)" buf check thread quit flag
				int cnt;

				cnt = 0;
				while (!axis_done(m_uiAxis))
				{
					if (::WaitForSingleObject(m_hMotionActionQuit, 20) == WAIT_OBJECT_0) 
						break;
					putinfo();
//					if (++cnt > 50) break;
				}
				double dPos;
				int err;
				get_counter(m_uiAxis, &dPos);
				err = abs((int)(iPos2 - dPos));//2011.10.10
				putinfo();
				if (err > inPosition)
				{
					char str[100];
					sprintf(str, "목표 pos2에 도달하지 못하고 axis_done()이 되었습니다 (err=%d)", err);
					m_nRptCmd = 0;
					AfxMessageBox(str);
				}

				if (dwell(m_uiAxis, m_uiDelay) != MMC_OK) m_nRptCmd = 0;
				else {
					if (PTP_Move(m_uiAxis, m_iPos1, m_uiVel, m_uiAccel, m_uiDecel, MoveMode) != MMC_OK)
					{
						char *pmsg = _error_message(get_local_error());
						sprintf(str, "위치1로의 이동명령에서 에러가 있습니다\r\n'%s'", pmsg);
						AfxMessageBox(str);
						m_nRptCmd = 0;
					} else {
						// same as "wait_for_done(ax)" buf check thread quit flag
						cnt = 0;
						while (!axis_done(m_uiAxis))
						{
							if (::WaitForSingleObject(m_hMotionActionQuit, 20) == WAIT_OBJECT_0) 
								break;
							putinfo();
	//						if (++cnt > 50) break;
						}

						get_counter(m_uiAxis, &dPos);
						err = abs((int)(m_iPos1 - dPos));//2011.10.10
						putinfo();
						if (err > inPosition)
						{
							char str[100];
							sprintf(str, "목표 pos1에 도달하지 못하고 axis_done()이 되었습니다 (err=%d)", err);
							m_nRptCmd = 0;
							AfxMessageBox(str);
						}
						if (dwell(m_uiAxis, m_uiDelay) != MMC_OK) m_nRptCmd = 0;
					}
				}
			}
		}
	}
}

LRESULT CActionPage::OnJogPos(WPARAM wParam, LPARAM lParam)
{
	UpdateData(TRUE);
	int err;
	char str[300];

	if (wParam == 0)	// pressed
		err = v_move(m_uiAxis, m_uiJogPS, m_uiAccel);
	else	// released
		err = v_move(m_uiAxis, 0, m_uiDecel);

	if (err != MMC_OK)
	{
		ReleaseCapture();
		char *pmsg = _error_message(get_local_error());
		sprintf(str, "위치1로의 이동명령에서 에러가 있습니다\r\n'%s'", pmsg);
		AfxMessageBox(str);
		Invalidate();
	}

	return 0;
}

LRESULT CActionPage::OnJogNeg(WPARAM wParam, LPARAM lParam)
{
	UpdateData(TRUE);
	int err;
	char str[300];

	double dvel = m_uiJogPS;
	dvel *= -1;

	if (wParam == 0)	// pressed
		err = v_move(m_uiAxis, dvel, m_uiAccel);
	else	// released
		err = v_move(m_uiAxis, 0, m_uiDecel);

	if (err != MMC_OK)
	{
		ReleaseCapture();
		char *pmsg = _error_message(get_local_error());
		sprintf(str, "위치1로의 이동명령에서 에러가 있습니다\r\n'%s'", pmsg);
		AfxMessageBox(str);
		Invalidate();
	}

	return 0;
}


void CActionPage::OnButtonClearall() 
{
	// TODO: Add your control notification handler code here
	clear_status(m_uiAxis);
}

void CActionPage::OnButtonFrameClear() 
{
	// TODO: Add your control notification handler code here
	frames_clear(m_uiAxis);
}

void CActionPage::OnButtonGetPosition() 
{
	// TODO: Add your control notification handler code here
	double dPos;
	get_position(m_uiAxis, &dPos);
	m_iEncoderPosition = (int)dPos;//2011.10.10
	UpdateData(FALSE);
}

void CActionPage::OnButtonSetPosition() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	double dPos = m_iEncoderPosition;
	set_position(m_uiAxis, dPos);
}

BOOL CActionPage::DestroyWindow() 
{
	// TODO: Add your specialized code here and/or call the base class
	SetEvent(m_hShowStatusQuit);
	::WaitForSingleObject(m_hShowStatusStopped, 500);

	return CPropertyPage::DestroyWindow();
}


void CActionPage::OnButtonDebug() 
{
	// TODO: Add your control notification handler code here
	if (bDbgMsgWnd == FALSE)
	{
		if (pConsole) delete pConsole;
		pConsole = new CConsole;
		pConsole->InitInstance("Action Page Msg. Window");
		bDbgMsgWnd = TRUE;
	} else {
		bDbgMsgWnd = FALSE;
		delete pConsole;
		pConsole = NULL;
	}
}

char *CActionPage::GetParamPath()
{
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;

	sprintf(m_strPath, "%s%s", "C:\\", AMC_PARAMETER_FILENAME);
	return m_strPath;
}


void CActionPage::OnButtonTqLimitSet() 
{
	// TODO: Add your control notification handler code here
	torque_limit(m_uiAxis, 1);
}

void CActionPage::OnButtonTqLimitReset() 
{
	// TODO: Add your control notification handler code here
	torque_limit(m_uiAxis, 0);
}
