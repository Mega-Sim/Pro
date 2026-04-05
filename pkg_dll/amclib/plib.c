#include "stdafx.h"
//#include <windows.h>
#include <stdio.h>
#include "crc/crc32.h"
#include "amc_internal.h"
#include <stdlib.h>
#include "pcdef.h"
#include "log.h"


extern char AMC_WORKDIR[128];
extern CRITICAL_SECTION m_csLockAxisComm;

INT MMCMutexLock (void)
{
	int err = 0;
    if (WaitForSingleObject(hMutex_Axis_Comm, AXIS_COMM_WAIT) != WAIT_OBJECT_0)
    {
        err = MMC_MUTEXLOCK_ERROR;
    }
    return err;
}

INT MMCMutexUnlock (void)
{
	ReleaseMutex(hMutex_Axis_Comm);
	return MMC_OK;
}

/**********
*	FUNCTION NAME	: MMCCommCheck(INT n_bn, INT *bn, INT comm, INT jnt)
*	FUNCTION       : INT. Command write to DSP Board
*********************************************************************/
INT		MMCCommCheck (INT n_bn, pINT bn, INT comm, INT jnt)
{
	INT	i;
	INT err;

	for (i=0; i<n_bn; i++)
	{
		_write_dpramreg(AXIS_REG, (CHAR)jnt);
		_flush_dpram((CHAR)comm);
	}

	if(_wait_for_reply(3000) == AMC_SUCCESS)
		mmc_error = err = MMC_OK;
	else									
		mmc_error = err = MMC_TIMEOUT_ERR;

	return err;
}

/**********
*	FUNCTION NAME	: CommWrite(INT ax)
*	FUNCTION       : Just Command write to DSP
*********************************************************************/
INT	CommWrite (INT ax, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
		mmc_error = err = MMCCommCheck (1, &bn, comm, jnt);
	MMCMutexUnlock ();
	return	err;
}


/**********
*	FUNCTION NAME	: CDIWrite(INT ax, INT val, INT comm)
*	FUNCTION		: Command & Data write to DSP
*********************************************************************/
INT		CDIWrite (INT ax, INT val, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) ==0 )
	{
		AxisDpram[ax]->Int_Type[0] = val;
		if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == 0)
		{
			if (comm==PUT_HOME_LEVEL)			BootFrame[bn].Home_Level[jnt]=(CHAR)val;
			else if(comm==PUT_POS_LEVEL)		BootFrame[bn].Pos_Level[jnt]=(CHAR)val;
			else if(comm==PUT_NEG_LEVEL)		BootFrame[bn].Neg_Level[jnt]=(CHAR)val;
			else if(comm==PUT_AMP_FAULT)		BootFrame[bn].Amp_Fault_Event[jnt]=(CHAR)val;
			else if(comm==PUT_AMP_FAULT_LEVEL)	BootFrame[bn].Amp_Level[jnt]=(CHAR)val;
			else if(comm==PUT_AMP_RESET_LEVEL)	BootFrame[bn].Amp_Reset_Level[jnt]=(CHAR)val;
			else if(comm==PUT_POS_EVENT)		BootFrame[bn].Pos_Limit_St[jnt]=(CHAR)val;
			else if(comm==PUT_NEG_EVENT)		BootFrame[bn].Neg_Limit_St[jnt]=(CHAR)val;
			else if(comm==PUT_HOME_EVENT)		BootFrame[bn].Home_Limit_St[jnt]=(CHAR)val;
			else if(comm==PUT_STOP_RATE)		BootFrame[bn].Stop_Rate[jnt]=(CHAR)val;
			else if(comm==PUT_E_STOP_RATE)		BootFrame[bn].E_Stop_Rate[jnt]=(CHAR)val;
			else if(comm==PUT_FEEDBACK_DEVICE)	BootFrame[bn].Encoder_Cfg[jnt]=(CHAR)val;
			else if(comm==PUT_VOLTAGE_DEVICE)	BootFrame[bn].Voltage_Cfg[jnt]=(CHAR)val;
			else if(comm==PUT_INDEX_REQUIRED)	BootFrame[bn].Home_Index[jnt]=(CHAR)val;
			else if(comm==PUT_CLOSED_LOOP)		BootFrame[bn].Loop_Cfg[jnt]=(CHAR)val;
			else if(comm==PUT_SERVO_ON_LEVEL)	BootFrame[bn].Amp_OnLevel[jnt]=(CHAR)val;
			else if(comm==PUT_INT_ENABLE)		BootFrame[bn].Io_Int_Enable[jnt] = (val==0) ? 0 : 1;
			else if(comm==PUT_INT_STOP)			BootFrame[bn].Int_Event_St[jnt] = (val==0) ? 0 : 1;
			else if(comm==PUT_INT_E_STOP)		BootFrame[bn].Int_Event_St[jnt] = (val==0) ? 0 : 2;
			else if(comm==PUT_VT_CONTROL)		BootFrame[bn].Control_Cfg[jnt]=(CHAR)val;
			else if(comm==PUT_POS_I_MODE)     	BootFrame[bn].PosImode[jnt]=(CHAR)val;
			else if(comm==PUT_VEL_I_MODE)     	BootFrame[bn].VelImode[jnt]=(CHAR)val;
			else if(comm==PUT_PULSE_MODE)		BootFrame[bn].PulseMode[jnt]=(CHAR)val;
			else if(comm==PUT_PULSE_RATIO)		BootFrame[bn].PulseRatio[jnt]=val;
			else if(comm==PUT_ENCODER_OFFSET)	BootFrame[bn].Encoder_Offset[ax]=val;
		}
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDIRead(INT ax, INT *val, INT comm)
*	FUNCTION       : Data Read From DSP
*********************************************************************/
INT		CDIRead (INT ax, INT *val, INT comm)
{
	INT		bn, jnt, err;
	
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
		if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == MMC_OK)
			*val = (int)AxisDpram[ax]->Int_Type[0];
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDLWrite(INT ax, LONG val, INT comm)
*	FUNCTION       : Command & Long type Data write to DSP
*********************************************************************/
INT		CDLWrite (INT ax, LONG val, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Long_Type = val;
		mmc_error = err = MMCCommCheck (1, &bn, comm, jnt);
		if((comm == PUT_POSITION) && (err == MMC_OK)) 
            Sleep(1); // 130109 syk 2.9.10 인코더 값 변환 안정 시간 확보.
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDLRead(INT ax, LONG *val, INT comm)
*	FUNCTION       : Long type Data Read From DSP
*********************************************************************/
INT		CDLRead (INT ax,LONG *val, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) !=MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == MMC_OK)
		{
			if (comm==GET_ES_POSITION)
			{
				val[0] = AxisDpram[SyncMotion.Master]->Long_Type;
				val[1] = AxisDpram[SyncMotion.Slave]->Long_Type;
			}
			else	*val = AxisDpram[ax]->Long_Type;
		}
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDFWrite(INT ax, FLOAT pos, INT comm)
*	FUNCTION       : WRITE Float type value To DSP
*********************************************************************/
INT		CDFWrite (INT ax, FLOAT pos, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Float_Type = pos;
		if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == MMC_OK)
		{
			if (comm==PUT_IN_POSITION) BootFrame[bn].In_Position[jnt] = pos;
			else if (comm==PUT_GEAR_RATIO)
			{
				BootFrame[bn].GearRatio[jnt] = pos;
			}
		}
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDFWrite(INT ax, FLOAT pos, INT comm)
*	FUNCTION       : WRITE Float type value To DSP
*********************************************************************/
INT		CDFDWrite (INT ax, double pos, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Float_Type = (float)pos;
		if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == MMC_OK)
		{
			if (comm==PUT_GEAR_RATIO)
			{
				BootFrame[bn].GearRatio[jnt] = (float)pos;
			}
		}

	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDFRead(INT ax, FLOAT *pos, INT comm)
