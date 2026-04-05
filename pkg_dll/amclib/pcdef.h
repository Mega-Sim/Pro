#ifndef __PCDEF_H__
#define __PCDEF_H__

/********************************************************************
*	FILE NAME  :  DEFINE.H
*********************************************************************/
#include	<windows.h>
#include	<winbase.h>

#include "amclib.h"
#include "amc_define.h"

#define		AMC_PARAM_FILE		"AmcParam.ini"
#define		AMC_DEF_PARAM_FILE	"DefaultParam.ini"


#define	CHAR		char
#define	UCHAR		unsigned char
#define	LONG		long
#define	FLOAT		float
#define	VOID		void
#define	INT4		int
#define	INT2		short
#define	UINT2		unsigned short
#define	UINT4		unsigned int

#define	FBINWRITE	"w"
#define	FBINREAD 	"r"

#define	AXIS_COMM_WAIT		2000L

#define		DPRAM_COMM_BASEOFS			0x180


/*-----------------------------------------------------------
*	MMC Library Version
*---------------------------------------------------------*/
#define	MMC_SW_VER				3.0
/*-----------------------------------------------------------
*	MMC COMMAND DEFINES
*---------------------------------------------------------*/
#define	PTP_MOVE					1
#define	CP_MOVE						2
#define	GET_GAIN_VALUE 				3
#define	PUT_GAIN_VALUE				4
#define	GET_VGAIN_VALUE 			5
#define	PUT_VGAIN_VALUE				6
#define	PUT_POSITION				7
#define	GET_A_POSITION				8
#define	GET_C_POSITION				9
#define	GET_ERROR     				10
#define	PUT_IN_POSITION				11
#define	GET_IN_POSITION				12
#define	PUT_ERR_LIMIT 				13
#define	GET_ERR_LIMIT				14
#define	GET_EMPTY_FRAME				15
#define	SET_BOOT_FRAME				16
#define	VEL_MOVE					17
#define	WRITE_IO					18		// 64bit in ���� �д´�.
#define	SET_IO_BIT					19
#define	RESET_IO_BIT				20
#define	MMC_DELAY					21
#define	GET_MOTOR_TYPE				22
#define	SET_MOTOR_TYPE				23
#define	MSERVO_ON					24
#define	MSERVO_OFF					25
#define	ALARM_RESET					26
#define	ALARM_SET					27
#define	PUT_CLEAR_STATUS			28
#define	PUT_STOP_EVENT				29
#define	PUT_STOP_RATE				30
#define	GET_STOP_RATE				31
#define	PUT_E_STOP_EVENT			32
#define	PUT_E_STOP_RATE				33
#define	GET_E_STOP_RATE				34
#define	PUT_POS_EVENT   			35
#define	GET_POS_EVENT   			36
#define	PUT_NEG_EVENT   			37
#define	GET_NEG_EVENT   			38
#define	PUT_POS_LEVEL   			39
#define	GET_POS_LEVEL   			40
#define	PUT_NEG_LEVEL   			41
#define	GET_NEG_LEVEL   			42
#define	PUT_HOME_EVENT				43
#define	GET_HOME_EVENT				44
#define	PUT_HOME_LEVEL				45
#define	GET_HOME_LEVEL				46
#define	PUT_INDEX_REQUIRED			47
#define	GET_INDEX_REQUIRED			48
#define	PUT_INT_STOP   				49
#define	PUT_INT_E_STOP     			50
#define	GET_AXIS_STAT   			51
#define	GET_AXIS_SOURCE				52
#define	PUT_AMP_FAULT  				53
#define	GET_AMP_FAULT  				54
#define	PUT_AMP_FAULT_LEVEL			55
#define	GET_AMP_FAULT_LEVEL			56
#define	PUT_AMP_RESET_LEVEL			57
#define	GET_AMP_RESET_LEVEL			58
#define	PUT_DAC_OUT         		59
#define	GET_DAC_OUT					60
#define	PUT_FEEDBACK_DEVICE			61
#define	GET_FEEDBACK_DEVICE			62
#define	PUT_VOLTAGE_DEVICE			63
#define	GET_VOLTAGE_DEVICE			64
#define	PUT_AXIS_BOOT_FRAME			65
#define	PUT_FRAMES_CLEAR   			66
#define	PUT_CLOSED_LOOP				67
#define	GET_CLOSED_LOOP				68
#define	PUT_SERVO_ON_LEVEL			69
#define	GET_SERVO_ON_LEVEL			70
#define	PUT_INT_ENABLE				71
#define	GET_BUF_POSITION			72
#define	PUT_CLEAR_STOP				73
#define	MAIN_DELAY					74
#define	PUT_IO_TRIGGER				75
#define	PUT_SW_UPPER_LIMIT			76
#define	GET_SW_UPPER_LIMIT			77
#define	PUT_SW_LOWER_LIMIT			78
#define	GET_SW_LOWER_LIMIT			79
#define	READ_OUT_IO   				80		// 64bit out ���� �д´�.
#define	PUT_VT_CONTROL				81
#define	GET_VT_CONTROL				82
#define	PUT_POS_I_MODE				83
#define	GET_POS_I_MODE				84
#define	PUT_VEL_I_MODE				85
#define	GET_VEL_I_MODE				86
#define	PUT_GEAR_RATIO				87
#define	GET_GEAR_RATIO				88
#define	PUT_SYNC_MAP_AXES			89
#define	PUT_SYNC_CONTROL_ON			90
#define	PUT_SYNC_CONTROL_OFF		91
#define	PUT_SYNC_GAIN				92
#define	GET_SYNC_GAIN				93
#define	PUT_COMPENSATION_POS		94
#define	PUT_PULSE_MODE				95
#define	GET_PULSE_MODE				96
#define	GET_E_POSITION				97
#define	GET_ES_POSITION				98
#define	PUT_PULSE_RATIO				99
#define	GET_PULSE_RATIO				100
#define	PUT_INTERPOLATION			101
#define	GET_EMPTY_INTERPOLATION		102
#define	PUT_ABS_ENCODER_RESET		103
#define	PUT_C_POSITION				104

