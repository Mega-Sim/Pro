package com.samsung.ocs.common.message;

import java.util.Vector;

/**
 * MsgVector Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2011. 6. 21.
 * @version 3.0
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class MsgVector extends Vector {
	/**
	 * 
	 * @param index
	 * @return
	 */
	public boolean toBool(int index) {
		Boolean B;
		B = (Boolean)super.elementAt(index);
		return B.booleanValue();
	}

	/**
	 * 
	 * @param index
	 * @return
	 */
	public int toInt(int index) {
		Integer I;
		I = (Integer)super.elementAt(index);
		return I.intValue();
	}

	/**
	 * 
	 * @param index
	 * @return
	 */
	public double toDouble(int index) {
		Double D;
		D = (Double)super.elementAt(index);
		return D.doubleValue();
	}

	/**
	 * 
	 * @param index
	 * @return
	 */
	public String toString(int index) {
		String S;
		S = (String)super.elementAt(index);
		return S;
	}

	/**
	 * 
	 * @param strData
	 * @param strFormat
	 * @return
	 */
	public boolean scanf(String strData, String strFormat) {
		String S = new String("");
		int idxS = 0; // 포맷 문자열 내에서 처리되는 위치(시작)
		int idxE = 0; // 포맷 문자열 내에서 처리되는 위치(끝)
		int idxD = 0; // 데이터 문자열 내에서 처리되는 위치(시작)
		int idxV = 0; // MsgVector 내에서 처리되는 인덱스
		int size = strFormat.length();
		String oneS = "";

		// 100 bytes blank line
		String blankLine = "                                                                                                    ";

		super.clear(); // 변수 사용하기 전에 초기화
		while (idxS < size) {
			oneS = strFormat.substring(idxS, idxS + 1);
			if (oneS.compareTo("%") == 0) { // 특수 문자인 경우

				int scanlength = 0;
				String strLength = "";
				String tempStr = "";
				// 숫자값은 그대로 버퍼에 저장
				do {
					idxS++;
					oneS = strFormat.substring(idxS, idxS + 1);
				} while ( (oneS.compareTo("+") == 0) || (oneS.compareTo("-") == 0));
				// +,-문자가 들어가는 경우에 대비해, 이렇게 처리할 것!!

				while ( (oneS.compareTo("0") >= 0) && (oneS.compareTo("9") <= 0)) {
					strLength = strLength + oneS;
					idxS++;
					oneS = strFormat.substring(idxS, idxS + 1);
				}
				if (strLength.length() > 0) {
					scanlength = Integer.parseInt(strLength);
				} else {
					System.out.println("ERROR: scan length is not defined.");
					return false;
				}

				// 처리 타입
				if (oneS.compareTo("d") == 0) { // int 타입(decimal)
					Integer nValue = new Integer(Integer.parseInt(strData.substring(idxD,
							idxD + scanlength).trim()));
					super.add(nValue);
				} else if (oneS.compareTo("f") == 0) { // float 타입
					Double dValue = new Double(Double.parseDouble(strData.substring(idxD,
							idxD + scanlength).trim()));
					super.add(dValue);
				} else if (oneS.compareTo("s") == 0) { // string 타입
					String sValue = new String(strData.substring(idxD,
							idxD + scanlength)).trim();
					super.add(sValue);
				} else if (oneS.compareTo("b") == 0) { // Boolean 타입
					String strBoolean = strData.substring(idxD, idxD + scanlength).trim().
					toUpperCase();
					Boolean bValue;
					if (strBoolean.equals("T") == true) {
						bValue = new Boolean(true);
					} else {
						bValue = new Boolean(false);
					}
					super.add(bValue);
				} else {
					System.out.println("ERROR: invalid type: [" + oneS + "].");
					return false;
				}

				// 처리한 부분만큼 index 뒤로 밀기
				idxD = idxD + scanlength;

			} else { // 특수문자가 아니면 그대로 출력
				//S = S + oneS;
				if (oneS.compareTo(strData.substring(idxD, idxD + 1)) != 0) {
					// 주어진 포맷과 데이터가 다르다면, 에러를 발생시킨다.
					System.out.println("ERROR: string format mismatch!!!");
					return false;
				}
				idxD++; // 한칸 진행하기
			}
			idxS++; // 항상 한칸씩 진행한다.
		}

		return true;

	}

	/**
	 * 
	 * @param strFormat
	 * @return
	 */
	public String printf(String strFormat) {
		String S = new String("");
		int idxS = 0; // 문자열 내에서 처리되는 위치
		int idxV = 0; // MsgVector 내에서 처리되는 위치
		int size = strFormat.length();
		String oneS = "";
		String blankLine = "                                                                                                    ";
		// 100 bytes blank line
		//String strArrTemp[] = null ;

		while (idxS < size) {
			oneS = strFormat.substring(idxS, idxS + 1);
			if (oneS.compareTo("%") == 0) {
				// format 처리 시작
				idxS++;
				int printlength = 0;
				boolean leftalign = true; // 왼쪽 정렬 (기본값)
				boolean blnFullZero = false; // 숫자 앞에 0을 채움
				String strLength = "";
				String tempStr = "";
				// 숫자값은 그대로 버퍼에 저장
				oneS = strFormat.substring(idxS, idxS + 1);

				// +,-등으로 기록시 정렬 방법 정의
				if ((oneS.compareTo("+") == 0) || (oneS.compareTo("-") == 0)) {
					if (oneS.compareTo("-") == 0) {
						// 오른쪽 정렬
						leftalign = false;
					}
					idxS++;
					oneS = strFormat.substring(idxS, idxS + 1);
				}

				// 맨 앞자리 숫자가 0이라면, 앞의 모든 자리는 0으로 채운다.
				if (oneS.compareTo("0") == 0) {
					leftalign = false;
					blnFullZero = true;
					idxS++;
					oneS = strFormat.substring(idxS, idxS + 1);
				}

				//
				while ((oneS.compareTo("0") >= 0) && (oneS.compareTo("9") <= 0)) {
					strLength = strLength + oneS;
					idxS++;
					oneS = strFormat.substring(idxS, idxS + 1);
				}
				if (strLength.length() > 0) {
					printlength = Integer.parseInt(strLength);
				}

				/////////////////////////////////
				// 소수점 아래 자릿수 처리
				int nPrecision = -1;
				if (oneS.compareTo(".") == 0) {
					nPrecision = 0;
					idxS++;
					oneS = strFormat.substring(idxS, idxS + 1);

					// nPrecision 찾음
					String strPrecision = "";
					while ((oneS.compareTo("0") >= 0) && (oneS.compareTo("9") <= 0)) {
						strPrecision = strPrecision + oneS;
						idxS++;
						oneS = strFormat.substring(idxS, idxS + 1);
					}
					if (strPrecision.length() > 0) {
						nPrecision = Integer.parseInt(strPrecision);
						if ((printlength > 0) && (nPrecision + 2) > printlength) { // 0.xxxx --> 4+2=6자리임
							System.out.println("ERROR: (nPrecision value("
									+ nPrecision +
									") must be 2 more than printlength value("
									+ printlength + ").\nINPUTTED FORMAT: " +
									strFormat
									+ "\nCURRENT RESULT: " + S);
							return "";
						}
					}
				}

				/////////////////////////////////
				// 처리 타입
				if (oneS.compareTo("d") == 0) { // int 타입(decimal)
					tempStr = String.valueOf(super.elementAt(idxV++));
					if (printlength > 0 && tempStr.length() > printlength) {
						System.out.println("ERROR: Integer(" + tempStr +
								")'s number length is longer than assigned length("
								+ printlength + ").\nINPUTTED FORMAT: " +
								strFormat
								+ "\nCURRENT RESULT: " + S);
						return "";
					}
				} else if (oneS.compareTo("f") == 0) { // float 타입
					tempStr = String.valueOf(super.elementAt(idxV++));
					int nIntLength = tempStr.indexOf(".");
					if ((printlength > 0) && (nIntLength > printlength)) {
						System.out.println("ERROR: Float(" + tempStr
								+
								")'s integer part length is longer than assigned length("
								+ printlength + ").\nINPUTTED FORMAT: " +
								strFormat
								+ "\nCURRENT RESULT: " + S);
						return "";
					}

					// 소숫점 아래 부분이 기본적으로 6자리가 되도록 설정
					if (nIntLength > 0) { // 소수점이 있다면,
						tempStr = tempStr + "000000";
					} else { // 소수점이 없다면,
						tempStr = tempStr + ".000000";
					}

					String[] sNumberPart = tempStr.split("\\.");
					if (nPrecision > -1) { // precision이 정의되어 있다면,
						if (nPrecision == 0) { // precision = 0이라면 정수부분만 출력
							tempStr = sNumberPart[0];
						} else { // precision 만큼 출력
							tempStr = sNumberPart[0] + "." +
							sNumberPart[1].substring(0, nPrecision);
						}
					} else {
						tempStr = sNumberPart[0] + "." + sNumberPart[1].substring(0, 6);
					}
				} else if (oneS.compareTo("s") == 0) { // string 타입
					tempStr = (String)super.elementAt(idxV++);
					if (tempStr == null)
						tempStr = "";
				} else if (oneS.compareTo("b") == 0) { // Boolean 타입
					if (super.elementAt(idxV++).equals(new Boolean(true)) == true) {
						tempStr = "T";
					} else {
						tempStr = "F";
					}
					//          tempStr = String.valueOf(super.elementAt(idxV++));
				} else if (oneS.compareTo("D") == 0) { // int 타입(decimal)
					tempStr = String.valueOf(super.elementAt(idxV++));
					// raw byte로 바꾸어 저장할 것.
				} else if (oneS.compareTo("F") == 0) { // float 타입
					tempStr = String.valueOf(super.elementAt(idxV++));
					// raw byte로 바꾸어 저장할 것.
				} else {
					// 잘못된 스트링이 있을때.
					System.out.println(
							"ERROR: cannot convert inputted string by using strFormat, invalid character: " +
							oneS
							+ "\nINPUTTED FORMAT: " + strFormat + "\nCURRENT RESULT: " + S);
					return "";
				}

				// 공백 추가
				if (printlength > 0) {
					if (tempStr.length() > printlength) { // 문자열이 더 길게 생성되었다면, 남는 길이만큼 잘라낸다.
						tempStr = tempStr.substring(0, printlength);
					}
					while (tempStr.length() < printlength) {
						if (leftalign == true) {
							tempStr = tempStr + " "; // 공백을 뒤에 추가
						} else if ((leftalign == false) && (blnFullZero == true)) {
							tempStr = "0" + tempStr; // "0"을 앞에 추가
						} else {
							tempStr = " " + tempStr; // 공백을 앞에 추가
						}
					}
				}
				S = S + tempStr;

			} else { // 특수문자가 아니면 그대로 출력
				S = S + oneS;
			}
			idxS++;
		}
		return S;
	}
}
