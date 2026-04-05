// SyncMotion.cpp : implementation file
//

#include "stdafx.h"
#include "amcsetup.h"
#include "SyncMotion.h"
#include "MyPropertySheet.h"
#include <time.h>

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CSyncMotion property page

IMPLEMENT_DYNCREATE(CSyncMotion, CPropertyPage)

CSyncMotion::CSyncMotion() : CPropertyPage(CSyncMotion::IDD)
{
	//{{AFX_DATA_INIT(CSyncMotion)
	m_nMAxis = 0;
	m_nSAxis = 1;
	m_fGain = 1.0f;
	m_bWdtControl = FALSE;
	m_bWdtExtra = FALSE;
	m_bWdtMain = FALSE;
	m_bWdtSubcontrol = FALSE;
	m_sWdtStatus = _T("");
	m_sDbgStatus = _T("");
	m_sDpramTest = _T("");

	sync_pm_axis = 0;
	sync_pm_use	 = 1;	
	sync_pm_value= 100000;

	sync_avm_axis	= 0;
	sync_avm_value	= 100000;
	sync_avm_event	= 0;
	sync_avm_time	= 50;
	//}}AFX_DATA_INIT
}

CSyncMotion::~CSyncMotion()
{
}

void CSyncMotion::DoDataExchange(CDataExchange* pDX)
{
	CPropertyPage::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CSyncMotion)
	DDX_Text(pDX, IDC_EDIT_MASTER_AXIS, m_nMAxis);
	DDV_MinMaxUInt(pDX, m_nMAxis, 0, 3);
	DDX_Text(pDX, IDC_EDIT_SLAVE_AXIS, m_nSAxis);
	DDV_MinMaxUInt(pDX, m_nSAxis, 0, 3);
	DDX_Text(pDX, IDC_EDIT_GAIN, m_fGain);
	DDX_Check(pDX, IDC_CHECK_CONTROL_STATUS, m_bWdtControl);
	DDX_Check(pDX, IDC_CHECK_EXTRA_STATUS, m_bWdtExtra);
	DDX_Check(pDX, IDC_CHECK_MAIN_STATUS, m_bWdtMain);
	DDX_Check(pDX, IDC_CHECK_SUBCONTROL_STATUS, m_bWdtSubcontrol);
	DDX_Text(pDX, IDC_EDIT_WDT_STATUS, m_sWdtStatus);
	DDX_Text(pDX, IDC_EDIT_DBG_STATUS, m_sDbgStatus);
	DDX_Text(pDX, IDC_EDIT_DPRAM_TEST, m_sDpramTest);
