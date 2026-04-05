// LimitSwitch.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "LimitSwitch.h"

#include "MyPropertySheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CLimitSwitch dialog


CLimitSwitch::CLimitSwitch(CWnd* pParent /*=NULL*/)
	: CDialog(CLimitSwitch::IDD, pParent)
{
	//{{AFX_DATA_INIT(CLimitSwitch)
	m_uiAxis = 0;
	m_bAEHighActive = FALSE;
	m_bAELowActive = TRUE;
	m_bAFHighActive = FALSE;
	m_bAFLowActive = TRUE;
	m_bARHighActive = FALSE;
	m_bARLowActive = TRUE;
	m_bHSHighActive = FALSE;
	m_bHSLowActive = TRUE;
	m_bIPHighActive = FALSE;
	m_bIPLowActive = TRUE;
	m_bNLHighActive = FALSE;
	m_bNLLowActive = TRUE;
	m_bPLHighActive = FALSE;
	m_bPLLowActive = TRUE;
	m_iAFAbort = -1;
	m_iAFEStop = -1;
	m_iAFNoevent = 0;
	m_iAFStop = -1;
	m_iHSAbort = -1;
	m_iHSEStop = -1;
	m_iHSNoevent = 0;
	m_iHSStop = -1;
	m_iNLAbort = -1;
	m_iNLEStop = -1;
	m_iNLNoevent = 0;
	m_iNLStop = -1;
	m_iPLAbort = -1;
	m_iPLEStop = -1;
	m_iPLNoevent = 0;
	m_iPLStop = -1;
	//}}AFX_DATA_INIT
}


