
#include "pcdef.h"
#include <stdio.h>
#include "amc_internal.h"
#include "log.h"

char AMC_WORKDIR[300] = "C:\\";

typedef struct _tag_COMMENT_DESC
{
	char *pNickName;
	char *pszComment;
} COMMENT_DESC;

COMMENT_DESC sComment[] = 
{
	{"VEL_LIMIT",	"# 최대동작 속도를 정하기 위함. 이기능은 v_move에서는 사용되지 않음. 기본 단위는 펄스/초 "},
	{"ACC_LIMIT",	"# 최대 가속 시간을 의미하며, 기본 단위는 1ms. 이 기능은 v_move에서는 사용되지 않음."},
	{"PULSE_RATIO",	"# 현재 사용하지 않음, 스탭모터를 구동할 때 출력할 펄스의 분주비를 설정할 때 사용하는 변수."},
	{"GEAR_RATIO",	"# 전자기어비 설정을 위해 사용하는 변수로 현재 1.0으로 공정되어 있음."},
	{"HWLOWER_LIMIT",	"# 사용하지 않음. 하드 리미트 센서와 Software Limit 사이에 설정하여 사용할 변수"},
	{"SWLOWER_LIMIT",	"# PTP Motion에서 프로파일을 생성하는 단계에서 이 영역을 벗어나면 Event 처리를 하기 위함."},
	{"SWUPPER_LIMIT_ST",	"# 해당 리미트 값을 벗어난 경우 처리할 Event를 설정하기 위함.\n# 0:NONE, 1:STOP, 2:ESTOP, 3:ABORT"},
	{"PGAIN",		"# 위치제어 PID 루프에서의 게인값, 아래 게인값은 Closed Loop인 경우이며, Open 루프인 경우 별도 게인을 사용해야 함."},
	{"VPGAIN",		"# 속도 제어 루프에서의 게인값, 제어 모드를 토오크 모드롤 설정한 경우에만 아래 게인값이 동작함.\n# Open 루프인 경우 별도 게인을 사용해야 함."},
	{"IN_POSITION",	"# PTP Motion에서 Motion 프로파일의 출력이 모두 끝난 상태에서 현재 위치값과 목표 위치값이 아래 값보다\n"	\
					"# 작은 경우 Position에 도작한 것을 알려줌. 속도나 토오크 모드(아날로그 출력)로 사용할 경우 서보 및\n"		\
					"# AMC 보드의 아날로그 Offset에 의해 위치 에러가 0으로 줄지 않는 경우가 있습니다.\n"						\
					"# 이 경우는 DAC Offset을 0V로 줄이기 위한 Offset 값을 초기화한 후 VIGAIN과 VILIMIT을 바꾸면서 튜닝하는 것이 필요함."},
	{"ERROR_LIMIT",	"# PTP Motion 명령어를 수행할 때 목표 펄스와 현재 모터의 위치 펄스값이 이 이상 차이가 나는 경우 에러 처리를 위함."},
	{"ERROR_LIMIT_ST",	"# 위 에러 리미트 조건이 된 경우 수행할 이벤트를 설정하기 위함."},
	{"POS_LEVEL",	"# Limit 센서의 동작 레벨. 0은 Active Low, 1은 Active High.\n"			\
					"# 포토커플러 입력 회로(Common Anode)로 구성되어 있으며, 포토커플러가 Off되면 High, 동작하면 Low가 입력됨."},
	{"POS_LEVEL_ST",	"# 위 리미트 센서가 동작할 때 동작할 Event"},
	{"AMP_LEVEL",	"# Amp Fault 레벨을 지정하기 위한 것으로 Amp 케이블이 빠진 경우도 Fault로 인식됨."},
	{"AMP_FAULE_EVENT",	"# Amp Fault 발생시 사용할 Event 설정"},
	{"AMP_RESET_LEVEL",	"# Amp 에러 발생시 리셋을 위한 레벨 설정"},

	{"MOTOR_TYPE",	"# 0:servo, 1:stepper, 2:micro_stepper"},
	{"ENCODER_CFG",	"# 피드백 장치를 설정하기 위한 값으로 항상 0, 일반 인코더\n# always 0 for encoder"},
	{"VOLTAGE_CFG",	"# 아날로그 출력 전압의 극성을 선택하기 위함. Bipolar는 -10V ~ +10V, Unipolar은 0 ~ +10V (+/-12V 전원을 사용한 경우)\n"	\
					"# 0:bipolar, 1:unipolar\n"		\
					"# 0:bipolar, 1:unipolar"},
	{"HOME_INDEX",	"# 인코더를 사용하여 원점을 찾는 경우 정확한 원점을 찾기 위해 모터의 Z상을 E-Stop 이벤트로 처리하기 위한 설정으로\n"	\
					"# 현재 사용하지 않음."},
	{"STOP_RATE",	"# 각종 Event 처리시 감속 기울기를 나타내며, 단위는 ms임."},

	{"CONTROL_CFG",	"# 서보 드라이버의 제어 모드를 설정하기 위한 것으로 0:velocity, 1:torque"},
	{"LOOP_CFG",	"# 제어 루프를 결정하기 위한 변수, 0:open loop, 1:close loop"},
	{"AMP_ON_LEVEL",	"#서보 드라이버의 동작 레벨을 지정하기 위함."},
	{"IO_INT_ENABLE",	"# Input 포트를 통해 인터럽트를 사용하기 위한 설정으로 현재 기능 없음."},
	{"PULSE_MODE",	"# 위치형 서보나 스탭 모터 드라이브를 사용할 때 펄스 출력 모드를 설정하기 위한 값"},
	{"INPOS_LEVEL",	"# 서보에서 입력되는 In Position 신호의 동작 레벨, 이 신호는 위치형 서보인 경우만 출력되는 신호임.\n"	\
					"# 따라서 현재 속도형이나 토오크형에서는 사용하지 않고, AMC 내부에서 Software적으로 In Position을 체크함."},
	{"DAC_BIAS"		"# AMC 보드 내부의 아날로그 회로의 Offset을 0V로 만들기 위해 사용하는 설정값으로,\n"		\
					"# 출하시 제공된 값을 이용하여 서보 드라이버와 튜닝을 하여 최적의 값을 구한다.\n"			\
					"# Open Loop로 제어 모드를 설정한 후 IGAIN, DGAIN을 0으로 하여 모터가 회전하지 않는\n"		\
					"# 최적의 값을 찾아 사용한다."},
	{"VTRACKING_FACTOR",	"# Mobile Robot의 경우 두 바퀴의 속도차를 이 변수를 이용하여 가변시킬 수 있다."},
	{"ENCODER_OFFSET",		"# 기구적인 원점을 만들기 위해 사용함. 모터의 위치를 읽으면 모터의 실제 위치에서 이 값을 뺀 값이 리턴됨. "},
	{"ENCODER_DIR",		"# 모터의 회전 방향과 로봇의 회전 방향의 부호가 달라 회전 방향을 변경시킬 경우 사용하는 기능\n"	\
						"# 모터 드라이버는 변경하지 않고, AMC 보드에서만 수정하면 됨.\n"		\
						"# 이 변수를 수정한 후에는 반드시 DSP는 리부팅을 시켜야 함.\n"			\
						"# ccw=0, cw=1"},
	{"POS_IMODE",	"# 0:only standing, 1:always"},
	{"VEL_IMODE",	"# I게인을 전구간 또는 정지 제어 상태에서 사용할 것인지를 설정, 0:only standing, 1:always"},
	{"USERIO_BOOTMODE",	"# NOT USED ON this AMC. 0:input, 1:output"},
	{"USERIO_BOOTVALUE",	"# initial user output value only for lower 32bits"},


	// 필터링관련 변수를 설명
	{"POS_NOTCH_FREQ",	"# 위치제어용 노치필터 주파수 설정, 0이면 기능 정지"},
	{"POS_LPF_FREQ",	"# 위치제어용 노치필터 주파수 설정, 0이면 기능 정지"},
	{"VEL_NOTCH_FREQ",	"# 토크제어용 노치필터 주파수 설정, 0이면 기능 정지"},
	{"VEL_LPF_FREQ",	"# 토크제어용 노치필터 주파수 설정, 0이면 기능 정지"},

	{"S_CURVE_FACTOR",	"# S_CURVE_FACTOR range = ( 0 ~ 0.5 ), 0-->Trapezoid profile"},


	{NULL, ""}
};


