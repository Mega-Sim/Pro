// AxisConfig.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "AxisConfig.h"

#include "MyPropertySheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CAxisConfig dialog


CAxisConfig::CAxisConfig(CWnd* pParent /*=NULL*/)
	: CDialog(CAxisConfig::IDD, pParent)
{
	//{{AFX_DATA_INIT(CAxisConfig)
	m_uiAxis = 0;
	m_iControlTorque = -1;
	m_iControlVelocity = -1;
	m_iControlCloseLoop = -1;
	m_iControlOpenLoop = -1;
	m_iEncoderCCW = -1;
	m_iEncoderCW = -1;
	m_iFeedbackAnalog = -1;
	m_iFeedbackBianalog = -1;
	m_iFeedbackEncoder = -1;
	m_iMotorMicro = -1;
	m_iMotorServo = -1;
	m_iMotorStepper = -1;
	m_iPosimodeAlways = -1;
	m_iPosimodeOnlyStanding = -1;
	m_iVelimodeAlways = -1;
	m_iVelimodeOnlyStanding = -1;
	m_iVoltageBipolar = -1;
	m_iVoltageUnipolar = -1;
	//}}AFX_DATA_INIT
}


void CAxisConfig::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CAxisConfig)
	DDX_Text(pDX, IDC_EDIT_AXIS, m_uiAxis);
	DDX_Radio(pDX, IDC_RADIO_CONTROL_TORQUE, m_iControlTorque);
	DDX_Radio(pDX, IDC_RADIO_CONTROL_VELOCITY, m_iControlVelocity);
	DDX_Radio(pDX, IDC_RADIO_CONTROL_CLOSEDLOOP, m_iControlCloseLoop);
	DDX_Radio(pDX, IDC_RADIO_CONTROL_OPENLOOP, m_iControlOpenLoop);
	DDX_Radio(pDX, IDC_RADIO_ENCODERDIRECTION_CCW, m_iEncoderCCW);
	DDX_Radio(pDX, IDC_RADIO_ENCODERDIRECTION_CW, m_iEncoderCW);
	DDX_Radio(pDX, IDC_RADIO_FEEDBACK_ANALOG, m_iFeedbackAnalog);
	DDX_Radio(pDX, IDC_RADIO_FEEDBACK_BIANALOG, m_iFeedbackBianalog);
	DDX_Radio(pDX, IDC_RADIO_FEEDBACK_ENCODER, m_iFeedbackEncoder);
	DDX_Radio(pDX, IDC_RADIO_MOTOR_MICRO, m_iMotorMicro);
	DDX_Radio(pDX, IDC_RADIO_MOTOR_SERVO, m_iMotorServo);
	DDX_Radio(pDX, IDC_RADIO_MOTOR_STEPPER, m_iMotorStepper);
	DDX_Radio(pDX, IDC_RADIO_POSIMODE_ALWAYS, m_iPosimodeAlways);
	DDX_Radio(pDX, IDC_RADIO_POSIMODE_ONLY_STANDING, m_iPosimodeOnlyStanding);
	DDX_Radio(pDX, IDC_RADIO_VELIMODE_ALWAYS, m_iVelimodeAlways);
	DDX_Radio(pDX, IDC_RADIO_VELIMODE_ONLY_STANDING, m_iVelimodeOnlyStanding);
	DDX_Radio(pDX, IDC_RADIO_VOLTAGE_BIPOLAR, m_iVoltageBipolar);
	DDX_Radio(pDX, IDC_RADIO_VOLTAGE_UNIPOLAR, m_iVoltageUnipolar);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CAxisConfig, CDialog)
	//{{AFX_MSG_MAP(CAxisConfig)
	ON_BN_CLICKED(IDC_RADIO_MOTOR_SERVO, OnRadioMotorServo)
	ON_BN_CLICKED(IDC_RADIO_MOTOR_STEPPER, OnRadioMotorStepper)
	ON_BN_CLICKED(IDC_RADIO_MOTOR_MICRO, OnRadioMotorMicro)
	ON_BN_CLICKED(IDC_RADIO_CONTROL_OPENLOOP, OnRadioControlOpenloop)
	ON_BN_CLICKED(IDC_RADIO_CONTROL_CLOSEDLOOP, OnRadioControlClosedloop)
	ON_BN_CLICKED(IDC_RADIO_CONTROL_VELOCITY, OnRadioControlVelocity)
	ON_BN_CLICKED(IDC_RADIO_CONTROL_TORQUE, OnRadioControlTorque)
	ON_BN_CLICKED(IDC_RADIO_VOLTAGE_BIPOLAR, OnRadioVoltageBipolar)
	ON_BN_CLICKED(IDC_RADIO_VOLTAGE_UNIPOLAR, OnRadioVoltageUnipolar)
	ON_BN_CLICKED(IDC_RADIO_FEEDBACK_ENCODER, OnRadioFeedbackEncoder)
	ON_BN_CLICKED(IDC_RADIO_FEEDBACK_ANALOG, OnRadioFeedbackAnalog)
	ON_BN_CLICKED(IDC_RADIO_FEEDBACK_BIANALOG, OnRadioFeedbackBianalog)
	ON_BN_CLICKED(IDC_RADIO_POSIMODE_ONLY_STANDING, OnRadioPosimodeOnlyStanding)
	ON_BN_CLICKED(IDC_RADIO_POSIMODE_ALWAYS, OnRadioPosimodeAlways)
	ON_BN_CLICKED(IDC_RADIO_VELIMODE_ONLY_STANDING, OnRadioVelimodeOnlyStanding)
	ON_BN_CLICKED(IDC_RADIO_VELIMODE_ALWAYS, OnRadioVelimodeAlways)
	ON_BN_CLICKED(IDC_RADIO_ENCODERDIRECTION_CW, OnRadioEncoderdirectionCw)
	ON_BN_CLICKED(IDC_RADIO_ENCODERDIRECTION_CCW, OnRadioEncoderdirectionCcw)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_PREV, OnButtonAxisPrev)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_NEXT, OnButtonAxisNext)
	ON_BN_CLICKED(ID_CLOSE, OnClose)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CAxisConfig message handlers

