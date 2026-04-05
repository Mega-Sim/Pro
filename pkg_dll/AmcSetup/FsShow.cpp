// FsShow.cpp : implementation file
//

#include "stdafx.h"
#include "amcsetup.h"
#include "FsShow.h"
#include "MyPropertySheet.h"


#include "../include/amc_fs.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


#define		LB_NAME_OFS		2

#define		PROGRESS_MSGID			WM_USER+100

extern CString FormattingVersion(int ver);

/////////////////////////////////////////////////////////////////////////////
// CFsShow property page

IMPLEMENT_DYNCREATE(CFsShow, CPropertyPage)

CFsShow::CFsShow() : CPropertyPage(CFsShow::IDD)
{
	//{{AFX_DATA_INIT(CFsShow)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
}

CFsShow::~CFsShow()
{
}

void CFsShow::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CFsShow)
	DDX_Control(pDX, IDC_PROGRESS1, m_cProgress);
	DDX_Control(pDX, IDC_LIST_FS_LIST, m_cList);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CFsShow, CPropertyPage)
	//{{AFX_MSG_MAP(CFsShow)
	ON_BN_CLICKED(IDC_BUTTON_REFRESH, OnButtonRefresh)
	ON_BN_CLICKED(IDC_BUTTON_DELETE, OnButtonDelete)
	ON_BN_CLICKED(IDC_BUTTON_DNLOAD, OnButtonDnload)
	ON_BN_CLICKED(IDC_BUTTON_UPLOAD, OnButtonUpload)
	ON_BN_CLICKED(IDC_BUTTON_SETBOOT, OnButtonSetboot)
	ON_BN_CLICKED(IDC_BUTTON_FORMAT, OnButtonFormat)
	ON_BN_CLICKED(IDC_BUTTON_SYSTEMINFO, OnButtonSysteminfo)
	ON_BN_CLICKED(IDC_BUTTON_RESTART, OnButtonRestart)
	ON_BN_CLICKED(IDC_BUTTON_DUMP1, OnButtonDump1)
	ON_BN_CLICKED(IDC_BUTTON_DUMP2, OnButtonDump2)
	ON_BN_CLICKED(IDC_BUTTON_DUMP3, OnButtonDump3)
	ON_BN_CLICKED(IDC_BUTTON_FILE_INSTALL, OnButtonFileInstall)
	//}}AFX_MSG_MAP
	ON_MESSAGE(PROGRESS_MSGID, OnProceedings)
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CFsShow message handlers




LRESULT CFsShow::OnProceedings(WPARAM wParam, LPARAM lParam)
{
	if (wParam == 0)
	{
		m_cProgress.SetRange32(0, lParam);
	}
	m_cProgress.SetPos(wParam);

	char str[100];
	sprintf(str, "%7dKB/%7dKB", wParam * 256 / 1024, lParam * 256 / 1024);
	if (wParam != (unsigned int)lParam)	//2011.10.10
		SetDlgItemText(IDC_STATIC_PROGRESS, str);
	else 
		SetDlgItemText(IDC_STATIC_PROGRESS, "처리중입니다...");

	return 0;
}


BOOL CFsShow::OnSetActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	SetInitialButtonState();
	OnButtonRefresh();

	return CPropertyPage::OnSetActive();
}

void CFsShow::OnButtonRefresh() 
{
	// TODO: Add your control notification handler code here
	DeleteAllItems();
	RefreshSystemData();
	RefreshFileLists();

	// 진행 Bar를 초기화 한다.
	OnProceedings(0, 100);
}

void CFsShow::OnButtonDelete() 
{
	// TODO: Add your control notification handler code here
	int pos = GetCurSel();
	if (pos < 0) return;

	char szfname[100];
	m_cList.GetItemText(pos, LB_NAME_OFS, szfname, 100);

	if (fs_deletefile(szfname) == MMC_OK) OnButtonRefresh();
}

void CFsShow::OnButtonDnload() 
{
	// TODO: Add your control notification handler code here
	CFileDialog cFDialog(TRUE, "*.*");
	if (cFDialog.DoModal() == IDOK)
	{
		CString sName = cFDialog.GetPathName();
		int rtn,err;
#if 0
		if (fs_download((char *) (LPCTSTR) sName, &rtn, GetSafeHwnd(), PROGRESS_MSGID) == MMC_OK)
		{
			Sleep(1000);
			OnButtonRefresh();
		}
		SetDlgItemText(IDC_STATIC_PROGRESS, "다운로드가 완료되었습니다");
#else// 120216 2.5.27v2.8.07 syk fs_install error 코드 처리 
		if (err=fs_download((char *) (LPCTSTR) sName, &rtn, GetSafeHwnd(), PROGRESS_MSGID) == MMC_OK)
		{
			Sleep(1000);
			OnButtonRefresh();
			if(rtn == MMC_OK)	SetDlgItemText(IDC_STATIC_PROGRESS, "[SUCCESS]설치가 완료되었습니다");
			else if(rtn == -1)  SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=File open error");
			else if(rtn == -2)	SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=NO ACK");
			else if(rtn == -3)	SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=illegal ACK");
			else if(rtn == -4)	SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=File Size Over");
			else if(rtn == -5)	SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=Lack Of Space");
			else				SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=illegal param");
		}
		else
		{
			CString str, str1;
			CString cStr;

			if(err == 2)		str ="PC=MMC_TIME_ERROR";
			else if(err == 10)	str ="PC=File_NON_EXIST";
			else			    str ="PC=illegal param";
			
			if(rtn == MMC_OK)	str1 ="DSP= NONE!";
			else if(rtn == -1)  str1 ="DSP=File open error";
			else if(rtn == -2)	str1 ="DSP=NO ACK";
			else if(rtn == -3)	str1 ="DSP=illegal ACK";
			else if(rtn == -4)	str1 ="DSP=File Size Over";
			else if(rtn == -5)	str1 ="DSP=Lack Of Space";
			else				str1 ="DSP=illegal param";

			cStr.Format("[FAIL]%s ,%s", str, str1);
			SetDlgItemText(IDC_STATIC_BT_PROGRESS, cStr);

		}
#endif
	}
}

