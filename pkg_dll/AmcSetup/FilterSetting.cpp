// FilterSetting.cpp : implementation file
//

#include "stdafx.h"
#include "amcsetup.h"
#include "FilterSetting.h"

#include "MyPropertySheet.h"
#include "../include/amc_filter.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CFilterSetting property page

IMPLEMENT_DYNCREATE(CFilterSetting, CPropertyPage)

CFilterSetting::CFilterSetting() : CPropertyPage(CFilterSetting::IDD)
{
	//{{AFX_DATA_INIT(CFilterSetting)
	m_nPosLPF = 2000;
	m_nPosNotch = 10;
	m_nVelLPF = 2000;
	m_nVelNotch = 10;
	m_uiAxis = 0;

	m_nmonipercent3p3 = 10;		
	m_nmonipercent5p = 10;
	m_nmonipercent12p = 10;
	m_nmonipercent12m = 10;
	m_nmonipercentaxis0 = 10;
	m_nmonipercentaxis1 = 10;
	m_nmonipercentaxis2 = 10;
	m_nmonipercentaxis3 = 10;

	//}}AFX_DATA_INIT
	m_hsystemmoniQuit = CreateEvent(NULL, TRUE, FALSE, NULL);

}

CFilterSetting::~CFilterSetting()
{
	CloseHandle(m_hsystemmoniQuit);	
}

void CFilterSetting::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CFilterSetting)
	DDX_Text(pDX, IDC_EDIT_POS_LPF, m_nPosLPF);
	DDV_MinMaxUInt(pDX, m_nPosLPF, 0, 2000);
	DDX_Text(pDX, IDC_EDIT_POS_NOTCH, m_nPosNotch);
	DDV_MinMaxUInt(pDX, m_nPosNotch, 0, 2000);
	DDX_Text(pDX, IDC_EDIT_VEL_LPF, m_nVelLPF);
	DDV_MinMaxUInt(pDX, m_nVelLPF, 0, 2000);
	DDX_Text(pDX, IDC_EDIT_VEL_NOTCH, m_nVelNotch);
	DDV_MinMaxUInt(pDX, m_nVelNotch, 0, 2000);
	DDX_Text(pDX, IDC_EDIT_AXIS, m_uiAxis);
	DDV_MinMaxUInt(pDX, m_uiAxis, 0, 3);

	DDX_Text(pDX, IDC_EDIT_MONI_PERCENT_3P3, m_nmonipercent3p3);
	DDV_MinMaxUInt(pDX, m_nmonipercent3p3, 0, 100);
	DDX_Text(pDX, IDC_EDIT_MONI_PERCENT_5P, m_nmonipercent5p);
	DDV_MinMaxUInt(pDX, m_nmonipercent5p, 0, 100);
	DDX_Text(pDX, IDC_EDIT_MONI_PERCENT_12P, m_nmonipercent12p);
	DDV_MinMaxUInt(pDX, m_nmonipercent12p, 0, 100);
	DDX_Text(pDX, IDC_EDIT_MONI_PERCENT_12M, m_nmonipercent12m);
	DDV_MinMaxUInt(pDX, m_nmonipercent12m, 0, 100);
	DDX_Text(pDX, IDC_EDIT_MONI_PERCENT_AXIS0, m_nmonipercentaxis0);
	DDV_MinMaxUInt(pDX, m_nmonipercentaxis0, 0, 100);
	DDX_Text(pDX, IDC_EDIT_MONI_PERCENT_AXIS1, m_nmonipercentaxis1);
	DDV_MinMaxUInt(pDX, m_nmonipercentaxis1, 0, 100);
	DDX_Text(pDX, IDC_EDIT_MONI_PERCENT_AXIS2, m_nmonipercentaxis2);
	DDV_MinMaxUInt(pDX, m_nmonipercentaxis2, 0, 100);
	DDX_Text(pDX, IDC_EDIT_MONI_PERCENT_AXIS3, m_nmonipercentaxis3);
	DDV_MinMaxUInt(pDX, m_nmonipercentaxis3, 0, 100);


	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CFilterSetting, CPropertyPage)
	//{{AFX_MSG_MAP(CFilterSetting)
	ON_BN_CLICKED(IDC_BUTTON_POS_GET, OnButtonPosGet)
	ON_BN_CLICKED(IDC_BUTTON_POS_SET, OnButtonPosSet)
	ON_BN_CLICKED(IDC_BUTTON_VEL_GET, OnButtonVelGet)
	ON_BN_CLICKED(IDC_BUTTON_VEL_SET, OnButtonVelSet)
	ON_BN_CLICKED(IDC_BUTTON_PREV_AXIS, OnButtonPrevAxis)
	ON_BN_CLICKED(IDC_BUTTON_NEXT_AXIS, OnButtonNextAxis)

	ON_BN_CLICKED(IDC_BUTTON_MONI_ENABLE_SET, OnButtonMoniEnableSet)
	ON_BN_CLICKED(IDC_BUTTON_MONI_DISABLE_SET, OnButtonMoniDisableSet)
	ON_BN_CLICKED(IDC_BUTTON_MONI_ENABLE_GET, OnButtonMoniEnableGet)
	ON_BN_CLICKED(IDC_BUTTON_MONI_CUTOFF_SET, OnButtonMoniCutoffSet)
	ON_BN_CLICKED(IDC_BUTTON_MONI_CUTOFF_GET, OnButtonMoniCutoffGet)
	ON_BN_CLICKED(IDC_BUTTON_MONI_VALUE_GET, OnButtonMoniValueGet)

	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CFilterSetting message handlers

