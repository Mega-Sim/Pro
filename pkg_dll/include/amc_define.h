#ifndef __AMC_DEFIND_H__
#define __AMC_DEFIND_H__

//OHT 버젼 에 따른 컴파일 선택 
//#define		__AMC_29x
//#define		__AMC_SMD
#define		__AMC_V70
#define		__AMC_V8x


#if (defined(__AMC_29x) && !defined(__AMC_SMD) && !defined(__AMC_V70))
#elif (!defined(__AMC_29x) && defined(__AMC_SMD) && !defined(__AMC_V70))
#elif (!defined(__AMC_29x) && !defined(__AMC_SMD) && defined(__AMC_V70))
#else
[Define 중복 error]
#endif

typedef int AMCBOOL;
#define AMCTRUE		1
#define AMCFALSE	0

/* Errors */
#define AMC_SUCCESS			0
#define AMC_NOTOPENED		-1
#define AMC_TIMEOUT			-10
#define AMC_INVALID_CMD		-11
#define AMC_INVALID_DATA	-12

#endif