typedef struct _tag_BOOTPARAM_DESC
{
	// 모든 파라미터는 DWORD형태로 저장한다.
	// 다음은 변수의 순서에 따른 설명임.
	// 1 : 파일에 저장될째 기록하는 이름
	// 2 : 저장되는 값의 타입을 나타냄, 1:char(1B), 2:short(2B), 4:int/long/float(4B), 5:UINT
	// 3 : 몇개의 아이템이 저장되는지를 나타남.
	//
	// 아래의 형태로 저장된다
	// "PGAIN = 4,4,00AB3276,00000001,0101ABCD,ABEFDEAD
	char *pNickName;
	int nType;
	int nItems;	// ','로 구분되어있는 값들의 총 갯수.
} BOOTPARAM_DESC;

BOOTPARAM_DESC sBootParamDesc[] =
{
	{"VEL_LIMIT",	4, BD_AXIS_NUM},	//0
	{"ACC_LIMIT",	4, BD_AXIS_NUM},

	{"PULSE_RATIO",	4, BD_AXIS_NUM},
	{"GEAR_RATIO", 4, BD_AXIS_NUM},
	{"HWLOWER_LIMIT", 3, BD_AXIS_NUM},
	{"HWUPPER_LIMIT", 3, BD_AXIS_NUM},	//5
	{"SWLOWER_LIMIT", 3, BD_AXIS_NUM},
	{"SWUPPER_LIMIT", 3, BD_AXIS_NUM},

	{"PGAIN", 4, BD_AXIS_NUM},
	{"IGAIN", 4, BD_AXIS_NUM},
	{"DGAIN", 4, BD_AXIS_NUM},			//10
	{"FGAIN", 4, BD_AXIS_NUM},
	{"ILIMIT", 4, BD_AXIS_NUM},

	{"VPGAIN", 4, BD_AXIS_NUM},
	{"VIGAIN", 4, BD_AXIS_NUM},
	{"VDGAIN", 4, BD_AXIS_NUM},			//15
	{"VFGAIN", 4, BD_AXIS_NUM},
	{"VILIMIT", 4, BD_AXIS_NUM},

	{"IN_POSITION", 4, BD_AXIS_NUM},
	{"ERROR_LIMIT", 4, BD_AXIS_NUM},
	{"MOTOR_TYPE", 1, BD_AXIS_NUM},		//20

	{"SWUPPER_LIMIT_ST", 1, BD_AXIS_NUM},
	{"SWLOWER_LIMIT_ST", 1, BD_AXIS_NUM},

	{"POS_LEVEL", 1, BD_AXIS_NUM},
	{"NEG_LEVEL", 1, BD_AXIS_NUM},
	{"HOME_LEVEL", 1, BD_AXIS_NUM},		//25
	{"AMP_LEVEL", 1, BD_AXIS_NUM},
	{"AMP_RESET_LEVEL", 1, BD_AXIS_NUM},

	{"POS_LEVEL_ST", 1, BD_AXIS_NUM},
	{"NEG_LEVEL_ST", 1, BD_AXIS_NUM},
	{"HOME_LEVEL_ST", 1, BD_AXIS_NUM},		//30
	{"ERROR_LIMIT_ST", 1, BD_AXIS_NUM},

	{"ENCODER_CFG", 1, BD_AXIS_NUM},
	{"VOLTAGE_CFG", 1, BD_AXIS_NUM},
	{"HOME_INDEX", 1, BD_AXIS_NUM},

	{"STOP_RATE", 5, BD_AXIS_NUM},		//35
	{"ESTOP_RATE", 5, BD_AXIS_NUM},

	{"CONTROL_CFG", 1, BD_AXIS_NUM},
	{"LOOP_CFG", 1, BD_AXIS_NUM},
	{"AMP_ON_LEVEL", 1, BD_AXIS_NUM},

	{"IO_INT_ENABLE", 4, BD_AXIS_NUM},	//40
	{"INT_EVENT_ST", 1, BD_AXIS_NUM},
	{"AMP_FAULT_ST", 1, BD_AXIS_NUM},		// 2008.1.14, _EVENT에서 _ST로 변경

	{"POS_IMODE", 1, BD_AXIS_NUM},
	{"VEL_IMODE", 1, BD_AXIS_NUM},
	{"PULSE_MODE", 1, BD_AXIS_NUM},		//45

	{"INPOS_LEVEL", 4, BD_AXIS_NUM},

	{"DPRAM_ADDR", 4, BD_AXIS_NUM},
	{"AXIS_NUM", 2, 1},
	{"ACTION_AXIS_NUM", 2, 1},

	{"USERIO_BOOTMODE", 2, 1},			//50
	{"USERIO_BOOTVALUE", 2, 1},

	{"DAC_BIAS", 4, BD_AXIS_NUM},
	{"VTRACKING_FACTOR", 4, BD_AXIS_NUM},

	{"ENCODER_OFFSET", 4, BD_AXIS_NUM},	//54

	// 2007.10.10, ckyu
	{"ENCODER_DIR", 1, BD_AXIS_NUM},	// 55
	
	// 2007.10.10, ckyu
	{"MOTOR_PAUSE", 1, BD_AXIS_NUM},			// 56
	{"MOTOR_PAUSE_LEVEL", 1, BD_AXIS_NUM},		// 57
	{"MOTOR_PAUSE_CHECKBIT", 1, BD_AXIS_NUM},	// 58


	// 필터링관련 INI 설정, 2008.3.5, ckyu
	{"POS_NOTCH_FREQ",	4, 1},		// int, one item //59
	{"POS_LPF_FREQ",	4, 1},		// int, one item //60
	{"VEL_NOTCH_FREQ",	4, 1},		// int, one item //61
	{"VEL_LPF_FREQ",	4, 1},		// int, one item //62

	{"S_CURVE_FACTOR",	4, 1},		// 
        

	// 구조체의 끝임을 나타낸다.
	{NULL, 0, 0}
};


