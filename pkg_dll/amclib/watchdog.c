#include "pcdef.h"
#include "amc_internal.h"
#include "log.h"


enum
{
	WDT_CMD_ENABLE_WDT	= 1,
	WDT_CMD_DISABLE_WDT,
	WDT_CMD_GET_WDT_STATUS,
	WDT_CMD_SET_WDT_STATUS,
	WDT_CMD_TRIGGER_WDT_REASON
};


static int _watchdog_op(int cmd, int reason, int *prtn)
{
	INT err;
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, 0);
		AxisDpram[0]->Int_Type[0] = cmd;
		AxisDpram[0]->Int_Type[1] = reason;

		// Wake DSP!. Send Command
		_flush_dpram(WATCHDOG_OPERATIONS);
		mmc_error = err = _wait_for_reply(3000);

		if (prtn != NULL) *prtn = (int)AxisDpram[0]->Int_Type[0];

		MMCMutexUnlock ();
	}
	return err;
}

AMCLIB_API INT enable_wdt_reason(int reason)
{
    MYLOG("enable_wdt_reason\n");

    return _watchdog_op(WDT_CMD_ENABLE_WDT, reason, NULL);
}

AMCLIB_API INT disable_wdt_reason(int reason)
{
    MYLOG("disable_wdt_reason\n");
    
    return _watchdog_op(WDT_CMD_DISABLE_WDT, reason, NULL);
}

AMCLIB_API INT get_wdt_status(unsigned int *puistatus)
{
    MYLOG("get_wdt_status\n");
    
    return _watchdog_op(WDT_CMD_GET_WDT_STATUS, 0, (int *)puistatus);
}

AMCLIB_API INT set_wdt_status(unsigned int uistatus)
{
    MYLOG("set_wdt_status\n");
    
    return _watchdog_op(WDT_CMD_SET_WDT_STATUS, uistatus, NULL);
}

AMCLIB_API INT clr_wdt_reason(int reason)
{
	return _watchdog_op(WDT_CMD_TRIGGER_WDT_REASON, reason, NULL);
}




static UCHAR __rsttable[10] = {
	0x80, 0x81, 0x73, 0x37, 0xf9, 
	0xf8, 0x57, 0x38, 0x58, 0x77
};

// dsp_reboot()함수에서 리부팅이 완료된 2초후 리턴토록 변경한다.
void dsp_reboot()
{
    MYLOG("dsp_reboot\n");
    
    int i, j;

	for (i = 0; i < 10; i ++)
	{
		for (j = 0; j < 10; j ++) _write_dpramreg(ADDR_DSP_ACK, __rsttable[j]);
		for (j = 0; j < 10; j ++) _write_dpramreg(ADDR_DSP_ACK, (unsigned char)(__rsttable[j] ^ 0xff));
	}

	// Now, DSP will be in reset state, so wait for release in reset state
	Sleep(5000);
}


void ClearAliveFlag()
{
	_write_dpramreg(ALIVE_CHK_ADDR1, 0);
	_write_dpramreg(ALIVE_CHK_ADDR2, 0);
}

int CheckAliveFlag(UINT uiMaxTOms)
{
	UINT	uiWaitCnt = 0;
	unsigned char ucval;
	
	while (uiWaitCnt++ * 10 < uiMaxTOms)
	{
		_read_dpramreg(ALIVE_CHK_ADDR1, &ucval);
		if (ucval == ALIVE_CHK_BYTE1)
		{
			_read_dpramreg(ALIVE_CHK_ADDR2, &ucval);
			if (ucval == ALIVE_CHK_BYTE2) return 1;
		}
		Sleep(10);
	}
	return 0;
}

int dsp_reboot_and_chk(UINT uiTOms)
{
	int i, j, rtn;

	ClearAliveFlag();

	for (i = 0; i < 10; i ++)
	{
		for (j = 0; j < 10; j ++) _write_dpramreg(ADDR_DSP_ACK, __rsttable[j]);
		for (j = 0; j < 10; j ++) _write_dpramreg(ADDR_DSP_ACK, (unsigned char) (__rsttable[j] ^ 0xff));
	}

	// Now, DSP will be in reset state, so wait for release in reset state
	Sleep(1000);

	rtn = CheckAliveFlag(uiTOms);
	return rtn;
}
