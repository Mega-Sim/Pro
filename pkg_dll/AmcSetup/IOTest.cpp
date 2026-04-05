// IOTest.cpp : implementation file
//

#include "stdafx.h"
#include "AmcSetup.h"
#include "IOTest.h"

#include "MyPropertySheet.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CIOTest property page

IMPLEMENT_DYNCREATE(CIOTest, CPropertyPage)

CIOTest::CIOTest() : CPropertyPage(CIOTest::IDD)
{
	//{{AFX_DATA_INIT(CIOTest)
	m_uiWaitTime = 0;
	m_uiIncount =0;
	m_uiOutcount =0;
	//}}AFX_DATA_INIT
	m_hInputMonQuit = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hInputStopped = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hAutoOutputQuit = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_hAutoOutputStopped = CreateEvent(NULL, TRUE, FALSE, NULL);
	m_uiWaitTime = 10;
	m_uiIncount =0;
	m_uiOutcount =0;
	s_iosel1 = 0;
	s_iosel2 = -1;
	s_iosel3 = -1;
	s_iosel4 = -1;


}

CIOTest::~CIOTest()
{
	CloseHandle(m_hInputMonQuit);
	CloseHandle(m_hInputStopped);
	CloseHandle(m_hAutoOutputQuit);
	CloseHandle(m_hAutoOutputStopped);
}