#define	CP_LINE_MOVE				107

#define	PUT_EXIST_CHK				111

#define SET_OHT_MODEL_ID     		112
#define	GET_OHT_MODEL_ID	    	113

#define	GET_AXIS_VAR				120		

#define	CMDID_DPRAM_TESTING			140
#define PUT_CLEANVOLTAGE 			141

#define	C2DSP_SAVE_MAP_INFO			151		// Download map info to DSP eeprom, added by ckyu, 2006.12.08
#define	C2DSP_SEND_STRING			152		// Download map info to DSP eeprom, added by ckyu, 2006.12.08
#define	C2DSP_RUNCONFIG_UPLOAD		153		// ��Ʈ�Ķ���͸� pc�� �����´�.
#define	C2DSP_RUNCONFIG_DNLOAD		154		// ��Ʈ�Ķ���͸� DSP�� ������ �����Ѵ�.
#define	C2DSP_PARAM_UPLOAD			155		// EEPROM�� Ư�� ������ PC�� �����´�.
#define	C2DSP_PARAM_DNLOAD			156		// Ư�� ������ DSP�� ������ EEPROM�� �����Ѵ�.

#define	C2DSP_SET_ENCODER_OFFSET	157
#define	C2DSP_GET_ENCODER_OFFSET	158

// ���ڴ��� �����V�� ȹ��/�����ϱ����� �뵵
#define	PUT_ENCODER_OFFSET			157
#define	GET_ENCODER_OFFSET			158	
#define	FLUSH_SYSPARAM_TO_EEPROM	159

#define	WATCHDOG_OPERATIONS			160
#define	GET_WATCHDOG_STATUS			161

#define	SET_ENCODER_DIRECTION		162
#define	GET_ENCODER_DIRECTION		163

#define	SET_SW_PAUSE				164
#define	GET_SW_PAUSE_EVENT			165
#define	SET_SW_PAUSE_CHECK_BIT		166
#define	GET_SW_PAUSE_CHECK_BIT		167

//move_xx ��ɰ���
#define	SEND_MOVE_X_CMD				168
#define	CLEAR_AMC_ERROR				169

//��ɼ����� �߻��� �������� �����Ѵ�.
#define	GET_ERROR_STATUS			170

//fs���� ����� ������ �̿��Ѵ�.
#define	AMC_FS_CMD					171

#define		AMC_FILTER_SET			172
#define		AMC_FILTER_GET			173
#define		AMC_DBG_STATUS_GET		174
#define		AMC_TORQUE_LIMIT		175

#define		TRACEUPDATE70_1		178
#define		TRACEUPDATE70			179
#define		GETAMCDATA70			180

#define		RELOAD_SERVOPACK_POS	181
#define		GET_VEL_CURVE			182
#define		PUT_VEL_CURVE			183

#define		GET_ACTVEL_MARGIN		184
#define		PUT_ACTVEL_MARGIN		185
#define		AMC_DBG_STATUS_GET2		186




#if (defined(__AMC_SMD) || defined(__AMC_V70))	
#define		SMDDPRAMBASE	AMC_DBG_STATUS_GET2

#define		SETIOOUPUT				(SMDDPRAMBASE+1)	//187		
#define		GETIOOUPUT				(SMDDPRAMBASE+2)	//188
#define		SETIOCOUNT				(SMDDPRAMBASE+3)	//189 - OHT v7.0, SMD IN/OUTPUT ���� ��� �� ������ ���� �Լ�
#define		GETIOCOUNT				(SMDDPRAMBASE+4)	//190
#define		RELOAD_SERVOPACK_ALL	(SMDDPRAMBASE+5)	//191
#endif

#if defined(__AMC_V70)
#define		V70DPRAMBASE	RELOAD_SERVOPACK_ALL

