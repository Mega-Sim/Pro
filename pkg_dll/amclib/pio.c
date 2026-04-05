#include "pcdef.h"
#include "amc_internal.h"
#include "log.h"

#include <stdio.h>

/**********
*	FUNCTION NAME	: home_switch(axis)
*	FUNCTION       : Read state of home sensor
*				     TRUE = HIGH(Sensor On),  FALSE = LOW(Sensor Off)
*                    MMC_TIMEOUT_ERR = MutexLock Error
*********************************************************************/
#ifndef MDF_FUNC	
INT home_switch(INT axis)
{
	INT2 err, status, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;
	pi = (INT2 *) &AxisDpram[axis]->AxisSource;
	read_dpram_int_filtering(pi, &status);
	MMCMutexUnlock ();
	status &= ST_HOME_SWITCH;
	if(status)		return	TRUE;
	return	FALSE;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT home_switch(INT axis, pINT chk_err)
{
	INT2 err, status, *pi;
	char filter_i=0;
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
		*chk_err = MMC_MUTEXLOCK_ERROR; 
		return 0;
	}
	pi = (INT2 *) &AxisDpram[axis]->AxisSource;

//필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int_filtering(pi, &status);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	MMCMutexUnlock ();

	*chk_err = MMC_OK;
	status &= ST_HOME_SWITCH;
	if(status)		return	TRUE;
	return	FALSE;
}
#endif


/**********
*	FUNCTION NAME	: pos_switch(axis)
*	FUNCTION       : Read state of positive limit sensor
*				     TRUE = HIGH(Sensor On),  FALSE = LOW(Sensor Off)
*                    MMC_TIMEOUT_ERR = MutexLock Error
*********************************************************************/
#ifndef MDF_FUNC	
INT pos_switch(INT	axis)
{
	INT2 err, status, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR; 
	pi = (INT2 *) &AxisDpram[axis]->AxisSource;
	read_dpram_int_filtering(pi, &status);
	MMCMutexUnlock ();
	status &= ST_POS_LIMIT;
	if(status)		return	TRUE;
	return	FALSE;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT pos_switch(INT	axis, pINT chk_err)
{
	INT2 err, status, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
		*chk_err = MMC_MUTEXLOCK_ERROR; 
		return 0;
	}
	pi = (INT2 *) &AxisDpram[axis]->AxisSource;

//필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int_filtering(pi, &status);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	MMCMutexUnlock ();

	*chk_err = MMC_OK;
	status &= ST_POS_LIMIT;
	if(status)		return	TRUE;
	return	FALSE;
}
#endif


/**********
*	FUNCTION NAME	: neg_switch(axis)
*	FUNCTION       : Read state of negative limit sensor
*				     TRUE = HIGH(Sensor On),  FALSE = LOW(Sensor Off)
*                    MMC_TIMEOUT_ERR = MutexLock Error
*********************************************************************/
#ifndef MDF_FUNC
INT neg_switch(INT	axis)
{
	INT2 err, status, *pi;
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;  
	pi = (INT2 *) &AxisDpram[axis]->AxisSource;
	read_dpram_int_filtering(pi, &status);
	MMCMutexUnlock ();
	status &= ST_NEG_LIMIT;
	if(status)		return	TRUE;
	return	FALSE;
}	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT neg_switch(INT	axis, pINT chk_err)
{
	INT2 err, status, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
		*chk_err = MMC_MUTEXLOCK_ERROR;
		return 0;
	}
	pi = (INT2 *) &AxisDpram[axis]->AxisSource;

//필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int_filtering(pi, &status);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	MMCMutexUnlock ();

	*chk_err = MMC_OK;
	status &= ST_NEG_LIMIT;
	if(status)		return	TRUE;
	return	FALSE;
}
#endif


/**********
*	FUNCTION NAME	: amp_fault_switch(axis)
*	FUNCTION       : Read state of amp fault bit
*				     TRUE = HIGH(Amp Fault), FALSE = LOW(Amp Ready)
*                    MMC_TIMEOUT_ERR = MutexLock Error
*********************************************************************/
#ifndef MDF_FUNC
INT amp_fault_switch(INT axis)
{
	INT2 err, status, *pi;
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;
	pi = (INT2 *) &AxisDpram[axis]->AxisSource;
	read_dpram_int_filtering(pi, &status);
	MMCMutexUnlock ();
	status &= ST_AMP_FAULT;
	if(status)		return	TRUE;
	return	FALSE;
}	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT amp_fault_switch(INT axis, pINT chk_err)
{
	INT2 err, status, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
		*chk_err = MMC_MUTEXLOCK_ERROR;
		return 0;
	}
	pi = (INT2 *) &AxisDpram[axis]->AxisSource;