static UINT m_uimonistID[8] = 
{
	IDC_CHECK_MONI_ST_3P3, IDC_CHECK_MONI_ST_5P, IDC_CHECK_MONI_ST_12P, IDC_CHECK_MONI_ST_12M,
	IDC_CHECK_MONI_ST_AXIS0, IDC_CHECK_MONI_ST_AXIS1, IDC_CHECK_MONI_ST_AXIS2, IDC_CHECK_MONI_ST_AXIS3
};

static UINT m_uimonisysID[8] = 
{
	IDC_CHECK_MONI_SYS_3P3, IDC_CHECK_MONI_SYS_5P, IDC_CHECK_MONI_SYS_12P, IDC_CHECK_MONI_SYS_12M,
	IDC_CHECK_MONI_SYS_AXIS0, IDC_CHECK_MONI_SYS_AXIS1, IDC_CHECK_MONI_SYS_AXIS2, IDC_CHECK_MONI_SYS_AXIS3
};

void CFilterSetting::OnButtonPosGet() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	GetAxisConfigurations(m_uiAxis, 0);
}

void CFilterSetting::OnButtonPosSet() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAxisConfigurations(m_uiAxis, 0);
}

void CFilterSetting::OnButtonVelGet() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	GetAxisConfigurations(m_uiAxis, 1);
}

void CFilterSetting::OnButtonVelSet() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAxisConfigurations(m_uiAxis, 1);
}

int CFilterSetting::DoButtonCheck(UINT uiID)
{
	CButton *pBtn = (CButton*) GetDlgItem(uiID);
	int stt = pBtn->GetCheck();	

	return stt;
}

void CFilterSetting::OnButtonMoniEnableSet() 
{
	// TODO: Add your control notification handler code here
	for(int i=0; i<4; i++)
	{
		if(DoButtonCheck(m_uimonisysID[i]))	system_moni_enable(i, TRUE);
	}
	OnButtonMoniEnableGet(); 
}

void CFilterSetting::OnButtonMoniDisableSet() 
{
	// TODO: Add your control notification handler code here
	for(int i=0; i<4; i++)
	{
		if(DoButtonCheck(m_uimonisysID[i]))	system_moni_enable(i, FALSE);
	}
	OnButtonMoniEnableGet(); 
}

void CFilterSetting::OnButtonMoniEnableGet() 
{
	// TODO: Add your control notification handler code here
	char enable;
	CString cStr;

	get_system_moni_enable(&enable);

	for (int i = 0; i < 4; i ++)
	{
		if (enable & (1 << i))  cStr = _T("ON");
		else					cStr = _T("OFF");


		switch(i)
		{
			case 0 : 	GetDlgItem(IDC_EDIT_MONI_ONOFF_3P3)->SetWindowText(cStr); break;
			case 1 : 	GetDlgItem(IDC_EDIT_MONI_ONOFF_5P)->SetWindowText(cStr); break;
			case 2 : 	GetDlgItem(IDC_EDIT_MONI_ONOFF_12P)->SetWindowText(cStr); break;
			case 3 : 	GetDlgItem(IDC_EDIT_MONI_ONOFF_12M)->SetWindowText(cStr); break;
//			case 4 : 	GetDlgItem(IDC_EDIT_MONI_ONOFF_AXIS0)->SetWindowText(cStr); break;
//			case 5 : 	GetDlgItem(IDC_EDIT_MONI_ONOFF_AXIS1)->SetWindowText(cStr); break;
//			case 6 : 	GetDlgItem(IDC_EDIT_MONI_ONOFF_AXIS2)->SetWindowText(cStr); break;
//			case 7 : 	GetDlgItem(IDC_EDIT_MONI_ONOFF_AXIS3)->SetWindowText(cStr); break;
			default :	break;
		}
	}
}