void CIOTest::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CIOTest)
	DDX_Text(pDX, IDC_EDIT_IO_WAIT_TIME, m_uiWaitTime);
	DDX_Text(pDX, IDC_EDIT_IN_COUNT, m_uiIncount);
	DDX_Text(pDX, IDC_EDIT_OUT_COUNT, m_uiOutcount);
	DDV_MinMaxUInt(pDX, m_uiWaitTime, 10, 500);
	DDV_MinMaxUInt(pDX, m_uiIncount, 0, 256);
	DDV_MinMaxUInt(pDX, m_uiOutcount, 0, 256);
	DDX_Radio(pDX, IDC_RADIO_SEL_IO1, s_iosel1);  //0~63 
	DDX_Radio(pDX, IDC_RADIO_SEL_IO2, s_iosel2);  //64~127
	DDX_Radio(pDX, IDC_RADIO_SEL_IO3, s_iosel3);  //128~191
	DDX_Radio(pDX, IDC_RADIO_SEL_IO4, s_iosel4);  //192~255 
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CIOTest, CPropertyPage)
	//{{AFX_MSG_MAP(CIOTest)
	ON_BN_CLICKED(IDC_BUTTON_OUTPUT_ALLSET, OnButtonOutputAllset)
	ON_BN_CLICKED(IDC_BUTTON_OUTPUT_ALLRESET, OnButtonOutputAllreset)
	ON_BN_CLICKED(IDC_BUTTON_OUTPUT_AUTO_CHK_START, OnButtonOutputAutoChkStart)
	ON_BN_CLICKED(IDC_BUTTON_OUTPUT_AUTO_CHK_STOP, OnButtonOutputAutoChkStop)
	ON_BN_CLICKED(IDC_BUTTON_IO_W, OnButtonInputOutputWrite)
	ON_BN_CLICKED(IDC_BUTTON_IO_R, OnButtonInputOutputRead)
	ON_BN_CLICKED(IDC_CHECK_OBIT0, OnCheckObit0)
	ON_BN_CLICKED(IDC_CHECK_OBIT1, OnCheckObit1)
	ON_BN_CLICKED(IDC_CHECK_OBIT2, OnCheckObit2)
	ON_BN_CLICKED(IDC_CHECK_OBIT3, OnCheckObit3)
	ON_BN_CLICKED(IDC_CHECK_OBIT4, OnCheckObit4)
	ON_BN_CLICKED(IDC_CHECK_OBIT5, OnCheckObit5)
	ON_BN_CLICKED(IDC_CHECK_OBIT6, OnCheckObit6)
	ON_BN_CLICKED(IDC_CHECK_OBIT7, OnCheckObit7)
	ON_BN_CLICKED(IDC_CHECK_OBIT8, OnCheckObit8)
	ON_BN_CLICKED(IDC_CHECK_OBIT9, OnCheckObit9)
	ON_BN_CLICKED(IDC_CHECK_OBIT10, OnCheckObit10)
	ON_BN_CLICKED(IDC_CHECK_OBIT11, OnCheckObit11)
	ON_BN_CLICKED(IDC_CHECK_OBIT12, OnCheckObit12)
	ON_BN_CLICKED(IDC_CHECK_OBIT13, OnCheckObit13)
	ON_BN_CLICKED(IDC_CHECK_OBIT14, OnCheckObit14)
	ON_BN_CLICKED(IDC_CHECK_OBIT15, OnCheckObit15)
	ON_BN_CLICKED(IDC_CHECK_OBIT16, OnCheckObit16)
	ON_BN_CLICKED(IDC_CHECK_OBIT17, OnCheckObit17)
	ON_BN_CLICKED(IDC_CHECK_OBIT18, OnCheckObit18)
	ON_BN_CLICKED(IDC_CHECK_OBIT19, OnCheckObit19)
	ON_BN_CLICKED(IDC_CHECK_OBIT20, OnCheckObit20)
	ON_BN_CLICKED(IDC_CHECK_OBIT21, OnCheckObit21)
	ON_BN_CLICKED(IDC_CHECK_OBIT22, OnCheckObit22)
	ON_BN_CLICKED(IDC_CHECK_OBIT23, OnCheckObit23)
	ON_BN_CLICKED(IDC_CHECK_OBIT24, OnCheckObit24)
	ON_BN_CLICKED(IDC_CHECK_OBIT25, OnCheckObit25)
	ON_BN_CLICKED(IDC_CHECK_OBIT26, OnCheckObit26)
	ON_BN_CLICKED(IDC_CHECK_OBIT27, OnCheckObit27)
	ON_BN_CLICKED(IDC_CHECK_OBIT28, OnCheckObit28)
	ON_BN_CLICKED(IDC_CHECK_OBIT29, OnCheckObit29)
	ON_BN_CLICKED(IDC_CHECK_OBIT30, OnCheckObit30)
	ON_BN_CLICKED(IDC_CHECK_OBIT31, OnCheckObit31)
	ON_BN_CLICKED(IDC_CHECK_OBIT32, OnCheckObit32)
	ON_BN_CLICKED(IDC_CHECK_OBIT33, OnCheckObit33)
	ON_BN_CLICKED(IDC_CHECK_OBIT34, OnCheckObit34)
	ON_BN_CLICKED(IDC_CHECK_OBIT35, OnCheckObit35)
	ON_BN_CLICKED(IDC_CHECK_OBIT36, OnCheckObit36)
	ON_BN_CLICKED(IDC_CHECK_OBIT37, OnCheckObit37)
	ON_BN_CLICKED(IDC_CHECK_OBIT38, OnCheckObit38)
	ON_BN_CLICKED(IDC_CHECK_OBIT39, OnCheckObit39)
	ON_BN_CLICKED(IDC_CHECK_OBIT40, OnCheckObit40)
	ON_BN_CLICKED(IDC_CHECK_OBIT41, OnCheckObit41)
	ON_BN_CLICKED(IDC_CHECK_OBIT42, OnCheckObit42)
	ON_BN_CLICKED(IDC_CHECK_OBIT43, OnCheckObit43)
	ON_BN_CLICKED(IDC_CHECK_OBIT44, OnCheckObit44)
	ON_BN_CLICKED(IDC_CHECK_OBIT45, OnCheckObit45)
	ON_BN_CLICKED(IDC_CHECK_OBIT46, OnCheckObit46)
	ON_BN_CLICKED(IDC_CHECK_OBIT47, OnCheckObit47)
	ON_BN_CLICKED(IDC_CHECK_OBIT48, OnCheckObit48)
	ON_BN_CLICKED(IDC_CHECK_OBIT49, OnCheckObit49)
	ON_BN_CLICKED(IDC_CHECK_OBIT50, OnCheckObit50)
	ON_BN_CLICKED(IDC_CHECK_OBIT51, OnCheckObit51)
	ON_BN_CLICKED(IDC_CHECK_OBIT52, OnCheckObit52)
	ON_BN_CLICKED(IDC_CHECK_OBIT53, OnCheckObit53)
	ON_BN_CLICKED(IDC_CHECK_OBIT54, OnCheckObit54)
	ON_BN_CLICKED(IDC_CHECK_OBIT55, OnCheckObit55)
	ON_BN_CLICKED(IDC_CHECK_OBIT56, OnCheckObit56)
	ON_BN_CLICKED(IDC_CHECK_OBIT57, OnCheckObit57)
	ON_BN_CLICKED(IDC_CHECK_OBIT58, OnCheckObit58)
	ON_BN_CLICKED(IDC_CHECK_OBIT59, OnCheckObit59)
	ON_BN_CLICKED(IDC_CHECK_OBIT60, OnCheckObit60)
	ON_BN_CLICKED(IDC_CHECK_OBIT61, OnCheckObit61)
	ON_BN_CLICKED(IDC_CHECK_OBIT62, OnCheckObit62)
	ON_BN_CLICKED(IDC_CHECK_OBIT63, OnCheckObit63)

	ON_BN_CLICKED(IDC_RADIO_SEL_IO1, OnRadio_sel_io1)
	ON_BN_CLICKED(IDC_RADIO_SEL_IO2, OnRadio_sel_io2)
	ON_BN_CLICKED(IDC_RADIO_SEL_IO3, OnRadio_sel_io3)
	ON_BN_CLICKED(IDC_RADIO_SEL_IO4, OnRadio_sel_io4)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CIOTest message handlers