//필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int_filtering(pi, &status);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	MMCMutexUnlock ();

	*chk_err = MMC_OK;
	status &= ST_AMP_FAULT;
	if(status)		return	TRUE;
	return	FALSE;
}
#endif

// out port 64bit 데이터를 설정한다.
/**********
*	FUNCTION NAME	: set_io(INT port, LONG value[2])
*	FUNCTION       : Set an 64-bit byte port value
*********************************************************************/
INT set_io64(INT port, pINT value)
{
	INT	bn,portNo, err=MMC_OK;

	if((bn=Find_IO_Port(port))<0)
	{
		mmc_error = MMC_ILLEGAL_IO;
		return	MMC_ILLEGAL_IO;
	}
	portNo=port - bn;

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;

	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue, &value[0], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue2, &value[1], 4);

	mmc_error = err = MMCCommCheck (1, &bn, WRITE_IO, portNo);
	MMCMutexUnlock ();
	return	err;

}

/**********
*	FUNCTION NAME	: get_io(INT bd_num, INT port, INT *value)
*	FUNCTION       : Get an 32-bit byte port value
*********************************************************************/
// 출력되는 64bit io값을 리턴한다.
INT get_out64(INT port, INT value[2])
{
    MYLOG("get_out64\n");
    
    INT	bn,portNo,err=MMC_OK;

	if((bn=Find_IO_Port(port))<0)
	{
		mmc_error = MMC_ILLEGAL_IO;
		return	MMC_ILLEGAL_IO;
	}
	portNo=port - bn;

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;
	mmc_error = err = MMCCommCheck(1,&bn,READ_OUT_IO,portNo);
	if(!err)
	{
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue, &value[0], 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue2, &value[1], 4);
	}
	MMCMutexUnlock ();
	return	err;
}


INT get_out_io(INT port, INT value[2])
{
	return get_out64(port, value);
}



/**********
*	FUNCTION NAME	: get_io(INT bd_num, INT port, INT *value)
*	FUNCTION       : Get an 32-bit byte port value
*********************************************************************/
// 입력되는 64bit io값을 리턴한다.
//void get_io64(INT port, pINT value)
int get_io64(INT port, pINT value)
{
    MYLOG("get_io64\n");
    
    INT	bn = 0;
	INT4 err, actv, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR; 

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue/sizeof(UINT2)));

//필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[0] = actv;

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue2/sizeof(UINT2)));

//필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[1] = actv;

	MMCMutexUnlock();
	return MMC_OK;
}

#if (defined(__AMC_SMD) || defined(__AMC_V70))	
// out port 256bit 데이터를 설정한다.
/**********
*	FUNCTION NAME	: set_io_output(INT port, LONG value[8])
*	FUNCTION       : Set an 256-bit byte port value
*********************************************************************/
INT set_io_output(INT port, pINT value)
{
	INT	bn,portNo, err=MMC_OK;

	if((bn=Find_IO_Port(port))<0)
	{
		mmc_error = MMC_ILLEGAL_IO;
		return	MMC_ILLEGAL_IO;
	}
	portNo=port - bn;

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;

	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue, &value[0], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue2, &value[1], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue3, &value[2], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue4, &value[3], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue5, &value[4], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue6, &value[5], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue7, &value[6], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue8, &value[7], 4);

	mmc_error = err = MMCCommCheck (1, &bn, SETIOOUPUT, portNo);
	MMCMutexUnlock ();
	return	err;

}