void CFilterSetting::OnButtonMoniCutoffSet() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if(DoButtonCheck(m_uimonisysID[0]))	set_monitering_Threshold_percent(0, (char)m_nmonipercent3p3);
	if(DoButtonCheck(m_uimonisysID[1]))	set_monitering_Threshold_percent(1, (char)m_nmonipercent5p);
	if(DoButtonCheck(m_uimonisysID[2]))	set_monitering_Threshold_percent(2, (char)m_nmonipercent12p);
	if(DoButtonCheck(m_uimonisysID[3]))	set_monitering_Threshold_percent(3, (char)m_nmonipercent12m);
//	if(DoButtonCheck(m_uimonisysID[4]))	set_monitering_Threshold_percent(4, (char)m_nmonipercentaxis0);
//	if(DoButtonCheck(m_uimonisysID[5]))	set_monitering_Threshold_percent(5, (char)m_nmonipercentaxis1);
//	if(DoButtonCheck(m_uimonisysID[6]))	set_monitering_Threshold_percent(6, (char)m_nmonipercentaxis2);
//	if(DoButtonCheck(m_uimonisysID[7]))	set_monitering_Threshold_percent(7, (char)m_nmonipercentaxis3);
}

void CFilterSetting::OnButtonMoniCutoffGet() 
{
	char pcnt;
	// TODO: Add your control notification handler code here
	if(DoButtonCheck(m_uimonisysID[0]))
	{
		get_monitering_Threshold_percent(0, &pcnt);
		m_nmonipercent3p3 = (int)pcnt;
	}
	if(DoButtonCheck(m_uimonisysID[1]))
	{
		get_monitering_Threshold_percent(1, &pcnt);
		m_nmonipercent5p = (int)pcnt;
	}
	if(DoButtonCheck(m_uimonisysID[2]))
	{
		get_monitering_Threshold_percent(2, &pcnt);
		m_nmonipercent12p = (int)pcnt;
	}
	if(DoButtonCheck(m_uimonisysID[3]))
	{
		get_monitering_Threshold_percent(3, &pcnt);
		m_nmonipercent12m = (int)pcnt;
	}
#if 0
	if(DoButtonCheck(m_uimonisysID[4]))
	{
		get_monitering_Threshold_percent(4, &pcnt);
		m_nmonipercentaxis0 = (int)pcnt;
	}
	if(DoButtonCheck(m_uimonisysID[5]))
	{
		get_monitering_Threshold_percent(5, &pcnt);
		m_nmonipercentaxis1 = (int)pcnt;
	}
	if(DoButtonCheck(m_uimonisysID[6]))
	{
		get_monitering_Threshold_percent(6, &pcnt);
		m_nmonipercentaxis2 = (int)pcnt;
	}
	if(DoButtonCheck(m_uimonisysID[7]))
	{
		get_monitering_Threshold_percent(7, &pcnt);
		m_nmonipercentaxis3 = (int)pcnt;
	}
#endif
	UpdateData(FALSE);	
}