*	FUNCTION       : Read Float type value From DSP
*********************************************************************/
INT		CDFRead (INT ax, FLOAT *pos, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
		return err;
	}
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == MMC_OK)
		*pos = AxisDpram[ax]->Float_Type;
	MMCMutexUnlock ();

	return	err;
}

/**********
*	FUNCTION NAME	: CDI3Write(INT ax, INT pos, INT action, INT comm)
*	FUNCTION       : WRITE INT type value To DSP
*********************************************************************/
INT		CDI3Write (INT ax, LONG pos, INT action, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		if (comm==PUT_IO_TRIGGER)
		{
			AxisDpram[ax]->Int_Type[0] = (INT)pos;
			AxisDpram[ax]->Char_Type[0] = (CHAR)action;
		}
		else
		{
			AxisDpram[ax]->Long_Type = pos;
			AxisDpram[ax]->Char_Type[0] = (CHAR)action;
		}
		if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == MMC_OK)
		{
			if (comm==PUT_ERR_LIMIT)
			{
				BootFrame[bn].Error_Limit[jnt] = pos;
				BootFrame[bn].Error_Limit_St[jnt] = (CHAR)action;
			}
			else if (comm==PUT_SW_UPPER_LIMIT)
			{
				BootFrame[bn].SwUpper_Limit[jnt] = pos;
				BootFrame[bn].SwUpper_LimitSt[jnt] = (CHAR)action;
			}
			else if (comm==PUT_SW_LOWER_LIMIT)
			{
				BootFrame[bn].SwLower_Limit[jnt] = pos;
				BootFrame[bn].SwLower_LimitSt[jnt] = (CHAR)action;
			}
		}
	}
	MMCMutexUnlock ();
	return	err;
}


/**********
*	FUNCTION NAME	: CDI3Read(INT ax, INT *pos, INT *action, INT comm)
*	FUNCTION       : Read INT type value From DSP
*********************************************************************/
INT		CDI3Read (INT ax, LONG *pos, INT *action, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == MMC_OK)
		{
			if (comm==GET_ERR_LIMIT)
			{
				*pos = AxisDpram[ax]->Long_Type;
				*action = (INT)AxisDpram[ax]->Char_Type[0];
			}
			else if (comm==GET_SW_UPPER_LIMIT)
			{
				*pos = AxisDpram[ax]->Long_Type;
				*action = (INT)AxisDpram[ax]->Char_Type[0];
			}
			else if(comm == GET_SW_LOWER_LIMIT)
			{
				*pos = AxisDpram[ax]->Long_Type;
				*action = (INT)AxisDpram[ax]->Char_Type[0];
			}
			else if(comm == GET_VEL_CURVE)						
			{
				*pos = AxisDpram[ax]->Long_Type;				//limit
				*action = (INT)AxisDpram[ax]->Char_Type[0];		//event
			}
			else {
				*pos = AxisDpram[ax]->Long_Type;
			}
		}
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: Find_Bd_Jnt(INT ax, INT *bn, INT *jnt)
*	FUNCTION       : find each board axis
*********************************************************************/
INT		Find_Bd_Jnt (INT ax, INT *bn, INT *jnt)
{
	INT err;
	mmc_error = err = MMC_OK;
	if ((ax>=TOTAL_AXIS_NUM) || ax<0) mmc_error = err = MMC_INVALID_AXIS;
	else if ((*bn=Find_MMC_Num (ax))<0) mmc_error = err = MMC_INVALID_AXIS;
	else if (*bn) *jnt = ax - BootFrame[*bn-1].Action_Axis_Num;
	else *jnt = ax;
	return err;
}