#define		GET_SYSTEM_STATUS				(V70DPRAMBASE+1)		//192
#define		SET_SYSTEM_MONI_ENABLE			(V70DPRAMBASE+2)		//193
#define		SET_SYSTEM_MONI_DISABLE			(V70DPRAMBASE+3)		//194
#define		GET_SYSTEM_MONI_ENABLE			(V70DPRAMBASE+4)		//195
#define		GET_SYSTEM_MONI_VALUE			(V70DPRAMBASE+5)		//196
#define		SET_MONITERING_THRESHOLD_PERCENT	(V70DPRAMBASE+6)	//197
#define		GET_MONITERING_THRESHOLD_PERCENT	(V70DPRAMBASE+7)	//198
#endif

#define		DOAUTOPATCH				199
#define		GETVERSION				200
#define		GETECMVERSION			201

/*-----------------------------------------------------------------------------------------
 *   DPRAM memory map (offset 0x0000 ~ 0x03ff)
 *-----------------------------------------------------------------------------------------
 *
 *	0x0000~0x017f : Axis info memory
 *			0x00~0x2f : Axis[0]
 *			0x30~0x5f : Axis[1]
 *			0x60~0x8f : Axis[2]
 *			0x90~0xbf : Axis[3]
 *			0xc0~0x17f : (Reserved)
 *	0x0180~0x03fb : Common memory
 *  
 *  0x03f0 : FPGA�� ���ͷ�Ʈ �߻� ���� ������� �������̽��� ������ (0x5a or 0xa5)
 *  0x03f1 : FPGA�� ���ͷ�Ʈ �߻� ���� ������� ���ͷ�Ʈ ī��Ʈ ���� (�����: "0" �� ������ ��)
 *	0x03fc : Axis num (0~3)
 *	0x03fd : 0���� ������ ����˻� ��ɿ� ���� ������ �ִ��� Ȯ���ϴ°�
 *	0x03fe : wake DSP (interrupting DSP)
 *			: DSP�� �����ϱ⸦ ���ϴ� ��ɾ PC�� ���⿡ �� �ִ´�.
 *			: ���ͷ�Ʈ�� �߻������� �о�� 0xa5��,
 *			  �׷��� �������� 0x5a�� ��������.
 *				DSP���� �̹����� ������ DSP���ͷ�Ʈ�� �����ȴ�.
 *	0x03ff : Ack from DSP (interrupting PC)
 *			: 0x3fe�� �ִ� ����� �о �̰��� ��������μ� 
 *				PC�� ���ͷ�Ʈ�� �ɸ���.
 *				ISR���� 0x3ff�� �ѹ��� �о ���� ��Ų��.
 *		
*/
#define		DPRAM_COMM_BASEOFS	0x180

// DPRAM���� ������ �Ǵ� �����V�� �����Ѵ�.
#define		DPOFS_AXISINFO(a)	AXIS_BASEADDR((a)&0x03)
#define		DPOFS_COMM			0x0180
#define		DPOFS_INTACK		0x03f0
#define		DPOFS_INTCNT		0x03f1

#define		DPOFS_WDT_TIME		0x03f2
#define		DPOFS_WDT_STS		0x03f3
#define		DPOFS_AMPON_BASE	0x03f4
#define		DPOFS_WDT_ENABLE	0x03f8

#define		DPOFS_AXISNUM		0x03fc
#define		DPOFS_AXISCMD		0x03fd
#define		DPOFS_WAKEDSP		0x03fe
#define		DPOFS_ACK2DSP		0x03ff

#define		DPOFS_AI_2BDEXIST	0
#define		DPOFS_AI_1INPOS		2
#define		DPOFS_AI_1INSEQ		3
#define		DPOFS_AI_2AXSOURCE	4
#define		DPOFS_AI_2AXSTATE	6
#define		DPOFS_AI_2VEL		8
#define		DPOFS_AI_2ACTVEL	10
#define		DPOFS_AI_1FRAMEQ	12
#define		DPOFS_AI_1FRAMEI	13
#define		DPOFS_AI_1CHAR_0	14
#define		DPOFS_AI_1CHAR_1	15
#define		DPOFS_AI_2INTTYPE_0	16
#define		DPOFS_AI_2INTTYPE_1	18
#define		DPOFS_AI_2INTTYPE_2	20
#define		DPOFS_AI_2INTTYPE_3	22
#define		DPOFS_AI_2INTTYPE_4	24
#define		DPOFS_AI_4LONG		26
#define		DPOFS_AI_4FLOAT		30
#define		DPOFS_AI_4LONG_1	34
#define		DPOFS_AI_4FLOAT_1	38


