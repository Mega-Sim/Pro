// Movex.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "Movex.h"
#include "MyPropertySheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

#define PULSETOMM   			        113.3736777	

/////////////////////////////////////////////////////////////////////////////
// CMovex property page

IMPLEMENT_DYNCREATE(CMovex, CPropertyPage)

CMovex::CMovex() : CPropertyPage(CMovex::IDD)
{
	//{{AFX_DATA_INIT(CMovex)
	m_uiAxis = 0;
	m_uiMaxSpeed = 8192*10;
	m_iStopPos = 8192*20;
	m_bAccMove = TRUE;
	m_bDivMove = FALSE;
	m_fAcc = 0.5f;
	m_fDcc = 0.5f;
	//}}AFX_DATA_INIT

	m_hQuitAll = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hStatusStopped = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hMovexStopped = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_sign = 1;

}

CMovex::~CMovex()
{
	SetEvent(m_hQuitAll);
	WaitForSingleObject(m_hStatusStopped, 2000);
	WaitForSingleObject(m_hMovexStopped, 2000);
}

void CMovex::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	DDX_Text(pDX, IDC_EDIT_AXIS, m_uiAxis);
	DDX_Text(pDX, IDC_EDIT_MOVEX_MAXSPEED, m_uiMaxSpeed);
	DDX_Text(pDX, IDC_EDIT_MOVEX_STOPPOS, m_iStopPos);
	DDX_Check(pDX, IDC_CHECK_ACCMOVE, m_bAccMove);
	DDX_Check(pDX, IDC_CHECK_DIVMOVE, m_bDivMove);
	DDX_Text(pDX, IDC_EDIT_MOVEX_ACC, m_fAcc);
	DDV_MinMaxFloat(pDX, m_fAcc, 1.e-003f, 4.f);
	DDX_Text(pDX, IDC_EDIT_MOVEX_DCC, m_fDcc);
	DDV_MinMaxFloat(pDX, m_fDcc, 1.e-003f, 4.f);
	DDX_Control(pDX, IDC_MOVEX_VIEW, m_cDataView);
}


BEGIN_MESSAGE_MAP(CMovex, CPropertyPage)
	//{{AFX_MSG_MAP(CMovex)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_PREV, OnButtonAxisPrev)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_NEXT, OnButtonAxisNext)
	ON_WM_TIMER()
	ON_BN_CLICKED(IDC_BUTTON_AMPON, OnButtonAmpon)
	ON_BN_CLICKED(IDC_BUTTON_AMPOFF, OnButtonAmpoff)
	ON_BN_CLICKED(IDC_BUTTON_PAUSE, OnButtonPause)
	ON_BN_CLICKED(IDC_BUTTON_PAUSECLEAR, OnButtonPauseclear)
	ON_BN_CLICKED(IDC_BUTTON_CLEARSTATUS, OnButtonClearstatus)
	ON_BN_CLICKED(IDC_BUTTON_MOVEP, OnButtonMovep)
	ON_BN_CLICKED(IDC_BUTTON_MOVEN, OnButtonMoven)
	ON_BN_CLICKED(IDC_BUTTON_MOVES, OnButtonMoves)
	ON_BN_CLICKED(IDC_BUTTON_MOVEDS, OnButtonMoveds)
	ON_BN_CLICKED(IDC_BUTTON_SETPOSITION_ZERO, OnButtonSetpositionZero)
	ON_BN_CLICKED(IDC_BUTTON_RUN, OnButtonRun)
	ON_BN_CLICKED(IDC_BUTTON_STOP, OnButtonStop)
	ON_BN_CLICKED(IDC_BUTTON_RUN_ONESHOT, OnButtonRunOneshot)
	ON_WM_MOUSEMOVE()
	ON_WM_SETFOCUS()
	ON_WM_SHOWWINDOW()
	ON_WM_KILLFOCUS()
	ON_WM_MOUSEWHEEL()
	ON_WM_PAINT()
	ON_BN_CLICKED(IDC_CHECK_ACCMOVE, OnCheckAccmove)
	ON_BN_CLICKED(IDC_CHECK_DIVMOVE, OnCheckDivmove)
	ON_BN_CLICKED(IDC_BUTTON_VIEW_HOLD, OnButtonViewHold)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CMovex message handlers