/**********
*	FUNCTION NAME	: get_io_output(INT bd_num, INT port, INT *value)
*	FUNCTION       : Get an 32-bit byte port value
*********************************************************************/
// 출력되는 256bit io값을 리턴한다.
INT get_io_output(INT port, INT value[8])
{
	INT	bn,portNo,err=MMC_OK;

	if((bn=Find_IO_Port(port))<0)
	{
		mmc_error = MMC_ILLEGAL_IO;
		return	MMC_ILLEGAL_IO;
	}
	portNo=port - bn;

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;
	mmc_error = err = MMCCommCheck(1,&bn,GETIOOUPUT,portNo);
	if(!err)
	{
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue, &value[0], 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue2, &value[1], 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue3, &value[2], 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue4, &value[3], 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue5, &value[4], 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue6, &value[5], 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue7, &value[6], 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue8, &value[7], 4);
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: get_io_input(INT port, INT *value)
*	FUNCTION       : Get an 32-bit byte port value
*********************************************************************/
// 입력되는 256bit io값을 리턴한다.
int get_io_input(INT port, pINT value) 
{
	INT	bn = 0;
	INT4 err, actv, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR; 

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue1_1/sizeof(UINT2)));
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[0] = actv;

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue2_1/sizeof(UINT2)));
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[1] = actv;

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue3/sizeof(UINT2)));
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[2] = actv;

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue4/sizeof(UINT2)));
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[3] = actv;

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue5/sizeof(UINT2)));
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[4] = actv;

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue6/sizeof(UINT2)));
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[5] = actv;

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue7/sizeof(UINT2)));
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[6] = actv;

	pi=(INT4 *) (&CommDpram[bn]->Command + (CD_Io_inValue8/sizeof(UINT2)));
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		return err;
	}
	value[7] = actv;

	MMCMutexUnlock();
	return MMC_OK;

}


/**********
*	FUNCTION NAME	: set_io_count(int in_c, int out_c)
*	FUNCTION       : Set Input,Output count
*********************************************************************/
INT	set_io_count(int in_c, int out_c)
{
    MYLOG("set_io_count\n");
    
    INT	bn, err=MMC_OK;

	if ((in_c < 0) || (in_c > 256))	
	{
		mmc_error = MMC_ILLEGAL_IO;
		return	MMC_ILLEGAL_IO;
	}

	if ((out_c < 0) || (out_c > 256))	
	{
		mmc_error = MMC_ILLEGAL_IO;
		return	MMC_ILLEGAL_IO;
	}

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;

	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue7, &in_c, 4);			// input의 count수 
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue8, &out_c, 4);			// output의 count수 

	mmc_error = err = MMCCommCheck (1, &bn, SETIOCOUNT, 0);
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: get_io_count(pINT in_c, pINT out_c)
*	FUNCTION       : get Input,Output count
*********************************************************************/
INT	get_io_count(pINT in_c, pINT out_c)
{
	INT	bn, err=MMC_OK;
	INT tmp_in,tmp_out;

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;

	mmc_error = err = MMCCommCheck(1,&bn,GETIOCOUNT,0);
	if(!err)
	{
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue7, &tmp_in, 4);
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue8, &tmp_out, 4);
		*in_c  = tmp_in;
		*out_c = tmp_out;
	}

	MMCMutexUnlock ();
	return	err;
}
#endif  // (defined(__AMC_SMD) || defined(__AMC_V70))	


/**********
*	FUNCTION NAME	: set_bit(INT bitNo)
*	FUNCTION       : Sets the state of an I/O bit to TRUE
*********************************************************************/
INT set_bit(INT bitNo)
{
	return	(SetReset_Bit_IO(bitNo,1));
}

/**********
*	FUNCTION NAME	: reset_bit(INT bitNo)
*	FUNCTION       : Sets the state of an I/O bit to FALSE
*********************************************************************/
INT reset_bit(INT bitNo)
{
	return	(SetReset_Bit_IO(bitNo,0));
}

#if defined(__AMC_29x)
int __get_bit(int bitno, INT *pdat)
{
	if (bitno < 32) return (pdat[0]>> bitno) & 0x01;
	return (pdat[1]>>(bitno-32))&0x01;
}
#endif 

#if (defined(__AMC_SMD) || defined(__AMC_V70))	
int __get_bit(int bitno, INT *pdat)
{
	if (bitno < 32)			return (pdat[0]>> bitno) & 0x01;
	else if(bitno < 64)		return (pdat[1]>>(bitno-32))&0x01;
	else if(bitno < 96)		return (pdat[2]>>(bitno-64))&0x01;
	else if(bitno < 128)	return (pdat[3]>>(bitno-96))&0x01;
	else if(bitno < 160)	return (pdat[4]>>(bitno-128))&0x01;
	else if(bitno < 192)	return (pdat[5]>>(bitno-160))&0x01;
	else if(bitno < 224)	return (pdat[6]>>(bitno-192))&0x01;
	else if(bitno < 256)	return (pdat[7]>>(bitno-224))&0x01;
	else return 0;
}
#endif 

#ifndef MDF_FUNC	
INT get_bit(INT bitno)
{
	INT dat[2];
	if (bitno < 0) return -1;
	if (bitno > 63) return -1;

	get_io64(0, dat);
	return __get_bit(bitno, dat);
}