// 20120326 syk 함수 추가
	DDX_Text(pDX, IDC_EDIT_PM_AXIS, sync_pm_axis);
	DDV_MinMaxUInt(pDX, sync_pm_axis, 0, 3);
	DDX_Text(pDX, IDC_EDIT_PM_USE, sync_pm_use);
	DDX_Text(pDX, IDC_EDIT_PM_VALUE, sync_pm_value);

	DDX_Text(pDX, IDC_EDIT_AVM_AXIS, sync_avm_axis);
	DDV_MinMaxUInt(pDX, sync_avm_axis, 0, 3);
	DDX_Text(pDX, IDC_EDIT_AVM_VALUE, sync_avm_value);
	DDX_Text(pDX, IDC_EDIT_AVM_EVENT, sync_avm_event);
	DDX_Text(pDX, IDC_EDIT_AVM_TIME, sync_avm_time);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CSyncMotion, CPropertyPage)
	//{{AFX_MSG_MAP(CSyncMotion)
	ON_BN_CLICKED(IDC_BUTTON_SET_MAP, OnButtonSetMap)
	ON_BN_CLICKED(IDC_BUTTON_CONTROL_ON, OnButtonControlOn)
	ON_BN_CLICKED(IDC_BUTTON_CONTROL_OFF, OnButtonControlOff)
	ON_BN_CLICKED(IDC_BUTTON_SET_GAIN, OnButtonSetGain)
	ON_BN_CLICKED(IDC_BUTTON_GET_GAIN, OnButtonGetGain)
	ON_WM_TIMER()
	ON_BN_CLICKED(IDC_BUTTON_ENABLE_WDT, OnButtonEnableWdt)
	ON_BN_CLICKED(IDC_BUTTON_SET_WDT_STATUS, OnButtonSetWdtStatus)
	ON_BN_CLICKED(IDC_BUTTON_GET_WDT_STATUS, OnButtonGetWdtStatus)
	ON_BN_CLICKED(IDC_CHECK_MAIN_STATUS, OnCheckMainStatus)
	ON_BN_CLICKED(IDC_CHECK_EXTRA_STATUS, OnCheckExtraStatus)
	ON_BN_CLICKED(IDC_CHECK_SUBCONTROL_STATUS, OnCheckSubcontrolStatus)
	ON_BN_CLICKED(IDC_CHECK_CONTROL_STATUS, OnCheckControlStatus)
	ON_BN_CLICKED(IDC_BUTTON_CLR_WDT_REASON, OnButtonClrWdtReason)
	ON_BN_CLICKED(IDC_BUTTON_GET_DBG_STATUS, OnButtonGetDbgStatus)
	ON_BN_CLICKED(IDC_BUTTON_GET_DBG_STATUS2, OnButtonGetDbgStatus2)
	ON_BN_CLICKED(IDC_BUTTON_DPRAM_TEST, OnButtonDpramTest)

	ON_BN_CLICKED(IDC_BUTTON_SET_VEL_CURVE, OnButtonSetVelCurve)
	ON_BN_CLICKED(IDC_BUTTON_GET_VEL_CURVE, OnButtonGetVelCurve)
	ON_BN_CLICKED(IDC_BUTTON_SET_ACTVEL_MARGIN, OnButtonSetActvelMargin)
	ON_BN_CLICKED(IDC_BUTTON_GET_ACTVEL_MARGIN, OnButtonGetActvelMargin)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CSyncMotion message handlers

void CSyncMotion::OnButtonSetMap() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if (IsValidAxis() == FALSE)
	{
		AfxMessageBox("Master축은 Slave 축보다 앞쪽의 것을 이용해야 합니다");
		return;
	}
	if (set_sync_map_axes(m_nMAxis, m_nSAxis) != MMC_OK)
	{
		AfxMessageBox("축을 설정하는데 에러가 발생했습니다");
		return;
	}
}

void CSyncMotion::OnButtonControlOn() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	if (IsValidAxis() == FALSE)
	{
		AfxMessageBox("Master축은 Slave 축보다 앞쪽의 것을 이용해야 합니다");
		return;
	}
	if (set_sync_control(TRUE) != MMC_OK)
		AfxMessageBox("제어를 설정하지 못했습니다");
	UpdateControlStatus();
}

void CSyncMotion::OnButtonControlOff() 
{
	// TODO: Add your control notification handler code here
	OnButtonSetMap();
	if (IsValidAxis() == FALSE)
	{
		AfxMessageBox("Master축은 Slave 축보다 앞쪽의 것을 이용해야 합니다");
		return;
	}
	if (set_sync_control(FALSE) != MMC_OK)
		AfxMessageBox("제어를 해제하지 못했습니다");
	UpdateControlStatus();
}

void CSyncMotion::OnButtonSetGain() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);

	if (IsValidAxis() == FALSE)
	{
		AfxMessageBox("축 지정에 잘못이 있습니다.\r\nMaster는 Slave보다 앞쪽의 축을 지정해야 합니다");
		return;
	}

	if (set_sync_gain(m_fGain) != MMC_OK) AfxMessageBox("이득을 설정하지 못했습니다");
}

void CSyncMotion::OnButtonGetGain() 
{
	// TODO: Add your control notification handler code here
	if (IsValidAxis() == FALSE)
	{
		AfxMessageBox("Master축은 Slave 축보다 앞쪽의 것을 이용해야 합니다");
		return;
	}
	if (get_sync_gain(&m_fGain) != MMC_OK) AfxMessageBox("이득을 가져오지 못했습니다");
	UpdateData(FALSE);
}