int CFsShow::GetCurSel()
{
	POSITION pos = m_cList.GetFirstSelectedItemPosition();
	return m_cList.GetNextSelectedItem(pos);
}

void CFsShow::OnButtonUpload() 
{
	// TODO: Add your control notification handler code here
	BOOL bf = 1;
	unsigned long ulSize;
	int pos = GetCurSel();
	if (pos < 0) return;

	char szfname[100];
	m_cList.GetItemText(pos, LB_NAME_OFS, szfname, 100);

	if (fs_getfilesize(pos, &ulSize) == MMC_OK)
	{
		char *pdata = (char*) malloc(ulSize);
		if (pdata != NULL)
		{
			FILE *fp = fopen(szfname, "rb");
			if (fp) fclose(fp);
			if (fp == NULL) 
			{
				unsigned long retsize;
				if (fs_upload(szfname, pdata, &retsize, GetSafeHwnd(), PROGRESS_MSGID) == MMC_OK)
				{
					if (retsize == ulSize) 
					{
						fp = fopen(szfname, "wb");
						if (fp)
						{
							fwrite(pdata, 1, retsize, fp);
							fclose(fp);
						}
					} else 
					{
						AfxMessageBox("수신한 데이터 크기가 다릅니다");
						bf = 0;
					}
				} else 
				{
					AfxMessageBox("동작에 실패했습니다");
					bf = 0;
				}
				SetDlgItemText(IDC_STATIC_PROGRESS, "업로드가 완료되었습니다");

			} else
			{
				AfxMessageBox("동일한 파일이 있어서 저장하지 못했습니다");
				bf = 0;
			}
		}
		free(pdata);
	}
	if (bf == 1)
	{
		char str[300];
		CString cStr;
		GetCurrentDirectory(300, str);
		cStr.Format("다음의 경로에 저장했습니다\r\n'%s\\%s'", str, szfname);
		AfxMessageBox(cStr);
	}
}

void CFsShow::OnButtonSetboot() 
{
	// TODO: Add your control notification handler code here
	int pos = GetCurSel();
	if (pos < 0) return;

	char szfname[100];
	m_cList.GetItemText(pos, LB_NAME_OFS, szfname, 100);

	if (fs_setbootfile(szfname) == MMC_OK) OnButtonRefresh();
}

void CFsShow::OnButtonFormat() 
{
	// TODO: Add your control notification handler code here
	if (fs_format() == MMC_OK) OnButtonRefresh();
}

void CFsShow::OnButtonSysteminfo() 
{
	// TODO: Add your control notification handler code here
	RefreshSystemData();
}

BOOL CFsShow::OnInitDialog() 
{
	CPropertyPage::OnInitDialog();
	
	// TODO: Add extra initialization here
	DeleteAllItems();
    
	m_cList.InsertColumn(0, "No.", LVCFMT_LEFT, 50);
	m_cList.InsertColumn(1, "Boot", LVCFMT_LEFT, 100);
	m_cList.InsertColumn(2, "Name", LVCFMT_LEFT, 150);
	m_cList.InsertColumn(3, "Size", LVCFMT_LEFT, 100);
	m_cList.ModifyStyle( LVS_TYPEMASK, LVS_REPORT );
	m_cList.SetExtendedStyle(
		m_cList.GetExtendedStyle()
		| LVS_EX_FULLROWSELECT 
		| LVS_EX_GRIDLINES 
		| LVS_EX_HEADERDRAGDROP 
//		| LVS_EX_CHECKBOXES
		);
	
	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}

void CFsShow::DeleteAllItems()
{
	int nLen = m_cList.GetItemCount();
	for( int i = nLen - 1; i >= 0; i-- )
	{
		m_cList.DeleteItem(i);
	}
}