INT get_outbit(INT bitno)
{
	INT dat[2];
	if (bitno < 0) return -1;
	if (bitno > 63) return -1;

	get_out64(0, dat);
	return __get_bit(bitno, dat);
}

#else	//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
#if defined(__AMC_29x)
INT get_bit(INT bitno, pINT chk_err)
{
	INT dat[2];
	int err;

	if (bitno < 0)
	{
		*chk_err = MMC_ILLEGAL_IO;
		return 0;
	}
	if (bitno > 63)
	{
		*chk_err = MMC_ILLEGAL_IO;
		return 0;
	}

	err = get_io64(0, dat);

	if(err)
	{
		*chk_err = err;
		return 0;
	}
 
	*chk_err = MMC_OK;
	return __get_bit(bitno, dat);
}

INT get_outbit(INT bitno, pINT chk_err)
{
	INT dat[2],tmp_go_e;
	if (bitno < 0)
	{
		*chk_err = MMC_ILLEGAL_IO;
		return 0;
	}
	if (bitno > 63)
	{
		*chk_err = MMC_ILLEGAL_IO;
		return 0;
	}

	if(tmp_go_e=get_out64(0, dat))
	{
		*chk_err = tmp_go_e;
		return 0;
	}

	*chk_err = MMC_OK;
	return __get_bit(bitno, dat);
}
#endif	//#if defined(__AMC_29x)

#if (defined(__AMC_SMD) || defined(__AMC_V70))	
INT get_bit(INT bitno, pINT chk_err)
{
	INT dat[8];
	int err;

	if (bitno < 0)
	{
		*chk_err = MMC_ILLEGAL_IO;
		return 0;
	}
	if (bitno > 255)
	{
		*chk_err = MMC_ILLEGAL_IO;
		return 0;
	}

    //get_io64 함수원형 수정 관련 하여 수정함.
	err = get_io_input(0, dat);

	if(err)
	{
		*chk_err = err;
		return 0;
	}
 
	*chk_err = MMC_OK;
	return __get_bit(bitno, dat);
}

INT get_outbit(INT bitno, pINT chk_err)
{
	INT dat[8],tmp_go_e;
	if (bitno < 0)
	{
		*chk_err = MMC_ILLEGAL_IO;
		return 0;
	}
	if (bitno > 255)
	{
		*chk_err = MMC_ILLEGAL_IO;
		return 0;
	}

	if(tmp_go_e=get_io_output(0, dat))
	{
		*chk_err = tmp_go_e;
		return 0;
	}

	*chk_err = MMC_OK;
	return __get_bit(bitno, dat);
}
#endif	// #if (defined(__AMC_SMD) || defined(__AMC_V70))
#endif	//#ifndef MDF_FUNC

/**********
*	FUNCTION NAME	: SetReset_Bit_IO(INT bitNo)
*	FUNCTION       : Set or Reset dedicated I/O bit
*********************************************************************/
INT	SetReset_Bit_IO(INT bitNo, INT flag)
{
	INT	bn,bit,comm, err=MMC_OK;

	if ((bn=Find_IO_Bit(bitNo))<0)	
	{
		mmc_error = MMC_ILLEGAL_IO;
		return	MMC_ILLEGAL_IO;
	}
	bit = bitNo/* - (bn*32)*/;
	comm = flag ? SET_IO_BIT : RESET_IO_BIT;

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;

    _write_dpramregs(DPRAM_COMM_BASEOFS + CD_Io_outValue, &bit, 4);

	mmc_error = err = MMCCommCheck (1, &bn, comm, 0);
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: Find_IO_Bit(INT bitNo)
*	FUNCTION       : Return Find IO Board Number
*********************************************************************/
INT	Find_IO_Bit(INT bitNo)
{
	if(bitNo<64)			return	MMC_BD1;
	else if(bitNo<(64*2) )	return	MMC_BD2;
	else if(bitNo<(64*3) )	return	MMC_BD3;
	else if(bitNo<(64*4))	return	MMC_BD4;
	else						return	FUNC_ERR;
}

/**********
*	FUNCTION NAME	: Find_IO_Bit(INT bitNo)
*	FUNCTION       : Return Find IO Board Number
*********************************************************************/
INT		Find_IO_Port (INT portNo)
{
	if(portNo<1)		return	MMC_BD1;
	else if(portNo<2)	return	MMC_BD2;
	else if(portNo<3)	return	MMC_BD3;
	else if(portNo<4)	return	MMC_BD4;
	else					return	FUNC_ERR;
}
