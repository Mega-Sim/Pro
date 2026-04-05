// SoftLimits.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "SoftLimits.h"

#include "MyPropertySheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CSoftLimits dialog


CSoftLimits::CSoftLimits(CWnd* pParent /*=NULL*/)
	: CDialog(CSoftLimits::IDD, pParent)
{
	//{{AFX_DATA_INIT(CSoftLimits)
	m_uiAxis = 0;
	m_uiErrorLimit = 0;
	m_uiEStopRate = 0;
	m_fGearRatio = 0.0f;
	m_iHighestPos = 0x7fffffff;
	m_uiInPosition = 0;
	m_iLowestPos = 0x80000000;
	m_uiMaxAccel = 0;
	m_uiMaxVelocity = 0;
	m_uiStopRate = 0;
	m_fVTrackingFactor = 0.0f;
	m_nHPAbort = -1;
	m_nHPEStop = -1;
	m_nHPNoevent = 0;
	m_nHPStop = -1;
	m_nLPAbort = -1;
	m_nLPEStop = -1;
	m_nLPNoevent = 0;
	m_nLPStop = -1;
	m_iPulseRatio = 0;
	m_nELAbort = -1;
	m_nELEStop = -1;
	m_nELNoevent = -1;
	m_nELStop = -1;
	//}}AFX_DATA_INIT
}


void CSoftLimits::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CSoftLimits)
	DDX_Text(pDX, IDC_EDIT_AXIS, m_uiAxis);
	DDV_MinMaxUInt(pDX, m_uiAxis, 0, 3);
	DDX_Text(pDX, IDC_EDIT_ERROR_LIMIT, m_uiErrorLimit);
	DDX_Text(pDX, IDC_EDIT_ESTOP_RATE, m_uiEStopRate);
	DDV_MinMaxUInt(pDX, m_uiEStopRate, 1, 255);
	DDX_Text(pDX, IDC_EDIT_GEAR_RATIO, m_fGearRatio);
	DDX_Text(pDX, IDC_EDIT_HIGHEST_POS, m_iHighestPos);
	DDX_Text(pDX, IDC_EDIT_IN_POSITION, m_uiInPosition);
	DDX_Text(pDX, IDC_EDIT_LOWEST_POS, m_iLowestPos);
	DDX_Text(pDX, IDC_EDIT_MAX_ACCEL, m_uiMaxAccel);
	DDV_MinMaxUInt(pDX, m_uiMaxAccel, 1, 2000);
	DDX_Text(pDX, IDC_EDIT_MAX_VELOCITY, m_uiMaxVelocity);
	DDV_MinMaxUInt(pDX, m_uiMaxVelocity, 1, 819200);
	DDX_Text(pDX, IDC_EDIT_STOP_RATE, m_uiStopRate);
	DDV_MinMaxUInt(pDX, m_uiStopRate, 1, 255);
	DDX_Text(pDX, IDC_EDIT_VTRACKING_FACTOR, m_fVTrackingFactor);
	DDX_Radio(pDX, IDC_RADIO_HP_ABORT, m_nHPAbort);
	DDX_Radio(pDX, IDC_RADIO_HP_ESTOP, m_nHPEStop);
	DDX_Radio(pDX, IDC_RADIO_HP_NOEVENT, m_nHPNoevent);
	DDX_Radio(pDX, IDC_RADIO_HP_STOP, m_nHPStop);
	DDX_Radio(pDX, IDC_RADIO_LP_ABORT, m_nLPAbort);
	DDX_Radio(pDX, IDC_RADIO_LP_ESTOP, m_nLPEStop);
	DDX_Radio(pDX, IDC_RADIO_LP_NOEVENT, m_nLPNoevent);
	DDX_Radio(pDX, IDC_RADIO_LP_STOP, m_nLPStop);
	DDX_Text(pDX, IDC_EDIT_PULSE_RATIO, m_iPulseRatio);
	DDX_Radio(pDX, IDC_RADIO_EL_ABORT, m_nELAbort);
	DDX_Radio(pDX, IDC_RADIO_EL_ESTOP, m_nELEStop);
	DDX_Radio(pDX, IDC_RADIO_EL_NOEVENT, m_nELNoevent);
	DDX_Radio(pDX, IDC_RADIO_EL_STOP, m_nELStop);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CSoftLimits, CDialog)
	//{{AFX_MSG_MAP(CSoftLimits)
	ON_BN_CLICKED(IDC_BUTTON_SL_DSP_READ, OnButtonSlDspRead)
	ON_BN_CLICKED(IDC_BUTTON_SL_DSP_WRITE, OnButtonSlDspWrite)
	ON_BN_CLICKED(IDC_BUTTON_SL_INI_FLUSH, OnButtonSlIniFlush)
	ON_BN_CLICKED(IDC_BUTTON_SL_AXIS_PREV, OnButtonSlAxisPrev)
	ON_BN_CLICKED(IDC_BUTTON_SL_AXIS_NEXT, OnButtonSlAxisNext)
	ON_BN_CLICKED(IDC_RADIO_HP_NOEVENT, OnRadioHpNoevent)
	ON_BN_CLICKED(IDC_RADIO_HP_STOP, OnRadioHpStop)
	ON_BN_CLICKED(IDC_RADIO_HP_ESTOP, OnRadioHpEstop)
	ON_BN_CLICKED(IDC_RADIO_HP_ABORT, OnRadioHpAbort)
	ON_BN_CLICKED(IDC_RADIO_LP_NOEVENT, OnRadioLpNoevent)
	ON_BN_CLICKED(IDC_RADIO_LP_STOP, OnRadioLpStop)
	ON_BN_CLICKED(IDC_RADIO_LP_ESTOP, OnRadioLpEstop)
	ON_BN_CLICKED(IDC_RADIO_LP_ABORT, OnRadioLpAbort)
	ON_BN_CLICKED(IDC_RADIO_EL_NOEVENT, OnRadioElNoevent)
	ON_BN_CLICKED(IDC_RADIO_EL_STOP, OnRadioElStop)
	ON_BN_CLICKED(IDC_RADIO_EL_ESTOP, OnRadioElEstop)
	ON_BN_CLICKED(IDC_RADIO_EL_ABORT, OnRadioElAbort)
	ON_BN_CLICKED(IDC_BUTTON_CLOSE, OnButtonClose)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CSoftLimits message handlers