void CMovex::OnButtonAxisPrev() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int i = m_uiAxis;
	if (--i < 0) i = 0;
	m_uiAxis = (UINT) i;

	UpdateData(FALSE);

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;
	pSheet->SetAxisNum(m_uiAxis);
}

void CMovex::OnButtonAxisNext() 
{
	UpdateData(TRUE);
	if (++m_uiAxis >= JPC_AXIS) m_uiAxis = JPC_AXIS-1;
	UpdateData(FALSE);

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;
	pSheet->SetAxisNum(m_uiAxis);
}

void CMovex::SetAxisConfigurations(UINT uiAxis)
{

}


static UINT _ShowStatus(LPVOID lpv)
{
	CMovex *pPage = (CMovex *) lpv;
	pPage->ShowStatus();
	AfxEndThread(0);

	return 0;
}

static UINT _MovexAction(LPVOID lpv)
{
	CMovex *pPage = (CMovex *) lpv;
	pPage->MovexAction();
	AfxEndThread(0);

	return 0;
}



BOOL CMovex::OnSetActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	// 상태를 표시하는 쓰레드를 실행한다.

	m_bThreadRun = FALSE;

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;
	if (pSheet->m_bDevOpen == FALSE)
	{
		AfxMessageBox("장치가 열려있지 않아 동작을 실행 할 수 없습니다");
	}

	m_uiAxis = pSheet->GetAxisNum();
	UpdateData(FALSE);

	AfxBeginThread(_ShowStatus, this);
	AfxBeginThread(_MovexAction, this);

	SetTimer(0, 500, NULL);
	m_bThreadRun = TRUE;

	m_cDataView.SetXYMarkWnd(GetDlgItem(IDC_MOVEX_XSCALE), GetDlgItem(IDC_MOVEX_YSCALE));
	m_cDataView.SetFocus();

	return CPropertyPage::OnSetActive();
}

void CMovex::OnTimer(UINT nIDEvent) 
{
	// TODO: Add your message handler code here and/or call default
	if (nIDEvent == 0)
	{
		KillTimer(0);
		SetTimer(1, 100, NULL);
	} else if (nIDEvent == 1)
	{
		KillTimer(1);

		int nCurMax;
		nCurMax = m_cDataView.GetMax();
		m_cDataView.SetYMaxMin(nCurMax);
		m_cDataView.SetFocus();

	}
	CPropertyPage::OnTimer(nIDEvent);
}