BOOL CSyncMotion::OnSetActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	CMyPropertySheet *pSheet = (CMyPropertySheet *) m_lpSheetPtr;
	if (pSheet->m_bDevOpen == FALSE)
	{
		GetDlgItem(IDC_BUTTON_SET_MAP)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_CONTROL_ON)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_CONTROL_OFF)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_SET_GAIN)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_GET_GAIN)->EnableWindow(FALSE);

		GetDlgItem(IDC_BUTTON_SET_VEL_CURVE)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_GET_VEL_CURVE)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_SET_ACTVEL_MARGIN)->EnableWindow(FALSE);
		GetDlgItem(IDC_BUTTON_GET_ACTVEL_MARGIN)->EnableWindow(FALSE);
		AfxMessageBox("장치가 열려있지 않아 정상적인 동작을 실행 할 수 없습니다");
	} else {
		GetDlgItem(IDC_BUTTON_SET_MAP)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_CONTROL_ON)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_CONTROL_OFF)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_SET_GAIN)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_GET_GAIN)->EnableWindow(TRUE);

		GetDlgItem(IDC_BUTTON_SET_VEL_CURVE)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_GET_VEL_CURVE)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_SET_ACTVEL_MARGIN)->EnableWindow(TRUE);
		GetDlgItem(IDC_BUTTON_GET_ACTVEL_MARGIN)->EnableWindow(TRUE);

		UpdateControlStatus();
	}
	SetTimer(1, 10, NULL);
	return CPropertyPage::OnSetActive();
}

void CSyncMotion::UpdateControlStatus()
{
//	BOOL bf;
	int bf;		//2.8.5, 2011.12.22 syk int aa= int2 bb;(typedef int BOOL)
	if (get_sync_control(&bf) == MMC_OK)	//s: int * = bool 
	{
		if (bf)
		{
			GetDlgItem(IDC_STATIC_CTRL_ON)->SetWindowText("<- ON");
			GetDlgItem(IDC_STATIC_CTRL_OFF)->SetWindowText("");
			GetDlgItem(IDC_BUTTON_CONTROL_ON)->EnableWindow(FALSE);
			GetDlgItem(IDC_BUTTON_CONTROL_OFF)->EnableWindow(TRUE);
		} else {
			GetDlgItem(IDC_STATIC_CTRL_ON)->SetWindowText("");
			GetDlgItem(IDC_STATIC_CTRL_OFF)->SetWindowText("<- Off");
			GetDlgItem(IDC_BUTTON_CONTROL_ON)->EnableWindow(TRUE);
			GetDlgItem(IDC_BUTTON_CONTROL_OFF)->EnableWindow(FALSE);
		}
	}
}

BOOL CSyncMotion::IsValidAxis()
{
	UpdateData(TRUE);
	if (m_nMAxis == m_nSAxis) return FALSE;
	return TRUE;
}

BOOL CSyncMotion::OnKillActive() 
{
	// TODO: Add your specialized code here and/or call the base class
	KillTimer(1);
	return CPropertyPage::OnKillActive();
}

void CSyncMotion::OnTimer(UINT nIDEvent) 
{
	// TODO: Add your message handler code here and/or call default
	if (nIDEvent != 1) return;

	double dpos1, dpos2;
	if (get_sync_position(&dpos1, &dpos2) != MMC_OK) return;

	char str[300];
	sprintf(str, "M=%f, S=%f", dpos1, dpos2);
	GetDlgItem(IDC_STATIC_SYNC_POSITION)->SetWindowText(str);

	CPropertyPage::OnTimer(nIDEvent);
}

void CSyncMotion::OnButtonEnableWdt() 
{
	// TODO: Add your control notification handler code here
	if (m_bWdtMain) enable_wdt_reason(WDT_MAINLOOP);
	else disable_wdt_reason(WDT_MAINLOOP);

	if (m_bWdtExtra) enable_wdt_reason(WDT_EXTRA);
	else disable_wdt_reason(WDT_EXTRA);

	if (m_bWdtSubcontrol) enable_wdt_reason(WDT_SUBCONTROL);
	else disable_wdt_reason(WDT_SUBCONTROL);

	if (m_bWdtControl) enable_wdt_reason(WDT_CONTROL);
	else disable_wdt_reason(WDT_CONTROL);
}