/////////////////////////////////////////////////////////////////
/*-------------------------- crc.c --------------------------*/
static unsigned int crc_table[256];
static int _nFirst = 1;
void _crc32_gen_table(void)                /* build the crc table */
{
    unsigned long crc, poly;
    int i, j;

    poly = 0xEDB88320L;
    for (i = 0; i < 256; i++)
        {
        crc = i;
        for (j = 8; j > 0; j--)
            {
            if (crc & 1)
                crc = (crc >> 1) ^ poly;
            else
                crc >>= 1;
            }
        crc_table[i] = crc;
        }
}

unsigned long _crc32_get_crc(char *pc, int nlen)    /* calculate the crc value */
{
    register unsigned int crc;
	int i;
    if (_nFirst)
    {
    	_nFirst = 0;
    	_crc32_gen_table();
    }

    crc = 0xFFFFFFFF;
    for (i = 0; i < nlen; i ++)
        crc = (crc>>8) ^ crc_table[ (crc ^ *pc ++) & 0xff ];

    return( crc^0xFFFFFFFF );
}


INT		DownloadLongBlock(void *pMap, UINT uiEepromAddr, int nBytes)
{
	INT		err;
	UINT	uiCRC = _crc32_get_crc((char *)pMap, nBytes);
	int		nBlkLen = nBytes >> 8;
	int		j;
	UINT	uiOfs;
	char	*pcaData = (char *) pMap;

	if ((nBlkLen * 256) != nBytes) nBlkLen += 1;

	char str[100];
	sprintf(str, "TotLen = %d, BlkLen=%d", nBytes, nBlkLen);

	mmc_error = err = MMC_OK;

	for (j = 0; j < nBlkLen; j ++)
	{
		if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
		{
			break;
		}
		
		int nEnd;

		// CRC, TOTLEN, TOTBLK, 
		// SaveEepromAddr
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTCRC4, &uiCRC, 4);
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTLEN4, &nBytes, 4);
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKLEN2, &nBlkLen, 2);
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKID2, &j, 2);
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_SAVEADDR4, &uiEepromAddr, 4);

		uiOfs = j * 256;

		// 남아있는 바이트만큼 DPRAM에 쓴다.
		nEnd = min(256, nBytes - uiOfs);
			
		// 한방에 필요한 바이트를 모두 쓴다.
		_write_dpramregs(C2DSPOFS_BODY_BASE_ADDR, &pcaData[uiOfs], nEnd);
		uiOfs += nEnd;

		// Send Command
		_flush_dpram(C2DSP_SAVE_MAP_INFO);

		if(_wait_for_reply(3000) == AMC_SUCCESS)	mmc_error = err = MMC_OK;
		else										mmc_error = err = MMC_TIMEOUT_ERR;

		MMCMutexUnlock ();
	}

	{
		char str[100];
		sprintf(str, "%d loops", j);
	}
	return	err;
}

INT amc_exchange_string(char *pMsg, int nSendByte, char *pRcvMsg, int *nRcvByte)
{
    MYLOG("amc_exchange_string\n");
    
    int i;
	unsigned char chChkSum, nrcvchksum, val, nerr = 0;
	INT err;

	nSendByte = min(250, nSendByte);

	// 체크섬을 계산한다.
	chChkSum = 0;
	for (i = 0; i < nSendByte; i ++) chChkSum += pMsg[i];

	if ( (mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;
	
	// 헤더를 초기화 한다.
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_RESERVED, 0);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_ACKLEN, 0);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_ACKCHKSUM, 0);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_ACKERRNO, 0);

	// 헤더를 써 넣는다.
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_LEN, (unsigned char)nSendByte);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_CHKSUM, (unsigned char)chChkSum);

	// Body를 써 넣는다.
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_BODY, pMsg, nSendByte);

	// Send Command
	_flush_dpram(C2DSP_SEND_STRING);

	if(_wait_for_reply(3000) == AMC_SUCCESS)	mmc_error = err = MMC_OK;
	else
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	
	if(_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_ACKLEN, &val)!=AMCTRUE)
	{
		MMCMutexUnlock ();
		mmc_error = MMC_NOT_INITIALIZED;
		return MMC_NOT_INITIALIZED;
	}
	*nRcvByte = (int)val;

	// 저장할 수 있을때만 DSP가 보내온 스트링을 복사한다.
	if (pRcvMsg)
		_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_BODY, pRcvMsg, *nRcvByte);

	 _read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_ACKCHKSUM, &nrcvchksum);
	 _read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + C2DSP_STRING_ACKERRNO, &nerr);

	MMCMutexUnlock ();
	
	if (pRcvMsg)
	{
		chChkSum = 0;
		for (i = 0; i < *nRcvByte; i ++) chChkSum += pRcvMsg[i];
		if ((nrcvchksum & 0xff) != (chChkSum & 0xff))
		{
			mmc_error = MMC_CHKSUM_OPEN_ERROR;
			return MMC_CHKSUM_OPEN_ERROR;
		}
	}
	// 통신중에 에러가 있었다면 -1을 리턴한다.
	// 에러가 없다면 0을 리턴한다. 
	return nerr;
}