UINT _InputMonitoring(LPVOID lpv)
{
	CIOTest *pTest = (CIOTest*) lpv;
	pTest->InputMonitoring();
	AfxEndThread(0);

	return 0L;
}


BOOL CIOTest::OnSetActive() 
{

	// TODO: Add your specialized code here and/or call the base class
	SetEvent(m_hInputStopped);
	ResetEvent(m_hInputMonQuit);

	SetEvent(m_hAutoOutputStopped);
	ResetEvent(m_hAutoOutputQuit);

	SetInitialButtonState();

	UpdateOutputBitStatus();
	Set_io_text();
	OnButtonInputOutputRead(); 
	AfxBeginThread(_InputMonitoring, this);

	return CPropertyPage::OnSetActive();
}

BOOL CIOTest::OnKillActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	SetEvent(m_hInputMonQuit);
	SetEvent(m_hAutoOutputQuit);

	::WaitForSingleObject(m_hInputStopped, 100);
	::WaitForSingleObject(m_hAutoOutputStopped, 100);

	return CPropertyPage::OnKillActive();
}


void CIOTest::PutIOStatus(UINT uiID[], INT ui[])
{
	UINT id;
	int val;
	int i;

	if(s_iosel1 ==0)
	{
		for (i = 0; i < 32; i ++)
		{
			id = uiID[i];
			val = 0;
			if (ui[0] & (1 << i)) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);
		}
		for (i = 32; i < 64; i ++)
		{
			id = uiID[i];
			val = 0;
			if (ui[1] & (1 << (i-32))) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);
		}
	}
	else if(s_iosel2 ==0)
	{
		for (int i = 0; i < 32; i ++)
		{
			id = uiID[i];
			val = 0;
			if (ui[2] & (1 << i)) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);
		}
		for (i = 32; i < 64; i ++)
		{
			id = uiID[i];
			val = 0;
			if (ui[3] & (1 << (i-32))) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);
		}
	}
	else if(s_iosel3 ==0)
	{
		for (int i = 0; i < 32; i ++)
		{
			id = uiID[i];
			val = 0;
			if (ui[4] & (1 << i)) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);
		}
		for (i = 32; i < 64; i ++)
		{
			id = uiID[i];
			val = 0;
			if (ui[5] & (1 << (i-32))) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);
		}
	}
	else if(s_iosel4 ==0)
	{
		for (int i = 0; i < 32; i ++)
		{
			id = uiID[i];
			val = 0;
			if (ui[6] & (1 << i)) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);
		}
		for (i = 32; i < 64; i ++)
		{
			id = uiID[i];
			val = 0;
			if (ui[7] & (1 << (i-32))) val = 1;
			CButton *pBtn = (CButton *) GetDlgItem(id);
			pBtn->SetCheck(val);
		}
	}
}


