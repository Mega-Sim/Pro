// ParamTuning.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "ParamTuning.h"
#include "MyPropertySheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CParamTuning dialog


CParamTuning::CParamTuning(CWnd* pParent /*=NULL*/)
	: CDialog(CParamTuning::IDD, pParent)
{
	//{{AFX_DATA_INIT(CParamTuning)
	m_uiAnalogLimit = 32767;
	m_nAnalogOffset = 0;
	m_uiAnalogOutput = 0;
	m_uiDGain = 0;
	m_uiFGain = 0;
	m_uiIGain = 0;
	m_uiILimit = 0;
	m_uiPGain = 0;
	m_uiVDGain = 0;
	m_uiVFGain = 0;
	m_uiVIGain = 0;
	m_uiVILimit = 0;
	m_uiVPGain = 0;
	m_uiAxis = 0;
	//}}AFX_DATA_INIT
}


void CParamTuning::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CParamTuning)
	DDX_Text(pDX, IDC_EDIT_ANALOG_LIMIT, m_uiAnalogLimit);
	DDX_Text(pDX, IDC_EDIT_ANALOG_OFFSET, m_nAnalogOffset);
	DDX_Text(pDX, IDC_EDIT_ANALOG_OUTPUT, m_uiAnalogOutput);
	DDX_Text(pDX, IDC_EDIT_DGAIN, m_uiDGain);
	DDX_Text(pDX, IDC_EDIT_FGAIN, m_uiFGain);
	DDX_Text(pDX, IDC_EDIT_IGAIN, m_uiIGain);
	DDX_Text(pDX, IDC_EDIT_ILIMIT, m_uiILimit);
	DDX_Text(pDX, IDC_EDIT_PGAIN, m_uiPGain);
	DDX_Text(pDX, IDC_EDIT_VDGAIN, m_uiVDGain);
	DDX_Text(pDX, IDC_EDIT_VFGAIN, m_uiVFGain);
	DDX_Text(pDX, IDC_EDIT_VIGAIN, m_uiVIGain);
	DDX_Text(pDX, IDC_EDIT_VILIMIT, m_uiVILimit);
	DDX_Text(pDX, IDC_EDIT_VPGAIN, m_uiVPGain);
	DDX_Text(pDX, IDC_EDIT_AXIS, m_uiAxis);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CParamTuning, CDialog)
	//{{AFX_MSG_MAP(CParamTuning)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_NEXT, OnButtonAxisNext)
	ON_BN_CLICKED(IDC_BUTTON_AXIS_PREV, OnButtonAxisPrev)
	ON_BN_CLICKED(IDC_BUTTON_READ, OnButtonRead)
	ON_BN_CLICKED(IDC_BUTTON_SAVE, OnButtonSave)
	ON_BN_CLICKED(IDC_BUTTON_WRITE, OnButtonWrite)
	ON_BN_CLICKED(IDC_BUTTON_CLOSE, OnButtonClose)
	ON_BN_CLICKED(IDC_BUTTON_EDITOFFSET, OnButtonEditoffset)
	ON_EN_KILLFOCUS(IDC_EDIT_ANALOG_OFFSET, OnKillfocusEditAnalogOffset)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CParamTuning message handlers

void CParamTuning::OnButtonAxisNext() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if (++m_uiAxis >= JPC_AXIS) m_uiAxis = JPC_AXIS-1;
	UpdateData(FALSE);

	ShowAxisSettings(m_uiAxis);
}

void CParamTuning::OnButtonAxisPrev() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int i = m_uiAxis;
	if (--i < 0) i = 0;
	m_uiAxis = (UINT) i;

	UpdateData(FALSE);

	ShowAxisSettings(m_uiAxis);
}

void CParamTuning::OnButtonRead() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	unsigned int coeff[5];
	get_filter(m_uiAxis, coeff);
	m_uiPGain = coeff[0] & 0xffff;
	m_uiIGain = coeff[1] & 0xffff;
	m_uiDGain = coeff[2] & 0xffff;
	m_uiFGain = coeff[3] & 0xffff;
	m_uiILimit = coeff[4] & 0xffff;

	get_v_filter(m_uiAxis, coeff);
	m_uiVPGain = coeff[0] & 0xffff;
	m_uiVIGain = coeff[1] & 0xffff;
	m_uiVDGain = coeff[2] & 0xffff;
	m_uiVFGain = coeff[3] & 0xffff;
	m_uiVILimit = coeff[4] & 0xffff;

	int val;
	get_analog_offset(m_uiAxis, &val);
	
	m_nAnalogOffset = val;
	m_uiAnalogLimit = 32767;
	UpdateData(FALSE);

}

void CParamTuning::OnButtonWrite() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	unsigned int coeff[5];

	coeff[0] = m_uiPGain;
	coeff[1] = m_uiIGain;
	coeff[2] = m_uiDGain;
	coeff[3] = m_uiFGain;
	coeff[4] = m_uiILimit;
	set_filter(m_uiAxis, coeff);

	coeff[0] = m_uiVPGain;
	coeff[1] = m_uiVIGain;
	coeff[2] = m_uiVDGain;
	coeff[3] = m_uiVFGain;
	coeff[4] = m_uiVILimit;
	set_v_filter(m_uiAxis, coeff);

	set_analog_offset(m_uiAxis, m_nAnalogOffset);

	OnButtonSave();
}

void CParamTuning::WriteIntIniParam(char *pszFile, char *pszSec, char *pszKey, int val) 
{
	char strVal[100];
	sprintf(strVal, " %d", val);
	WritePrivateProfileString(pszSec, pszKey, strVal, pszFile);
}