int GetVariableStringAndIndex(char *pszstr, float *pfval);
void SetParamValue(INT *pPtr, char *pszLine);

double GetBootFrameValue(int nIndex, int nAx)
{
	if (nIndex == 0)
	{
		if (BootFrame[0].Vel_Limit [nAx] < 1) BootFrame[0].Vel_Limit [nAx] = 1;
		else if (BootFrame[0].Vel_Limit [nAx] > MMC_VEL_LIMIT) BootFrame[0].Vel_Limit [nAx] = MMC_VEL_LIMIT;
		return (double) BootFrame[0].Vel_Limit [nAx];
	}
	else if (nIndex == 1)
	{
		if (BootFrame[0].Accel_Limit [nAx] < 1) BootFrame[0].Accel_Limit [nAx] = 1;
		else if (BootFrame[0].Accel_Limit [nAx] > MMC_ACCEL_LIMIT) BootFrame[0].Accel_Limit [nAx] = MMC_ACCEL_LIMIT;
		return (double) BootFrame[0].Accel_Limit [nAx];
	}
	else if (nIndex == 2) return (double) BootFrame[0].PulseRatio [nAx];
	else if (nIndex == 3) return (double) BootFrame[0].GearRatio [nAx];
	
    else if (nIndex == 4) return (double)BootFrame[0].HwLower_Limit [nAx];	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	else if (nIndex == 5) return (double)BootFrame[0].HwUpper_Limit [nAx];	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	else if (nIndex == 6) return (double)BootFrame[0].SwLower_Limit [nAx];	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	else if (nIndex == 7) return (double)BootFrame[0].SwUpper_Limit [nAx];	//2.8.5, 2011.12.22 syk int aa= int2 bb;
	
    else if (nIndex == 8) return (double) BootFrame[0].PGain [nAx];
	else if (nIndex == 9) return (double) BootFrame[0].IGain [nAx];
	else if (nIndex == 10) return (double) BootFrame[0].DGain [nAx];
	else if (nIndex == 11) return (double) BootFrame[0].FGain [nAx];
	else if (nIndex == 12) return (double) BootFrame[0].ILimit [nAx];
	else if (nIndex == 13) return (double) BootFrame[0].VPgain [nAx];
	else if (nIndex == 14) return (double) BootFrame[0].VIgain [nAx];
	else if (nIndex == 15) return (double) BootFrame[0].VDgain [nAx];
	else if (nIndex == 16) return (double) BootFrame[0].VFgain [nAx];

    else if (nIndex == 17) return (double) BootFrame[0].VIlimit [nAx];
	else if (nIndex == 18) return (double) BootFrame[0].In_Position [nAx];
	else if (nIndex == 19) return (double) BootFrame[0].Error_Limit [nAx];
	else if (nIndex == 20) return (double) BootFrame[0].Motor_Type [nAx];

    else if (nIndex == 21) return (double) BootFrame[0].SwUpper_LimitSt [nAx];
	else if (nIndex == 22) return (double) BootFrame[0].SwLower_LimitSt [nAx];

    else if (nIndex == 23) return (double) BootFrame[0].Pos_Level [nAx];
	else if (nIndex == 24) return (double) BootFrame[0].Neg_Level [nAx];
	else if (nIndex == 25) return (double) BootFrame[0].Home_Level [nAx];
	else if (nIndex == 26) return (double) BootFrame[0].Amp_Level [nAx];
	else if (nIndex == 27) return (double) BootFrame[0].Amp_Reset_Level [nAx];

	else if (nIndex == 28) return (double) BootFrame[0].Pos_Limit_St [nAx];
	else if (nIndex == 29) return (double) BootFrame[0].Neg_Limit_St [nAx];
	else if (nIndex == 30) return (double) BootFrame[0].Home_Limit_St [nAx];
	else if (nIndex == 31) return (double) BootFrame[0].Error_Limit_St [nAx];

	else if (nIndex == 32) return (double) BootFrame[0].Encoder_Cfg [nAx];
	else if (nIndex == 33) return (double) BootFrame[0].Voltage_Cfg [nAx];
	else if (nIndex == 34) return (double) BootFrame[0].Home_Index [nAx];
	else if (nIndex == 35) return (double) BootFrame[0].Stop_Rate [nAx];
	else if (nIndex == 36) return (double) BootFrame[0].E_Stop_Rate [nAx];
	else if (nIndex == 37) return (double) BootFrame[0].Control_Cfg [nAx];
	else if (nIndex == 38) return (double) BootFrame[0].Loop_Cfg [nAx];
	else if (nIndex == 39) return (double) BootFrame[0].Amp_OnLevel [nAx];
	else if (nIndex == 40) return (double) BootFrame[0].Io_Int_Enable [nAx];
	else if (nIndex == 41) return (double) BootFrame[0].Int_Event_St [nAx];
	else if (nIndex == 42) return (double) BootFrame[0].Amp_Fault_Event [nAx];
	else if (nIndex == 43) return (double) BootFrame[0].PosImode [nAx];
	else if (nIndex == 44) return (double) BootFrame[0].VelImode [nAx];
	else if (nIndex == 45) return (double) BootFrame[0].PulseMode [nAx];
	else if (nIndex == 46) return (double) BootFrame[0].Inpos_Level [nAx];
	else if (nIndex == 47) return (double) BootFrame[0].Dpram_Addr [nAx];
	else if (nIndex == 48) return (double) BootFrame[0].Axis_Num;
	else if (nIndex == 49) return (double) BootFrame[0].Action_Axis_Num;
	else if (nIndex == 50) return (double) BootFrame[0].UserIO_BootMode;
	else if (nIndex == 51) return (double) BootFrame[0].UserIO_BootValue;
	else if (nIndex == 52) return (double) BootFrame[0].dac_bias [nAx];
	else if (nIndex == 53) return (double) BootFrame[0].V_TrackingFactor [nAx];
	else if (nIndex == 54) return (double) BootFrame[0].Encoder_Offset [nAx];
	else if (nIndex == 55) return (double) BootFrame[0].Encoder_direction [nAx];
	else if (nIndex == 56) return (double) BootFrame[0].Motor_Pause [nAx];
	else if (nIndex == 57) return (double) BootFrame[0].Motor_Pause_Level[nAx];
	else if (nIndex == 58) return (double) BootFrame[0].Motor_Pause_CheckBit[nAx];

	//  필터링관련 변수룰 INI에/로 설정
	else if (nIndex == 59) return (double) BootFrame[0].naPositionNotchFreq[nAx];
	else if (nIndex == 60) return (double) BootFrame[0].naPositionLPFFreq[nAx];
	else if (nIndex == 61) return (double) BootFrame[0].naVelocityNotchFreq[nAx];
	else if (nIndex == 62) return (double) BootFrame[0].naVelocityLPFFreq[nAx];
	
	// S_CURVE
	else if (nIndex == 63) return (double) BootFrame[0].ScurveSmoothingFactor[nAx];

	return 0.;
}