static UINT m_uiInputID[64] = 
{
	IDC_CHECK_IBIT0, IDC_CHECK_IBIT1, IDC_CHECK_IBIT2, IDC_CHECK_IBIT3, IDC_CHECK_IBIT4,
	IDC_CHECK_IBIT5, IDC_CHECK_IBIT6, IDC_CHECK_IBIT7, IDC_CHECK_IBIT8, IDC_CHECK_IBIT9,
	IDC_CHECK_IBIT10, IDC_CHECK_IBIT11, IDC_CHECK_IBIT12, IDC_CHECK_IBIT13, IDC_CHECK_IBIT14,
	IDC_CHECK_IBIT15, IDC_CHECK_IBIT16, IDC_CHECK_IBIT17, IDC_CHECK_IBIT18, IDC_CHECK_IBIT19,
	IDC_CHECK_IBIT20, IDC_CHECK_IBIT21, IDC_CHECK_IBIT22, IDC_CHECK_IBIT23, IDC_CHECK_IBIT24,
	IDC_CHECK_IBIT25, IDC_CHECK_IBIT26, IDC_CHECK_IBIT27, IDC_CHECK_IBIT28, IDC_CHECK_IBIT29,
	IDC_CHECK_IBIT30, IDC_CHECK_IBIT31, IDC_CHECK_IBIT32, IDC_CHECK_IBIT33, IDC_CHECK_IBIT34,
	IDC_CHECK_IBIT35, IDC_CHECK_IBIT36, IDC_CHECK_IBIT37, IDC_CHECK_IBIT38, IDC_CHECK_IBIT39,
	IDC_CHECK_IBIT40, IDC_CHECK_IBIT41, IDC_CHECK_IBIT42, IDC_CHECK_IBIT43, IDC_CHECK_IBIT44,
	IDC_CHECK_IBIT45, IDC_CHECK_IBIT46, IDC_CHECK_IBIT47, IDC_CHECK_IBIT48, IDC_CHECK_IBIT49,
	IDC_CHECK_IBIT50, IDC_CHECK_IBIT51, IDC_CHECK_IBIT52, IDC_CHECK_IBIT53, IDC_CHECK_IBIT54,
	IDC_CHECK_IBIT55, IDC_CHECK_IBIT56, IDC_CHECK_IBIT57, IDC_CHECK_IBIT58, IDC_CHECK_IBIT59,
	IDC_CHECK_IBIT60, IDC_CHECK_IBIT61, IDC_CHECK_IBIT62, IDC_CHECK_IBIT63
};
static UINT m_uiOutputID[64] = 
{
	IDC_CHECK_OBIT0, IDC_CHECK_OBIT1, IDC_CHECK_OBIT2, IDC_CHECK_OBIT3, IDC_CHECK_OBIT4,
	IDC_CHECK_OBIT5, IDC_CHECK_OBIT6, IDC_CHECK_OBIT7, IDC_CHECK_OBIT8, IDC_CHECK_OBIT9,
	IDC_CHECK_OBIT10, IDC_CHECK_OBIT11, IDC_CHECK_OBIT12, IDC_CHECK_OBIT13, IDC_CHECK_OBIT14,
	IDC_CHECK_OBIT15, IDC_CHECK_OBIT16, IDC_CHECK_OBIT17, IDC_CHECK_OBIT18, IDC_CHECK_OBIT19,
	IDC_CHECK_OBIT20, IDC_CHECK_OBIT21, IDC_CHECK_OBIT22, IDC_CHECK_OBIT23, IDC_CHECK_OBIT24,
	IDC_CHECK_OBIT25, IDC_CHECK_OBIT26, IDC_CHECK_OBIT27, IDC_CHECK_OBIT28, IDC_CHECK_OBIT29,
	IDC_CHECK_OBIT30, IDC_CHECK_OBIT31, IDC_CHECK_OBIT32, IDC_CHECK_OBIT33, IDC_CHECK_OBIT34,
	IDC_CHECK_OBIT35, IDC_CHECK_OBIT36, IDC_CHECK_OBIT37, IDC_CHECK_OBIT38, IDC_CHECK_OBIT39,
	IDC_CHECK_OBIT40, IDC_CHECK_OBIT41, IDC_CHECK_OBIT42, IDC_CHECK_OBIT43, IDC_CHECK_OBIT44,
	IDC_CHECK_OBIT45, IDC_CHECK_OBIT46, IDC_CHECK_OBIT47, IDC_CHECK_OBIT48, IDC_CHECK_OBIT49,
	IDC_CHECK_OBIT50, IDC_CHECK_OBIT51, IDC_CHECK_OBIT52, IDC_CHECK_OBIT53, IDC_CHECK_OBIT54,
	IDC_CHECK_OBIT55, IDC_CHECK_OBIT56, IDC_CHECK_OBIT57, IDC_CHECK_OBIT58, IDC_CHECK_OBIT59,
	IDC_CHECK_OBIT60, IDC_CHECK_OBIT61, IDC_CHECK_OBIT62, IDC_CHECK_OBIT63
};