void CSoftLimits::OnButtonSlDspRead() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	double dLimit;
	int act;

	// Highest Pos, ST
	get_positive_sw_limit(m_uiAxis, &dLimit, &act);
	m_iHighestPos = (int)dLimit;//2011.10.10
	SetHPAction(act);

	// Lowest Pos. ST
	get_negative_sw_limit(m_uiAxis, &dLimit, &act);
	m_iLowestPos = (int)dLimit;//2011.10.10
	SetLPAction(act);

	// Error limit, ST
	get_error_limit(m_uiAxis, &dLimit, &act);
	m_uiErrorLimit = (int)dLimit;//2011.10.10
	SetELAction(act);

	// vel. limit
	get_vel_limit(m_uiAxis, &dLimit);
	m_uiMaxVelocity = (int)dLimit;//2011.10.10

	// acc. limit
	int iLimit;
	get_accel_limit(m_uiAxis, &iLimit);
	m_uiMaxAccel = iLimit;

	// in-position
	get_in_position(m_uiAxis, &dLimit);
	m_uiInPosition = (int)dLimit;//2011.10.10

	// stop-rate
	get_stop_rate(m_uiAxis, &iLimit);
	m_uiStopRate = (UINT) (iLimit & 0xff);

	// e-stop rate.
	get_e_stop_rate(m_uiAxis, &iLimit);
	m_uiEStopRate = iLimit;

	// gear ratio
	get_electric_gear(m_uiAxis, &dLimit);
	m_fGearRatio = (float)dLimit;//2011.10.10

	// Pulse ratio
	get_pulse_ratio(m_uiAxis, &iLimit);
	m_iPulseRatio = iLimit;

	// tracking factor
	char strVal[20];
	char strSection[20];
	sprintf(strSection, "AXIS_%d", m_uiAxis + 1);
	GetPrivateProfileString(strSection, "VTRACKING_FACTOR", "1.0", strVal, 20, GetParamPath());//"C:\\" AMC_PARAMETER_FILENAME);
	m_fVTrackingFactor = (float)atof(strVal);//2011.10.10

	UpdateData(FALSE);
}