void CMovex::ShowStatus()
{
	CString cStr;
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;

	ResetEvent(m_hQuitAll);
	while (::WaitForSingleObject(m_hQuitAll, 10) != WAIT_OBJECT_0)
	{
		if (pSheet->m_bDevOpen == FALSE) continue;

		int av;
		// Axis Statue
#ifndef MDF_FUNC	
		cStr = (in_sequence(m_uiAxis) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_INSEQUENCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		cStr = (in_motion(m_uiAxis) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_INMOTION)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		cStr	= (in_position(m_uiAxis) == TRUE) ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_INPOSITION)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		cStr = (axis_done(m_uiAxis) == TRUE)  ? _T("YES") : _T("NO");
		GetDlgItem(IDC_EDIT_AXISDONE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		cStr.Format("0x%04X", 0xffff & axis_source(m_uiAxis));
		GetDlgItem(IDC_EDIT_AXISSOURCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		cStr.Format("0x%02X", axis_state(m_uiAxis));
		GetDlgItem(IDC_EDIT_AXISSTATE)->SetWindowText(cStr);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
		int chk_errss;
		int tmp_as_v, tmp_as_e;
		int loop_i=0;
/////////////////////////////////////////////////////////////////////////////////
		cStr = (in_sequence(m_uiAxis,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_INSEQUENCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		cStr = (in_motion(m_uiAxis,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_INMOTION)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		cStr	= (in_position(m_uiAxis,&chk_errss) == TRUE) ? _T("YES") : _T("NO");
		if(chk_errss != MMC_OK) cStr = _T("ERROR");
		GetDlgItem(IDC_EDIT_INPOSITION)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		cStr = (axis_done(m_uiAxis) == TRUE)  ? _T("YES") : _T("NO");	//변경 없음
		GetDlgItem(IDC_EDIT_AXISDONE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=0xffff & axis_source(m_uiAxis ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}

		cStr.Format("0x%04X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AXISSOURCE)->SetWindowText(cStr);
/////////////////////////////////////////////////////////////////////////////////
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_as_v=axis_state(m_uiAxis ,&tmp_as_e);
			if(tmp_as_e==MMC_OK) break;
		}
		cStr.Format("0x%02X", tmp_as_v);
		GetDlgItem(IDC_EDIT_AXISSTATE)->SetWindowText(cStr);

#endif

		double dPos;
		// Position
		get_command(m_uiAxis, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_CURPOS)->SetWindowText(cStr);

		get_counter(m_uiAxis, &dPos);	cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_ACTUALPOS)->SetWindowText(cStr);

		get_error(m_uiAxis, &dPos);		cStr.Format("%.3f", dPos);
		GetDlgItem(IDC_EDIT_ERRORPOS)->SetWindowText(cStr);

#ifndef MDF_FUNC	
		av = get_com_velocity(m_uiAxis);	cStr.Format("%d", av);
		GetDlgItem(IDC_EDIT_COMVEL)->SetWindowText(cStr);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5유형 사용자open함수 원형 변경으로 인해 수정
		int chk_err_cs;
		av = get_com_velocity(m_uiAxis,&chk_err_cs);
		if(chk_err_cs ==MMC_OK)
		{
			cStr.Format("%d", av);
			GetDlgItem(IDC_EDIT_COMVEL)->SetWindowText(cStr);
		}
#endif
	}

	SetEvent(m_hStatusStopped);
}

BOOL CMovex::OnKillActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	SetEvent(m_hQuitAll);

	m_cDataView.KillFocus();

	return CPropertyPage::OnKillActive();
}

void CMovex::OnButtonAmpon() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	set_amp_enable(m_uiAxis, 1);
}

void CMovex::OnButtonAmpoff() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	set_amp_enable(m_uiAxis, 0);
}

void CMovex::OnButtonPause() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	set_stop(m_uiAxis);
}

void CMovex::OnButtonPauseclear() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	clear_stop(m_uiAxis);
}

void CMovex::OnButtonClearstatus() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	clear_status(m_uiAxis);
}

void CMovex::OnButtonMovep() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetMoveButtonStatus(BUTTON_MOVEP);

	m_sign = 1;

	double acc, vel;
	acc = m_fAcc;
	vel = m_uiMaxSpeed;
	if (m_bDivMove == 0) 
		move_pt(m_uiAxis, acc*1000, vel);
	else
		move_p(m_uiAxis, acc*PULSETOMM*1000, vel);
}

void CMovex::OnButtonMoven() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetMoveButtonStatus(BUTTON_MOVEN);

	m_sign = -1;

	double acc, vel;
	acc = m_fAcc;
	vel = m_uiMaxSpeed;

	int err = 0;
	if (m_bDivMove == 0) 
		err = move_nt(m_uiAxis, acc*1000, vel);
	else
		err = move_n(m_uiAxis, acc*PULSETOMM*1000, vel);		

	err += 0;
}

void CMovex::OnButtonMoves() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetMoveButtonStatus(BUTTON_MOVES);

	double dcc, vel;
	dcc = m_fDcc;
	vel = m_uiMaxSpeed;
	if (m_bDivMove == 0) 
		move_st(m_uiAxis, dcc*1000);
	else
		move_s(m_uiAxis, dcc*PULSETOMM*1000);		
}