void SetBootFrameValue(int nIndex, int nAx, float fVal, int iVal)
{
	BootFrame[0].Dpram_Addr [nAx] = 0;
	BootFrame[0].Axis_Num = 4;
	BootFrame[0].Action_Axis_Num = 4;

	if (nIndex == 0) 
	{
		if (iVal < 1) iVal = 1;
		else if (iVal > MMC_VEL_LIMIT) iVal = MMC_VEL_LIMIT;
		BootFrame[0].Vel_Limit [nAx] = iVal;
	}
	else if (nIndex == 1) 
	{
		if (iVal < 1) iVal = 1;
		else if (iVal > MMC_ACCEL_LIMIT) iVal = MMC_ACCEL_LIMIT;
		BootFrame[0].Accel_Limit [nAx] = iVal;
	}
	else if (nIndex == 2) BootFrame[0].PulseRatio [nAx] = iVal;
	else if (nIndex == 3) BootFrame[0].GearRatio [nAx] = fVal;
	else if (nIndex == 4) BootFrame[0].HwLower_Limit [nAx] = iVal;
	else if (nIndex == 5) BootFrame[0].HwUpper_Limit [nAx] = iVal;
	else if (nIndex == 6) BootFrame[0].SwLower_Limit [nAx] = iVal;
	else if (nIndex == 7) BootFrame[0].SwUpper_Limit [nAx] = iVal;
	else if (nIndex == 8) BootFrame[0].PGain [nAx] = (int)fVal;
	else if (nIndex == 9) BootFrame[0].IGain [nAx] = (int)fVal;
	else if (nIndex == 10) BootFrame[0].DGain [nAx] = (int)fVal;
	else if (nIndex == 11) BootFrame[0].FGain [nAx] = (int)fVal;
	else if (nIndex == 12) BootFrame[0].ILimit [nAx] = (int)fVal;
	else if (nIndex == 13) BootFrame[0].VPgain [nAx] = (int)fVal;
	else if (nIndex == 14) BootFrame[0].VIgain [nAx] = (int)fVal;
	else if (nIndex == 15) BootFrame[0].VDgain [nAx] = (int)fVal;
	else if (nIndex == 16) BootFrame[0].VFgain [nAx] = (int)fVal;
	else if (nIndex == 17) BootFrame[0].VIlimit [nAx] = (int)fVal;

	else if (nIndex == 18) BootFrame[0].In_Position [nAx] = fVal;
	else if (nIndex == 19) BootFrame[0].Error_Limit [nAx] = (int)fVal;
	else if (nIndex == 20) BootFrame[0].Motor_Type [nAx] = (char)fVal;

	else if (nIndex == 21) BootFrame[0].SwUpper_LimitSt [nAx] = (char)fVal;
	else if (nIndex == 22) BootFrame[0].SwLower_LimitSt [nAx] = (char)fVal;

	else if (nIndex == 23) BootFrame[0].Pos_Level [nAx] = (char)fVal;
	else if (nIndex == 24) BootFrame[0].Neg_Level [nAx] = (char)fVal;
	else if (nIndex == 25) BootFrame[0].Home_Level [nAx] = (char)fVal;
	else if (nIndex == 26) BootFrame[0].Amp_Level [nAx] = (char)fVal;
	else if (nIndex == 27) BootFrame[0].Amp_Reset_Level [nAx] = (char)fVal;

	else if (nIndex == 28) BootFrame[0].Pos_Limit_St [nAx] = (char)fVal;
	else if (nIndex == 29) BootFrame[0].Neg_Limit_St [nAx] = (char)fVal;
	else if (nIndex == 30) BootFrame[0].Home_Limit_St [nAx] = (char)fVal;
	else if (nIndex == 31) BootFrame[0].Error_Limit_St [nAx] = (char)fVal;

	else if (nIndex == 32) BootFrame[0].Encoder_Cfg [nAx] = (char)fVal;
	else if (nIndex == 33) BootFrame[0].Voltage_Cfg [nAx] = (char)fVal;
	else if (nIndex == 34) BootFrame[0].Home_Index [nAx] = (char)fVal;
	else if (nIndex == 35) BootFrame[0].Stop_Rate [nAx] = (unsigned int)fVal;
	else if (nIndex == 36) BootFrame[0].E_Stop_Rate [nAx] = (unsigned int)fVal;

	else if (nIndex == 37) BootFrame[0].Control_Cfg [nAx] = (char)fVal;
	else if (nIndex == 38) BootFrame[0].Loop_Cfg [nAx] = (char)fVal;
	else if (nIndex == 39) BootFrame[0].Amp_OnLevel [nAx] = (char)fVal;
	else if (nIndex == 40) BootFrame[0].Io_Int_Enable [nAx] = (int)fVal;
	else if (nIndex == 41) BootFrame[0].Int_Event_St [nAx] = (char)fVal;
	else if (nIndex == 42) BootFrame[0].Amp_Fault_Event [nAx] = (char)fVal;

	else if (nIndex == 43) BootFrame[0].PosImode [nAx] = (char)fVal;
	else if (nIndex == 44) BootFrame[0].VelImode [nAx] = (char)fVal;
	else if (nIndex == 45) BootFrame[0].PulseMode [nAx] = (char)fVal;
	else if (nIndex == 46) BootFrame[0].Inpos_Level [nAx] = (int)fVal;
	else if (nIndex == 47) BootFrame[0].Dpram_Addr [nAx] = (int)fVal;
	else if (nIndex == 48) BootFrame[0].Axis_Num = (int)fVal;
	else if (nIndex == 49) BootFrame[0].Action_Axis_Num = (int)fVal;
	else if (nIndex == 50) BootFrame[0].UserIO_BootMode = (int)fVal;

	else if (nIndex == 51) BootFrame[0].UserIO_BootValue = (int)fVal;
	else if (nIndex == 52) BootFrame[0].dac_bias [nAx] = (int)fVal;
	else if (nIndex == 53) BootFrame[0].V_TrackingFactor [nAx] = fVal;
	else if (nIndex == 54) BootFrame[0].Encoder_Offset [nAx] = (int)fVal;
	else if (nIndex == 55) BootFrame[0].Encoder_direction [nAx] = (int)fVal;
	else if (nIndex == 56) BootFrame[0].Motor_Pause [nAx] = (int)fVal;
	else if (nIndex == 57) BootFrame[0].Motor_Pause_Level [nAx] = (int)fVal;
	else if (nIndex == 58) BootFrame[0].Motor_Pause_CheckBit [nAx] = (int)fVal;

	//  필터링관련 변수룰 INI에/로 설정
	else if (nIndex == 59) BootFrame[0].naPositionNotchFreq[nAx] = iVal;
	else if (nIndex == 60) BootFrame[0].naPositionLPFFreq[nAx]	 = iVal;
	else if (nIndex == 61) BootFrame[0].naVelocityNotchFreq[nAx] = iVal;
	else if (nIndex == 62) BootFrame[0].naVelocityLPFFreq[nAx]	 = iVal;
	
	// S_CURVE
	else if (nIndex == 63) BootFrame[0].ScurveSmoothingFactor[nAx]	 = fVal;
	else;
}