#define		DPOFS_CO_VEL(a)			(4+((a)&0x03)*4)
#define		DPOFS_CO_ACC(a)			(36+((a)&0x03)*2)
#define		DPOFS_CO_AXIS(a)		(52+((a)&0x03)*2)
#define		DPOFS_CO_POS(a)			(68+((a)&0x03)*4)
#define		DPOFS_CO_ANALOG(a)		(132+((a)&0x03)*2)
#define		DPOFS_CO_DCC(a)			(148+((a)&0x03)*2)
#define		DPOFS_CO_SPLPOS(a,b)	(172+(((a)&0x03)*12)*b)

// DPRAM COMM Area offset Map /////////////////////////////////////////////////////////////////
#define		CD_Command		0 	/*  2 byte	int		*/
#define		CD_Len			2	/*  2 byte	int		*/
#define		CD_Vel			4	/* 32 byte	float[8]	*/
#define		CD_Acc			36	/* 16 byte	int[8]		*/
#define		CD_Axis			52	/* 16 byte	int[JPC_AXIS];	*/
#define		CD_Pos			68 	/* 32 byte	int[JPC_AXIS];*/
#define		CD_Ox			100	/*  4 byte	float		*/
#define		CD_Oy			104	/*  4 byte	float		*/
#define		CD_Angle		108	/*  4 byte	float		*/
#define		CD_L_Vel		112	/*  4 byte	float		*/
#define		CD_L_Acc		116	/*  2 byte	int		*/
#define		CD_CirDir		118     /*  2 byte 	dummy		*/
#define		CD_L_Deg		120	/*  4 byte	float		*/
#define		CD_Io_outValue		124	/*  4 byte	int 		*/
#define		CD_Io_inValue		128	/*  4 byte	int 		*/
#define		CD_AnalogChannel1	132	/*  2 byte  int			*/
#define		CD_AnalogChannel2	134	/*  2 byte  int					*/
#define		CD_AnalogChannel3	136	/*  2 byte  int					*/
#define		CD_AnalogChannel4	138	/*  2 byte	int					*/
#define		CD_Delay		140	/*	 4 byte	int					*/
#define		CD_AxisNum		144	/*	 2 byte	int  --145			*/
#define		CD_Ver			146	/*	 2 byte	int  --147			*/
#define		CD_Dcc			148	/* 16 byte	int[8] -- 163		*/
#define		CD_Temp_Acc		164	/*  2 byte  int					*/
#define		CD_Temp_Dcc		166	/*  2 byte  int					*/
#define		CD_Temp_Vel		168	/*  4 byte  int					*/
#define		CD_Spl_Pos		172	/* 16 byte	int[8] -- 163		*/

#if (defined(__AMC_SMD) || defined(__AMC_V70))	
#define		CD_Io_inValueR	472	/*  4 byte	int 		*/
#define		CD_Io_inValue1_1	476	/*  4 byte	int 		*/
#define		CD_Io_inValue2_1	480	/*  4 byte	int 		*/
#define		CD_Io_outValue3	484/*  4 byte	int 		*/
#define		CD_Io_inValue3	488	/*  4 byte	int 		*/
#define		CD_Io_outValue4	492	/*  4 byte	int 		*/
#define		CD_Io_inValue4	496	/*  4 byte	int 		*/
#define		CD_Io_outValue5	500	/*  4 byte	int 		*/
#define		CD_Io_inValue5	504	/*  4 byte	int 		*/
#define		CD_Io_outValue6	508	/*  4 byte	int 		*/
#define		CD_Io_inValue6	512	/*  4 byte	int 		*/
#define		CD_Io_outValue7	516	/*  4 byte	int 		*/
#define		CD_Io_inValue7	520	/*  4 byte	int 		*/
#define		CD_Io_outValue8	524	/*  4 byte	int 		*/
#define		CD_Io_inValue8	528	/*  4 byte	int 		*/
#endif

#define		CD_Io_outValue2	532	/*  4 byte	int 		*/
#define		CD_Io_inValue2	536	/*  4 byte	int 		*/

#if defined(__AMC_V70)
/*-----------------------------------------------------------
*	system monitering source
*---------------------------------------------------------*/
#define		POWER3P3	0
#define		POWER5P		1
#define		POWER12P	2	
#define		POWER12M	3
#define		AXIS0_VEL	4
#define		AXIS1_VEL	5
#define		AXIS2_VEL	6
#define		AXIS3_VEL	7


#define		PERCENT0	0	
#define		PERCENT100	100
#endif

/*-----------------------------------------------------------
*	SAVE_MAP_INFO CMD DEFINES
*---------------------------------------------------------*/
#define		C2DSPOFS_HDR_BASE_ADDR	0x240

// Long Block�� ������ ���� �Ʒ�ó�� �̿��Ѵ�.
#define		C2DSPOFS_TOTCRC4		0		// ��ü CRC32��
#define		C2DSPOFS_TOTLEN4		4		// ������ �� ���� ����Ʈ��
#define		C2DSPOFS_BLKLEN2		8		// ������ ���� ���� (256����Ʈ ����)
#define		C2DSPOFS_BLKID2			10		// Body�� 256B�� ����� �� 1�� �����. ó������ 0.
#define		C2DSPOFS_SAVEADDR4		12
#define		C2DSPOFS_BODY_OFS		16
#define		C2DSPOFS_BODY_BASE_ADDR	0x250