void CSoftLimits::OnButtonSlDspWrite() 
{
	// TODO: Add your control notification handler code here
	double dLimit;
	int act;
	int iLimit;
	char strVal[20];
	char strSection[20];

	UpdateData(TRUE);

	// Highest Pos, ST
	dLimit = m_iHighestPos;
	act = GetHPAction();
	set_positive_sw_limit(m_uiAxis, dLimit, act);

	// Lowest Pos. ST
	dLimit = m_iLowestPos;
	act = GetLPAction();
	set_negative_sw_limit(m_uiAxis, dLimit, act);

	// Error limit, ST
	dLimit = m_uiErrorLimit;
	act = GetELAction();
	set_error_limit(m_uiAxis, dLimit, act);

	// vel. limit
	dLimit = m_uiMaxVelocity;
	set_vel_limit(m_uiAxis, dLimit);

	// acc. limit
	iLimit = m_uiMaxAccel;
	set_accel_limit(m_uiAxis, iLimit);

	// in-position
	dLimit = m_uiInPosition;
	set_in_position(m_uiAxis, dLimit);

	// stop-rate
	iLimit = m_uiStopRate;
	set_stop_rate(m_uiAxis, iLimit);

	// e-stop rate.
	iLimit = m_uiEStopRate;
	set_e_stop_rate(m_uiAxis, iLimit);

	// gear ratio
	dLimit = m_fGearRatio;
	set_electric_gear(m_uiAxis, dLimit);

	// Pulse ratio
	iLimit = m_iPulseRatio;
	set_pulse_ratio(m_uiAxis, iLimit);

	// tracking factor
	sprintf(strSection, "AXIS_%d", m_uiAxis + 1);
	sprintf(strVal, "%f", m_fVTrackingFactor);
	WritePrivateProfileString(strSection, "VTRACKING_FACTOR", strVal, GetParamPath());//"C:\\" AMC_PARAMETER_FILENAME);

	OnButtonSlIniFlush();
}

void CSoftLimits::OnButtonSlIniFlush() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	char szFile[100];
	char szSection[100];
	char szVal[100];

	strcpy(szFile, GetParamPath());
	sprintf(szSection, "AXIS_%d", m_uiAxis + 1);

	int act;

	// Highest Pos, ST
	act = GetHPAction();
	sprintf(szVal, " %d", m_iHighestPos);
	DWORD dwRtn = WritePrivateProfileString(szSection, "SWUPPER_LIMIT", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);
	sprintf(szVal, " %d", act);
	dwRtn = WritePrivateProfileString(szSection, "SWUPPER_LIMIT_ST", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// Lowest Pos. ST
	act = GetLPAction();
	sprintf(szVal, " %d", m_iLowestPos);
	dwRtn = WritePrivateProfileString(szSection, "SWLOWER_LIMIT", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);
	sprintf(szVal, " %d", act);
	dwRtn = WritePrivateProfileString(szSection, "SWLOWER_LIMIT_ST", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// Error limit, ST
	act = GetELAction();
	sprintf(szVal, " %d", m_uiErrorLimit);
	dwRtn = WritePrivateProfileString(szSection, "ERROR_LIMIT", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);
	sprintf(szVal, " %d", act);
	dwRtn = WritePrivateProfileString(szSection, "ERROR_LIMIT_ST", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// vel. limit
	sprintf(szVal, " %d", m_uiMaxVelocity);
	dwRtn = WritePrivateProfileString(szSection, "VEL_LIMIT", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// acc. limit
	if (m_uiMaxAccel < 1) m_uiMaxAccel = 1;
	else if (m_uiMaxAccel > 2000) m_uiMaxAccel = 2000;
	sprintf(szVal, " %d", m_uiMaxAccel);
	dwRtn = WritePrivateProfileString(szSection, "ACC_LIMIT", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// in-position
	sprintf(szVal, " %d", m_uiInPosition);
	dwRtn = WritePrivateProfileString(szSection, "IN_POSITION", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// stop-rate
	sprintf(szVal, " %d", m_uiStopRate);
	dwRtn = WritePrivateProfileString(szSection, "STOP_RATE", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// e-stop rate.
	sprintf(szVal, " %d", m_uiEStopRate);
	dwRtn = WritePrivateProfileString(szSection, "ESTOP_RATE", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// gear ratio
	sprintf(szVal, " %f", m_fGearRatio);
	dwRtn = WritePrivateProfileString(szSection, "GEAR_RATIO", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	// Pulse ratio
	sprintf(szVal, " %d", m_iPulseRatio);
	dwRtn = WritePrivateProfileString(szSection, "PULSE_RATIO", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	sprintf(szVal, "%f", m_fVTrackingFactor);
	dwRtn = WritePrivateProfileString(szSection, "VTRACKING_FACTOR", szVal, szFile);//"C:\\" AMC_PARAMETER_FILENAME);

	WritePrivateProfileStringW(NULL, NULL, NULL, L"C:\\AMCParam.ini");
}

char *CSoftLimits::GetParamPath()
{
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;

	sprintf(m_strPath, "%s%s", pSheet->m_szAbsPath, AMC_PARAMETER_FILENAME);
	return m_strPath;
}

void CSoftLimits::OnButtonSlAxisPrev() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int i = m_uiAxis;
	if (--i < 0) i = 0;
	m_uiAxis = (UINT) i;

	SetAxisConfigurations(m_uiAxis);
}

void CSoftLimits::OnButtonSlAxisNext() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if (++m_uiAxis >= JPC_AXIS) m_uiAxis = JPC_AXIS-1;
	UpdateData(FALSE);

	SetAxisConfigurations(m_uiAxis);
}

void CSoftLimits::OnRadioHpNoevent() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHPObject(0);
}

void CSoftLimits::OnRadioHpStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHPObject(1);
}

void CSoftLimits::OnRadioHpEstop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHPObject(2);
}

void CSoftLimits::OnRadioHpAbort() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHPObject(3);
}

void CSoftLimits::SetHPObject(int nOnOfs)
{
	m_nHPNoevent = (nOnOfs == 0) ? 0 : -1;
	m_nHPStop = (nOnOfs == 1) ? 0 : -1;
	m_nHPEStop = (nOnOfs == 2) ? 0 : -1;
	m_nHPAbort = (nOnOfs == 3) ? 0 : -1;
	UpdateData(FALSE);
}

void CSoftLimits::OnRadioLpNoevent() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetLPObject(0);
}

void CSoftLimits::OnRadioLpStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetLPObject(1);
}

void CSoftLimits::OnRadioLpEstop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetLPObject(2);
}