INT	BootFrameRead(BOOL *pbParamExist)
{
	int nIndex = 0, i;
	FILE *fp;
	char strFile[300];

    MYLOG("BootFrameRead\n");

	sprintf(strFile, "%s%s", AMC_WORKDIR, AMC_PARAM_FILE);
	if ((fp = fopen(strFile, "rt")) != NULL)
	{
		fclose(fp);
		*pbParamExist = TRUE;

		for (i = 0; i < BD_AXIS_NUM; i ++)
		{
			float fVal;
			int iVal;
			char szKey[50], szStr[40];
			
			nIndex = 0;

			sprintf(szKey, "AXIS_%d", i + 1);

			while (sBootParamDesc[nIndex].pNickName != NULL)
			{
				DWORD dwrtn;
				dwrtn = GetPrivateProfileString(szKey, sBootParamDesc[nIndex].pNickName, NULL, szStr, 30, strFile);
				if (dwrtn)
				{
					fVal = (float)atof(szStr);
					iVal = atoi(szStr);
					SetBootFrameValue(nIndex, i, fVal, iVal);
				}
                else
                {
                    nIndex += 0;
                }
				nIndex ++;
			}
		}
	} 
    else 
    {
		*pbParamExist = FALSE;
		return MMC_BOOTPARAM_NOT_EXIST;
	}

	mmc_error = MMC_OK;
	return	(MMC_OK);
}

