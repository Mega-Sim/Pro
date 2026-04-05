
#include  	"pcdef.h"

/**********
*	FUNCTION NAME	: CDINoBootWrite(INT ax, INT val, INT comm)
*	FUNCTION       : Command & Data write to DSP
*********************************************************************/
INT		CDINoBootWrite (INT ax,INT val, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Int_Type[0] = val;
		AxisDpram[ax]->Int_Type[1] = 0;	// no eeprom writing
		mmc_error = err = MMCCommCheck (1, &bn, comm, jnt);
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDFNoBootWrite(INT ax, FLOAT pos, INT comm)
*	FUNCTION       : WRITE Float type value To DSP
*********************************************************************/
INT		CDFNoBootWrite (INT ax, FLOAT pos, INT comm)
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Float_Type = pos;
		mmc_error = err = MMCCommCheck (1, &bn, comm, jnt);
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDFDNoBootWrite(INT ax, FLOAT pos, INT comm)
*	FUNCTION       : WRITE Float type value To DSP
*********************************************************************/
INT		CDFDNoBootWrite (INT ax, double pos, INT comm)
{
	INT	bn,jnt, err=MMC_OK;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Float_Type = (float)pos;
		mmc_error = err = MMCCommCheck (1, &bn, comm, jnt);
		if ((err==MMC_OK) && (comm==PUT_GEAR_RATIO))
			BootFrame[0].GearRatio[ax] = (float)pos;
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: CDI3NoBootWrite(INT ax, INT pos, INT action, INT comm)
*	FUNCTION       : WRITE INT type value To DSP
*********************************************************************/
INT		CDI3NoBootWrite (INT ax, LONG pos, INT action, INT comm)
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
		mmc_error = err = MMCCommCheck (1, &bn, comm, jnt);
	}
	MMCMutexUnlock ();
	return	err;
}