INT	UploadSysParam(char *pData, int *pnBytes)
{
	INT		err;
	UINT	uiCRC;
	int		i, len, nNextBlk;
	UINT	uiOfs;
	UINT	uiTotLen;
	char	*pcaData = pData;
	int		nBlkLen;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;

	// BLKID = 0을 기록하여 필요한 정보를 받아온다.
	nBlkLen = 0;
	_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKID2, &nBlkLen, 2);

	// Wake DSP!. Send Command
	_flush_dpram(C2DSP_RUNCONFIG_UPLOAD);

	if(_wait_for_reply(3000) != AMC_SUCCESS) 
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}

	// CRC와 블럭Len을 읽는다.
	_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTCRC4, &uiCRC, 4);
	_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTLEN4, &uiTotLen, 4);
	_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKLEN2, &nBlkLen, 2);

	*pnBytes = (int)uiTotLen;

	// 블럭의 갯수만큼을 읽는다.
	for (i = 0; i < nBlkLen; i ++)
	{
		// 데이터를 읽는다.
		uiOfs = i * 256;
		len = min(uiTotLen - uiOfs, 256);
		if (pData)
			_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BODY_OFS, pcaData + uiOfs, len);


		// 읽을 블럭이 더 있으면 DSP를 깨운다.
		if (i < nBlkLen - 1)
		{
			nNextBlk = i + 1;

			// 더 읽을 블럭의 번호를 써 넣는다.
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKID2, &nNextBlk, 2);

			// Wake DSP!. Send Command
			_flush_dpram(C2DSP_RUNCONFIG_UPLOAD);

			if(mmc_error = err = _wait_for_reply(3000) != AMC_SUCCESS) break;
		}
	}
	MMCMutexUnlock ();
	
	return err;
}


INT	DnloadSysParam(char *pSrcData, int nBytes)
{
	INT		err;
	int		j;
	UINT	uiOfs;
	char	*pcaData = pSrcData;
	int		nBlkLen = nBytes >> 8;
	int		nWrite;

	if ((nBlkLen * 256) < nBytes) nBlkLen += 1;

	{
		char str[100];
		sprintf(str, "TotLen = %d, BlkLen=%d", nBytes, nBlkLen);
	}

	mmc_error = err = MMC_OK;

	for (j = 0; j < nBlkLen; j ++)
	{
		if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
		{
			break;
		}

		uiOfs = j * 256;
		nWrite = min(nBytes - uiOfs, 256);
		
		// BLKID를 기록한다.
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKID2, &j, 2);
		// data를 기록한다.
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BODY_OFS, pcaData + uiOfs, nWrite);

		if (j == 0)
		{
			int ncrc = _crc32_get_crc(pSrcData, nBytes);

			// Totlen, CRC, BlkLen을 기록한다. 
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTLEN4, &nBytes, 4);
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTCRC4, &ncrc, 4);
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKLEN2, &nBlkLen, 2);
		}
		
		// Wake DSP!. Send Command
		_flush_dpram(C2DSP_RUNCONFIG_DNLOAD);

		if(_wait_for_reply(8000) != AMC_SUCCESS)
		{
			MMCMutexUnlock ();
			mmc_error = err = MMC_TIMEOUT_ERR;
			break;
		}

		if (j == nBlkLen - 1)
		{
			int nack = 0;
			_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTLEN4, &nack, 4);
			mmc_error = err = MMC_OK;

            if (nack != 0) 
			{
				MMCMutexUnlock ();
				mmc_error = err = MMC_BOOTPARAM_SIZE_ERROR;
				break;
			}
		}
		MMCMutexUnlock ();
	}

	return	err;
}

// EEPROM의 특정영역에서 지정한 바이트만큼을 읽어온다.
INT		UploadParam(char *pData, UINT uiEepromAddr, int nBytes)
{
	INT		err;
	UINT	uiCRC;
	int		j;
	UINT	uiOfs;
	char	*pcaData = pData;
	int		nBlkLen = nBytes >> 8;
	int nRead;

	if ((nBlkLen * 256) != nBytes) nBlkLen += 1;

	{
		char str[100];
		sprintf(str, "TotLen = %d, BlkLen=%d", nBytes, nBlkLen);
	}

	mmc_error = err = MMC_OK;

	for (j = 0; j < nBlkLen; j ++)
	{
		if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) break;
		
		// BLKID를 기록한다.
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKID2, &j, 2);

		if (j == 0)
		{
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_SAVEADDR4, &uiEepromAddr, 4);
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTLEN4, &nBytes, 4);
		}
		
		// Wake DSP!. Send Command
		_flush_dpram(C2DSP_PARAM_UPLOAD);

		if(_wait_for_reply(3000) != AMC_SUCCESS)
		{
			MMCMutexUnlock ();
			mmc_error = err = MMC_TIMEOUT_ERR;
			break;
		}

		if (j == 0)
		{
			int nblklen = 0;
			// CRC와 블럭Len을 읽는다.
			_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTCRC4, &uiCRC, 4);
			_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKLEN2, &nblklen, 2);
			if (nBlkLen != nblklen) 
			{
				MMCMutexUnlock ();
				mmc_error = err = MMC_BOOTPARAM_SIZE_ERROR;
				break;
			}
		}

		uiOfs = j * 256;

		// 남아있는 바이트수를 계산한다.
		nRead = min(256, nBytes - uiOfs);
		_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BODY_OFS, pcaData + uiOfs, nRead);

		MMCMutexUnlock ();
	}

	return	err;
}