void CLimitSwitch::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CLimitSwitch)
	DDX_Text(pDX, IDC_EDIT_AXIS, m_uiAxis);
	DDX_Check(pDX, IDC_CHECK_AE_HIGHACTIVE, m_bAEHighActive);
	DDX_Check(pDX, IDC_CHECK_AE_LOWACTIVE, m_bAELowActive);
	DDX_Check(pDX, IDC_CHECK_AF_HIGHACTIVE, m_bAFHighActive);
	DDX_Check(pDX, IDC_CHECK_AF_LOWACTIVE, m_bAFLowActive);
	DDX_Check(pDX, IDC_CHECK_AR_HIGHACTIVE, m_bARHighActive);
	DDX_Check(pDX, IDC_CHECK_AR_LOWACTIVE, m_bARLowActive);
	DDX_Check(pDX, IDC_CHECK_HS_HIGHACTIVE, m_bHSHighActive);
	DDX_Check(pDX, IDC_CHECK_HS_LOWACTIVE, m_bHSLowActive);
	DDX_Check(pDX, IDC_CHECK_IP_HIGHACTIVE, m_bIPHighActive);
	DDX_Check(pDX, IDC_CHECK_IP_LOWACTIVE, m_bIPLowActive);
	DDX_Check(pDX, IDC_CHECK_NL_HIGHACTIVE, m_bNLHighActive);
	DDX_Check(pDX, IDC_CHECK_NL_LOWACTIVE, m_bNLLowActive);
	DDX_Check(pDX, IDC_CHECK_PL_HIGHACTIVE, m_bPLHighActive);
	DDX_Check(pDX, IDC_CHECK_PL_LOWACTIVE, m_bPLLowActive);
	DDX_Radio(pDX, IDC_RADIO_AF_ABORT, m_iAFAbort);
	DDX_Radio(pDX, IDC_RADIO_AF_ESTOP, m_iAFEStop);
	DDX_Radio(pDX, IDC_RADIO_AF_NOEVENT, m_iAFNoevent);
	DDX_Radio(pDX, IDC_RADIO_AF_STOP, m_iAFStop);
	DDX_Radio(pDX, IDC_RADIO_HS_ABORT, m_iHSAbort);
	DDX_Radio(pDX, IDC_RADIO_HS_ESTOP, m_iHSEStop);
	DDX_Radio(pDX, IDC_RADIO_HS_NOEVENT, m_iHSNoevent);
	DDX_Radio(pDX, IDC_RADIO_HS_STOP, m_iHSStop);
	DDX_Radio(pDX, IDC_RADIO_NL_ABORT, m_iNLAbort);
	DDX_Radio(pDX, IDC_RADIO_NL_ESTOP, m_iNLEStop);
	DDX_Radio(pDX, IDC_RADIO_NL_NOEVENT, m_iNLNoevent);
	DDX_Radio(pDX, IDC_RADIO_NL_STOP, m_iNLStop);
	DDX_Radio(pDX, IDC_RADIO_PL_ABORT, m_iPLAbort);
	DDX_Radio(pDX, IDC_RADIO_PL_ESTOP, m_iPLEStop);
	DDX_Radio(pDX, IDC_RADIO_PL_NOEVENT, m_iPLNoevent);
	DDX_Radio(pDX, IDC_RADIO_PL_STOP, m_iPLStop);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CLimitSwitch, CDialog)
	//{{AFX_MSG_MAP(CLimitSwitch)
	ON_BN_CLICKED(IDC_RADIO_PL_NOEVENT, OnRadioPlNoevent)
	ON_BN_CLICKED(IDC_RADIO_PL_STOP, OnRadioPlStop)
	ON_BN_CLICKED(IDC_RADIO_PL_ESTOP, OnRadioPlEstop)
	ON_BN_CLICKED(IDC_RADIO_PL_ABORT, OnRadioPlAbort)
	ON_BN_CLICKED(IDC_CHECK_PL_HIGHACTIVE, OnCheckPlHighactive)
	ON_BN_CLICKED(IDC_CHECK_PL_LOWACTIVE, OnCheckPlLowactive)
	ON_BN_CLICKED(IDC_RADIO_NL_NOEVENT, OnRadioNlNoevent)
	ON_BN_CLICKED(IDC_RADIO_NL_STOP, OnRadioNlStop)
	ON_BN_CLICKED(IDC_RADIO_NL_ESTOP, OnRadioNlEstop)
	ON_BN_CLICKED(IDC_RADIO_NL_ABORT, OnRadioNlAbort)
	ON_BN_CLICKED(IDC_CHECK_NL_HIGHACTIVE, OnCheckNlHighactive)
	ON_BN_CLICKED(IDC_CHECK_NL_LOWACTIVE, OnCheckNlLowactive)
	ON_BN_CLICKED(IDC_RADIO_HS_NOEVENT, OnRadioHsNoevent)
	ON_BN_CLICKED(IDC_RADIO_HS_STOP, OnRadioHsStop)
	ON_BN_CLICKED(IDC_RADIO_HS_ESTOP, OnRadioHsEstop)
	ON_BN_CLICKED(IDC_RADIO_HS_ABORT, OnRadioHsAbort)
	ON_BN_CLICKED(IDC_CHECK_HS_HIGHACTIVE, OnCheckHsHighactive)
	ON_BN_CLICKED(IDC_CHECK_HS_LOWACTIVE, OnCheckHsLowactive)
	ON_BN_CLICKED(IDC_RADIO_AF_NOEVENT, OnRadioAfNoevent)
	ON_BN_CLICKED(IDC_RADIO_AF_STOP, OnRadioAfStop)
	ON_BN_CLICKED(IDC_RADIO_AF_ESTOP, OnRadioAfEstop)
	ON_BN_CLICKED(IDC_RADIO_AF_ABORT, OnRadioAfAbort)
	ON_BN_CLICKED(IDC_CHECK_AF_HIGHACTIVE, OnCheckAfHighactive)
	ON_BN_CLICKED(IDC_CHECK_AF_LOWACTIVE, OnCheckAfLowactive)
	ON_BN_CLICKED(IDC_CHECK_AR_HIGHACTIVE, OnCheckArHighactive)
	ON_BN_CLICKED(IDC_CHECK_AR_LOWACTIVE, OnCheckArLowactive)
	ON_BN_CLICKED(IDC_CHECK_AE_HIGHACTIVE, OnCheckAeHighactive)
	ON_BN_CLICKED(IDC_CHECK_AE_LOWACTIVE, OnCheckAeLowactive)
	ON_BN_CLICKED(IDC_CHECK_IP_HIGHACTIVE, OnCheckIpHighactive)
	ON_BN_CLICKED(IDC_CHECK_IP_LOWACTIVE, OnCheckIpLowactive)
	ON_BN_CLICKED(IDC_BUTTON_LS_AXIS_PREV, OnButtonLsAxisPrev)
	ON_BN_CLICKED(IDC_BUTTON_LS_AXIS_NEXT, OnButtonLsAxisNext)
	ON_BN_CLICKED(IDC_BUTTON_CLOSE, OnButtonClose)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CLimitSwitch message handlers