int GetVariableStringAndIndex(char *pszstr, float *pfVal)
{
	char szSave[100];
	BOOTPARAM_DESC *pBParam = &sBootParamDesc[0];
	int i = 0;

	sscanf(pszstr, "%s", szSave);
	while (pBParam->pNickName != NULL)
	{
		if (strcmp(szSave, sBootParamDesc[i].pNickName) == 0)
		{
			char *ptr = strchr(pszstr, '=');
			if (ptr == NULL) return -1;
			sscanf(ptr + 1, "%f", pfVal);
			return i;
		}
		pBParam ++;
		i ++;
	}
	return -1;
}

void SetParamValue(INT *pPtr, char *pszLine)
{
	int nType, nItems, nValue;
	int i, nOfs;
	char *ptr = strchr(pszLine, '=');
	if (ptr == NULL) return ;

	// 처음 숫자(Type)를 찾는다.
	sscanf(ptr+1, "%d", &nType);
	ptr = strchr(ptr+1, ',');
	if (ptr == NULL) return;

	// 두번째 숫자(아이템갯수)를 찾는다.
	sscanf(ptr+1,"%d", &nItems);
	ptr = strchr(ptr+1, ',');
	if (ptr == NULL) return;

	// 배열의 인덱스를 지정하는 값을 저장한다.
	nOfs = 0;

	// 포인터를 값들이 시작하는 위치에 옮겨 놓는다.
	for (i = 0; i < nItems; i ++)
	{
		sscanf(ptr+1,"%X", &nValue);
		pPtr[nOfs++] = nValue;
		ptr = strchr(ptr+1, ',');
		if (ptr == NULL) break;
	}
}
/**********
*	FUNCTION NAME	: BootFrameStore(VOID)
*	FUNCTION       : Store Boot Memory
*********************************************************************/
INT	BootFrameStore(VOID)
{
	INT	i;
	INT err;

	for(i=0; i<MMC_BOARD_NUM; i++)
	{
		if(mmc_error = err = BootFrameStoreBd(i, NULL))	return	err;
	}
	mmc_error = MMC_OK;
	return	MMC_OK;
}


void PutCommentIfNecessary(char *pszNickName, FILE *fp)
{
	COMMENT_DESC *pComment = &sComment[0];
	while (pComment->pNickName != NULL)
	{
		if (strcmp(pComment->pNickName, pszNickName) == 0)
		{
			fprintf(fp, "\n%s\n", pComment->pszComment);
		}
		pComment ++;
	}
}


BOOL IsCommentAlready(int ax, char *pszVarName)
{
	char szKey[100];
	char str[300], szrtn[30];
	DWORD dwrtn;

	sprintf(szKey, "AXIS_%d", ax + 1);
	sprintf(str, "%s%s", AMC_WORKDIR, AMC_PARAM_FILE);
	dwrtn = GetPrivateProfileString(szKey, pszVarName, NULL, szrtn, 30, str);
	if (dwrtn == 0) return TRUE;
	return FALSE;
}