void CSyncMotion::OnButtonSetWdtStatus() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int val = atoi((char *) (LPCTSTR) m_sWdtStatus);
	set_wdt_status(val);
}

void CSyncMotion::OnButtonGetWdtStatus() 
{
	// TODO: Add your control notification handler code here
	unsigned int val;
	get_wdt_status(&val);
	m_sWdtStatus.Format("%02X", val);
	UpdateData(FALSE);
}

void CSyncMotion::OnCheckMainStatus() 
{
	// TODO: Add your control notification handler code here
	m_bWdtMain ^= 1;
}

void CSyncMotion::OnCheckExtraStatus() 
{
	// TODO: Add your control notification handler code here
	m_bWdtExtra ^= 1;
}

void CSyncMotion::OnCheckSubcontrolStatus() 
{
	// TODO: Add your control notification handler code here
	m_bWdtSubcontrol ^= 1;
}

void CSyncMotion::OnCheckControlStatus() 
{
	// TODO: Add your control notification handler code here
	m_bWdtControl ^= 1;
}

void CSyncMotion::OnButtonClrWdtReason() 
{
	// TODO: Add your control notification handler code here
	if (m_bWdtMain) clr_wdt_reason(WDT_MAINLOOP);

	if (m_bWdtExtra) clr_wdt_reason(WDT_EXTRA);

	if (m_bWdtSubcontrol) clr_wdt_reason(WDT_SUBCONTROL);

	if (m_bWdtControl) clr_wdt_reason(WDT_CONTROL);
}


void CSyncMotion::OnButtonGetDbgStatus() 
{
	// TODO: Add your control notification handler code here
	unsigned char ucstatus[256];
	if (get_dbg_status(ucstatus) == MMC_OK) PutDbgStatus(ucstatus);
}


FILE *fpcan;
void CSyncMotion::OnButtonGetDbgStatus2() 
{
	unsigned char ucstatus[256];
    time_t rawtime;
    struct tm * timeinfo;
	char name[80]="****CANTOPS DBG LOG****\r\n";
	fpcan = fopen("c:\\cantops\\file.txt","w");
	if(!fpcan)
	{
		//puts("lib 로그 파일 열기 실패");
		AfxMessageBox("lib 로그 파일 열기 실패");
		return ;
	}


	fprintf(fpcan,"%s \r\n",name);
    time ( &rawtime );
    timeinfo = localtime ( &rawtime );
    fprintf (fpcan, "Current local time and date: %s", asctime (timeinfo) );

//1~3  :  sMotionMakeMsgBuff[MOTIONMAKE_MSG]
//4~13 :  sMotionCalMsgBuff[MOTIONCAL_MSG]
//14~43:  sEventMsgBuff[EVENT_MSG][MMC_AXIS]
    fprintf (fpcan, "\r\n===Motion Make Debug DATA 12===\r\n");
	if (get_dbg_status2(ucstatus,1) == MMC_OK) PutMotionMakeDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,2) == MMC_OK) PutMotionMakeDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,3) == MMC_OK) PutMotionMakeDbgStatus(ucstatus);
    fprintf (fpcan, "\r\n===Motion Calculation Debug DATA 40===\r\n");
	if (get_dbg_status2(ucstatus,4) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,5) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,6) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,7) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,8) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,9) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,10) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,11) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,12) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,13) == MMC_OK) PutMotionCalDbgStatus(ucstatus);
    fprintf (fpcan, "\r\n===Motion Calculation Debug DATA 30===\r\n");
	if (get_dbg_status2(ucstatus,14) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,15) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,16) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,17) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,18) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,19) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,20) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,21) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,22) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,23) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,24) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,25) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,26) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,27) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,28) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,29) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,30) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,31) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,32) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,33) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,34) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,35) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,36) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,37) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,38) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,39) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,40) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,41) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,42) == MMC_OK) PutEventDbgStatus(ucstatus);
	if (get_dbg_status2(ucstatus,43) == MMC_OK) PutEventDbgStatus(ucstatus);
	fclose(fpcan);
}