// PC에 있는 내용을 EEPROM의 특정 어드레스에 써 넣는다.
INT	DnloadParam(char *pSrcData, UINT uiEepromAddr, int nBytes)
{
	INT		err;
	int		j;
	UINT	uiOfs;
	char	*pcaData = pSrcData;
	int		nBlkLen = nBytes >> 8;
	int		nWrite;

	if ((nBlkLen * 256) != nBytes) nBlkLen += 1;

	char str[100];
	sprintf(str, "TotLen = %d, BlkLen=%d", nBytes, nBlkLen);

	mmc_error = err = MMC_OK;

	for (j = 0; j < nBlkLen; j ++)
	{
		if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) 
		{
			break;
		}

		uiOfs = j * 256;
		nWrite = min(nBytes - uiOfs, 256);
		
		// BLKID를 기록한다.
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKID2, &j, 2);
		// data를 기록한다.
		_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BODY_OFS, pcaData + uiOfs, nWrite);

		if (j == 0)
		{
			int ncrc = _crc32_get_crc(pSrcData, nBytes);

			// SaveAddress, Totlen, CRC, BlkLen을 기록한다. 
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_SAVEADDR4, &uiEepromAddr, 4);
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTLEN4, &nBytes, 4);
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTCRC4, &ncrc, 4);
			_write_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_BLKLEN2, &nBlkLen, 2);
		}
		
		// Wake DSP!. Send Command
		_flush_dpram(C2DSP_PARAM_DNLOAD);

		if(_wait_for_reply(3000) != AMC_SUCCESS)
		{
			MMCMutexUnlock ();
			mmc_error = err = MMC_TIMEOUT_ERR;
			break;
		}

		if (j == nBlkLen - 1)
		{
			int nack = 0;
			_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + C2DSPOFS_TOTLEN4, &nack, 4);
			mmc_error = err = MMC_OK;
			if (nack != 0) 
			{
				MMCMutexUnlock ();
				mmc_error = err = MMC_BOOTPARAM_SIZE_ERROR;
				break;
			}
		}
		MMCMutexUnlock ();
	}
	return	err;
}


INT set_encoder_offset(INT ax, INT nOfs)
{
    MYLOG("set_encoder_offset\n");
    
    INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;
    
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);
		AxisDpram[ax]->Long_Type = nOfs;
		AxisDpram[ax]->Char_Type[0] = 1;		// 부트파라미터로 등록하게 한다.

		// Wake DSP!. Send Command
		_flush_dpram(C2DSP_SET_ENCODER_OFFSET);
		mmc_error = err = _wait_for_reply(3000);

		MMCMutexUnlock ();
		
		if (err != AMC_SUCCESS) 
		{
			mmc_error = MMC_TIMEOUT_ERR;
			return MMC_TIMEOUT_ERR;
		}
	}

	return err;
}

INT get_encoder_offset(INT ax, INT *pnOfs)
{
    MYLOG("get_encoder_offset\n");
    
    INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);

		// Wake DSP!. Send Command
		_flush_dpram(C2DSP_GET_ENCODER_OFFSET);
		mmc_error = err = _wait_for_reply(3000);

		*pnOfs = AxisDpram[ax]->Long_Type;

		MMCMutexUnlock ();
		
		if (err != AMC_SUCCESS) err = MMC_TIMEOUT_ERR;
	}

	return err;
}

INT	amc_flush_sysparam_to_eeprom()
{
    MYLOG("amc_flush_sysparam_to_eeprom\n");
    
    INT err;
	mmc_error = err = MMC_OK;
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		// Wake DSP!. Send Command
		_flush_dpram(FLUSH_SYSPARAM_TO_EEPROM);
		mmc_error = err = _wait_for_reply(10000);

		MMCMutexUnlock ();
		
		if (err != AMC_SUCCESS) err = MMC_TIMEOUT_ERR;
	}

	return err;
}

INT amc_adopt_ini_param()
{
	UBOOTPARA uBootPara;
	int nBytes;
	int nNewCRC;
	BOOL bf;
	INT err;

	// DSP에서 부트값을 읽기전에 초기화 하는 것으로 한다.
	mmc_error = err = Init_Boot_Frame(0);

	mmc_error = err = UploadSysParam(NULL, &nBytes);
	if (sizeof(uBootPara) != nBytes) 
	{
		mmc_error = MMC_BOOTPARAM_SIZE_ERROR;
		return MMC_BOOTPARAM_SIZE_ERROR;
	}
	if(err != MMC_OK) return err;

	mmc_error = err = UploadSysParam((char *) & uBootPara, &nBytes);
	if(err != MMC_OK) return err;

	// CRC를 저장하기위해 할당된 곳은 CRC계산에 이용되지 않도록 한다.
	nNewCRC = _crc32_get_crc((char *) & uBootPara, sizeof(uBootPara) - sizeof(int));
	if (nNewCRC != uBootPara.nCRC32)
	{
		mmc_error = MMC_BOOTPARAM_CRC_ERROR;
		return MMC_BOOTPARAM_CRC_ERROR;
	}

	// 로컬 부트파라미터에 저장한다.
	memcpy(&BootFrame[0], &uBootPara.st_boot, sizeof(BootFrame[0]));

	BootFrameRead(&bf);

	if (bf == FALSE) return MMC_BOOTPARAM_NOT_EXIST;

	mmc_error = MMC_OK;
	return MMC_OK;
}

// DSP의 sysparam을 로딩하여 지정한 경로의 이름으로 파일을 저장한다.
// 지정경로등이 없는경우 "C:\AMCPARAM"으로 저장한다.
INT amc_load_dsp_sysparam_with_localfile(BOOL bCopyToLocalParam, char *pszPath)
{
    MYLOG("amc_load_dsp_sysparam_with_localfile\n");
    
    int err;

	err = amc_adopt_ini_param();

	if (err != 0) { return (mmc_error = err); }

	BootFrameStoreBd(0, pszPath);

	mmc_error = MMC_OK;
	return MMC_OK;
}

INT amc_save_local_sysparam_to_dsp()
{
    MYLOG("amc_save_local_sysparam_to_dsp\n");
    
    // BootFrame[0]에 저장되어있는 값을 DSP로 내려보내 실행메모리와 eeprom에 저장하도록 한다.
	UBOOTPARA uBootPara;

	memcpy(&uBootPara.st_boot, &BootFrame[0], sizeof(BOOT_FRAME_TYPE));
	uBootPara.nMagic = EEPROM_BOOTPARAM_MAGIC_NO;
	uBootPara.nBlockSize = sizeof(uBootPara);
	uBootPara.nBoardNo = 1;
	uBootPara.nAxisNo = BD_AXIS_NUM;
	uBootPara.nVerMaj = 1;
	uBootPara.nVerMin = 0;

	uBootPara.nCRC32 = _crc32_get_crc((char *) & uBootPara, sizeof(uBootPara) - sizeof(int));

	return DnloadSysParam((char *)&uBootPara, sizeof(uBootPara));
}