void CLimitSwitch::OnRadioPlNoevent() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetPLEvent(0);
}

void CLimitSwitch::OnRadioPlStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetPLEvent(1);
}

void CLimitSwitch::OnRadioPlEstop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetPLEvent(2);
}

void CLimitSwitch::OnRadioPlAbort() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetPLEvent(3);
}

void CLimitSwitch::OnCheckPlHighactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetPLLevel(1);
}

void CLimitSwitch::OnCheckPlLowactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetPLLevel(0);
}

static char szSection[100];
static char szVal[10];

char * GetSection(int ax)
{
	sprintf(szSection, "AXIS_%d", ax + 1);
	return szSection;
}
char *GetString(int val)
{
	sprintf(szVal, " %d", val);
	return szVal;
}

void CLimitSwitch::SetPLEvent(int ofs)
{
	m_iPLNoevent	= (ofs == 0) ? 0 : -1;
	m_iPLStop		= (ofs == 1) ? 0 : -1;
	m_iPLEStop		= (ofs == 2) ? 0 : -1;
	m_iPLAbort		= (ofs == 3) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"POS_LEVEL_ST", 
		GetString(GetPLEvent()), 
		GetParamPath());
}

void CLimitSwitch::SetPLLevel(int ofs)
{
	m_bPLHighActive = (ofs == 1) ? 1 : 0;
	m_bPLLowActive = (ofs == 0) ? 1 : 0;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"POS_LEVEL", 
		GetString(GetPLLevel()), 
		GetParamPath());
}

INT CLimitSwitch::GetPLEvent()
{
	if (m_iPLNoevent == 0) return NO_EVENT;
	if (m_iPLStop == 0) return STOP_EVENT;
	if (m_iPLEStop == 0) return E_STOP_EVENT;
	if (m_iPLAbort == 0) return ABORT_EVENT;
	return NO_EVENT;
}

INT CLimitSwitch::GetPLLevel()
{
	if (m_bPLHighActive) return 1;
	return 0;
}

INT CLimitSwitch::GetNLEvent()
{
	if (m_iNLNoevent == 0) return NO_EVENT;
	if (m_iNLStop == 0) return STOP_EVENT;
	if (m_iNLEStop == 0) return E_STOP_EVENT;
	if (m_iNLAbort == 0) return ABORT_EVENT;
	return NO_EVENT;
}
INT CLimitSwitch::GetNLLevel()
{
	if (m_bNLHighActive) return 1;
	return 0;
}
INT CLimitSwitch::GetHSEvent()
{
	if (m_iHSNoevent == 0) return NO_EVENT;
	if (m_iHSStop == 0) return STOP_EVENT;
	if (m_iHSEStop == 0) return E_STOP_EVENT;
	if (m_iHSAbort == 0) return ABORT_EVENT;
	return NO_EVENT;
}
INT CLimitSwitch::GetHSLevel()
{
	if (m_bHSHighActive) return 1;
	return 0;
}
INT CLimitSwitch::GetAFEvent()
{
	if (m_iAFNoevent == 0) return NO_EVENT;
	if (m_iAFStop == 0) return STOP_EVENT;
	if (m_iAFEStop == 0) return E_STOP_EVENT;
	if (m_iAFAbort == 0) return ABORT_EVENT;
	return NO_EVENT;
}
INT CLimitSwitch::GetAFLevel()
{
	if (m_bAFHighActive) return 1;
	return 0;
}
INT CLimitSwitch::GetAELevel()
{
	if (m_bAEHighActive) return 1;
	return 0;
}
INT CLimitSwitch::GetARLevel()
{
	if (m_bARHighActive) return 1;
	return 0;
}
INT CLimitSwitch::GetIPLevel()
{
	if (m_bIPHighActive) return 1;
	return 0;
}