void CMovex::OnButtonMoveds() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetMoveButtonStatus(BUTTON_MOVEDS);

	double acc, dcc, vel, pos;
	acc = m_fAcc;
	dcc = m_fDcc;
	vel = m_uiMaxSpeed;
	pos = m_iStopPos;

	int err = 0;
	if (m_bDivMove == 0) 
		err = move_dst(m_uiAxis, acc*1000, dcc*1000, vel, pos);
	else 
		err = move_ds(m_uiAxis, acc*PULSETOMM*1000, dcc*PULSETOMM*1000, vel, pos);	

	err += 0;
}

void CMovex::SetMoveButtonStatus(int curBtn)
{
	CButton *pBtn;
	switch (curBtn)
	{
	case BUTTON_MOVEP:
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEP); pBtn->EnableWindow(TRUE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEN); pBtn->EnableWindow(FALSE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVES); pBtn->EnableWindow(TRUE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEDS); pBtn->EnableWindow();
		break;

	case BUTTON_MOVEN:
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEP); pBtn->EnableWindow(FALSE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEN); pBtn->EnableWindow(TRUE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVES); pBtn->EnableWindow(TRUE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEDS); pBtn->EnableWindow(TRUE);
		break;

	case BUTTON_MOVES:
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEP); pBtn->EnableWindow(TRUE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEN); pBtn->EnableWindow(TRUE);
//		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVES); pBtn->EnableWindow(FALSE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEDS); pBtn->EnableWindow(TRUE);
		break;

	case BUTTON_MOVEDS:
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEP); pBtn->EnableWindow(TRUE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEN); pBtn->EnableWindow(TRUE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVES); pBtn->EnableWindow(TRUE);
		pBtn = (CButton *) GetDlgItem(IDC_BUTTON_MOVEDS); pBtn->EnableWindow(TRUE);
		break;

	}
}

void CMovex::OnButtonSetpositionZero() 
{
	// TODO: Add your control notification handler code here
	set_position(m_uiAxis, 0);
	set_command(m_uiAxis, 0);
	clear_amc_error();
	frames_clear(0);
}

void CMovex::OnButtonRun() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	m_bRun = TRUE;
}

void CMovex::OnButtonStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	m_bRun = FALSE;
}

void CMovex::MovexAction()
{
	int volt;
	int count;

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;

	m_bRun = FALSE;

	ResetEvent(m_hQuitAll);
	while (::WaitForSingleObject(m_hQuitAll, 0) != WAIT_OBJECT_0)
	{
		Sleep(10);
		if (pSheet->m_bDevOpen == FALSE) continue;

		get_dac_output(m_uiAxis, &volt);

		m_cDataView.SetData(-volt);

		if (m_bRun == TRUE)
		{
			double acc, dcc, vel, pos;
			acc = m_fAcc;
			dcc = m_fDcc;
			vel = m_uiMaxSpeed;
			pos = m_iStopPos;
			count = 0;

			// 동작을 원하는 축을 일단 멈추도록 하고, 멈출때까지 기다린다.
			move_s(m_uiAxis, vel / (double) 90);

			while (1)
			{
				Sleep(10);
				get_dac_output(m_uiAxis, &volt);

				m_cDataView.SetData(-volt);

				double accstep = vel / acc;
				double dccstep = vel / dcc;

				if (m_bDivMove)
				{
					accstep = vel / acc;
					dccstep = vel / dcc;

					if (count ++ == 0) move_ds(m_uiAxis, accstep, dccstep, vel, pos);	// 8192*50/70, 8192*50/70, 8192*12, 8192*2
					else if (count == 150) break;
					else if (::WaitForSingleObject(m_hQuitAll, 0) == WAIT_OBJECT_0) break;
				} else {
					accstep = acc;
					dccstep = dcc;

					if (count ++ == 0) move_dst(m_uiAxis, accstep, dccstep, vel, pos);	// 8192*50/70, 8192*50/70, 8192*12, 8192*2
					else if (count == 150) break;
					else if (::WaitForSingleObject(m_hQuitAll, 0) == WAIT_OBJECT_0) break;
				}
			}
		}

	}

	SetEvent(m_hMovexStopped);
}