int CSyncMotion::GetInt(char *ptr)
{
	int *pi = (int *) ptr;
	return *pi;
}
float CSyncMotion::GetFloat(char *ptr)
{
	float *pf = (float *) ptr;
	return *pf;
}
short CSyncMotion::GetShort(char *ptr)
{
	short *ps = (short *) ptr;
	return *ps;
}
char *CSyncMotion::GetCommandString(char cmd)
{
	switch (cmd)
	{
	case 1: return "v_move";
	case 2: return "move_p";
	case 3: return "move_n";
	case 4: return "move_s";
	case 5: return "move_ds";
	case 6: return "ptp";
	case 7: return "io";
	case 8: return "dwell";
	}
	return "None";
}

void CSyncMotion::PutMotionMakeDbgStatus(unsigned char ucstatus[256])
{
	DBG_MOTIONMAKE_MSG_BUFFER *psMsgBuff = (DBG_MOTIONMAKE_MSG_BUFFER *) ucstatus;
//	fprintf(fpcan, "#Buffer[%d]\r\n",psMsgBuff->Buffer_no & 0xff));

	for (int i = 0; i < 4; i ++, psMsgBuff++)
	{
		fprintf(fpcan, "#Buffer[%d]\r\n",psMsgBuff->buffer_no & 0xff);
		fprintf(fpcan, "motion_point : %d \r\n",psMsgBuff->motion_sort & 0xff);
		fprintf(fpcan, "ax : %d \r\n",psMsgBuff->ax & 0xff);
		fprintf(fpcan, "acc: %f \r\n",psMsgBuff->acc);
		fprintf(fpcan, "dcc : %f \r\n",psMsgBuff->dcc);
		fprintf(fpcan, "vel: %f \r\n",psMsgBuff->vel);
		fprintf(fpcan, "pos : %d \r\n",psMsgBuff->pos);
		fprintf(fpcan, "virtual_pos : %f \r\n",psMsgBuff->virtual_pos);
		fprintf(fpcan, "dtBox : %f \r\n",psMsgBuff->dtBox);
		fprintf(fpcan, "ds_profile_point : %d \r\n",psMsgBuff->ds_profile_point);
		fprintf(fpcan, "vm_count : %d \r\n",psMsgBuff->vm_count);
		fprintf(fpcan, "vm_accflag : %d \r\n",psMsgBuff->vm_accflag & 0xff);
		fprintf(fpcan, "vm_dccflag : %d \r\n",psMsgBuff->vm_dccflag & 0xff);
		fprintf(fpcan, "vm_adv_aord : %d \r\n",psMsgBuff->vm_adv_aord & 0xff);
		fprintf(fpcan, "qhead : %d \r\n",psMsgBuff->qhead & 0xff);
		fprintf(fpcan, "qtail : %d \r\n",psMsgBuff->qtail & 0xff);
		fprintf(fpcan, "pos1 : %d \r\n",psMsgBuff->q_pos1);
		fprintf(fpcan, "pos2 : %d \r\n",psMsgBuff->q_pos2);
		fprintf(fpcan, "pos3 : %d \r\n",psMsgBuff->q_pos3);
		fprintf(fpcan, "m : %d \r\n",psMsgBuff->q_pos4);
		fprintf(fpcan, "err : %d \r\n",psMsgBuff->err & 0xff);
		fprintf(fpcan, "err_point : %d \r\n",psMsgBuff->err_point & 0xff);
		fprintf(fpcan, "time  : %d \r\n\n\n",psMsgBuff->time );
	}

}