void CFsShow::SetInitialButtonState()
{
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpParentSheet;
	if (pSheet->m_bDevOpen == FALSE)
	{
		GetDlgItem(IDC_BUTTON_REFRESH)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_DELETE)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_DNLOAD)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_UPLOAD)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_SETBOOT)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_FORMAT)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_SYSTEMINFO)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_RESTART)->EnableWindow(FALSE);
	} else {
		GetDlgItem(IDC_BUTTON_REFRESH)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_DELETE)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_DNLOAD)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_UPLOAD)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_SETBOOT)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_FORMAT)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_SYSTEMINFO)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_RESTART)->EnableWindow(TRUE);
	}
}


void CFsShow::RefreshSystemData()
{
	SYSTEM_DATA sData;
	CString cstr = _T("");
	if (fs_getsysteminfo(&sData) == MMC_OK)
	{
		cstr.Format("Type:%d,   Boot File Name : '%s'", 
			sData.m_nCPUType, sData.m_sRunFileName);

	}
	GetDlgItem(IDC_EDIT_SYSTEMINFO)->SetWindowText(cstr);
}

void CFsShow::RefreshFileLists()
{
	int nfile;
	SYSTEM_DATA sData;
	char szname[100];
	unsigned long ulsize;
	BOOL bf;
	
	if (fs_getsysteminfo(&sData) != MMC_OK) return;
	
	UINT uiTot = 0;
	if (fs_files(&nfile) == MMC_OK)
	{
		for (int i = 0; i < nfile; i ++)
		{
			if (fs_getfilename(i, szname) != MMC_OK) break;
			if (fs_getfilesize(i, &ulsize) != MMC_OK) break;

			if (strcmp(sData.m_sRunFileName, szname) == 0) bf = 1;
			else bf = 0;

			char szNo[10];
			sprintf(szNo, "%d", i + 1);
			m_cList.InsertItem(i, szNo, 0 );
			m_cList.SetItemText(i, 1, bf ? "Boot" : "");
			m_cList.SetItemText(i, 2, szname);

			sprintf(szname, "%d", ulsize);
			m_cList.SetItemText(i, 3, szname);

			uiTot += ulsize;
		}
	}

	sprintf(szname, "%d", uiTot);
	SetDlgItemText(IDC_EDIT_TOTAL_BYTES, szname);
}


void CFsShow::OnButtonRestart() 
{
	// TODO: Add your control notification handler code here
	dsp_reboot();

	AfxMessageBox("이 프로그램을 종료한 후 재시작 하시기 바랍니다");
}

void CFsShow::OnButtonDump1() 
{
	// TODO: Add your control notification handler code here
	fs_dumpinfo(0);
}

void CFsShow::OnButtonDump2() 
{
	// TODO: Add your control notification handler code here
	fs_dumpinfo(1);
}

void CFsShow::OnButtonDump3() 
{
	// TODO: Add your control notification handler code here
	fs_dumpinfo(2);
}

void CFsShow::OnButtonFileInstall() 
{
	// TODO: Add your control notification handler code here
	// TODO: Add your control notification handler code here
	CFileDialog cFDialog(TRUE, "*.*");
	if (cFDialog.DoModal() == IDOK)
	{
		CString sName = cFDialog.GetPathName();
		int rtn,err;
#if 0
		if (fs_install("coffboot.bin", (char *) (LPCTSTR) sName, &rtn, GetSafeHwnd(), PROGRESS_MSGID) == MMC_OK)
		{
			Sleep(1000);
			OnButtonRefresh();
		}
		SetDlgItemText(IDC_STATIC_PROGRESS, "설치가 완료되었습니다");
#else // 120216 2.5.27v2.8.07 syk fs_install error 코드 처리  
		if (err=fs_install("coffboot.bin", (char *) (LPCTSTR) sName, &rtn, GetSafeHwnd(), PROGRESS_MSGID) == MMC_OK)
		{
			Sleep(1000);
			OnButtonRefresh();
			if(rtn == MMC_OK)	SetDlgItemText(IDC_STATIC_PROGRESS, "[SUCCESS]설치가 완료되었습니다");
			else if(rtn == -1)  SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=File open error");
			else if(rtn == -2)	SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=NO ACK");
			else if(rtn == -3)	SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=illegal ACK");
			else if(rtn == -4)	SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=File Size Over");
			else if(rtn == -5)	SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=Lack Of Space");
			else				SetDlgItemText(IDC_STATIC_PROGRESS, "[FAIL]PC=OK, DSP=illegal param");
		}
		else
		{
			CString str, str1;
			CString cStr;

			if(err == 2)		str ="PC=MMC_TIME_ERROR";
			else if(err == 10)	str ="PC=File_NON_EXIST";
			else			    str ="PC=illegal param";
			
			if(rtn == MMC_OK)	str1 ="DSP= NONE!";
			else if(rtn == -1)  str1 ="DSP=File open error";
			else if(rtn == -2)	str1 ="DSP=NO ACK";
			else if(rtn == -3)	str1 ="DSP=illegal ACK";
			else if(rtn == -4)	str1 ="DSP=File Size Over";
			else if(rtn == -5)	str1 ="DSP=Lack Of Space";
			else				str1 ="DSP=illegal param";

			cStr.Format("[FAIL]%s ,%s", str, str1);
			SetDlgItemText(IDC_STATIC_BT_PROGRESS, cStr);

		}
#endif

	}
}