void CMovex::OnButtonRunOneshot() 
{
	// TODO: Add your control notification handler code here
	GatheringTraceData_1();
	GatheringTraceData();
}

void CMovex::OnSetFocus(CWnd* pOldWnd) 
{
	CPropertyPage::OnSetFocus(pOldWnd);
	
	// TODO: Add your message handler code here
	
}

void CMovex::OnShowWindow(BOOL bShow, UINT nStatus) 
{
	CPropertyPage::OnShowWindow(bShow, nStatus);
	
	// TODO: Add your message handler code here
	
}

void CMovex::OnKillFocus(CWnd* pNewWnd) 
{
	CPropertyPage::OnKillFocus(pNewWnd);
	
	// TODO: Add your message handler code here
	
}

BOOL CMovex::OnMouseWheel(UINT nFlags, short zDelta, CPoint pt) 
{
	// TODO: Add your message handler code here and/or call default
	CWnd *pWnd = GetDlgItem(IDC_MOVEX_YSCALE);
	CRect rect;
	pWnd->GetWindowRect(&rect);

	if (rect.PtInRect(pt)) 
	{
		int nCurMax = m_cDataView.GetMax();
		if (zDelta < 0)
		{
			if (nCurMax > 2000) nCurMax -= 1000;
			else if (nCurMax > 1000) nCurMax -= 500;
			else if (nCurMax > 500) nCurMax -= 200;
			else if (nCurMax > 200) nCurMax -= 100;
			else if (nCurMax > 100) nCurMax -= 50;
			else nCurMax = 50;
		} else {
			if (nCurMax < 100) nCurMax += 50;
			else if (nCurMax < 200) nCurMax += 100;
			else if (nCurMax < 500) nCurMax += 200;
			else if (nCurMax < 1000) nCurMax += 500;
			else if (nCurMax > 32767) nCurMax = 32767;
			else nCurMax += 1000;
		}
		m_cDataView.SetYMaxMin(nCurMax);
		m_cDataView.SetFocus();
	}

	return CPropertyPage::OnMouseWheel(nFlags, zDelta, pt);
}

void CMovex::OnMouseMove(UINT nFlags, CPoint point) 
{
	// TODO: Add your message handler code here and/or call default
	CWnd *pWnd = GetDlgItem(IDC_MOVEX_VIEW);
	CRect rect;
	pWnd->GetWindowRect(&rect);
	ScreenToClient(&rect);

	CPropertyPage::OnMouseMove(nFlags, point);
}




void CMovex::OnPaint() 
{
	CPaintDC dc(this); // device context for painting
	
	// TODO: Add your message handler code here
	int nCurMax = m_cDataView.GetMax();
	m_cDataView.SetYMaxMin(nCurMax);
	m_cDataView.SetFocus();

	SetTimer(1, 500, NULL);
	// Do not call CPropertyPage::OnPaint() for painting messages
}

void CMovex::OnCheckAccmove() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	m_bAccMove = TRUE;
	m_bDivMove = FALSE;
	UpdateData(FALSE);
}

void CMovex::OnCheckDivmove() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	m_bDivMove = TRUE;
	m_bAccMove = FALSE;
	UpdateData(FALSE);
}

void CMovex::OnButtonViewHold() 
{
	// TODO: Add your control notification handler code here
	static BOOL bHold = FALSE;
	CWnd *pWnd = GetDlgItem(IDC_BUTTON_VIEW_HOLD);
	if (bHold == FALSE)
	{
		bHold = TRUE;
		pWnd->SetWindowText("Run");
	} else {
		bHold = FALSE;
		pWnd->SetWindowText("Hold");
	}
	m_cDataView.SetHold(bHold);
}