void CSyncMotion::PutMotionCalDbgStatus(unsigned char ucstatus[256])
{
	DBG_MOTIONCAL_MSG_BUFFER *psMsgBuff = (DBG_MOTIONCAL_MSG_BUFFER *) ucstatus;

	for (int i = 0; i < 4; i ++, psMsgBuff++)
	{
		fprintf(fpcan, "#Buffer[%d]\r\n",psMsgBuff->buffer_no1 & 0xff);
		fprintf(fpcan, "cal_point : %d \r\n",psMsgBuff->cal_sort & 0xff);
		fprintf(fpcan, "ax : %d \r\n",psMsgBuff->ax & 0xff);
		fprintf(fpcan, "vel: %f \r\n",psMsgBuff->vel);
		fprintf(fpcan, "pos : %d \r\n",psMsgBuff->pos);
		fprintf(fpcan, "vm_prev_vel : %f \r\n",psMsgBuff->vm_prev_vel);
		fprintf(fpcan, "vm_vel : %f \r\n",psMsgBuff->vm_vel);
		fprintf(fpcan, "vm_count : %d \r\n",psMsgBuff->vm_count);
		fprintf(fpcan, "vm_acc : %d \r\n",psMsgBuff->vm_acc);
		fprintf(fpcan, "vm_dcc : %d \r\n",psMsgBuff->vm_dcc);
		fprintf(fpcan, "vm_accflag : %d \r\n",psMsgBuff->vm_accflag & 0xff);
		fprintf(fpcan, "vm_dccflag : %d \r\n",psMsgBuff->vm_dccflag & 0xff);
		fprintf(fpcan, "qhead : %d \r\n",psMsgBuff->qhead & 0xff);
		fprintf(fpcan, "qtail : %d \r\n",psMsgBuff->qtail & 0xff);
		fprintf(fpcan, "ds_cal_point : %d \r\n",psMsgBuff->ds_cal_point & 0xff);
		fprintf(fpcan, "dtbox : %f \r\n",psMsgBuff->dtbox);
		fprintf(fpcan, "fvel : %f \r\n",psMsgBuff->fvel);
		fprintf(fpcan, "virtual_pos : %f \r\n",psMsgBuff->virtual_pos);
		fprintf(fpcan, "dac_bias : %d \r\n",psMsgBuff->dac_bias);
		fprintf(fpcan, "time  : %d \r\n\n\n",psMsgBuff->time );
	}

}

void CSyncMotion::PutEventDbgStatus(unsigned char ucstatus[256])
{
	DBG_EVENT_MSG_BUFFER *psMsgBuff = (DBG_EVENT_MSG_BUFFER *) ucstatus;

	for (int i = 0; i < 4; i ++, psMsgBuff++)
	{
		fprintf(fpcan, "#Channel[%d]\r\n",i);

		fprintf(fpcan, "jtpos : %d \r\n",psMsgBuff->jtpos);
		fprintf(fpcan, "Goal pos : %d \r\n",(int) psMsgBuff->mfGoal_pos);
		fprintf(fpcan, "Event_st: 0x%02X\r\n",psMsgBuff->event_st & 0xff);
		fprintf(fpcan, "Axis source : 0x%04X\r\n", psMsgBuff->axis_source & 0xffff);
		fprintf(fpcan, "dac_code : %d\r\n", psMsgBuff->dac_code & 0xffff);
		fprintf(fpcan, "vm_accflag : %d\r\n", psMsgBuff->vm_accflag & 0xff);
		fprintf(fpcan, "vm_count : %d\r\n", psMsgBuff->vm_count & 0xff);
		fprintf(fpcan, "vm_flag : %d\r\n", psMsgBuff->vm_flag & 0xff);
		fprintf(fpcan, "vm_bMoveDs : %d\r\n", psMsgBuff->vm_bMoveDs & 0xff);
		fprintf(fpcan, "vm_bPosWait : %d\r\n", psMsgBuff->vm_bPosWait & 0xff);
		fprintf(fpcan, "vm_WaitPos : %d\r\n", (int) psMsgBuff->vm_WaitPos);
		fprintf(fpcan, "vm_adv_aord : %d\r\n", psMsgBuff->vm_adv_aord & 0xff);
		fprintf(fpcan, "velerr : %d\r\n", (int) psMsgBuff->velerr);
		fprintf(fpcan, "tqrcmd : %d\r\n", (int) psMsgBuff->tqrcmd);
		fprintf(fpcan, "dtBox : %f\r\n", psMsgBuff->dtBox);
		fprintf(fpcan, "Q head : %d\r\n", psMsgBuff->qhead & 0xff);
		fprintf(fpcan, "Q tail : %d\r\n", psMsgBuff->qtail & 0xff);
		fprintf(fpcan, "Watch-Dog : 0x%02X\r\n", psMsgBuff->wdt_status & 0xff);
		fprintf(fpcan, "H/W Status : 0x%02X\r\n", psMsgBuff->servo_status & 0xff);

		fprintf(fpcan, "profile_limit : %d\r\n", psMsgBuff->profile_limit);
		fprintf(fpcan, "actvel_margin : %d\r\n", psMsgBuff->actvel_margin);
		fprintf(fpcan, "encoder_vel : %d\r\n", psMsgBuff->encoder_vel);
		fprintf(fpcan, "mdBasePos : %f\r\n", psMsgBuff->mdBasePos);
		fprintf(fpcan, "limit_curve_chk_cnt : %d\r\n", psMsgBuff->limit_curve_chk_cnt & 0xff);
		fprintf(fpcan, "val : %d\r\n", psMsgBuff->val & 0xff);
		fprintf(fpcan, "Loop_Counter(%d) : %d\r\n", i, psMsgBuff->loop_cnt);
	}

}