INT set_encoder_direction(int ax, int cwccw)
{
	INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;
	
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);
		AxisDpram[ax]->Int_Type[0] = cwccw;

		// Wake DSP!. Send Command
		_flush_dpram(SET_ENCODER_DIRECTION);
		mmc_error = err = _wait_for_reply(3000);

		MMCMutexUnlock ();
		
		if (err != AMC_SUCCESS) 
		{
			mmc_error = MMC_TIMEOUT_ERR;
			return MMC_TIMEOUT_ERR;
		}
	}

	return err;
}

INT fset_encoder_direction(int ax, int cwccw)
{
	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return set_encoder_direction(ax, cwccw);
}

INT get_encoder_direction(int ax, int *pcwccw)
{
	INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);

		// Wake DSP!. Send Command
		_flush_dpram(GET_ENCODER_DIRECTION);
		mmc_error = err = _wait_for_reply(3000);

		*pcwccw = (int)AxisDpram[ax]->Int_Type[0];

		MMCMutexUnlock ();
		
	}

	return err;
}

INT fget_encoder_direction(int ax, int *pcwccw)
{
	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return get_encoder_direction(ax, pcwccw);
}

INT set_sw_pause(int ax, int bOn)
{
	INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;
	
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);
		AxisDpram[ax]->Int_Type[0] = bOn;

		// Wake DSP!. Send Command
		_flush_dpram(SET_SW_PAUSE);
		mmc_error = err = _wait_for_reply(3000);

		MMCMutexUnlock ();
		
		if (err != AMC_SUCCESS)
		{
			mmc_error = MMC_TIMEOUT_ERR;
			return MMC_TIMEOUT_ERR;
		}
	}

	return err;
}

INT get_sw_pause_event(int ax, int *pbOn)
{
	INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;
	
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);

		// Wake DSP!. Send Command
		_flush_dpram(GET_SW_PAUSE_EVENT);
		mmc_error = err = _wait_for_reply(3000);
		
		*pbOn = (int)AxisDpram[ax]->Int_Type[0];

		MMCMutexUnlock ();
	}
	return err;
}

INT set_sw_pause_checkbit(int ax, int bBitNo, int ActvLvl)
{
	INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;
	
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);
		AxisDpram[ax]->Int_Type[0] = bBitNo;
		AxisDpram[ax]->Int_Type[1] = ActvLvl;

		// Wake DSP!. Send Command
		_flush_dpram(SET_SW_PAUSE_CHECK_BIT);
		mmc_error = err = _wait_for_reply(3000);

		MMCMutexUnlock ();
		
		if (err != AMC_SUCCESS) 
		{
			mmc_error = MMC_TIMEOUT_ERR;
			return MMC_TIMEOUT_ERR;
		}
	}
	return err;
}
INT get_sw_pause_checkbit(int ax, int *pbBitNo, int *pActvLvl)
{
	INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;
	
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);

		// Wake DSP!. Send Command
		_flush_dpram(GET_SW_PAUSE_CHECK_BIT);
		mmc_error = err = _wait_for_reply(3000);

		*pbBitNo = (int)AxisDpram[ax]->Int_Type[0];
		*pActvLvl = (int)AxisDpram[ax]->Int_Type[1];

		MMCMutexUnlock ();
	}

    return err;
}


typedef struct
{
	char	mode;	// 0: pulse/sec^2, 1:pulse/sec
	char	cmd;	// 1:move_p, 
					// 2:move_n
					// 3:move_s
					// 4:move_ds
					// 5:move_dc
					// 6:move_dds
					// 7:move_ddds
	short int	ax;
	float	a[5];
	float	v[5];
	float	m[5];
} MOVE_X_PARAM;


int move_x_cmd_send(MOVE_X_PARAM *mvxParam)
{
	INT err;
	int ax;
	ax = mvxParam->ax;

	if ((ax >=TOTAL_AXIS_NUM) || ax <0) 	return	mmc_error = MMC_INVALID_AXIS;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		// 파라미터 구조체를 통체로 기록한다.
		_write_dpramregs(C2DSPOFS_BODY_BASE_ADDR, mvxParam, sizeof(MOVE_X_PARAM));
		
		// Wake DSP!. Send Command
		_flush_dpram(SEND_MOVE_X_CMD);

		if(_wait_for_reply(3000) != AMC_SUCCESS)
		{
			mmc_error = err = MMC_TIMEOUT_ERR;
		} else {// 실행상태를 읽기 위해서 다시한번 DSP에 명령을 내려 보낸다.
			_write_dpramreg(AXIS_REG, (CHAR) ax);
			_flush_dpram(GET_ERROR_STATUS);
			mmc_error = err = _wait_for_reply(3000);
			if (err == MMC_OK)
				mmc_error = err = AxisDpram[ax]->Long_Type;//에러 코드를 리턴 받음
		}

		MMCMutexUnlock ();
	}
	return	err;
}

extern void LockMovex();
extern void UnlockMovex();