void CAxisConfig::OnRadioMotorServo() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetMotorStatus(0);
}

void CAxisConfig::OnRadioMotorStepper() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetMotorStatus(1);
}

void CAxisConfig::OnRadioMotorMicro() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	SetMotorStatus(2);
}

void CAxisConfig::SetMotorStatus(int ofs)
{
	m_iMotorServo = (ofs == 0) ? 0 : -1;
	m_iMotorStepper = (ofs == 1) ? 0 : -1;
	m_iMotorMicro = (ofs == 2) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"MOTOR_TYPE", 
		GetString(ofs), 
		GetParamPath());
}

void CAxisConfig::OnRadioControlOpenloop() 
{
	// TODO: Add your control notification handler code here
	SetControlLoop(0);
}

void CAxisConfig::OnRadioControlClosedloop() 
{
	// TODO: Add your control notification handler code here
	SetControlLoop(1);
}

void CAxisConfig::SetControlLoop(int ofs)
{
	m_iControlOpenLoop	= (ofs == 0) ? 0 : -1;
	m_iControlCloseLoop = (ofs == 1) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"LOOP_CFG", 
		GetString(ofs), 
		GetParamPath());
}

void CAxisConfig::OnRadioControlVelocity() 
{
	// TODO: Add your control notification handler code here
	SetControl(0);
}

void CAxisConfig::OnRadioControlTorque() 
{
	// TODO: Add your control notification handler code here
	SetControl(1);
}

void CAxisConfig::SetControl(int ofs)
{
	m_iControlVelocity	= (ofs == 0) ? 0 : -1;
	m_iControlTorque	= (ofs == 1) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"CONTROL_CFG", 
		GetString(ofs), 
		GetParamPath());
}

void CAxisConfig::OnRadioVoltageBipolar() 
{
	// TODO: Add your control notification handler code here
	SetVoltage(0);
}

void CAxisConfig::OnRadioVoltageUnipolar() 
{
	// TODO: Add your control notification handler code here
	SetVoltage(1);
}

void CAxisConfig::SetVoltage(int ofs)
{
	m_iVoltageBipolar = (ofs == 0) ? 0 : -1;
	m_iVoltageUnipolar = (ofs == 1) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"VOLTAGE_CFG", 
		GetString(ofs), 
		GetParamPath());
}

void CAxisConfig::OnRadioFeedbackEncoder() 
{
	// TODO: Add your control notification handler code here
	SetFeedback(0);
}

void CAxisConfig::OnRadioFeedbackAnalog() 
{
	// TODO: Add your control notification handler code here
	SetFeedback(1);
}

void CAxisConfig::OnRadioFeedbackBianalog() 
{
	// TODO: Add your control notification handler code here
	SetFeedback(2);
}

void CAxisConfig::SetFeedback(int ofs)
{
	m_iFeedbackEncoder	= (ofs == 0) ? 0 : -1;
	m_iFeedbackAnalog	= (ofs == 1) ? 0 : -1;
	m_iFeedbackBianalog = (ofs == 2) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"ENCODER_CFG", 
		GetString(ofs), 
		GetParamPath());
}