/**********
*	FUNCTION NAME	: BootFrameStoreBd(INT bd_num)
*	FUNCTION       : Store Boot Memory by one_bd
*********************************************************************/
INT		BootFrameStoreBd (INT bn, char *pszFile)
{
	BOOTPARAM_DESC *pBParam = &sBootParamDesc[0];
	int nIndex = 0, i;
	FILE *fp;
	char str[200];
	double fVal;

	if (pszFile != NULL)
		sprintf(str, "%s", pszFile);
	else 
		sprintf(str, "%s%s", AMC_WORKDIR, AMC_DEF_PARAM_FILE);
	fp = fopen(str, "wt");
	if (fp) 
	{
		fprintf(fp, "[BOOTPARAM_BD_0]\n");	// 
		for (i = 0; i < BD_AXIS_NUM; i ++)
		{
			nIndex = 0;
			fprintf(fp, "\n[AXIS_%d]\n", i + 1);
			while (sBootParamDesc[nIndex].pNickName != NULL && fp != NULL)
			{
				fVal = GetBootFrameValue(nIndex, i);

				// 주석이 필요한 변수의 경우 변수를 넣어 놓는다.
				PutCommentIfNecessary(sBootParamDesc[nIndex].pNickName, fp);

				// 주석처리할 변수의 경우를 처리한다.
				if (strcmp(sBootParamDesc[nIndex].pNickName, "DAC_BIAS") == 0)
					sprintf(str,"#%s = ", sBootParamDesc[nIndex].pNickName);
				else if (strcmp(sBootParamDesc[nIndex].pNickName, "DPRAM_ADDR") == 0)
					sprintf(str,"#%s = ", sBootParamDesc[nIndex].pNickName);
				else if (strcmp(sBootParamDesc[nIndex].pNickName, "AXIS_NUM") == 0)
					sprintf(str,"#%s = ", sBootParamDesc[nIndex].pNickName);
				else if (strcmp(sBootParamDesc[nIndex].pNickName, "ACTION_AXIS_NUM") == 0)
					sprintf(str,"#%s = ", sBootParamDesc[nIndex].pNickName);
				else if (IsCommentAlready(i, sBootParamDesc[nIndex].pNickName) == TRUE)
					sprintf(str,"#%s = ", sBootParamDesc[nIndex].pNickName);
				else
					sprintf(str,"%s = ", sBootParamDesc[nIndex].pNickName);

				if (sBootParamDesc[nIndex].nType == 1)
					sprintf(str + strlen(str), "%d", (int) fVal);
				else if (sBootParamDesc[nIndex].nType == 2)
					sprintf(str + strlen(str), "%d", (short) fVal);
				else if (sBootParamDesc[nIndex].nType == 4)
					sprintf(str + strlen(str), "%f", fVal);
				else if (sBootParamDesc[nIndex].nType == 5)
					sprintf(str + strlen(str), "%d", (UINT) fVal);
				else 
				{
					int iVal = (int)fVal;
					sprintf(str + strlen(str), "%d", iVal);
				}

				strcat(str, "\n");
				fprintf(fp, str); 

				nIndex ++;
			}
		}
		fclose(fp);
	}
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: Init_Boot_Frame(INT bd_num)
*	FUNCTION       : Initialize Boot Memory
*********************************************************************/
INT	Init_Boot_Frame(INT bd_num)
{
	INT	i;

	for(i=0; i<BD_AXIS_NUM; i++)
	{
		BootFrame[bd_num].Vel_Limit[i]=MMC_VEL_LIMIT;
		BootFrame[bd_num].Accel_Limit[i]=MMC_ACCEL_LIMIT;

		BootFrame[bd_num].PulseRatio[i]=8;  	      			/* system Gear Ratio	   */
		BootFrame[bd_num].GearRatio[i]=1.0;  	      			/* system Gear Ratio	   */
		BootFrame[bd_num].HwLower_Limit[i]= MMC_NEG_SW_LIMIT;	/* system lower limit   *///2011.10.8, warning
		BootFrame[bd_num].HwUpper_Limit[i]= MMC_POS_SW_LIMIT; /* system upper limit   *///2011.10.8, warning
		BootFrame[bd_num].SwLower_Limit[i]= 10*8192;	/* system lower limit   */
		BootFrame[bd_num].SwUpper_Limit[i]= -10*8192;	/* system upper limit   */
		
		if (i < 2)
		{
			BootFrame[bd_num].PGain[i]=12000;       		/* P gain    value      */
			BootFrame[bd_num].IGain[i]=0;    	    	/* I gain    value      */
			BootFrame[bd_num].DGain[i]=0;       	 	/* D gain    value      */
			BootFrame[bd_num].FGain[i]=0;        		/* F gain    value      */
			BootFrame[bd_num].ILimit[i]=1000;       	/* I_LIMIT   value      */

			BootFrame[bd_num].VPgain[i]=0;       	/* VELP gain    value   */
			BootFrame[bd_num].VIgain[i]=0;        	/* VELI gain    value   */
			BootFrame[bd_num].VDgain[i]=0;        		/* VELD gain    value   */
			BootFrame[bd_num].VFgain[i]=0;        		/* VELF gain    value   */
			BootFrame[bd_num].VIlimit[i]=1000;     	/* VELI_LIMIT   value   */
		} else {
			BootFrame[bd_num].PGain[i]=1200;       		/* P gain    value      */
			BootFrame[bd_num].IGain[i]=20;    	    	/* I gain    value      */
			BootFrame[bd_num].DGain[i]=0;       	 	/* D gain    value      */
			BootFrame[bd_num].FGain[i]=0;        		/* F gain    value      */
			BootFrame[bd_num].ILimit[i]=1000;       	/* I_LIMIT   value      */

			BootFrame[bd_num].VPgain[i]=1000;       	/* VELP gain    value   */
			BootFrame[bd_num].VIgain[i]=10;        	/* VELI gain    value   */
			BootFrame[bd_num].VDgain[i]=0;        		/* VELD gain    value   */
			BootFrame[bd_num].VFgain[i]=0;        		/* VELF gain    value   */
			BootFrame[bd_num].VIlimit[i]=3276800;     	/* VELI_LIMIT   value   */
		}
		BootFrame[bd_num].In_Position[i]=1000.0;	//(FLOAT)100.0;
		BootFrame[bd_num].Error_Limit[i]=MMC_ERROR_LIMIT;
		BootFrame[bd_num].Motor_Type[i]=SERVO_MOTOR;	//MICRO_STEPPER;			/* Servo (0), Stepper(1)	*/

		BootFrame[bd_num].Pos_Level[i]=LOW;
		BootFrame[bd_num].Neg_Level[i]=LOW;
		BootFrame[bd_num].Home_Level[i]=LOW;
		BootFrame[bd_num].Amp_Level[i]=HIGH;
		BootFrame[bd_num].Amp_Reset_Level[i]=LOW;
		BootFrame[bd_num].Amp_OnLevel[i]=LOW;

		BootFrame[bd_num].Pos_Limit_St[i]=NO_EVENT;
		BootFrame[bd_num].Neg_Limit_St[i]=NO_EVENT;

		BootFrame[bd_num].Home_Limit_St[i]=NO_EVENT;
		BootFrame[bd_num].Error_Limit_St[i]=ABORT_EVENT;
		BootFrame[bd_num].Amp_Fault_Event[i]=ABORT_EVENT;

		BootFrame[bd_num].SwLower_LimitSt[i]=ABORT_EVENT;
		BootFrame[bd_num].SwUpper_LimitSt[i]=ABORT_EVENT;

		BootFrame[bd_num].Encoder_Cfg[i]=0;
		BootFrame[bd_num].Voltage_Cfg[i]=0;
		BootFrame[bd_num].Home_Index[i]=0;

		BootFrame[bd_num].Stop_Rate[i]=10;
		BootFrame[bd_num].E_Stop_Rate[i]=10;


		BootFrame[bd_num].Control_Cfg[i]=0;		// 1;			/* Torque(1),Velocity(0)	*/
		BootFrame[bd_num].Loop_Cfg[i]=0; 			/* Closed(1),Open_Loop(0)	*/

		BootFrame[bd_num].Io_Int_Enable[i]=0;
		BootFrame[bd_num].Int_Event_St[i]=NO_EVENT;

		BootFrame[bd_num].PosImode[i]=IN_ALWAYS;
		BootFrame[bd_num].VelImode[i]=IN_ALWAYS;
		BootFrame[bd_num].PulseMode[i]=TWO_PULSE;

		BootFrame[bd_num].Dpram_Addr[i]=(/*0xA8000000 + 0x1000000*bd_num+ */0x30*i);

		BootFrame[bd_num].V_TrackingFactor[i] = 1.;
		BootFrame[bd_num].Encoder_Offset[i] = 0;

	}
	return	MMC_OK;//2011.10.8, warning
}

/**********
*	FUNCTION NAME	: ChkSumStore(INT bn)
*	FUNCTION       : Store Boot Memory ChkSum
*********************************************************************/
INT	ChkSumStore(INT bn)
{
	FILE 	*fp;
	CHAR	name[20];

	sprintf(name, "%sCHKSUM%d.DAT", AMC_WORKDIR, bn+1);
	fp = fopen(name, FBINWRITE);

	if (!fp) 
	{
		mmc_error = MMC_CHKSUM_OPEN_ERROR;
		return	MMC_CHKSUM_OPEN_ERROR;
	}
	fwrite(&ChkSum_Parity[bn], 1, 1, fp);
	fclose(fp);

	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: ChkSumReStore(VOID)
*	FUNCTION       : Store Boot Memory ChkSum
*********************************************************************/
INT	ChkSumReStore(VOID)
{
	FILE 	*fp;
	CHAR	name[20];
	INT		i;
	INT err;

	for(i=0; i<MMC_BOARD_NUM; i++) {
		sprintf(name, "%sCHKSUM%d.DAT", AMC_WORKDIR, i+1);
		if((fp = fopen(name, FBINREAD))==NULL)
		{
			if(mmc_error = err = InitChkSum(i))	return	err;
		}
		fread(&ChkSum_Parity[i], 1, 1, fp);
		fclose(fp);
	}

	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: InitChkSum(INT bn)
*	FUNCTION       : initialize Memory ChkSum
*********************************************************************/
INT	InitChkSum(INT bn)
{
	FILE 	*fp;
	CHAR	name[20];

	sprintf(name, "%sCHKSUM%d.DAT", AMC_WORKDIR, bn+1);
	ChkSum_Parity[bn]=0xff;
	fp = fopen(name, FBINWRITE);

	if (!fp) 
	{
		mmc_error = MMC_CHKSUM_OPEN_ERROR;
		return	MMC_CHKSUM_OPEN_ERROR;
	}
	fwrite(&ChkSum_Parity[bn], 1, 1, fp);
	fclose(fp);

	return	MMC_OK;
}