void CIOTest::InputMonitoring()
{
//	INT uiInput[2];
	CString cStr;
	INT uiInput[8];

	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;
	if (pSheet->m_bDevOpen == FALSE)
	{
//		AfxMessageBox("장치가 열려있지 않아 상태를 확인할 수 없습니다");
		return;
	}

	while (::WaitForSingleObject(m_hInputMonQuit, 10 != WAIT_OBJECT_0))
	{
//		get_io64(0, uiInput);              //v2.9.04 , 20120607 syk 시간 관계상 AMC_SETUP은 나중에 수정하는 걸로, 그렇게 중요하지 않으니..
		get_io_input(0, uiInput); 
		PutIOStatus(m_uiInputID, uiInput);
	
		cStr.Format("%08X",uiInput[0]);
		GetDlgItem(IDC_EDIT_IO_IN1)->SetWindowText(cStr);
	
		cStr.Format("%08X",uiInput[1]);
		GetDlgItem(IDC_EDIT_IO_IN2)->SetWindowText(cStr);
	
		cStr.Format("%08X",uiInput[2]);
		GetDlgItem(IDC_EDIT_IO_IN3)->SetWindowText(cStr);
	
		cStr.Format("%08X",uiInput[3]);
		GetDlgItem(IDC_EDIT_IO_IN4)->SetWindowText(cStr);
	
		cStr.Format("%08X",uiInput[4]);
		GetDlgItem(IDC_EDIT_IO_IN5)->SetWindowText(cStr);
	
		cStr.Format("%08X",uiInput[5]);
		GetDlgItem(IDC_EDIT_IO_IN6)->SetWindowText(cStr);
	
		cStr.Format("%08X",uiInput[6]);
		GetDlgItem(IDC_EDIT_IO_IN7)->SetWindowText(cStr);
	
		cStr.Format("%08X",uiInput[7]);
		GetDlgItem(IDC_EDIT_IO_IN8)->SetWindowText(cStr);
	}
	SetEvent(m_hInputStopped);
}


void CIOTest::SetInitialButtonState()
{
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;
	if (pSheet->m_bDevOpen == FALSE)
	{
		GetDlgItem(IDC_BUTTON_OUTPUT_AUTO_CHK_STOP)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_OUTPUT_AUTO_CHK_START)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_OUTPUT_ALLSET)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_OUTPUT_ALLRESET)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_IO_W)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_IO_R)->EnableWindow(FALSE);

		AfxMessageBox("장치가 열려있지 않아 입/출력 상태를 확인할 수 없습니다");
		return;
	}

	GetDlgItem(IDC_BUTTON_OUTPUT_AUTO_CHK_STOP)->EnableWindow(FALSE);
	GetDlgItem(IDC_BUTTON_OUTPUT_AUTO_CHK_START)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_OUTPUT_ALLSET)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_OUTPUT_ALLRESET)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_IO_W)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_IO_R)->EnableWindow(TRUE);
}

void CIOTest::OnButtonOutputAllset() 
{
	// TODO: Add your control notification handler code here
#if 0
	int ui[2];
	ui[0] = -1;
	ui[1] = -1;
	set_io64(0, ui);
	UpdateOutputBitStatus();
#endif
	int ui[8];
	ui[0] = -1;
	ui[1] = -1;
	ui[2] = -1;
	ui[3] = -1;
	ui[4] = -1;
	ui[5] = -1;
	ui[6] = -1;
	ui[7] = -1;
	set_io_output(0, ui);
	UpdateOutputBitStatus();
}
void CIOTest::OnButtonInputOutputWrite() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int tmp_in  = m_uiIncount;
	int tmp_out = m_uiOutcount;	

	if (set_io_count(tmp_in, tmp_out) != MMC_OK) AfxMessageBox("ERROR : INPUT/OUTPUT SETTING");
}