void CAxisConfig::OnRadioPosimodeOnlyStanding() 
{
	// TODO: Add your control notification handler code here
	SetPosimode(0);
}

void CAxisConfig::OnRadioPosimodeAlways() 
{
	// TODO: Add your control notification handler code here
	SetPosimode(1);
}

void CAxisConfig::SetPosimode(int ofs)
{
	m_iPosimodeOnlyStanding = (ofs == 0) ? 0:-1;
	m_iPosimodeAlways = (ofs == 1) ? 0:-1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"POS_IMODE", 
		GetString(ofs), 
		GetParamPath());
}

void CAxisConfig::OnRadioVelimodeOnlyStanding() 
{
	// TODO: Add your control notification handler code here
	SetVelimode(0);
}

void CAxisConfig::OnRadioVelimodeAlways() 
{
	// TODO: Add your control notification handler code here
	SetVelimode(1);
}

void CAxisConfig::SetVelimode(int ofs)
{
	m_iVelimodeOnlyStanding		= (ofs == 0) ? 0:-1;
	m_iVelimodeAlways			= (ofs == 1) ? 0:-1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"VEL_IMODE", 
		GetString(ofs), 
		GetParamPath());
}

void CAxisConfig::OnRadioEncoderdirectionCw() 
{
	// TODO: Add your control notification handler code here
	SetEncoderDirection(CIR_CW);
}

void CAxisConfig::OnRadioEncoderdirectionCcw() 
{
	// TODO: Add your control notification handler code here
	SetEncoderDirection(CIR_CCW);
}

void CAxisConfig::SetEncoderDirection(int ofs)
{
	m_iEncoderCW	= (ofs == CIR_CW) ? 0 : -1;
	m_iEncoderCCW	= (ofs == CIR_CCW) ? 0 : -1;
	UpdateData(FALSE);

	WritePrivateProfileString(
		GetSection(m_uiAxis),
		"ENCODER_DIR", 
		GetString(ofs), 
		GetParamPath());
}

BOOL CAxisConfig::OnInitDialog() 
{
	CDialog::OnInitDialog();
	
	// TODO: Add extra initialization here
	SetAxisConfiguration(m_uiAxis);

	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}

void CAxisConfig::OnButtonAxisPrev() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int i = m_uiAxis;
	if (--i < 0) i = 0;
	m_uiAxis = (UINT) i;

	UpdateData(FALSE);

	SetAxisConfiguration(m_uiAxis);
}

void CAxisConfig::OnButtonAxisNext() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if (++m_uiAxis >= JPC_AXIS) m_uiAxis = JPC_AXIS-1;
	UpdateData(FALSE);

	SetAxisConfiguration(m_uiAxis);
}

char *CAxisConfig::GetParamPath()
{
	static char strPath[300];

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;

	sprintf(strPath, "%s%s", pSheet->m_szAbsPath, AMC_PARAMETER_FILENAME);
	return strPath;
}

void CAxisConfig::SetAxisConfiguration(int ax)
{
//	char szVal[20];//2011.10.10

	SetMotorStatus(	GetPrivateProfileInt(GetSection(ax), "MOTOR_TYPE", 0,GetParamPath()) );
	SetControlLoop( GetPrivateProfileInt(GetSection(ax), "LOOP_CFG", 0, GetParamPath()) );
	SetControl(		GetPrivateProfileInt(GetSection(ax), "CONTROL_CFG", 0, GetParamPath()) );
	SetVoltage(		GetPrivateProfileInt(GetSection(ax), "VOLTAGE_CFG", 1, GetParamPath()) );

	SetFeedback(	GetPrivateProfileInt(GetSection(ax), "ENCODER_CFG", 0, GetParamPath()) );
	SetFeedback( 0 );	// always for amc
	
	SetPosimode(	GetPrivateProfileInt(GetSection(ax), "POS_IMODE", 1, GetParamPath()) );
	SetVelimode(	GetPrivateProfileInt(GetSection(ax), "VEL_IMODE", 1, GetParamPath()) );
	SetEncoderDirection(GetPrivateProfileInt(GetSection(ax), "ENCODER_DIR", 1, GetParamPath()) );
}


void CAxisConfig::OnClose() 
{
	// TODO: Add your control notification handler code here
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen) 
	{
		amc_adopt_ini_param();
		amc_save_local_sysparam_to_dsp();
		amc_flush_sysparam_to_eeprom();
	}
	CDialog::OnOK();
}

void CAxisConfig::OnCancel() 
{
	// TODO: Add extra cleanup here
	CDialog::OnCancel();
}

void CAxisConfig::SetAxis(UINT uiAxis)
{
	m_uiAxis = uiAxis;
}