char *CLimitSwitch::GetParamPath()
{
	static char strPath[300];

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;

	sprintf(strPath, "%s%s", pSheet->m_szAbsPath, AMC_PARAMETER_FILENAME);
	return strPath;
}


BOOL CLimitSwitch::OnInitDialog() 
{
	CDialog::OnInitDialog();
	
	// TODO: Add extra initialization here
	SetAxisConfigurations(m_uiAxis);

	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}

void CLimitSwitch::OnRadioNlNoevent() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetNLEvent(0);
}

void CLimitSwitch::OnRadioNlStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetNLEvent(1);
}

void CLimitSwitch::OnRadioNlEstop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetNLEvent(2);
}

void CLimitSwitch::OnRadioNlAbort() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetNLEvent(3);
}

void CLimitSwitch::OnCheckNlHighactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetNLLevel(1);
}

void CLimitSwitch::OnCheckNlLowactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetNLLevel(0);
}

void CLimitSwitch::SetNLEvent(int ofs)
{
	m_iNLNoevent	= (ofs == 0) ? 0 : -1;
	m_iNLStop		= (ofs == 1) ? 0 : -1;
	m_iNLEStop		= (ofs == 2) ? 0 : -1;
	m_iNLAbort		= (ofs == 3) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"NEG_LEVEL_ST", 
		GetString(GetNLEvent()), 
		GetParamPath());
}

void CLimitSwitch::SetNLLevel(int ofs)
{
	m_bNLHighActive = (ofs == 1) ? 1 : 0;
	m_bNLLowActive = (ofs == 0) ? 1 : 0;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"NEG_LEVEL", 
		GetString(GetNLLevel()), 
		GetParamPath());
}

void CLimitSwitch::OnRadioHsNoevent() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHSEvent(0);
}

void CLimitSwitch::OnRadioHsStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHSEvent(1);
}

void CLimitSwitch::OnRadioHsEstop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHSEvent(2);
}

void CLimitSwitch::OnRadioHsAbort() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHSEvent(3);
}

void CLimitSwitch::OnCheckHsHighactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHSLevel(1);
}

void CLimitSwitch::OnCheckHsLowactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetHSLevel(0);
}

void CLimitSwitch::SetHSEvent(int ofs)
{
	m_iHSNoevent	= (ofs == 0) ? 0 : -1;
	m_iHSStop		= (ofs == 1) ? 0 : -1;
	m_iHSEStop		= (ofs == 2) ? 0 : -1;
	m_iHSAbort		= (ofs == 3) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"HOME_LEVEL_ST", 
		GetString(GetHSEvent()), 
		GetParamPath());
}

void CLimitSwitch::SetHSLevel(int ofs)
{
	m_bHSHighActive = (ofs == 1) ? 1 : 0;
	m_bHSLowActive = (ofs == 0) ? 1 : 0;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"HOME_LEVEL", 
		GetString(GetHSLevel()), 
		GetParamPath());
}

void CLimitSwitch::OnRadioAfNoevent() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAFEvent(0);
}

void CLimitSwitch::OnRadioAfStop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAFEvent(1);
}

void CLimitSwitch::OnRadioAfEstop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAFEvent(2);
}

void CLimitSwitch::OnRadioAfAbort() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAFEvent(3);
}

void CLimitSwitch::OnCheckAfHighactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAFLevel(1);
}

void CLimitSwitch::OnCheckAfLowactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAFLevel(0);
}

void CLimitSwitch::SetAFEvent(int ofs)
{
	m_iAFNoevent	= (ofs == 0) ? 0 : -1;
	m_iAFStop		= (ofs == 1) ? 0 : -1;
	m_iAFEStop		= (ofs == 2) ? 0 : -1;
	m_iAFAbort		= (ofs == 3) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"AMP_FAULT_ST", 
		GetString(GetAFEvent()), 
		GetParamPath());
}