void CSyncMotion::PutDbgStatus(unsigned char ucstatus[256])
{
	CString cstr;

	DBG_MSG_BUFFER *psMsgBuff = (DBG_MSG_BUFFER *) ucstatus;

	m_sDbgStatus = _T("");

	for (int i = 0; i < 4; i ++, psMsgBuff++)
	{
		cstr.Format("#Channel %d\r\n", i);
		m_sDbgStatus += cstr;
	
		// jtpos, int
		cstr.Format("jtpos : %d\r\n", psMsgBuff->jtpos);
		m_sDbgStatus += cstr;

		// mdGoal_pos
		cstr.Format("Goal pos : %d\r\n", (int) psMsgBuff->mfGoal_pos);
		m_sDbgStatus += cstr;

		// event_st
		cstr.Format("Event_st : 0x%02X\r\n", psMsgBuff->event_st & 0xff);
		m_sDbgStatus += cstr;

		// axis_source
		cstr.Format("Axis source : 0x%04X\r\n", psMsgBuff->axis_source & 0xffff);
		m_sDbgStatus += cstr;

		// dac_code
		cstr.Format("dac_code : %d\r\n", psMsgBuff->dac_code & 0xffff);
		m_sDbgStatus += cstr;

		// vm_accflag
		cstr.Format("vm_accflag : %d\r\n", psMsgBuff->vm_accflag & 0xff);
		m_sDbgStatus += cstr;

		// vm_count
		cstr.Format("vm_count : %d\r\n", psMsgBuff->vm_count & 0xff);
		m_sDbgStatus += cstr;

		// vm_flag
		cstr.Format("vm_flag : %d\r\n", psMsgBuff->vm_flag & 0xff);
		m_sDbgStatus += cstr;

		// vm_bMoveDs
		cstr.Format("vm_bMoveDs : %d\r\n", psMsgBuff->vm_bMoveDs & 0xff);
		m_sDbgStatus += cstr;

		// vm_bPosWait
		cstr.Format("vm_bPosWait : %d\r\n", psMsgBuff->vm_bPosWait & 0xff);
		m_sDbgStatus += cstr;

		// vm_WaitPos
		cstr.Format("vm_WaitPos : %d\r\n", (int) psMsgBuff->vm_WaitPos);
		m_sDbgStatus += cstr;

		// vm_adv_aord
		cstr.Format("vm_adv_aord : %d\r\n", psMsgBuff->vm_adv_aord & 0xff);
		m_sDbgStatus += cstr;

		// velcmd
		cstr.Format("velcmd : %d\r\n", (int) psMsgBuff->velcmd);
		m_sDbgStatus += cstr;

		// velerr
		cstr.Format("velerr : %d\r\n", (int) psMsgBuff->velerr);
		m_sDbgStatus += cstr;

		// tqrcmd
		cstr.Format("tqrcmd : %d\r\n", (int) psMsgBuff->tqrcmd);
		m_sDbgStatus += cstr;

		// dtBox
		cstr.Format("dtBox : %f\r\n", psMsgBuff->dtBox);
		m_sDbgStatus += cstr;


		// move_x, ptp 관련 명령들
		cstr.Format("Command-cmd : %s\r\n", GetCommandString(psMsgBuff->cmd));
		m_sDbgStatus += cstr;

		// acc
		cstr.Format("Command-Acc : %f\r\n", psMsgBuff->acc);
		m_sDbgStatus += cstr;

		// dcc
		cstr.Format("Command-Dcc : %f\r\n", psMsgBuff->dcc);
		m_sDbgStatus += cstr;

		// vel
		cstr.Format("Command-Vel : %f\r\n", psMsgBuff->vel);
		m_sDbgStatus += cstr;

		// pos
		cstr.Format("Command-pos : %d\r\n", psMsgBuff->pos);
		m_sDbgStatus += cstr;


		// Q Head
		cstr.Format("Q head : %d\r\n", psMsgBuff->qhead & 0xff);
		m_sDbgStatus += cstr;

		// Q-tail
		cstr.Format("Q tail : %d\r\n", psMsgBuff->qtail & 0xff);
		m_sDbgStatus += cstr;




		// wdt_status
		cstr.Format("Watch-Dog : 0x%02X\r\n", psMsgBuff->wdt_status & 0xff);
		m_sDbgStatus += cstr;

		// motor hw status
		cstr.Format("H/W Status : 0x%02X\r\n", psMsgBuff->servo_status & 0xff);
		m_sDbgStatus += cstr;

		// 2011.7.21, AMC의 main, 200us, 1ms, DPRAM 인터럽트 수행 카운터를 확인
		cstr.Format("Loop_Counter(%d) : %d\r\n", i, psMsgBuff->loop_cnt);
		m_sDbgStatus += cstr;


		m_sDbgStatus += "\r\n";
	}

	UpdateData(FALSE);
}