INT __move_p(int mode, int ax, double a, double v)
{
	INT err;
	MOVE_X_PARAM mvxParam;

	mvxParam.mode = mode;
	mvxParam.cmd = CMD_MOVE_P;
	mvxParam.ax = ax;
	mvxParam.a[0] = (float)a;// * BootFrame[0].GearRatio[ax];//2011.10.8, warning
	mvxParam.v[0] = (float)v;// * BootFrame[0].GearRatio[ax];//2011.10.8, warning

	mmc_error = err = move_x_cmd_send(&mvxParam);

	return err;
}
INT __move_n(int mode, int ax, double a, double v)
{
	INT err;
	MOVE_X_PARAM mvxParam;

	mvxParam.mode = mode;
	mvxParam.cmd = CMD_MOVE_N;
	mvxParam.ax = ax;
	mvxParam.a[0] = (float)a;
	mvxParam.v[0] = (float)v;

	mmc_error = err = move_x_cmd_send(&mvxParam);

	return err;
}
INT __move_s(int mode, int ax, double a)
{
	INT err;
	MOVE_X_PARAM mvxParam;

	mvxParam.mode = mode;
	mvxParam.cmd = CMD_MOVE_S;
	mvxParam.ax = ax;
	mvxParam.a[0] = (float)a;

	mmc_error = err = move_x_cmd_send(&mvxParam);

	return err;
}
INT __move_ds(int mode, int ax, double a1, double a2, double v, double m)
{
	INT err;
	MOVE_X_PARAM mvxParam;

	mvxParam.mode = mode;
	mvxParam.cmd = CMD_MOVE_DS;
	mvxParam.ax = ax;
	mvxParam.a[0] = (float)a1;
	mvxParam.a[1] = (float)a2;
	mvxParam.v[0] = (float)v;
	mvxParam.m[0] = (float)m;

	mmc_error = err = move_x_cmd_send(&mvxParam);

    return err;
}

INT move_pt(int ax, double acc, double v) { return __move_p(1, ax, acc, v); }
INT move_p(int ax, double acc, double v) {  return __move_p(0, ax, acc, v); }
INT move_nt(int ax, double acc, double v) { return __move_n(1, ax, acc, v); }
INT move_n(int ax, double acc, double v) {  return __move_n(0, ax, acc, v); }
INT move_st(int ax, double acc) { return __move_s(1, ax, acc); }
INT move_s(int ax, double acc) {  return __move_s(0, ax, acc); }
INT move_dst(int ax, double a1, double a2, double v, double m)
{
	return __move_ds(1, ax, a1, a2, v, m);
}
INT move_ds(int ax, double a1, double a2, double v, double m)
{
	return __move_ds(0, ax, a1, a2, v, m);
}

int clear_amc_error()
{
	INT err;
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		// Wake DSP!. Send Command
		_flush_dpram(CLEAR_AMC_ERROR);
		mmc_error = err = _wait_for_reply(3000);

		MMCMutexUnlock();
	}	
	return err;
}

INT get_dbg_status(UCHAR *pucstatus)
{
	INT err;
	if (pucstatus == NULL) return MMC_OK;

	if ((err = MMCMutexLock ()) != MMC_OK)
	{
		mmc_error = err;
		return err;
	}

	// Wake DSP!. Send Command
	_flush_dpram(AMC_DBG_STATUS_GET);

	if(_wait_for_reply(8000) != AMC_SUCCESS)
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}

	_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0x10, pucstatus, 256);

	MMCMutexUnlock();
	mmc_error = MMC_OK;
	return MMC_OK;
}

INT get_dbg_status2(UCHAR *pucstatus, char nOfs)
{
	INT err;
	int ax =1;
	if (pucstatus == NULL) return MMC_OK;

	if ((err = MMCMutexLock ()) != MMC_OK)
	{
		mmc_error = err;
		return err;
	}


	_write_dpramreg(AXIS_REG, (CHAR)ax);
	AxisDpram[ax]->Char_Type[1] = nOfs;	

	// Wake DSP!. Send Command
	_flush_dpram(AMC_DBG_STATUS_GET2);

	if(_wait_for_reply(8000) != AMC_SUCCESS)
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}

	_read_dpramregs(C2DSPOFS_HDR_BASE_ADDR + 0x10, pucstatus, 256);	

	MMCMutexUnlock();
	mmc_error = MMC_OK;
	return MMC_OK;
}

#define		TEST_VAL1		0xac
#define		TEST_VAL2		0x53