void CIOTest::OnButtonInputOutputRead() 
{
	// TODO: Add your control notification handler code here

	int tmp_in;
	int tmp_out;	

	if (get_io_count(&tmp_in, &tmp_out) != MMC_OK) AfxMessageBox("ERROR : INPUT/OUTPUT READING");
	
	m_uiIncount = tmp_in;
	m_uiOutcount= tmp_out;
	
	UpdateData(FALSE);

}

void CIOTest::OnButtonOutputAllreset() 
{
	// TODO: Add your control notification handler code here
#if 0
	int ui[2];
	ui[0] = 0;
	ui[1] = 0;
	set_io64(0, ui);
	UpdateOutputBitStatus();
#endif
	int ui[8];
	ui[0] = 0;
	ui[1] = 0;
	ui[2] = 0;
	ui[3] = 0;
	ui[4] = 0;
	ui[5] = 0;
	ui[6] = 0;
	ui[7] = 0;
	set_io_output(0, ui);
	UpdateOutputBitStatus();
}

UINT _OutputMon(LPVOID lpv)
{
	CIOTest *pTest = (CIOTest *) lpv;
	pTest->OutputMon();
	AfxEndThread(0);
	return 0;
}

void CIOTest::OnButtonOutputAutoChkStart() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	ResetEvent(m_hAutoOutputQuit);
	AfxBeginThread(_OutputMon, this);

	GetDlgItem(IDC_BUTTON_OUTPUT_AUTO_CHK_START)->EnableWindow(FALSE);
	GetDlgItem(IDC_BUTTON_OUTPUT_AUTO_CHK_STOP)->EnableWindow(TRUE);
}

void CIOTest::OnButtonOutputAutoChkStop() 
{
	// TODO: Add your control notification handler code here
	SetEvent(m_hAutoOutputQuit);
	::WaitForSingleObject(m_hAutoOutputStopped, 100);

	GetDlgItem(IDC_BUTTON_OUTPUT_AUTO_CHK_START)->EnableWindow(TRUE);
	GetDlgItem(IDC_BUTTON_OUTPUT_AUTO_CHK_STOP)->EnableWindow(FALSE);
	UpdateData(FALSE);
}

void CIOTest::OutputMon()
{
#if 0
	int ofs = 0;
	int ui[2] = {0, 0};
	
	while (::WaitForSingleObject(m_hAutoOutputQuit, m_uiWaitTime) != WAIT_OBJECT_0)
	{
		ui[0] = ui[1] = 0;
		if (ofs < 32) ui[0] = (1 << ofs);
		else if (ofs < 64) ui[1] = (1 << (ofs - 32));
		else ofs = 0;

		set_io64(0, ui);

		UpdateOutputBitStatus();

		ofs ++;
	}
	SetEvent(m_hAutoOutputStopped);
#endif

	int ofs = 0;
	int ui[8] = {0, 0, 0, 0, 0, 0, 0, 0};
	
	while (::WaitForSingleObject(m_hAutoOutputQuit, m_uiWaitTime) != WAIT_OBJECT_0)
	{
		ui[0] = ui[1] = ui[2]= ui[3]= ui[4]= ui[5]= ui[6]= ui[7]= 0;

		if(ofs ==0)		Set_io_Object(0);
		if(ofs ==64)	Set_io_Object(1);
		if(ofs ==128)	Set_io_Object(2);
		if(ofs ==192)	Set_io_Object(3);

		if (ofs < 32) ui[0] = (1 << ofs);
		else if (ofs < 64) ui[1] = (1 << (ofs - 32));
		else if (ofs < 96) ui[2] = (1 << (ofs - 64));
		else if (ofs < 128) ui[3] = (1 << (ofs - 96));
		else if (ofs < 160) ui[4] = (1 << (ofs - 128));
		else if (ofs < 192) ui[5] = (1 << (ofs - 160));
		else if (ofs < 224) ui[6] = (1 << (ofs - 192));
		else if (ofs < 256) ui[7] = (1 << (ofs - 224));
		else ofs = 0;

		set_io_output(0, ui);

		UpdateOutputBitStatus();

		if(ofs >= 255)  ofs =0;
		else			ofs ++;
	}
	SetEvent(m_hAutoOutputStopped);

}