void CParamTuning::OnButtonSave() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	char strFile[100];
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	sprintf(strFile, "%s%s", pSheet->m_szAbsPath, AMC_PARAMETER_FILENAME);
//	sprintf(strFile, "C:\\%s", AMC_PARAMETER_FILENAME);

	unsigned int coeff[5];

	coeff[0] = m_uiPGain;
	coeff[1] = m_uiIGain;
	coeff[2] = m_uiDGain;
	coeff[3] = m_uiFGain;
	coeff[4] = m_uiILimit;
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "PGAIN", coeff[0]);
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "IGAIN", coeff[1]);
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "DGAIN", coeff[2]);
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "FGAIN", coeff[3]);
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "ILIMIT", coeff[4]);

	coeff[0] = m_uiVPGain;
	coeff[1] = m_uiVIGain;
	coeff[2] = m_uiVDGain;
	coeff[3] = m_uiVFGain;
	coeff[4] = m_uiVILimit;
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "VPGAIN", coeff[0]);
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "VIGAIN", coeff[1]);
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "VDGAIN", coeff[2]);
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "VFGAIN", coeff[3]);
	WriteIntIniParam(strFile, GetSection(m_uiAxis), "VILIMIT", coeff[4]);

	WriteIntIniParam(strFile, GetSection(m_uiAxis), "DAC_BIAS", m_nAnalogOffset);

}

BOOL CParamTuning::OnInitDialog() 
{
	CDialog::OnInitDialog();
	
	// TODO: Add extra initialization here
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen == FALSE)
	{
		DisableAllButtons();
		AfxMessageBox("장치가 초기화되지 않아 모든 기능을 사용할 수 없습니다");
	}
	
	ShowAxisSettings(m_uiAxis);
	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}

char *CParamTuning::GetParamPath()
{
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;

	sprintf(m_strPath, "%s%s", pSheet->m_szAbsPath, AMC_PARAMETER_FILENAME);
	return m_strPath;
}


void CParamTuning::DisableAllButtons()
{
	GetDlgItem(IDC_BUTTON_READ)->EnableWindow(FALSE);
	GetDlgItem(IDC_BUTTON_WRITE)->EnableWindow(FALSE);
	GetDlgItem(IDC_BUTTON_AXIS_PREV)->EnableWindow(FALSE);
	GetDlgItem(IDC_BUTTON_AXIS_NEXT)->EnableWindow(FALSE);
	GetDlgItem(IDCANCEL)->EnableWindow(TRUE);

	// 오프셑 설정하는 창을 비활성화 시킨다.
	OnKillfocusEditAnalogOffset();
}

void CParamTuning::OnCancel() 
{
	// TODO: Add extra cleanup here
	CDialog::OnCancel();
}

void CParamTuning::OnButtonClose() 
{
	// TODO: Add your control notification handler code here
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen == TRUE)
	{
		// boot parameter에 INI의 내용을 덮어쓴 후 다시 DSP에 내려 보낸다.
		amc_adopt_ini_param();
		amc_save_local_sysparam_to_dsp();
		amc_flush_sysparam_to_eeprom();
	}
	CDialog::OnCancel();
}

void CParamTuning::ShowAxisSettings(int ax)
{
//	int coeff[5];//2011.10.10

	m_uiPGain = GetPrivateProfileInt(GetSection(ax), "PGAIN", 61500, GetParamPath());
	m_uiIGain = GetPrivateProfileInt(GetSection(ax), "IGAIN", 0, GetParamPath());
	m_uiDGain = GetPrivateProfileInt(GetSection(ax), "DGAIN", 0, GetParamPath());
	m_uiFGain = GetPrivateProfileInt(GetSection(ax), "FGAIN", 0, GetParamPath());
	m_uiILimit = GetPrivateProfileInt(GetSection(ax), "ILIMIT", 1000, GetParamPath());

	m_uiVPGain = GetPrivateProfileInt(GetSection(ax), "VPGAIN", 61500, GetParamPath());
	m_uiVIGain = GetPrivateProfileInt(GetSection(ax), "VIGAIN", 0, GetParamPath());
	m_uiVDGain = GetPrivateProfileInt(GetSection(ax), "VDGAIN", 0, GetParamPath());
	m_uiVFGain = GetPrivateProfileInt(GetSection(ax), "VFGAIN", 0, GetParamPath());
	m_uiVILimit = GetPrivateProfileInt(GetSection(ax), "VILIMIT", 1000, GetParamPath());

	int val;
	get_analog_offset(m_uiAxis, &val);
	
	m_nAnalogOffset = val;
	m_uiAnalogLimit = 32767;

	UpdateData(FALSE);

	// 오프셑 설정하는 창을 비활성화 시킨다.
	OnKillfocusEditAnalogOffset();
}

void CParamTuning::SetAxis(UINT uiAxis)
{
	m_uiAxis = uiAxis;
}

void CParamTuning::OnButtonEditoffset() 
{
	// TODO: Add your control notification handler code here
	CWnd *pWnd = GetDlgItem(IDC_EDIT_ANALOG_OFFSET);
	pWnd->EnableWindow(TRUE);
}

void CParamTuning::OnKillfocusEditAnalogOffset() 
{
	// TODO: Add your control notification handler code here
	CWnd *pWnd = GetDlgItem(IDC_EDIT_ANALOG_OFFSET);
	pWnd->EnableWindow(FALSE);
}