// ��Ʈ���� �ְ���� ���� �Ʒ�ó�� �̿�ȴ�.
// ó�� 6����Ʈ�� ����̰�, ������ 250����Ʈ�� ��Ʈ���� ����Ǵ� ��ü��.
#define		C2DSP_STRING_LEN		0
#define		C2DSP_STRING_CHKSUM		1
#define		C2DSP_STRING_RESERVED	2
#define		C2DSP_STRING_ACKLEN		3
#define		C2DSP_STRING_ACKCHKSUM	4
#define		C2DSP_STRING_ACKERRNO	5	// 0�̸� ���� ����.
#define		C2DSP_STRING_BODY		6	// 250����Ʈ�� �̿��

#define		ALIVE_CHK_ADDR1		(0x23e)
#define		ALIVE_CHK_ADDR2		(0x23f)
#define		ALIVE_CHK_BYTE1		(0x9A)
#define		ALIVE_CHK_BYTE2		(ALIVE_CHK_BYTE1^0xff)




/*-----------------------------------------------------------
*	Q_BUFFER COMMAND DEFINES
*---------------------------------------------------------*/
#define	AX_T_MOVE			1	/* Trapezoid motion single axis	*/
#define	AX_S_MOVE			2	/* S Curve motion single axis */
#define	AX_R_MOVE			3	/* Relative motion single axis */
#define	AX_RS_MOVE			4	/* Relative motion single axis */
#define	AX_P_MOVE			5	/* Parabolic motion single axis	*/
#define	AX_DELAY			6	/* Delay */
#define	AX_IO_TRIGGER		7	/* I/O Trigger Bit */

#define	CIRCLE_MOVE			11
#define	MAIN_INTERPOLATION	12

/*-----------------------------------------------------------
*	Limit Command Define
*---------------------------------------------------------*/
#define	PUT_P_SW_LIMIT		101
#define	PUT_N_SW_LIMIT		102
#define	GET_P_SW_LIMIT		103
#define	GET_N_SW_LIMIT		104

/*-----------------------------------------------------------
*	Trajectory Define
*	Profile Define, ckyu
*---------------------------------------------------------*/
#define	TRAPEZOID		1
#define	S_CURVE			2
#define	T_RELATIVE		3	// Trapezoidal Relative
#define	S_RELATIVE		4	// S-curve Relative
#define	PARABOLIC		5

#define	VELOCITY		20


/*-----------------------------------------------------------
*	Limit Sensor
*---------------------------------------------------------*/
#define	NEG_SWITCH		1
#define	HOME_SWITCH		2
#define	POS_SWITCH		3

#define	AMP_FAULT		1

/*-----------------------------------------------------------
*	Boot Frame Save or None
*---------------------------------------------------------*/
#define	BOOT_SAVE		1
#define	TEMPORARY		0