INT test_dpram(int ncount, HWND hWnd, UINT uiMsgID)
{
	unsigned char val1, val2;
	int nerrcnt = 0;
	int i;
	INT err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;

	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 0);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 3, 1);
	_flush_dpram(CMDID_DPRAM_TESTING);
	MMCMutexUnlock(); 
	return MMC_OK;

	// dsp가 준비할 때까지 대기한다.
	// step 0 start
	if (hWnd) SendMessage(hWnd, uiMsgID, 0, 1);
	while (1)
	{
		val1 = 1;
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 3, &val1);
		if (val1 == 0) break;
		if (++nerrcnt > 100000) break;
	}
	if (nerrcnt > 100000) 
	{
		// 실패
		if (hWnd) SendMessage(hWnd, uiMsgID, 0, 2);
		_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 5);

		MMCMutexUnlock();

		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}
	// step 0 success
	if (hWnd) SendMessage(hWnd, uiMsgID, 0, 3);

	// step 1 start
	if (hWnd) SendMessage(hWnd, uiMsgID, 1, 1);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 1, TEST_VAL1);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 2, TEST_VAL2);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 1);
	for(i = 0; i < ncount; i ++)
	{
		val1 = val2 = 0;
		_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 1, TEST_VAL1);
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 2, &val1);
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 3, &val2);
		if (val1 != TEST_VAL2 || val2 == 1)
		{
			// step1 test fail
			if (hWnd) SendMessage(hWnd, uiMsgID, 1, 2);
			_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 5);

			MMCMutexUnlock();

			return MMC_OK;
		}
	}
	// step 1 success
	if (hWnd) SendMessage(hWnd, uiMsgID, 1, 3);

	// step 2 start
	if (hWnd) SendMessage(hWnd, uiMsgID, 2, 1);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 2);
	for(i = 0; i < ncount; i ++)
	{
		val1 = val2 = 0;
		_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 1, TEST_VAL1);
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 3, &val2);
		if (val2 == 1)
		{
			// step2 test fail
			if (hWnd) SendMessage(hWnd, uiMsgID, 2, 2);
			_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 5);

			MMCMutexUnlock();

			return MMC_OK;
		}
	}
	// step 2 success
	if (hWnd) SendMessage(hWnd, uiMsgID, 2, 3);

	// step 3 start
	if (hWnd) SendMessage(hWnd, uiMsgID, 3, 1);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 3);
	for(i = 0; i < ncount; i ++)
	{
		val1 = val2 = 0;
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 2, &val1);
		if (val1 != TEST_VAL2)
		{
			// step3 test fail
			if (hWnd) SendMessage(hWnd, uiMsgID, 3, 2);
			_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 5);

			MMCMutexUnlock();

			return MMC_OK;
		}
	}
	// step 3 success
	if (hWnd) SendMessage(hWnd, uiMsgID, 3, 3);

	// step 4 start
	if (hWnd) SendMessage(hWnd, uiMsgID, 4, 1);
	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 4);
	for(i = 0; i < ncount; i ++)
	{
		val1 = val2 = 0;
		_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 1, TEST_VAL1);
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 1, &val1);
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 3, &val2);
		if (val1 != TEST_VAL1 || val2 == 1)
		{
			// step4 test fail
			if (hWnd) SendMessage(hWnd, uiMsgID, 4, 2);
			_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 5);

			MMCMutexUnlock();

			return MMC_OK;
		}
		_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 2, TEST_VAL2);
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 2, &val1);
		_read_dpramreg(C2DSPOFS_HDR_BASE_ADDR + 3, &val2);
		if (val1 != TEST_VAL2 || val2 == 1)
		{
			// step4 test fail
			if (hWnd) SendMessage(hWnd, uiMsgID, 4, 2);
			_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 5);

			MMCMutexUnlock();

			return MMC_OK;
		}
	}
	// step 4 success
	if (hWnd) SendMessage(hWnd, uiMsgID, 4, 3);

	_write_dpramreg(C2DSPOFS_HDR_BASE_ADDR, 5);

	MMCMutexUnlock();
	return MMC_OK;
}



INT reload_encoder_position(int ax)
{
	int bd = 0;
	INT err;

	if ((ax>=TOTAL_AXIS_NUM) || ax<0) 	return	mmc_error = MMC_INVALID_AXIS;
	
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		mmc_error = err = MMCCommCheck(1, &bd, RELOAD_SERVOPACK_POS, ax);
		if (err != MMC_OK)  err= _wait_for_reply(3000);

		MMCMutexUnlock();
	}
	return err;
}

INT reload_servopack_all()
{
	int bd = 0;
	int ax =1;
	INT err;
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		mmc_error = err = MMCCommCheck(1, &bd, RELOAD_SERVOPACK_ALL, ax);
		if (err != MMC_OK)  err= _wait_for_reply(8000);

		MMCMutexUnlock();
	}
	return err;
}

//필터링 방식 변경 -> 1회 5번 read 
int	read_dpram_int_filtering(INT2 *dpram_adrs, INT2 *dpram_data)
{
	INT2 actv_n_1, actv_n;
	INT2 tmp1, tmp2, tmp3;

	actv_n_1 = *dpram_adrs;
	actv_n = *dpram_adrs;

	if(actv_n_1 != actv_n) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp1 = *dpram_adrs;

	if(actv_n != tmp1) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp2 = *dpram_adrs;

	if(tmp1 != tmp2) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp3 = *dpram_adrs;

	if(tmp2 != tmp3) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	*dpram_data = tmp3;

	return 0;
}

int	read_dpram_char_filtering(unsigned char *dpram_adrs, unsigned char *dpram_data)
{
	unsigned char actv_n_1, actv_n;
	unsigned char tmp1, tmp2, tmp3;

	actv_n_1 = *dpram_adrs;
	actv_n = *dpram_adrs;

	if(actv_n_1 != actv_n) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp1 = *dpram_adrs;

	if(actv_n != tmp1) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp2 = *dpram_adrs;

	if(tmp1 != tmp2) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp3 = *dpram_adrs;

	if(tmp2 != tmp3) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	*dpram_data = tmp3;

	return 0;
}

int	read_dpram_int4_filtering(INT4 *dpram_adrs, INT4 *dpram_data)
{
	INT4 actv_n_1, actv_n;
	INT4 tmp1, tmp2, tmp3;

	actv_n_1 = *dpram_adrs;
	actv_n = *dpram_adrs;

	if(actv_n_1 != actv_n) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp1 = *dpram_adrs;

	if(actv_n != tmp1) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp2 = *dpram_adrs;

	if(tmp1 != tmp2) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp3 = *dpram_adrs;

	if(tmp2 != tmp3) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	*dpram_data = tmp3;

	return 0;
}

int	read_dpram_float_filtering(float *dpram_adrs, float *dpram_data)
{
	float actv_n_1, actv_n;
	float tmp1, tmp2, tmp3;

	actv_n_1 = *dpram_adrs;
	actv_n = *dpram_adrs;

	if(actv_n_1 != actv_n) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp1 = *dpram_adrs;

	if(actv_n != tmp1) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp2 = *dpram_adrs;

	if(tmp1 != tmp2) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	tmp3 = *dpram_adrs;

	if(tmp2 != tmp3) 
	{
		*dpram_data = 0;
		return MMC_FILTERING_ERROR;
	}
	*dpram_data = tmp3;

	return 0;
}