void CSyncMotion::OnButtonDpramTest() 
{
	// TODO: Add your control notification handler code here
}

void CSyncMotion::OnButtonSetVelCurve() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int axis  = sync_pm_axis;
	int limit =	sync_pm_value;	
	int action=	sync_pm_use;

	if (set_vel_curve(axis,limit,action) != MMC_OK) AfxMessageBox("ERROR : PROFILE MARGIN SETTING");
}

void CSyncMotion::OnButtonGetVelCurve() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int axis  = sync_pm_axis;
	int limit, action;

	if (get_vel_curve(axis, &limit, &action) != MMC_OK) AfxMessageBox("ERROR : PROFILE MARGIN READING");
	sync_pm_value = limit;
	sync_pm_use	  = action;

	UpdateData(FALSE);
}

void CSyncMotion::OnButtonSetActvelMargin() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int axis  = sync_avm_axis;
	int limit =	sync_avm_value;	
	int event =	sync_avm_event;
	int time  =	sync_avm_time;

	if (set_actvel_margin(axis,limit,event,time) != MMC_OK) AfxMessageBox("ERROR : ACTVEL MONITERRING SETTING");
}

void CSyncMotion::OnButtonGetActvelMargin() 
{
	// TODO: Add your control notification handler code here
	UpdateData(TRUE);
	int axis  = sync_avm_axis;
	int limit, event, time;

	if (get_actvel_margin(axis,&limit,&event,&time) != MMC_OK) AfxMessageBox("ERROR : ACTVEL MONITERRING READING");	
	sync_avm_value = limit;	
	sync_avm_event = event;
	sync_avm_time  = time;	
	UpdateData(FALSE);
}