void CIOTest::UpdateOutputBitStatus() 
{
#if 0
	int ui[2] = {0, 0};
	get_out64(0, ui);	// 출력된 값을 다시 읽는다.
	PutIOStatus(m_uiOutputID, ui);
#endif
	CString cStr;
	int ui[8] = {0, 0, 0, 0, 0, 0, 0, 0};
	get_io_output(0, ui);	// 출력된 값을 다시 읽는다.
	PutIOStatus(m_uiOutputID, ui);

	cStr.Format("%08X",ui[0]);
	GetDlgItem(IDC_EDIT_IO_OUT1)->SetWindowText(cStr);

	cStr.Format("%08X",ui[1]);
	GetDlgItem(IDC_EDIT_IO_OUT2)->SetWindowText(cStr);

	cStr.Format("%08X",ui[2]);
	GetDlgItem(IDC_EDIT_IO_OUT3)->SetWindowText(cStr);

	cStr.Format("%08X",ui[3]);
	GetDlgItem(IDC_EDIT_IO_OUT4)->SetWindowText(cStr);

	cStr.Format("%08X",ui[4]);
	GetDlgItem(IDC_EDIT_IO_OUT5)->SetWindowText(cStr);

	cStr.Format("%08X",ui[5]);
	GetDlgItem(IDC_EDIT_IO_OUT6)->SetWindowText(cStr);

	cStr.Format("%08X",ui[6]);
	GetDlgItem(IDC_EDIT_IO_OUT7)->SetWindowText(cStr);

	cStr.Format("%08X",ui[7]);
	GetDlgItem(IDC_EDIT_IO_OUT8)->SetWindowText(cStr);

}

void CIOTest::OnCheckObit0() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT0);
}

void CIOTest::OnCheckObit1() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT1);
}

void CIOTest::OnCheckObit2() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT2);
}

void CIOTest::OnCheckObit3() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT3);
}

void CIOTest::OnCheckObit4() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT4);
}

void CIOTest::OnCheckObit5() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT5);
}

void CIOTest::OnCheckObit6() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT6);
}

void CIOTest::OnCheckObit7() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT7);
}

void CIOTest::OnCheckObit8() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT8);
}

void CIOTest::OnCheckObit9() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT9);
}

void CIOTest::OnCheckObit10() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT10);
}

void CIOTest::OnCheckObit11() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT11);
}

void CIOTest::OnCheckObit12() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT12);
}

void CIOTest::OnCheckObit13() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT13);
}

void CIOTest::OnCheckObit14() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT14);
}

void CIOTest::OnCheckObit15() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT15);
}

void CIOTest::OnCheckObit16() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT16);
}

void CIOTest::OnCheckObit17() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT17);
}

void CIOTest::OnCheckObit18() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT18);
}

void CIOTest::OnCheckObit19() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT19);
}

void CIOTest::OnCheckObit20() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT20);
}

void CIOTest::OnCheckObit21() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT21);
}

void CIOTest::OnCheckObit22() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT22);
}

void CIOTest::OnCheckObit23() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT23);
}

void CIOTest::OnCheckObit24() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT24);
}

void CIOTest::OnCheckObit25() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT25);
}

void CIOTest::OnCheckObit26() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT26);
}

void CIOTest::OnCheckObit27() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT27);
}

void CIOTest::OnCheckObit28() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT28);
}

void CIOTest::OnCheckObit29() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT29);
}

void CIOTest::OnCheckObit30() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT30);
}

void CIOTest::OnCheckObit31() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT31);
}

void CIOTest::OnCheckObit32() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT32);
}

void CIOTest::OnCheckObit33() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT33);
}

void CIOTest::OnCheckObit34() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT34);
}

void CIOTest::OnCheckObit35() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT35);
}

void CIOTest::OnCheckObit36() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT36);
}

void CIOTest::OnCheckObit37() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT37);
}

void CIOTest::OnCheckObit38() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT38);
}