void CSoftLimits::OnRadioLpAbort() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetLPObject(3);
}

void CSoftLimits::OnRadioElNoevent() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetELObject(0);
}

void CSoftLimits::OnRadioElStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetELObject(1);
}

void CSoftLimits::OnRadioElEstop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetELObject(2);
}

void CSoftLimits::OnRadioElAbort() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetELObject(3);
}

void CSoftLimits::SetLPObject(int nOnOfs)
{
	m_nLPNoevent = (nOnOfs == 0) ? 0 : -1;
	m_nLPStop = (nOnOfs == 1) ? 0 : -1;
	m_nLPEStop = (nOnOfs == 2) ? 0 : -1;
	m_nLPAbort = (nOnOfs == 3) ? 0 : -1;
	UpdateData(FALSE);
}

void CSoftLimits::SetELObject(int nOnOfs)
{
	m_nELNoevent = (nOnOfs == 0) ? 0 : -1;
	m_nELStop = (nOnOfs == 1) ? 0 : -1;
	m_nELEStop = (nOnOfs == 2) ? 0 : -1;
	m_nELAbort = (nOnOfs == 3) ? 0 : -1;
	UpdateData(FALSE);
}

int CSoftLimits::GetHPAction()
{
	if (m_nHPNoevent == 0) return NO_EVENT;
	if (m_nHPStop == 0) return STOP_EVENT;
	if (m_nHPEStop == 0) return E_STOP_EVENT;
	if (m_nHPAbort == 0) return ABORT_EVENT;
	return NO_EVENT;
}

int CSoftLimits::GetLPAction()
{
	if (m_nLPNoevent == 0) return NO_EVENT;
	if (m_nLPStop == 0) return STOP_EVENT;
	if (m_nLPEStop == 0) return E_STOP_EVENT;
	if (m_nLPAbort == 0) return ABORT_EVENT;
	return NO_EVENT;
}

int CSoftLimits::GetELAction()
{
	if (m_nELNoevent == 0) return NO_EVENT;
	if (m_nELStop == 0) return STOP_EVENT;
	if (m_nELEStop == 0) return E_STOP_EVENT;
	if (m_nELAbort == 0) return ABORT_EVENT;
	return NO_EVENT;
}

void CSoftLimits::SetHPAction(int act)
{
	switch (act)
	{
	default:
	case NO_EVENT:		SetHPObject(0); break;
	case STOP_EVENT:	SetHPObject(1); break;
	case E_STOP_EVENT:	SetHPObject(2); break;
	case ABORT_EVENT:	SetHPObject(3); break;
	}
}