void CLimitSwitch::SetAFLevel(int ofs)
{
	m_bAFHighActive = (ofs == 1) ? 1 : 0;
	m_bAFLowActive = (ofs == 0) ? 1 : 0;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"AMP_FAULT", 
		GetString(GetAFLevel()), 
		GetParamPath());
}

void CLimitSwitch::OnCheckArHighactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetARLevel(1);
}

void CLimitSwitch::OnCheckArLowactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetARLevel(0);
}

void CLimitSwitch::SetARLevel(int ofs)
{
	m_bARHighActive = (ofs == 1) ? 1 : 0;
	m_bARLowActive = (ofs == 0) ? 1 : 0;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"AMP_RESET_LEVEL", 
		GetString(GetARLevel()), 
		GetParamPath());
}

void CLimitSwitch::OnCheckAeHighactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAELevel(1);
}

void CLimitSwitch::OnCheckAeLowactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetAELevel(0);
}

void CLimitSwitch::SetAELevel(int ofs)
{
	m_bAEHighActive = (ofs == 1) ? 1 : 0;
	m_bAELowActive = (ofs == 0) ? 1 : 0;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"AMP_ON_LEVEL", 
		GetString(GetAELevel()), 
		GetParamPath());
}

void CLimitSwitch::OnCheckIpHighactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetIPLevel(1);
}

void CLimitSwitch::OnCheckIpLowactive() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetIPLevel(0);
}

void CLimitSwitch::SetIPLevel(int ofs)
{
	m_bIPHighActive = (ofs == 1) ? 1 : 0;
	m_bIPLowActive = (ofs == 0) ? 1 : 0;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"INPOS_LEVEL", 
		GetString(GetIPLevel()), 
		GetParamPath());
}

void CLimitSwitch::OnButtonLsAxisPrev() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int i = m_uiAxis;
	if (--i < 0) i = 0;
	m_uiAxis = (UINT) i;

	UpdateData(FALSE);

	SetAxisConfigurations(m_uiAxis);
}

void CLimitSwitch::OnButtonLsAxisNext() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if (++m_uiAxis >= JPC_AXIS) m_uiAxis = JPC_AXIS-1;
	UpdateData(FALSE);

	SetAxisConfigurations(m_uiAxis);
}

void CLimitSwitch::SetAxisConfigurations(int ax)
{
//	char szStr[100];//2011.10.10

	SetPLEvent( GetPrivateProfileInt(GetSection(ax), "POS_LEVEL_ST", 0, GetParamPath()) );
	SetPLLevel( GetPrivateProfileInt(GetSection(ax), "POS_LEVEL", 0, GetParamPath()) );

	SetNLEvent( GetPrivateProfileInt(GetSection(ax), "NEG_LEVEL_ST", 0, GetParamPath()) );
	SetNLLevel( GetPrivateProfileInt(GetSection(ax), "NEG_LEVEL", 0, GetParamPath()) );

	SetHSEvent( GetPrivateProfileInt(GetSection(ax), "HOME_LEVEL_ST", 0, GetParamPath()) );
	SetHSLevel( GetPrivateProfileInt(GetSection(ax), "HOME_LEVEL", 0, GetParamPath()) );

	SetAFEvent( GetPrivateProfileInt(GetSection(ax), "AMP_FAULT_ST", 0, GetParamPath()) );
	SetAFLevel( GetPrivateProfileInt(GetSection(ax), "AMP_FAULT", 0, GetParamPath()) );

	SetARLevel( GetPrivateProfileInt(GetSection(ax), "AMP_RESET_LEVEL", 0, GetParamPath()) );
	SetAELevel( GetPrivateProfileInt(GetSection(ax), "AMP_ON_LEVEL", 0, GetParamPath()) );
	SetIPLevel( GetPrivateProfileInt(GetSection(ax), "INPOS_LEVEL", 0, GetParamPath()) );

}

void CLimitSwitch::OnCancel() 
{
	// TODO: Add extra cleanup here
	CDialog::OnCancel();
}

void CLimitSwitch::OnButtonClose() 
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

void CLimitSwitch::SetAxis(UINT uiAxis)
{
	m_uiAxis = uiAxis;
}