void CFilterSetting::OnButtonMoniValueGet() 
{
	// TODO: Add your control notification handler code here
	int value;
	int rvalue;
	int cvalue;
	CString cStr;

	if(DoButtonCheck(m_uimonisysID[0]))
	{
		get_system_monitering_value(0, &value, &rvalue, &cvalue);

		cStr.Format("%.2f", (float)value/100);
		GetDlgItem(IDC_EDIT_MONI_VALUE_3P3)->SetWindowText(cStr);
		cStr.Format("%d", rvalue);
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_3P3)->SetWindowText(cStr);
	}
	else
	{
		cStr = _T("Check");
		GetDlgItem(IDC_EDIT_MONI_VALUE_3P3)->SetWindowText(cStr);
		cStr = _T("Source");
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_3P3)->SetWindowText(cStr);
	}

	if(DoButtonCheck(m_uimonisysID[1]))
	{
		get_system_monitering_value(1, &value, &rvalue, &cvalue);

		cStr.Format("%.2f", (float)value/100);
		GetDlgItem(IDC_EDIT_MONI_VALUE_5P)->SetWindowText(cStr);
		cStr.Format("%d", rvalue);
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_5P)->SetWindowText(cStr);		
	}
	else
	{
		cStr = _T("Check");
		GetDlgItem(IDC_EDIT_MONI_VALUE_5P)->SetWindowText(cStr);
		cStr = _T("Source");
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_5P)->SetWindowText(cStr);
	}

	if(DoButtonCheck(m_uimonisysID[2]))
	{
		get_system_monitering_value(2, &value, &rvalue, &cvalue);

		cStr.Format("%.2f", (float)value/100);
		GetDlgItem(IDC_EDIT_MONI_VALUE_12P)->SetWindowText(cStr);
		cStr.Format("%d", rvalue);
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_12P)->SetWindowText(cStr);
	}
	else
	{
		cStr = _T("Check");
		GetDlgItem(IDC_EDIT_MONI_VALUE_12P)->SetWindowText(cStr);
		cStr = _T("Source");
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_12P)->SetWindowText(cStr);
	}

	if(DoButtonCheck(m_uimonisysID[3]))
	{
		get_system_monitering_value(3, &value, &rvalue, &cvalue);

		cStr.Format("%.2f", (float)value/100);
		GetDlgItem(IDC_EDIT_MONI_VALUE_12M)->SetWindowText(cStr);
		cStr.Format("%d", rvalue);
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_12M)->SetWindowText(cStr);
	}
	else
	{
		cStr = _T("Check");
		GetDlgItem(IDC_EDIT_MONI_VALUE_12M)->SetWindowText(cStr);
		cStr = _T("Source");
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_12M)->SetWindowText(cStr);
	}

	if(DoButtonCheck(m_uimonisysID[4]))
	{
		get_system_monitering_value(4, &value, &rvalue, &cvalue);

		cStr.Format("%d", value);
		GetDlgItem(IDC_EDIT_MONI_VALUE_AXIS0)->SetWindowText(cStr);
		cStr.Format("%d", rvalue);
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_AXIS0)->SetWindowText(cStr);
		cStr.Format("%d", cvalue);
		GetDlgItem(IDC_EDIT_MONI_CPA_VALUE_AXIS0)->SetWindowText(cStr);
	}
	else
	{
		cStr = _T("Check");
		GetDlgItem(IDC_EDIT_MONI_VALUE_AXIS0)->SetWindowText(cStr);
		cStr = _T("Source");
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_AXIS0)->SetWindowText(cStr);
		cStr = _T("...");
		GetDlgItem(IDC_EDIT_MONI_CPA_VALUE_AXIS0)->SetWindowText(cStr);
	}

	if(DoButtonCheck(m_uimonisysID[5]))
	{
		get_system_monitering_value(5, &value, &rvalue, &cvalue);
		cStr.Format("%d", value);
		GetDlgItem(IDC_EDIT_MONI_VALUE_AXIS1)->SetWindowText(cStr);
		cStr.Format("%d", rvalue);
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_AXIS1)->SetWindowText(cStr);
		cStr.Format("%d", cvalue);
		GetDlgItem(IDC_EDIT_MONI_CPA_VALUE_AXIS1)->SetWindowText(cStr);
	}
	else
	{
		cStr = _T("Check");
		GetDlgItem(IDC_EDIT_MONI_VALUE_AXIS1)->SetWindowText(cStr);
		cStr = _T("Source");
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_AXIS1)->SetWindowText(cStr);
		cStr = _T("...");
		GetDlgItem(IDC_EDIT_MONI_CPA_VALUE_AXIS1)->SetWindowText(cStr);
	}

	if(DoButtonCheck(m_uimonisysID[6]))
	{
		get_system_monitering_value(6, &value, &rvalue, &cvalue);
		cStr.Format("%d", value);
		GetDlgItem(IDC_EDIT_MONI_VALUE_AXIS2)->SetWindowText(cStr);
		cStr.Format("%d", rvalue);
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_AXIS2)->SetWindowText(cStr);
		cStr.Format("%d", cvalue);
		GetDlgItem(IDC_EDIT_MONI_CPA_VALUE_AXIS2)->SetWindowText(cStr);
	}
	else
	{
		cStr = _T("Check");
		GetDlgItem(IDC_EDIT_MONI_VALUE_AXIS2)->SetWindowText(cStr);
		cStr = _T("Source");
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_AXIS2)->SetWindowText(cStr);
		cStr = _T("...");
		GetDlgItem(IDC_EDIT_MONI_CPA_VALUE_AXIS2)->SetWindowText(cStr);
	}

	if(DoButtonCheck(m_uimonisysID[7]))
	{
		get_system_monitering_value(7, &value, &rvalue, &cvalue);
		cStr.Format("%d", value);
		GetDlgItem(IDC_EDIT_MONI_VALUE_AXIS3)->SetWindowText(cStr);
		cStr.Format("%d", rvalue);
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_AXIS3)->SetWindowText(cStr);
		cStr.Format("%d", cvalue);
		GetDlgItem(IDC_EDIT_MONI_CPA_VALUE_AXIS3)->SetWindowText(cStr);
	}
	else
	{
		cStr = _T("Check");
		GetDlgItem(IDC_EDIT_MONI_VALUE_AXIS3)->SetWindowText(cStr);
		cStr = _T("Source");
		GetDlgItem(IDC_EDIT_MONI_RAW_VALUE_AXIS3)->SetWindowText(cStr);
		cStr = _T("...");
		GetDlgItem(IDC_EDIT_MONI_CPA_VALUE_AXIS3)->SetWindowText(cStr);
	}