void CSoftLimits::SetLPAction(int act)
{
	switch (act)
	{
	default:
	case NO_EVENT:		SetLPObject(0); break;
	case STOP_EVENT:	SetLPObject(1); break;
	case E_STOP_EVENT:	SetLPObject(2); break;
	case ABORT_EVENT:	SetLPObject(3); break;
	}
}

void CSoftLimits::SetELAction(int act)
{
	switch (act)
	{
	default:
	case NO_EVENT:		SetELObject(0); break;
	case STOP_EVENT:	SetELObject(1); break;
	case E_STOP_EVENT:	SetELObject(2); break;
	case ABORT_EVENT:	SetELObject(3); break;
	}
}

BOOL CSoftLimits::OnInitDialog() 
{
	CDialog::OnInitDialog();
	
	// TODO: Add extra initialization here
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen == FALSE)
	{
		DisableSomeButtons();
		AfxMessageBox("장치가 초기화되지 않아 모든 기능을 사용할 수 없습니다");
	}

	SetAxisConfigurations(m_uiAxis);

	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}

void CSoftLimits::DisableSomeButtons()
{
	GetDlgItem(IDC_BUTTON_SL_DSP_READ)->EnableWindow(FALSE);
	GetDlgItem(IDC_BUTTON_SL_DSP_WRITE)->EnableWindow(FALSE);
}

void CSoftLimits::SetAxisConfigurations(int ax)
{
	char szStr[100];
	int st;

	GetPrivateProfileString(GetSection(ax), "SWUPPER_LIMIT", "2147483647", szStr, 20, GetParamPath());
	m_iHighestPos = atol(szStr);

	st = GetPrivateProfileInt(GetSection(ax), "SWUPPER_LIMIT_ST", 0, GetParamPath());
	SetHPAction( st );

	m_iLowestPos = GetPrivateProfileInt(GetSection(ax), "SWLOWER_LIMIT", 0x80000000, GetParamPath());
	st = GetPrivateProfileInt(GetSection(ax), "SWLOWER_LIMIT_ST", 0, GetParamPath()) ;
	SetLPAction( st );

	m_uiErrorLimit = GetPrivateProfileInt(GetSection(ax), "ERROR_LIMIT", 0x80000000, GetParamPath());
	st = GetPrivateProfileInt(GetSection(ax), "ERROR_LIMIT_ST", 0, GetParamPath()) ;
	SetELAction(st);

	m_uiMaxVelocity = GetPrivateProfileInt(GetSection(ax), "VEL_LIMIT", 100000, GetParamPath());
	m_uiMaxAccel	= GetPrivateProfileInt(GetSection(ax), "ACC_LIMIT", 100000, GetParamPath());
	m_uiInPosition	= GetPrivateProfileInt(GetSection(ax), "IN_POSITION", 100000, GetParamPath());
	m_uiStopRate	= GetPrivateProfileInt(GetSection(ax), "STOP_RATE", 100000, GetParamPath());
	m_uiEStopRate	= GetPrivateProfileInt(GetSection(ax), "ESTOP_RATE", 100000, GetParamPath());
	
	GetPrivateProfileString(GetSection(ax), "GEAR_RATIO", "1.0", szStr, 100, GetParamPath());
	m_fGearRatio = (float)atof(szStr);//2011.10.10

	m_iPulseRatio = GetPrivateProfileInt(GetSection(ax), "PULSE_RATIO", 1, GetParamPath());

	GetPrivateProfileString(GetSection(ax), "VTRACKING_FACTOR", "1.0", szStr, 10, GetParamPath());
	m_fVTrackingFactor = (float)atof(szStr);//2011.10.10

	// 화면을 갱신한다.
	UpdateData(FALSE);
}

void CSoftLimits::OnCancel() 
{
	// TODO: Add extra cleanup here
	CDialog::OnCancel();
}

void CSoftLimits::OnButtonClose() 
{
	// TODO: Add your control notification handler code here
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen) 
	{
		amc_adopt_ini_param();
		amc_save_local_sysparam_to_dsp();
		amc_flush_sysparam_to_eeprom();
	}
	CDialog::OnCancel();
}

void CSoftLimits::SetAxis(UINT uiAxis)
{
	m_uiAxis = uiAxis;
}
