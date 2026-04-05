// DeviceOpenPage.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "DeviceOpenPage.h"

#include "MyPropertySheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

#define		AMC_DEFAULT_PARAM		NULL//"C:\\DefaultParam.ini"


#define		DPRAM_TESTING_STEPS		(WM_USER + 12)

CString FormattingVersion(int nver)
{
	char sz[10];
	char str[100];
	sprintf(str, "%05d", nver);
	str[5] = 0;
	sz[0] = str[0];
	sz[1] = '.';
	memcpy(sz+2, str + 1, 2);
	sz[4] = '.';
	memcpy(sz+5, str+3, 2);
	sz[7] = 0;

	return (CString) sz;
}


/////////////////////////////////////////////////////////////////////////////
// CDeviceOpenPage property page

IMPLEMENT_DYNCREATE(CDeviceOpenPage, CPropertyPage)

CDeviceOpenPage::CDeviceOpenPage() : CPropertyPage(CDeviceOpenPage::IDD)
{
	//{{AFX_DATA_INIT(CDeviceOpenPage)
	m_sPath = _T("");
	m_byIRQNum = 0;
	m_sAddr = _T("");
	m_sDspVer = _T("");
	m_sPcVer = _T("");
	m_sThisVer = _T("");
	m_nEncoderOffset = 0;
	m_nDpramTestCount = 100;
	//}}AFX_DATA_INIT
	//m_sPath = _T("C:\\User\\OHT\\DataFiles");
	m_sPath = _T("C:");

	m_sAddr = _T("0xd0000");
	m_byIRQNum = 11;

//111029 syk_start
	m_sThisVer = FormattingVersion(VERSION_SETUP);
//111029 syk_end
}

CDeviceOpenPage::~CDeviceOpenPage()
{
}

void CDeviceOpenPage::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CDeviceOpenPage)
	DDX_Text(pDX, IDC_EDIT_ABS_PATH, m_sPath);
	DDX_Text(pDX, IDC_EDIT_IRQ_NO, m_byIRQNum);
	DDV_MinMaxByte(pDX, m_byIRQNum, 5, 15);
	DDX_Text(pDX, IDC_EDIT_ADDR, m_sAddr);
	DDV_MaxChars(pDX, m_sAddr, 7);
	DDX_Text(pDX, IDC_EDIT_VERSION_DSP, m_sDspVer);
	DDX_Text(pDX, IDC_EDIT_VERSION_LIB, m_sPcVer);
	DDX_Text(pDX, IDC_EDIT_VERSION_THIS, m_sThisVer);
	DDX_Text(pDX, IDC_EDIT_ENCODER_OFFSET, m_nEncoderOffset);
	DDX_Text(pDX, IDC_EDIT_DPRAM_TEST_COUNT, m_nDpramTestCount);
	DDV_MinMaxUInt(pDX, m_nDpramTestCount, 1, 100000);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CDeviceOpenPage, CPropertyPage)
	//{{AFX_MSG_MAP(CDeviceOpenPage)
	ON_BN_CLICKED(IDC_BUTTON_DEV_OPEN, OnButtonDevOpen)
	ON_BN_CLICKED(IDC_BUTTON_DSP_RESET, OnButtonDspReset)
	ON_BN_CLICKED(IDC_BUTTON_FLUSH, OnButtonFlush)
	ON_BN_CLICKED(IDC_BUTTON_DPRAM_TEST, OnButtonDpramTest)
	ON_BN_CLICKED(IDC_BUTTON_BT_INIT, OnButtonBtInit)
	ON_BN_CLICKED(IDC_BUTTON_BT_STARTSTOP, OnButtonBtStartstop)
	//}}AFX_MSG_MAP
	ON_MESSAGE(DPRAM_TESTING_STEPS, OnDpramTestMsg)
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CDeviceOpenPage message handlers