#ifdef __cplusplus
extern "C" {
#endif

#pragma	pack(2)
typedef struct _CommDPRAMType {
		UINT2    	Command;      		/*	0	-	1	*/
		UINT2   	Len;               	/*	2	-	3	*/
		FLOAT		Vel[BD_AXIS_NUM]; 	/*	4	-	35	*/
		INT2    	Acc[BD_AXIS_NUM]; 	/*	36	-	51	*/
		INT2    	Axis[BD_AXIS_NUM]; 	/*	52	-	67	*/
		INT4   	Pos[BD_AXIS_NUM];  	/*	68	-	99	*/
		FLOAT   Ox;          		/*	100	-	103	*/
		FLOAT   Oy;              	/*	104	-	107	*/
		FLOAT   Angle;             	/*	108	-	111	*/
		FLOAT   L_Vel;      		/*	112	-	115	*/
		INT2		L_Acc;   			/*	116	-	117	*/
		INT2		Cir_Dir;    		/*	118	-	119	*/
		FLOAT   L_Deg;            	/*	120	-	123	*/

		INT4	Io_outValue;    	/*	124	-	127	*/
		INT4	Io_inValue;       	/*	128	-	131	*/
		INT2		AnalogChannel[4];  	/*	132	-	139	*/
		INT4	Delay;				/*	140	-	143	*/
		INT2		Axis_Num;      		/*	144	-	145	*/
		INT2		Ver;				/*	146	-	147	*/
		INT2		Dcc[BD_AXIS_NUM]; 	/*	148	-	163	*/
		INT2		Temp_Acc;         	/*	164	-	165	*/
		INT2		Temp_Dcc;           /*	166	-	167	*/
		FLOAT	Temp_Vel;         	/*	168	-	171	*/
#if defined(__AMC_29x)	
		FLOAT	Spl_Pos[30][3];		/*	172	-  531	*/
#endif

#if (defined(__AMC_SMD) || defined(__AMC_V70))
		FLOAT	Spl_Pos[25][3];		/*	172	-  483	*/  

		INT4		IoinValueR;			/* 472 ~ 475 */			// //120831 2.9.8 SYK io�߰��� ���� ����.
		INT4		IoinValue1_1;		/* 476 ~ 479 */
		INT4		IoinValue2_1;		/* 480 ~ 483 */

		INT4		Io_outValue3;		/* 484 ~ 497 */			// 120818 2.9.8  syk io�߰��� ����
		INT4		IoinValue3;			/* 488 ~ 491 */
		INT4		Io_outValue4;		/* 492 ~ 495 */
		INT4		IoinValue4;			/* 496 ~ 499 */
		INT4		Io_outValue5;		/* 500 ~ 503 */
		INT4		IoinValue5;			/* 504 ~ 507 */								
		INT4		Io_outValue6;		/* 508 ~ 511 */
		INT4		IoinValue6;			/* 512 ~ 515 */
		INT4		Io_outValue7;		/* 516 ~ 519 */
		INT4		IoinValue7;			/* 520 ~ 523 */
		INT4		Io_outValue8;		/* 524 ~ 527 */
		INT4		IoinValue8;			/* 528 ~ 531 */
#endif
		INT4	Io_outValue2;    	/*	532	-	535	*/
		INT4	Io_inValue2;       	/*	536	-	539	*/
} COMM_DPRAM_TYPE;					/*	0x300 -  space	*/

typedef struct _AxisDpramType {
	INT2    	Board_Exist;    		/*	0 	-	1 	*/
	CHAR	In_Pos_Flag; 			/*	2 	-	 	*/
	CHAR	In_Sequence;			/*	3  - 	  	*/
	INT2		AxisSource;				/*	4	-	5	*/
	INT2		AxisState;				/*	6 	-	7 	*/
	INT2		Vel;           			/*	8 	-	9 	*/
	INT2		Actual_Vel;				/*	10	-	11	*/
	CHAR	Frames_Q;				/*	12	-	  	*/
	CHAR	Frames_Interpolation;	/*	13	-	  	*/
	CHAR	Char_Type[2];			/*	14	-	15	*/
	INT2		Int_Type[5];			/*	16	-	25	*/
	INT4	Long_Type;  			/*	26	-	29	*/
	FLOAT	Float_Type;				/*	30	-	33	*/
	INT4	Long_Type1;  			/*	34	-	37	*/	// com RPS, 2007.12.18
	FLOAT	Float_Type1;			/*	38	-	41	*/	// act RPS, 2007.12.18
} AXIS_DPRAM_TYPE;  				/*	42	-	48	*/





// �Ʒ��� ����ü�� ������ �߰��ϴ� ����.
// 1. �Ʒ��� BOOT_FRAME_TYPE����ü�� CHAR, INT, FLOAT���� ������ �߰��Ѵ�.
// 2. DSP �ҽ��� ��Ʈ �Ķ���� ����ü�� ���Ͽ뵵�� ���� �߰�
// 3. DSP �ҽ��� �Ķ���� ����ü�� ���Ͽ뵵�� ���� �߰�
// 4. psave.c�� �ִ� ����ü sBootParamDesc[]�� �߰��� ������ �ش��ϴ� �ν��Ͻ� �߰�
// 5. psave.c�� Init_Boot_Frame()�� �߰��� ������ �ʱ�ȭ �κ� ����
// 6. �� ������.



//
// �� ����ü�� DSP���� ST_BOOTPARA ����ο� ������ ������ ũ��� ���߾��� ����, 2007.3.13, ckyu
// int Encoder_Offset[BD_AXIS_NUM]; ������ �ؿ� �߰���, 2007.4.13, ckyu
//
#pragma	pack(4)
typedef struct _BootFrameType {
		INT/*FLOAT*//*double*/ 	Vel_Limit[BD_AXIS_NUM];
		INT   	Accel_Limit[BD_AXIS_NUM];

		INT	  	PulseRatio[BD_AXIS_NUM];    	/* system gear ratio    */
		FLOAT/*double*/  	GearRatio[BD_AXIS_NUM];    	/* system gear ratio    */
		INT  	HwLower_Limit[BD_AXIS_NUM]; 	/* system lower limit   */
		INT  	HwUpper_Limit[BD_AXIS_NUM]; 	/* system upper limit   */
		INT   	SwLower_Limit[BD_AXIS_NUM]; 	/* system lower limit   */
		INT   	SwUpper_Limit[BD_AXIS_NUM]; 	/* system upper limit   */

		INT   	PGain[BD_AXIS_NUM];       		/* P gain    value      */
		INT   	IGain[BD_AXIS_NUM];        	/* I gain    value      */
		INT   	DGain[BD_AXIS_NUM];        	/* D gain    value      */
		INT   	FGain[BD_AXIS_NUM];        	/* F gain    value      */
		INT		ILimit[BD_AXIS_NUM];       	/* I_LIMIT   value      */

		INT 		VPgain[BD_AXIS_NUM];       	/* VELP gain    value   */
		INT  		VIgain[BD_AXIS_NUM];        	/* VELI gain    value   */
		INT  		VDgain[BD_AXIS_NUM];        	/* VELD gain    value   */
		INT  		VFgain[BD_AXIS_NUM];        	/* VELF gain    value   */
		INT  		VIlimit[BD_AXIS_NUM];     		/* VELI_LIMIT   value   */

		float		In_Position[BD_AXIS_NUM];
		INT 		Error_Limit[BD_AXIS_NUM];
		CHAR 		Motor_Type[BD_AXIS_NUM];

		CHAR		SwUpper_LimitSt[BD_AXIS_NUM];
		CHAR		SwLower_LimitSt[BD_AXIS_NUM];

		CHAR		Pos_Level[BD_AXIS_NUM];
		CHAR		Neg_Level[BD_AXIS_NUM];
		CHAR		Home_Level[BD_AXIS_NUM];
		CHAR		Amp_Level[BD_AXIS_NUM];
		CHAR		Amp_Reset_Level[BD_AXIS_NUM];

		CHAR		Pos_Limit_St[BD_AXIS_NUM];
		CHAR		Neg_Limit_St[BD_AXIS_NUM];
		CHAR		Home_Limit_St[BD_AXIS_NUM];
		CHAR		Error_Limit_St[BD_AXIS_NUM];

		CHAR		Encoder_Cfg[BD_AXIS_NUM];
		CHAR		Voltage_Cfg[BD_AXIS_NUM];
		CHAR		Home_Index[BD_AXIS_NUM];

		INT 		Stop_Rate[BD_AXIS_NUM];
		INT     E_Stop_Rate[BD_AXIS_NUM];

		CHAR		Control_Cfg[BD_AXIS_NUM];
		CHAR		Loop_Cfg[BD_AXIS_NUM];
		CHAR		Amp_OnLevel[BD_AXIS_NUM];

		INT 		Io_Int_Enable[BD_AXIS_NUM];
		CHAR		Int_Event_St[BD_AXIS_NUM];
		CHAR		Amp_Fault_Event[BD_AXIS_NUM];

		CHAR		PosImode[BD_AXIS_NUM];
		CHAR		VelImode[BD_AXIS_NUM];
		CHAR		PulseMode[BD_AXIS_NUM];

		//DSP���� ���߱� ���ؼ� �߰��Ǿ���.
		INT			Inpos_Level[BD_AXIS_NUM];

		INT			Dpram_Addr[BD_AXIS_NUM];
		INT  		Axis_Num;
		INT  		Action_Axis_Num;

		//DSP���� ���߱� ���ؼ� �߰��Ǿ���.
		INT			UserIO_BootMode;
		INT			UserIO_BootValue;

		//DSP���� ���߱� ���ؼ� �߰��Ǿ���.
		// offset variables
		INT			dac_bias[BD_AXIS_NUM];
		FLOAT		V_TrackingFactor[BD_AXIS_NUM];

		//���ڴ��� 0 ��ġ�� �����ϱ� ���� �뵵.
		int Encoder_Offset[BD_AXIS_NUM];

		//���ڴ��� ������ �����ϱ����� ����
		int Encoder_direction[BD_AXIS_NUM];

		//������ �Ͻ�������� ���� �Լ�.
		int Motor_Pause[BD_AXIS_NUM];
		int Motor_Pause_Level[BD_AXIS_NUM];	// Active High (default)
		int Motor_Pause_CheckBit[BD_AXIS_NUM];

	// ���Ͱ��� ����
	// DSP�� ���̺귯���� axisdef.h (ST_BOOTPARA)�� �߰��� ������.
	int		naPositionNotchFreq[BD_AXIS_NUM];	// 0�̸� ���͸� ����
	int		naPositionLPFFreq[BD_AXIS_NUM];	// 0�̸� ���͸� ����
	int		naVelocityNotchFreq[BD_AXIS_NUM];	// Torque ����, 0�̸� ���͸� ����
	int		naVelocityLPFFreq[BD_AXIS_NUM];	// Torque ����, 0�̸� ���͸� ����
	
    float ScurveSmoothingFactor[BD_AXIS_NUM];    
	
}BOOT_FRAME_TYPE;


// DSP�� sysparam �����͸� �ְ�ޱ� ���ؼ� ����� ����ü
typedef struct _UBOOTPARA
{
	int nMagic;
	int nBlockSize;
	char nBoardNo;
	char nAxisNo;
	char nVerMaj;
	char nVerMin;

	BOOT_FRAME_TYPE	st_boot;

	int nCRC32;
} UBOOTPARA, *LPUBOOTPARA;


typedef struct _MoveFrameType {
		FLOAT/*double*/	L_Vel;   						/* CP Motion Velocity 	*/
		INT   	L_Acc;    	               /* CP Motion Accel    	*/
		FLOAT/*double*/	L_Deg; 	                  /* ARC Motion Degrees  	*/
		FLOAT/*double*/  	Ox;
		FLOAT/*double*/  	Oy;
		FLOAT/*double*/  	Angle;
		FLOAT		Deg_Div;
		FLOAT/*double*/ 	Vel[TOTAL_AXIS_NUM];			/* joint speed data		*/
		INT		Len;
		INT    	Acc[TOTAL_AXIS_NUM];       /* acceleration data 	*/
		INT    	Dcc[TOTAL_AXIS_NUM];       /* acceleration data 	*/
		INT		Axis[TOTAL_AXIS_NUM];
		FLOAT/*double*/  	Pos[TOTAL_AXIS_NUM];
		INT		MapFunc;
		INT		Cir_Dir;
}MOVE_FRAME_TYPE;

typedef struct _MotionDisp {
		LONG		delay;
		double	pos[2];
		double	vel;
		INT		acc;
		INT		dcc;
		INT		traj;
		INT  		flag;
		INT  		axis;
		INT		cnt;
		INT		cordx[4];
		INT		cordy[4];
}MOTION_DISP;

typedef struct _SyncMotion {
		INT		Master;
		INT		Slave;
		INT  		Flag;
		FLOAT  		Gain;
}SYNC_MOTION;

extern	COMM_DPRAM_TYPE		*CommDpram[];
extern 	AXIS_DPRAM_TYPE		*AxisDpram[];
extern	BOOT_FRAME_TYPE		BootFrame[];
extern	MOVE_FRAME_TYPE		Mf;
extern	MOTION_DISP			Mdisp[];
extern	SYNC_MOTION			SyncMotion;

extern	CHAR				*AxisInfo[];
extern	CHAR				*Ack2Dsp[];
extern	CHAR				*Int2Dsp[];
extern	CHAR				*DpramExistChk[MMC_BOARD_NUM][2];
extern	unsigned LONG		Dpram_Addr[MMC_BOARD_NUM][BD_AXIS_NUM];

extern	INT					mmc_error;
extern	INT					MMC_Bd_Num;
extern	INT					Active_Axis_Num;
extern	INT					Power_Flag[];
extern	unsigned CHAR		ChkSum_Parity[];
extern	LONG				Virtual_Pos[];
extern	CHAR				*Error_Msg[];
extern	INT					Version_Info[];

//#elif OSTYPE == OS_WINDOWSNT
//	#include <winioctl.h>
//	#include "WinRTctl.h"		/* Driver tool header file */
//	#include "ioaccess.h"
//	#include "DDMapMem.h"

#define	MEMORY_LENGTH	0xF0

extern	HANDLE			hMutex_Axis_Comm;
extern	HANDLE			_hThread;
extern	HANDLE			hWinRT;		/* handle to WinRT driver */
extern	DWORD			iLength;	/* return length from Ioctl call */
extern	DWORD			_dwThreadId;
extern	DWORD			FreeAddr[];
extern	BOOL			fInit;
extern	BOOL			fLoadOk;

extern	DWORD			profile_do (LPVOID);
extern	DWORD			OpenWinRT (void);
extern	DWORD			CloseWinRT (void);
extern	DWORD			GetPhysicalAddr (unsigned long);
extern	DWORD			FreePhysicalAddrWinRT (unsigned long);

INT		MMCMutexLock (void);
INT		MMCMutexUnlock (void);
INT		CommWrite(INT,INT);
INT		CDIWrite(INT, INT, INT);
INT		CDIRead(INT, pINT, INT);
INT		CDLWrite(INT ,LONG, INT);
INT		CDLRead(INT ,pLONG, INT);
INT		CDFWrite(INT, FLOAT, INT);
INT		CDFRead(INT, pFLOAT, INT);
INT		CDFDWrite(INT ax, double pos, INT comm);
INT		CDI3Write(INT, LONG, INT, INT);
INT		CDI3Read(INT, pLONG, pINT, INT);
INT		MMCCommCheck(INT, pINT, INT, INT);
INT		Find_Bd_Jnt(INT, pINT, pINT);
INT		CDINoBootWrite(INT ,INT , INT);
INT		CDFNoBootWrite(INT ,FLOAT, INT);
INT		CDFDNoBootWrite(INT, double, INT);
INT		CDI3NoBootWrite(INT ,LONG, INT, INT);

//syk ������ ���͸�
int	read_dpram_int_filtering(INT2 *dpram_adrs, INT2 *dpram_data);	
int	read_dpram_char_filtering(unsigned char *dpram_adrs, unsigned char *dpram_data);
int	read_dpram_int4_filtering(INT4 *dpram_adrs, INT4 *dpram_data);
int	read_dpram_float_filtering(float *dpram_adrs, float *dpram_data);

#ifdef __cplusplus
}
#endif

#endif
