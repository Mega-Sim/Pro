#ifndef		__CRC32_h
#define		__CRC32_h


/* generate the crc table. Must be called before calculating the crc value */
extern void _crc32_gen_table(void);
extern unsigned long _crc32_get_crc(char *p, int nlen);   /* calculate the crc32 value */
/*-----------------------------------------------------------*/


#endif