void CDeviceOpenPage::OnButtonDevOpen() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;
	pSheet->SetAbsPath("");
	if (pSheet->m_bDevOpen == FALSE)
	{
		BOOL bfOpen = FALSE;
		if(amc_open(m_byIRQNum, ToHex(m_sAddr), (char *) (LPCTSTR) m_sPath) == AMCFALSE)
		{
			AfxMessageBox("장치를 열 수 없습니다.\r\n다른 프로그램에 의해서 사용중일 수 있습니다.");
			return;
		}
		else
		{
			int err;
			char *pmsg;
			char str[300];

			err = mmc_init();

			if (err == MMC_BOOTPARAM_NOT_EXIST)
			{
				amc_load_dsp_sysparam_with_localfile(TRUE, AMC_DEFAULT_PARAM);
				amc_close();
				sprintf(str,"AMCPARAM.INI 파일이 없습니다.");
				AfxMessageBox(str);
			}else if (err == MMC_NON_EXIST)//MMC_NON_EXIST)
			{
				sprintf(str,"경고 : AMC 보드가 없습니다.");
				AfxMessageBox(str);
				amc_close(); 
			} else if ((err != MMC_OK) && (err != AMC_VERSION_ERROR))
			{
				amc_close();

				//2.8.05, 2011.10.20 장치를 초기화 할 수 없는 이유 표시 
				pmsg = _error_message(get_local_error());
				sprintf(str, "장치를 초기화 할 수 없습니다.'%s'", pmsg);
				AfxMessageBox(str);
//				AfxMessageBox("장치를 초기화 할 수 없습니다.");
			}else{
				if (err == AMC_VERSION_ERROR)//AMC_VERSION_ERROR)
				{
					sprintf(str,"경고 : 버전 다름");
					AfxMessageBox(str);
				} 
				amc_load_dsp_sysparam_with_localfile(TRUE, AMC_DEFAULT_PARAM);
				amc_save_local_sysparam_to_dsp();
				amc_flush_sysparam_to_eeprom();
	
				bfOpen = TRUE;
	
				pSheet->SetAbsPath((char *) (LPCTSTR) m_sPath);
	
				int nVer;
				version_chk(0, &nVer);
				CString cstr;
				cstr = ::FormattingVersion(nVer);
				GetDlgItem(IDC_EDIT_VERSION_DSP)->SetWindowText(cstr);

				AfxMessageBox("초기화 완료!" );
			}
		}
		pSheet->m_bDevOpen = bfOpen;
		if (bfOpen == TRUE)
		{
			// 버튼의 이름을 바꾼다.
			GetDlgItem(IDC_BUTTON_DEV_OPEN)->SetWindowText("Device Close");
			GetDlgItem(IDC_BUTTON_DSP_RESET)->EnableWindow(FALSE);
		}
	} else {
		amc_close();
		pSheet->m_bDevOpen = FALSE;

		// 버튼의 이름을 바꾼다.
		GetDlgItem(IDC_BUTTON_DEV_OPEN)->SetWindowText("Device Open");
		GetDlgItem(IDC_BUTTON_DSP_RESET)->EnableWindow(TRUE);
	}
}

BOOL CDeviceOpenPage::OnInitDialog() 
{
	CPropertyPage::OnInitDialog();
	
	// TODO: Add extra initialization here

	try
	{
//111029 syk_start
		m_sPcVer += FormattingVersion(version_chk_pc());
//111029 syk_end
	} catch (...)
	{
		AfxMessageBox(
			"사용되고있는 amc.dll이 구버전이어서 버전을 읽어올 수 없었습니다\r\n"	\
			"모든 기능이 정상동작하지 않을 수 있습니다"
			);
	}
	UpdateData(FALSE);
	
	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}

UINT CDeviceOpenPage::ToHex(CString str)
{
	UINT ui = 0;
	int len = str.GetLength();
	char *pstr = new char[len + 1];
	memcpy(pstr, str, len);
	pstr[len] = 0;

	for (int i = 0; i < len; i ++) pstr[i] = toupper(pstr[i]);

	if (strncmp(pstr, "0X", 2) == 0) sscanf(pstr+2, "%X", &ui);
	else sscanf(pstr, "%d", &ui);
	delete [] pstr;

	return ui;
}




