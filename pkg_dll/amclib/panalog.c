#include  	"pcdef.h"

#include	"amc_internal.h"



/**********
*	FUNCTION NAME	: get_analog(INT channel, INT *value)
*	FUNCTION       : Get an analog input value
*********************************************************************/
INT get_analog (INT channel, pINT value)
{
	INT		bn, ChannelNo;
	INT err;

	mmc_error = err = MMC_OK;
	if ((bn=Find_Analog_Channel (channel))<0) mmc_error = err = MMC_ILLEGAL_ANALOG;
	else
	{
		ChannelNo = channel - (bn*4);
		*value = (int)CommDpram[bn]->AnalogChannel[ChannelNo];
	}
	return	err;
}

/**********
*	FUNCTION NAME	: set_dac_output(INT axis, INT voltage)
*	FUNCTION       : Set the value of the 12bit analog output
*********************************************************************/
INT set_dac_output(INT ax, INT voltage)
{
	INT		bn, jnt, err;

	// 2007.12.3, ckyu
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Int_Type[0] = voltage;
		AxisDpram[ax]->Int_Type[1] = 0;	// no eeprom writing
		AxisDpram[ax]->Int_Type[2] = 0;	// 0: dac_output, 1:analog_offset
		mmc_error = err = MMCCommCheck (1, &bn, PUT_DAC_OUT, jnt);
	}
	MMCMutexUnlock ();

    return	err;
}

/**********
*	FUNCTION NAME	: get_dac_output(INT axis, INT voltage)
*	FUNCTION       : Get the value of the 12bit analog output
*********************************************************************/
INT get_dac_output(INT ax, pINT voltage)
{
	INT		bn, jnt, err;

	// 2007.12.3, ckyu
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Int_Type[2] = 0;	// 0: dac_output, 1:analog_offset
		if ((mmc_error = err = MMCCommCheck (1, &bn, GET_DAC_OUT, jnt)) == MMC_OK)
		{
			AxisDpram[ax]->Int_Type[2] = 0;	// 0: dac_output, 1:analog_offset
			*voltage = (int)AxisDpram[ax]->Int_Type[0];
		}
	}
	MMCMutexUnlock ();
	return	err;
}


/**********
*	FUNCTION NAME	: set_dac_output(INT axis, INT voltage)
*	FUNCTION       : Set the value of the 12bit analog output
*********************************************************************/
INT set_analog_offset(INT ax, INT offset)
{
	INT		bn, jnt, err;

	// 2007.12.3, ckyu
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Int_Type[0] = offset;
		AxisDpram[ax]->Int_Type[1] = 0;	// no eeprom writing
		AxisDpram[ax]->Int_Type[2] = 1;	// 0: dac_output, 1:analog_offset
		mmc_error = err = MMCCommCheck (1, &bn, PUT_DAC_OUT, jnt);
	}
	MMCMutexUnlock ();

    return	err;
}

/**********
*	FUNCTION NAME	: get_dac_output(INT axis, INT voltage)
*	FUNCTION       : Get the value of the 12bit analog output
*********************************************************************/
INT get_analog_offset(INT ax, pINT offset)
{
	INT		bn, jnt, err;

	// 2007.12.3, ckyu
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Int_Type[2] = 1;	// 0: dac_output, 1:analog_offset
		if ((mmc_error = err = MMCCommCheck (1, &bn, GET_DAC_OUT, jnt)) == MMC_OK)
		{
			AxisDpram[ax]->Int_Type[2] = 1;	// 0: dac_output, 1:analog_offset
			*offset = (int)AxisDpram[ax]->Int_Type[0];
		}
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: Find_Analog_Channel(INT ChannelNo)
*	FUNCTION       : Return Find A/D Channel
*********************************************************************/
INT Find_Analog_Channel(INT ChannelNo)
{
	if(ChannelNo<4)			return	MMC_BD1;
	else if(ChannelNo<8)		return	MMC_BD2;
	else if(ChannelNo<12)	return	MMC_BD3;
	else if(ChannelNo<16)	return	MMC_BD4;
	else							return	FUNC_ERR;
}