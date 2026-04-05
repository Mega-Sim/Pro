#ifndef		__AMC_FILTER_H
#define		__AMC_FILTER_H


#include "amc_type.h"



#ifdef AMCLIB_EXPORTS
#define AMCLIB_API __declspec(dllexport)
#else
#define AMCLIB_API __declspec(dllimport)
#endif








#ifdef		__cplusplus
extern "C" {
#endif



AMCLIB_API int set_position_lpf(int ax, int nfreq);
AMCLIB_API int get_position_lpf(int ax, int *pnfreq);

AMCLIB_API int set_velocity_lpf(int ax, int nfreq);
AMCLIB_API int get_velocity_lpf(int ax, int *pnfreq);


AMCLIB_API int set_position_notch_filter(int ax, int nfreq);
AMCLIB_API int get_position_notch_filter(int ax, int *pnfreq);

AMCLIB_API int set_velocity_notch_filter(int ax, int nfreq);
AMCLIB_API int get_velocity_notch_filter(int ax, int *pnfreq);

#ifdef		__cplusplus
}
#endif



#endif