void CDeviceOpenPage::OnButtonDspReset() 
{
	// TODO: Add your control notification handler code here
	if (amc_open(m_byIRQNum, ToHex(m_sAddr), (char *) (LPCTSTR) m_sPath) == AMCFALSE)
	{
		// 이미 열려 있는 경우이므로 dsp_reboot()만 한다.
		dsp_reboot_and_chk(15000);//2.5.22, 2011.10.13 dsp_reboot_and_chk로 수정
	} else
	{
		// 현재 함수에서 열린것 이므로 dsp_reboot후 장치를 닫는다.
		dsp_reboot_and_chk(15000);//2.5.22, 2011.10.13 dsp_reboot_and_chk로 수정
		amc_close();
	}
}

void CDeviceOpenPage::OnButtonFlush() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	set_encoder_offset(0, m_nEncoderOffset);
	amc_flush_sysparam_to_eeprom();
}

void CDeviceOpenPage::OnButtonDpramTest() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	test_dpram(m_nDpramTestCount, GetSafeHwnd(), DPRAM_TESTING_STEPS);
}

LRESULT CDeviceOpenPage::OnDpramTestMsg(WPARAM wParam, LPARAM lParam)
{
	CString cstr;
	
	if (lParam == 1) 
		cstr.Format("Step #%d start", wParam);
	else if (lParam == 2) 
		cstr.Format("Step #%d fail", wParam);
	else
		cstr.Format("Step #%d success", wParam);
	SetDlgItemText(IDC_STATIC_DPRAM_TEST_MSG, cstr);

	return 0;
}


void CDeviceOpenPage::OnCancel() 
{
	// TODO: Add your specialized code here and/or call the base class
	
	//CPropertyPage::OnCancel();
}



static BOOL __gb_StopTesting = 1;
static int __gui_TestCount = 0;
void CDeviceOpenPage::SetTestingCount(UINT uiID, int cnt) 
{
	CString cStr;
	cStr.Format("%d", cnt);
	SetDlgItemText(uiID, cStr);
}

void CDeviceOpenPage::OnButtonBtInit() 
{
	// TODO: Add your control notification handler code here
	__gui_TestCount = 0;
	
	SetTestingCount(IDC_EDIT_BT_COUNT, __gui_TestCount);
}

UINT __RunTesting(LPVOID lpv)
{
	CDeviceOpenPage *pTest = (CDeviceOpenPage *) lpv;
	pTest->RunTesting();
	AfxEndThread(0);

	return 0;
}

void CDeviceOpenPage::OnButtonBtStartstop() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	CString cStr;
	GetDlgItemText(IDC_BUTTON_BT_STARTSTOP, cStr);
	if (cStr == "Start")
	{
		// 새로 시작해야 하는경우
		SetDlgItemText(IDC_BUTTON_BT_STARTSTOP, "Progress\r\nStop");
		__gb_StopTesting = 0;
		AfxBeginThread(__RunTesting, this);
	} else 
	{
		GetDlgItem(IDC_BUTTON_BT_STARTSTOP)->EnableWindow(FALSE);
		__gb_StopTesting = 1;
	}
}