void CIOTest::OnCheckObit39() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT39);
}

void CIOTest::OnCheckObit40() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT40);
}

void CIOTest::OnCheckObit41() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT41);
}

void CIOTest::OnCheckObit42() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT42);
}

void CIOTest::OnCheckObit43() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT43);
}

void CIOTest::OnCheckObit44() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT44);
}

void CIOTest::OnCheckObit45() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT45);
}

void CIOTest::OnCheckObit46() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT46);
}

void CIOTest::OnCheckObit47() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT47);
}

void CIOTest::OnCheckObit48() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT48);
}

void CIOTest::OnCheckObit49() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT49);
}

void CIOTest::OnCheckObit50() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT50);
}

void CIOTest::OnCheckObit51() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT51);
}

void CIOTest::OnCheckObit52() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT52);
}

void CIOTest::OnCheckObit53() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT53);
}

void CIOTest::OnCheckObit54() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT54);
}

void CIOTest::OnCheckObit55() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT55);
}

void CIOTest::OnCheckObit56() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT56);
}

void CIOTest::OnCheckObit57() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT57);
}

void CIOTest::OnCheckObit58() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT58);
}

void CIOTest::OnCheckObit59() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT59);
}

void CIOTest::OnCheckObit60() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT60);
}

void CIOTest::OnCheckObit61() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT61);
}

void CIOTest::OnCheckObit62() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT62);
}

void CIOTest::OnCheckObit63() 
{
	// TODO: Add your control notification handler code here
	DoOnOff(IDC_CHECK_OBIT63);
}

void CIOTest::Set_io_text()
{
	CString cStr;
	if(s_iosel1 ==0)		cStr = _T("IO 0 ~ 63 테스트 모드");
	else if(s_iosel2 ==0)	cStr = _T("IO 64 ~ 127 테스트 모드");
	else if(s_iosel3 ==0)	cStr = _T("IO 128 ~ 191 테스트 모드");
	else if(s_iosel4 ==0)	cStr = _T("IO 192 ~ 255 테스트 모드");
	else					cStr = _T("IO를 재 선택 하세요 !!");

	GetDlgItem(IDC_EDIT_IOSEL)->SetWindowText(cStr);
}

void CIOTest::Set_io_Object(int nOnOfs)
{
	s_iosel1 = (nOnOfs == 0) ? 0 : -1;
	s_iosel2 = (nOnOfs == 1) ? 0 : -1;
	s_iosel3 = (nOnOfs == 2) ? 0 : -1;
	s_iosel4 = (nOnOfs == 3) ? 0 : -1;
	Set_io_text();
}

void CIOTest::OnRadio_sel_io1() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	Set_io_Object(0);
	UpdateData(FALSE);
	UpdateOutputBitStatus();

}
void CIOTest::OnRadio_sel_io2() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	Set_io_Object(1);
	UpdateData(FALSE);
	UpdateOutputBitStatus();
}
void CIOTest::OnRadio_sel_io3() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	Set_io_Object(2);
	UpdateData(FALSE);
	UpdateOutputBitStatus();
}
void CIOTest::OnRadio_sel_io4() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	Set_io_Object(3);
	UpdateData(FALSE);
	UpdateOutputBitStatus();
}

void CIOTest::DoOnOff(UINT uiID)
{
	// 출력비트를 나타내는 ID값이 연번이 아니어서 좀 복잡해짐.
	int ofs;
	if (uiID >= IDC_CHECK_OBIT1) ofs = uiID - IDC_CHECK_OBIT1 + 1;
	else ofs = 0;

	CButton *pBtn = (CButton*) GetDlgItem(uiID);
	int stt = pBtn->GetCheck();

	if(s_iosel1 ==0)
	{
		if (stt) set_bit(ofs);
		else reset_bit(ofs);
	}
	else if(s_iosel2 ==0)
	{
		if (stt) set_bit(ofs+64);
		else reset_bit(ofs+64);
	}
	else if(s_iosel3 ==0)
	{
		if (stt) set_bit(ofs+128);
		else reset_bit(ofs+128);
	}
	else if(s_iosel4 ==0)
	{
		if (stt) set_bit(ofs+192);
		else reset_bit(ofs+192);
	}
	UpdateOutputBitStatus();
}