//	UpdateData(FALSE);	
}

BOOL CFilterSetting::OnKillActive() 
{
	// TODO: Add your specialized code here and/or call the base class

	SetEvent(m_hsystemmoniQuit);
	
	return CPropertyPage::OnKillActive();
}

UINT _SystemMonitoring(LPVOID lpv)
{
	CFilterSetting *pTest = (CFilterSetting*) lpv;
	pTest->SystemMonitoring();
	AfxEndThread(0);

	return 0L;

}

BOOL CFilterSetting::OnSetActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen == FALSE)
	{
		AfxMessageBox("장치가 열려있지 않아 정확한 동작을 실행 할 수 없습니다");
	} else {

		ResetEvent(m_hsystemmoniQuit);
		m_uiAxis = pSheet->GetAxisNum();
		GetAxisConfigurations(m_uiAxis, 0);
		GetAxisConfigurations(m_uiAxis, 1);
		AfxBeginThread(_SystemMonitoring, this);
	}

	OnButtonMoniEnableGet();
	OnButtonMoniValueGet();
	return CPropertyPage::OnSetActive();
}

void CFilterSetting::OnButtonPrevAxis() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int i = m_uiAxis;
	if (--i < 0) i = 0;
	m_uiAxis = (UINT) i;

	UpdateData(FALSE);

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	pSheet->SetAxisNum(m_uiAxis);

	GetAxisConfigurations(m_uiAxis, 0);
	GetAxisConfigurations(m_uiAxis, 1);
}

void CFilterSetting::OnButtonNextAxis() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if (++m_uiAxis >= JPC_AXIS) m_uiAxis = JPC_AXIS-1;
	UpdateData(FALSE);

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	pSheet->SetAxisNum(m_uiAxis);

	GetAxisConfigurations(m_uiAxis, 0);
	GetAxisConfigurations(m_uiAxis, 1);
}

void CFilterSetting::SetAxisConfigurations(int ax, int mode)
{
	if (mode == 0)
	{
		set_position_lpf(ax, m_nPosLPF);
		set_position_notch_filter(ax, m_nPosNotch);
		GetAxisConfigurations(ax, 0);
	} else {
		set_velocity_lpf(ax, m_nVelLPF);
		set_velocity_notch_filter(ax, m_nVelNotch);
		GetAxisConfigurations(ax, 1);
	}
}

void CFilterSetting::GetAxisConfigurations(int ax, int mode)
{
	if (mode == 0)
	{
		get_position_lpf(ax, (int *) &m_nPosLPF);
		get_position_notch_filter(ax, (int *) &m_nPosNotch);
	} else {
		get_velocity_lpf(ax, (int *) &m_nVelLPF);
		get_velocity_notch_filter(ax, (int *) &m_nVelNotch);
	}
	UpdateData(FALSE);
}


void CFilterSetting::SystemMonitoring()
{
	char status;
	UINT id;
	int val;
	int stt;
	
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen == FALSE)
	{
//		AfxMessageBox("장치가 열려있지 않아 상태를 확인할 수 없습니다");
		return;
	}

	while (::WaitForSingleObject(m_hsystemmoniQuit, 10 != WAIT_OBJECT_0))
	{
		system_status(&status);
		
		for (int i = 0; i < 8; i ++)
		{
			id = m_uimonistID[i];
			val = 0;
			if (status & (1 << i)) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);

			CButton *pBtn1 = (CButton*) GetDlgItem(IDC_CHECK_MONI_AUTO);
			stt = pBtn1->GetCheck();	

			if(stt)
			{
				OnButtonMoniValueGet();
			}
		}
		Sleep(1);
	}
	SetEvent(m_hsystemmoniQuit);

}