void CDeviceOpenPage::RunTesting() 
{
	CString cstrVersion;
	int nIRQ;
	int nBdNum = 1;

	// 비교를 원하는 버전을 가져온다.
	GetDlgItemText(IDC_EDIT_BT_WAIT_AMCVERSION, cstrVersion);

	nIRQ = m_byIRQNum;

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;
	pSheet->SetAbsPath("");

	while (__gb_StopTesting == 0)
	{
		BOOL bfOpen = FALSE;
		if(amc_open(m_byIRQNum, ToHex(m_sAddr), (char *) (LPCTSTR) m_sPath) == AMCFALSE)
		{
			AfxMessageBox("장치를 열 수 없습니다.\r\n다른 프로그램에 의해서 사용중일 수 있습니다.");
			break;
		}
		else
		{
			int err;
			err = mmc_init();
			if (err == MMC_BOOTPARAM_NOT_EXIST)
			{
				amc_load_dsp_sysparam_with_localfile(TRUE, AMC_DEFAULT_PARAM);
				amc_close();
				SetDlgItemText(IDC_STATIC_BT_PROGRESS, "파라미터를 초기화 하였습니다.\r\n다시한번 실행해 주십시요");
				break;
			} else if (err != MMC_OK)
			{
				amc_close();
				SetDlgItemText(IDC_STATIC_BT_PROGRESS, "장치를 초기화 할 수 없습니다.");
				break;
			} else 
			{
				amc_load_dsp_sysparam_with_localfile(TRUE, AMC_DEFAULT_PARAM);
				amc_save_local_sysparam_to_dsp();
				amc_flush_sysparam_to_eeprom();

				bfOpen = TRUE;

				pSheet->SetAbsPath((char *) (LPCTSTR) m_sPath);

				int nVer;
				version_chk(0, &nVer);

//				amc_close();

				CString cstr;
				cstr = ::FormattingVersion(nVer);
				GetDlgItem(IDC_EDIT_VERSION_DSP)->SetWindowText(cstr);

				if (cstr != cstrVersion)
				{
					CString cStr;
					cStr.Format("버전 다름\r\nAMC ver = '%s'", cstr);
					SetDlgItemText(IDC_STATIC_BT_PROGRESS, cStr);
					break;
				} else
				{
					SetDlgItemText(IDC_STATIC_BT_PROGRESS, "초기화 완료!" );
					bfOpen = TRUE;
				}
			}
		}

		if (bfOpen == TRUE)
		{
			__gui_TestCount ++;
			SetTestingCount(IDC_EDIT_BT_COUNT, __gui_TestCount);
		}
//retryTesting://2011.10.10
		;
	}
	SetDlgItemText(IDC_BUTTON_BT_STARTSTOP, "Start");
	GetDlgItem(IDC_BUTTON_BT_STARTSTOP)->EnableWindow(TRUE);
}

BOOL CDeviceOpenPage::OnSetActive() 
{
	// TODO: Add your specialized code here and/or call the base class

	// CTRL 키가 눌려져 있는 경우 몇가지 버튼을 보이게 처리한다.
	BOOL bfShow = FALSE;
	int key = ::GetKeyState( VK_CONTROL);
	if (key & 0x80)
		bfShow = TRUE;
	{
		CWnd *pWnd;
		
		pWnd = GetDlgItem(IDC_STATIC_BT_GROUPTEXT); pWnd->ShowWindow(bfShow);
		pWnd = GetDlgItem(IDC_STATIC_BT_COMMENT); pWnd->ShowWindow(bfShow);
		pWnd = GetDlgItem(IDC_STATIC_BT_COUNT); pWnd->ShowWindow(bfShow);
		pWnd = GetDlgItem(IDC_EDIT_BT_COUNT); pWnd->ShowWindow(bfShow);
		pWnd = GetDlgItem(IDC_STATIC_BT_VERSION); pWnd->ShowWindow(bfShow);
		pWnd = GetDlgItem(IDC_EDIT_BT_WAIT_AMCVERSION); pWnd->ShowWindow(bfShow);
		pWnd = GetDlgItem(IDC_STATIC_BT_PROGRESS); pWnd->ShowWindow(bfShow);

		pWnd = GetDlgItem(IDC_BUTTON_BT_INIT); pWnd->ShowWindow(bfShow);
		pWnd = GetDlgItem(IDC_BUTTON_BT_STARTSTOP); pWnd->ShowWindow(bfShow);

		// "Device Open" 과 "DSP Reset"버튼을 비활성화 시킨다.
		pWnd = GetDlgItem(IDC_BUTTON_DEV_OPEN); pWnd->ShowWindow(bfShow == FALSE);
		pWnd = GetDlgItem(IDC_BUTTON_DSP_RESET); pWnd->ShowWindow(bfShow == FALSE);

	}

	return CPropertyPage::OnSetActive();
}
