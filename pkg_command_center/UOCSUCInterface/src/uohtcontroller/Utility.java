package uohtcontroller;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 */

class MsgVector
    extends Vector {
  public boolean toBool(int index) {
    Boolean B;
    B = (Boolean)super.elementAt(index);
    return B.booleanValue();
  }

  public int toInt(int index) {
    Integer I;
    I = (Integer)super.elementAt(index);
    return I.intValue();
  }

  public double toDouble(int index) {
    Double D;
    D = (Double)super.elementAt(index);
    return D.doubleValue();
  }

  public String toString(int index) {
    String S;
    S = (String)super.elementAt(index);
    return S;
  }

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
        }
        while ( (oneS.compareTo("+") == 0) || (oneS.compareTo("-") == 0));
        // +,-문자가 들어가는 경우에 대비해, 이렇게 처리할 것!!

        while ( (oneS.compareTo("0") >= 0) && (oneS.compareTo("9") <= 0)) {
          strLength = strLength + oneS;
          idxS++;
          oneS = strFormat.substring(idxS, idxS + 1);
        }
        if (strLength.length() > 0) {
          scanlength = Integer.parseInt(strLength);
        }
        else {
          System.out.println("ERROR: scan length is not defined.");
          return false;
        }

        // 처리 타입
        if (oneS.compareTo("d") == 0) { // int 타입(decimal)
          Integer nValue = new Integer(Integer.parseInt(strData.substring(idxD,
              idxD + scanlength).trim()));
          super.add(nValue);
        }
        else if (oneS.compareTo("f") == 0) { // float 타입
          Double dValue = new Double(Double.parseDouble(strData.substring(idxD,
              idxD + scanlength).trim()));
          super.add(dValue);
        }
        else if (oneS.compareTo("s") == 0) { // string 타입
          String sValue = new String(strData.substring(idxD,
              idxD + scanlength)).trim();
          super.add(sValue);
        }
        else if (oneS.compareTo("b") == 0) { // Boolean 타입
          String strBoolean = strData.substring(idxD, idxD + scanlength).trim().
              toUpperCase();
          Boolean bValue;
          if (strBoolean.equals("T") == true) {
            bValue = new Boolean(true);
          }
          else {
            bValue = new Boolean(false);
          }
          super.add(bValue);
        }
        else {
          System.out.println("ERROR: invalid type: [" + oneS + "].");
          return false;
        }

        // 처리한 부분만큼 index 뒤로 밀기
        idxD = idxD + scanlength;

      }
      else { // 특수문자가 아니면 그대로 출력
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
        if ( (oneS.compareTo("+") == 0) || (oneS.compareTo("-") == 0)) {
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
        while ( (oneS.compareTo("0") >= 0) && (oneS.compareTo("9") <= 0)) {
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
          while ( (oneS.compareTo("0") >= 0) && (oneS.compareTo("9") <= 0)) {
            strPrecision = strPrecision + oneS;
            idxS++;
            oneS = strFormat.substring(idxS, idxS + 1);
          }
          if (strPrecision.length() > 0) {
            nPrecision = Integer.parseInt(strPrecision);
            if ( (printlength > 0) && (nPrecision + 2) > printlength) { // 0.xxxx --> 4+2=6자리임
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
        }
        else if (oneS.compareTo("f") == 0) { // float 타입
          tempStr = String.valueOf(super.elementAt(idxV++));
          int nIntLength = tempStr.indexOf(".");
          if ( (printlength > 0) && (nIntLength > printlength)) {
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
          }
          else { // 소수점이 없다면,
            tempStr = tempStr + ".000000";
          }

          String sNumberPart[] = tempStr.split("\\.");
          if (nPrecision > -1) { // precision이 정의되어 있다면,
            if (nPrecision == 0) { // precision = 0이라면 정수부분만 출력
              tempStr = sNumberPart[0];
            }
            else { // precision 만큼 출력
              tempStr = sNumberPart[0] + "." +
                  sNumberPart[1].substring(0, nPrecision);
            }
          }
          else {
            tempStr = sNumberPart[0] + "." + sNumberPart[1].substring(0, 6);
          }
        }
        else if (oneS.compareTo("s") == 0) { // string 타입
          tempStr = (String)super.elementAt(idxV++);
          if (tempStr == null) {
            tempStr = "";
          }
        }
        else if (oneS.compareTo("b") == 0) { // Boolean 타입
          if (super.elementAt(idxV++).equals(new Boolean(true)) == true) {
            tempStr = "T";
          }
          else {
            tempStr = "F";
          }
//          tempStr = String.valueOf(super.elementAt(idxV++));
        }
        else if (oneS.compareTo("D") == 0) { // int 타입(decimal)
          tempStr = String.valueOf(super.elementAt(idxV++));
          // raw byte로 바꾸어 저장할 것.
        }
        else if (oneS.compareTo("F") == 0) { // float 타입
          tempStr = String.valueOf(super.elementAt(idxV++));
          // raw byte로 바꾸어 저장할 것.
        }
        else {
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
            }
            else if ( (leftalign == false) && (blnFullZero == true)) {
              tempStr = "0" + tempStr; // "0"을 앞에 추가
            }
            else {
              tempStr = " " + tempStr; // 공백을 앞에 추가
            }
          }
        }
        S = S + tempStr;

      }
      else { // 특수문자가 아니면 그대로 출력
        S = S + oneS;
      }
      idxS++;
    }
    return S;
  }
}

class MyHashtable
    extends Hashtable {
  public boolean toBool(String key, int index) {
    String s, sClassName;
    boolean b;

    Object o = super.get(key);
    if (o != null) {
      sClassName = o.getClass().getName();
      if (sClassName.indexOf("Vector") > -1) {
        Vector v = (Vector)super.get(key);
        if (v.elementAt(index).getClass().getName().indexOf("String") > -1) {
          s = (String) v.elementAt(index);
        }
        else {
          s = v.elementAt(index).toString();
        }

      }
      else {
        s = super.get(key).toString();
      }
    }
    else {
      s = "";
    }
    b = new Boolean(s).booleanValue();
    return b;
  }

  public int toInt(String key, int index) {
    String s, sClassName;
    int i;

    Object o = super.get(key);
    if (o != null) {
      sClassName = o.getClass().getName();
      if (sClassName.indexOf("Vector") > -1) {
        Vector v = (Vector)super.get(key);
        if (v.elementAt(index).getClass().getName().indexOf("String") > -1) {
          s = (String) v.elementAt(index);
        }
        else if (v.elementAt(index).getClass().getName().indexOf("Integer") >
                 -1) {
          s = v.elementAt(index).toString();
        }
        else {
          s = null;
        }
      }
      else {
        s = super.get(key).toString();
      }
    }
    else {
      s = "0";

    }
    if (s == null) {
      s = "0";
    }
    i = new Integer(s).intValue();
    return i;
  }

  public double toDouble(String key, int index) {
    String s, sClassName;
    double d;

    Object o = super.get(key);
    if (o != null) {
      sClassName = o.getClass().getName();
      if (sClassName.indexOf("Vector") > -1) {
        Vector v = (Vector)super.get(key);
        if (v.elementAt(index).getClass().getName().indexOf("String") > -1) {
          s = (String) v.elementAt(index);
        }
        else if (v.elementAt(index).getClass().getName().indexOf("Double") > -1) {
          s = v.elementAt(index).toString();
        }
        else {
          s = null;
        }

      }
      else if (sClassName.indexOf("String") > -1) {
        s = (String)super.get(key);
      }
      else {
        s = super.get(key).toString();
      }
    }
    else {
      s = "0.0";

    }
    if (s == null) {
      s = "0.0";
    }
    d = new Double(s).doubleValue();
    return d;
  }

  public String toString(String key, int index) {
    String s, sClassName;
    boolean b;

    Object o = super.get(key);
    if (o != null) {
      sClassName = o.getClass().getName();
      if (sClassName.indexOf("Vector") > -1) {
        Vector v = (Vector)super.get(key);
        if (v.elementAt(index) != null) {
          if (v.elementAt(index).getClass().getName().indexOf("String") > -1) {
            s = (String) v.elementAt(index);
          }
          else {
            s = v.elementAt(index).toString();
          }
        }
        else {
          s = null;
        }

      }
      else {
        s = super.get(key).toString();
      }
    }
    else {
      s = "";

    }
    if (s == null) {
      s = "";

    }
    return s;
  }
}

class CMessage {
  private final String DELIMITER = ".";
  private final int STX = 0x02;
  private final int ETX = 0x03;
  private final int MTX = 0x04;
  private String m_sSTX;
  private String m_sETX;
  private String m_sMTX;

  private String m_sName;
  private char m_cType;
  private MsgVector m_vMsgList;
  private String m_MessageString;

  /* Constructor */
  CMessage() {
    // STX, ETX, MTX 초기화
    char c;

    m_sSTX = " ";
    m_sETX = " ";
    m_sMTX = " ";

    c = (char) STX;
    m_sSTX = m_sSTX.replace(' ', c);

    c = (char) ETX;
    m_sETX = m_sETX.replace(' ', c);

    c = (char) MTX;
    m_sMTX = m_sMTX.replace(' ', c);

    // 초기화
    m_sName = "";
    m_cType = 'n';
    m_vMsgList = new MsgVector();
  }

  /* Set Message Name */
  boolean SetMessageName(String Name) {
    m_sName = Name;
    return true;
  }

  /* Get Message Name */
  String GetMessageName() {
    return m_sName;
  }

  /* Reset */
  boolean Reset() {
    m_vMsgList.removeAllElements();
    m_vMsgList.clear();
    return true;
  }

  /* Set Message Item : bool*/
  boolean SetMessageItem(String Name, boolean Value, boolean AddArray) {
    boolean bReturn;
    bReturn = SetMessageItemRecursive(Name, Value, AddArray);
    if (bReturn == false) {
      ErrorLog("SetMessageItem", Name);
    }
    return bReturn;
  }

  boolean SetMessageItemRecursive(String Name, boolean Value, boolean AddArray) {
    CMessage SubMessage;
    Boolean bValue;
    int i, Find, size;
    String SubName;

    size = m_vMsgList.size();
    Find = Name.indexOf(DELIMITER);
    if (Find == -1) {
      if (m_cType == 'n') {
        m_cType = 'l';
      }
      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(Name) == 0) {
            if (SubMessage.m_cType == 'b') {
              if (AddArray == true) {
                SubMessage.m_cType = 'B';
                bValue = new Boolean(Value);
                SubMessage.m_vMsgList.add(bValue);
              }
              else {
                bValue = new Boolean(Value);
                SubMessage.m_vMsgList.set(0, bValue);
              }
              return true;
            }
            else if (SubMessage.m_cType == 'B') {
              if (AddArray == true) {
                bValue = new Boolean(Value);
                SubMessage.m_vMsgList.add(bValue);
                return true;
              }
            }
            return false;
          }
        }

        SubMessage = new CMessage();
        SubMessage.m_sName = Name;
        SubMessage.m_cType = 'b';
        bValue = new Boolean(Value);
        SubMessage.m_vMsgList.add(bValue);
        m_vMsgList.add(SubMessage);
        return true;
      }
    }
    else {
      SubName = Name.substring(0, Find);
      Name = Name.substring(Find + 1);
      if (m_cType == 'n') {
        m_cType = 'l';
      }
      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(SubName) == 0) {
            return SubMessage.SetMessageItem(Name, Value, AddArray);
          }
        }
        if (!Name.equals("")) {
          SubMessage = new CMessage();
          SubMessage.m_sName = SubName;
          m_vMsgList.add(SubMessage);
          return SubMessage.SetMessageItem(Name, Value, AddArray);
        }
      }
    }
    return false;
  }

  /* Set Message Item : int*/
  boolean SetMessageItem(String Name, int Value, boolean AddArray) {
    boolean bReturn;
    bReturn = SetMessageItemRecursive(Name, Value, AddArray);
    if (bReturn == false) {
      ErrorLog("SetMessageItem", Name);
    }
    return bReturn;
  }

  boolean SetMessageItemRecursive(String Name, int Value, boolean AddArray) {
    CMessage SubMessage;
    Integer iValue;
    int i, Find, size;
    String SubName;

    size = m_vMsgList.size();
    Find = Name.indexOf(DELIMITER);
    if (Find == -1) {
      if (m_cType == 'n') {
        m_cType = 'l';
      }
      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(Name) == 0) {
            if (SubMessage.m_cType == 'i') {
              if (AddArray == true) {
                SubMessage.m_cType = 'I';
                iValue = new Integer(Value);
                SubMessage.m_vMsgList.add(iValue);
              }
              else {
                iValue = new Integer(Value);
                SubMessage.m_vMsgList.set(0, iValue);
              }
              return true;
            }
            else if (SubMessage.m_cType == 'I') {
              if (AddArray == true) {
                iValue = new Integer(Value);
                SubMessage.m_vMsgList.add(iValue);
                return true;
              }
            }
            return false;
          }
        }

        SubMessage = new CMessage();
        SubMessage.m_sName = Name;
        SubMessage.m_cType = 'i';
        iValue = new Integer(Value);
        SubMessage.m_vMsgList.add(iValue);
        m_vMsgList.add(SubMessage);
        return true;
      }
    }
    else {
      SubName = Name.substring(0, Find);
      Name = Name.substring(Find + 1);
      if (m_cType == 'n') {
        m_cType = 'l';
      }
      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(SubName) == 0) {
            return SubMessage.SetMessageItem(Name, Value, AddArray);
          }
        }
        if (!Name.equals("")) {
          SubMessage = new CMessage();
          SubMessage.m_sName = SubName;
          m_vMsgList.add(SubMessage);
          return SubMessage.SetMessageItem(Name, Value, AddArray);
        }
      }
    }
    return false;
  }

  /* Set Message Item : double*/
  boolean SetMessageItem(String Name, double Value, boolean AddArray) {
    boolean bReturn;
    bReturn = SetMessageItemRecursive(Name, Value, AddArray);
    if (bReturn == false) {
      ErrorLog("SetMessageItem", Name);
    }
    return bReturn;
  }

  boolean SetMessageItemRecursive(String Name, double Value, boolean AddArray) {
    CMessage SubMessage;
    Double dValue;
    int i, Find, size;
    String SubName;

    size = m_vMsgList.size();
    Find = Name.indexOf(DELIMITER);
    if (Find == -1) {
      if (m_cType == 'n') {
        m_cType = 'l';
      }
      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(Name) == 0) {
            if (SubMessage.m_cType == 'd') {
              if (AddArray == true) {
                SubMessage.m_cType = 'D';
                dValue = new Double(Value);
                SubMessage.m_vMsgList.add(dValue);
              }
              else {
                dValue = new Double(Value);
                SubMessage.m_vMsgList.set(0, dValue);
              }
              return true;
            }
            else if (SubMessage.m_cType == 'D') {
              if (AddArray == true) {
                dValue = new Double(Value);
                SubMessage.m_vMsgList.add(dValue);
                return true;
              }
            }
            return false;
          }
        }

        SubMessage = new CMessage();
        SubMessage.m_sName = Name;
        SubMessage.m_cType = 'd';
        dValue = new Double(Value);
        SubMessage.m_vMsgList.add(dValue);
        m_vMsgList.add(SubMessage);
        return true;
      }
    }
    else {
      SubName = Name.substring(0, Find);
      Name = Name.substring(Find + 1);
      if (m_cType == 'n') {
        m_cType = 'l';
      }
      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(SubName) == 0) {
            return SubMessage.SetMessageItem(Name, Value, AddArray);
          }
        }
        if (!Name.equals("")) {
          SubMessage = new CMessage();
          SubMessage.m_sName = SubName;
          m_vMsgList.add(SubMessage);
          return SubMessage.SetMessageItem(Name, Value, AddArray);
        }
      }
    }
    return false;
  }

  /* Set Message Item : String*/
  boolean SetMessageItem(String Name, String Value, boolean AddArray) {
    boolean bReturn;
    bReturn = SetMessageItemRecursive(Name, Value, AddArray);
    if (bReturn == false) {
      ErrorLog("SetMessageItem", Name);
    }
    return bReturn;
  }

  boolean SetMessageItemRecursive(String Name, String Value, boolean AddArray) {
    CMessage SubMessage;
    int i, Find, size;
    String SubName;

    size = m_vMsgList.size();
    Find = Name.indexOf(DELIMITER);
    if (Find == -1) {
      if (m_cType == 'n') {
        m_cType = 'l';
      }
      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(Name) == 0) {
            if (SubMessage.m_cType == 's') {
              if (AddArray == true) {
                SubMessage.m_cType = 'S';
                SubMessage.m_vMsgList.add(Value);
              }
              else {
                SubMessage.m_vMsgList.set(0, Value);
              }
              return true;
            }
            else if (SubMessage.m_cType == 'S') {
              if (AddArray == true) {
                SubMessage.m_vMsgList.add(Value);
                return true;
              }
            }
            return false;
          }
        }

        SubMessage = new CMessage();
        SubMessage.m_sName = Name;
        SubMessage.m_cType = 's';
        SubMessage.m_vMsgList.add(Value);
        m_vMsgList.add(SubMessage);
        return true;
      }
    }
    else {
      SubName = Name.substring(0, Find);
      Name = Name.substring(Find + 1);
      if (m_cType == 'n') {
        m_cType = 'l';
      }
      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(SubName) == 0) {
            return SubMessage.SetMessageItem(Name, Value, AddArray);
          }
        }
        if (!Name.equals("")) {
          SubMessage = new CMessage();
          SubMessage.m_sName = SubName;
          m_vMsgList.add(SubMessage);
          return SubMessage.SetMessageItem(Name, Value, AddArray);
        }
      }
    }
    return false;
  }

  /* Get Message Item */
  boolean GetMessageItem(String Name, MsgVector ValueList, int Index,
                         boolean ErrorLog) {
    boolean bReturn;

    // reset
    ValueList.removeAllElements();

    bReturn = GetMessageItemRecursive(Name, ValueList, Index);
    if ( (bReturn == false) && (ErrorLog == true)) {
      ErrorLog("GetMessageItem", Name);
    }
    return bReturn;
  }

  boolean GetMessageItemRecursive(String Name, MsgVector ValueList, int Index) {
    CMessage SubMessage;
    int i, Find, size, size_s;
    String SubName;
    Boolean bVal;
    Integer iVal;
    Double dVal;
    String sVal;

    size = m_vMsgList.size();
    Find = Name.indexOf(DELIMITER);
    if (Find == -1) {
      if ( (m_sName.compareTo(Name) == 0) && (m_cType != 'l')) {
        switch (m_cType) {
          case 'b':
            bVal = (Boolean) m_vMsgList.elementAt(0);
            ValueList.add(bVal);
            return true;
          case 'B':
            if (Index == -1) {
              for (i = 0; i < size; i++) {
                bVal = (Boolean) m_vMsgList.elementAt(i);
                ValueList.add(bVal);
              }
            }
            else {
              bVal = (Boolean) m_vMsgList.elementAt(Index);
              ValueList.add(bVal);
            }
            return true;
          case 'i':
            iVal = (Integer) m_vMsgList.elementAt(0);
            ValueList.add(iVal);
            return true;
          case 'I':
            if (Index == -1) {
              for (i = 0; i < size; i++) {
                iVal = (Integer) m_vMsgList.elementAt(i);
                ValueList.add(iVal);
              }
            }
            else {
              iVal = (Integer) m_vMsgList.elementAt(Index);
              ValueList.add(iVal);
            }
            return true;
          case 'd':
            dVal = (Double) m_vMsgList.elementAt(0);
            ValueList.add(dVal);
            return true;
          case 'D':
            if (Index == -1) {
              for (i = 0; i < size; i++) {
                dVal = (Double) m_vMsgList.elementAt(i);
                ValueList.add(dVal);
              }
            }
            else {
              dVal = (Double) m_vMsgList.elementAt(Index);
              ValueList.add(dVal);
            }
            return true;
          case 's':
            sVal = (String) m_vMsgList.elementAt(0);
            ValueList.add(sVal);
            return true;
          case 'S':
            if (Index == -1) {
              for (i = 0; i < size; i++) {
                sVal = (String) m_vMsgList.elementAt(i);
                ValueList.add(sVal);
              }
            }
            else {
              sVal = (String) m_vMsgList.elementAt(Index);
              ValueList.add(sVal);
            }
            return true;
          default:
            return false;
        }
      }
      else if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(Name) == 0) {
            size_s = SubMessage.m_vMsgList.size();
            switch (SubMessage.m_cType) {
              case 'b':
                bVal = (Boolean) SubMessage.m_vMsgList.elementAt(0);
                ValueList.add(bVal);
                return true;
              case 'B':
                if (Index == -1) {
                  for (i = 0; i < size_s; i++) {
                    bVal = (Boolean) SubMessage.m_vMsgList.elementAt(i);
                    ValueList.add(bVal);
                  }
                }
                else {
                  bVal = (Boolean) SubMessage.m_vMsgList.elementAt(Index);
                  ValueList.add(bVal);
                }
                return true;
              case 'i':
                iVal = (Integer) SubMessage.m_vMsgList.elementAt(0);
                ValueList.add(iVal);
                return true;
              case 'I':
                if (Index == -1) {
                  for (i = 0; i < size_s; i++) {
                    iVal = (Integer) SubMessage.m_vMsgList.elementAt(i);
                    ValueList.add(iVal);
                  }
                }
                else {
                  iVal = (Integer) SubMessage.m_vMsgList.elementAt(Index);
                  ValueList.add(iVal);
                }
                return true;
              case 'd':
                dVal = (Double) SubMessage.m_vMsgList.elementAt(0);
                ValueList.add(dVal);
                return true;
              case 'D':
                if (Index == -1) {
                  for (i = 0; i < size_s; i++) {
                    dVal = (Double) SubMessage.m_vMsgList.elementAt(i);
                    ValueList.add(dVal);
                  }
                }
                else {
                  dVal = (Double) SubMessage.m_vMsgList.elementAt(Index);
                  ValueList.add(dVal);
                }
                return true;
              case 's':
                sVal = (String) SubMessage.m_vMsgList.elementAt(0);
                ValueList.add(sVal);
                return true;
              case 'S':
                if (Index == -1) {
                  for (i = 0; i < size_s; i++) {
                    sVal = (String) SubMessage.m_vMsgList.elementAt(i);
                    ValueList.add(sVal);
                  }
                }
                else {
                  sVal = (String) SubMessage.m_vMsgList.elementAt(Index);
                  ValueList.add(sVal);
                }
                return true;
              default:
                return false;
            }
          }
        }
      }
    }
    else {
      SubName = Name.substring(0, Find);
      Name = Name.substring(Find + 1);

      if (m_cType == 'l') {
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          if (SubMessage.m_sName.compareTo(SubName) == 0) {
            return SubMessage.GetMessageItem(Name, ValueList, Index, true);
          }
        }
      }
    }
    return false;
  }

  /* ToMessage : Make MessageString by Sender side*/
  String ToMessage() {
    String sReturn = "";

    sReturn = m_sSTX + ToMessageRecursive() + m_sETX;
    return sReturn;
  }

  String ToMessageRecursive() {
    CMessage SubMessage;
    int i, size;
    ;
    StringBuffer buffer = new StringBuffer();

    size = m_vMsgList.size();

    buffer.append(m_sName);
    buffer.append(m_sMTX);
    buffer.append(m_cType);
    buffer.append(size);
    buffer.append(m_sMTX);

    switch (m_cType) {
      case 'b':
      case 'B':
        for (i = 0; i < size; i++) {
          buffer.append(m_vMsgList.toBool(i));
          buffer.append(m_sMTX);
        }
        break;
      case 'i':
      case 'I':
        for (i = 0; i < size; i++) {
          buffer.append(m_vMsgList.toInt(i));
          buffer.append(m_sMTX);
        }
        break;
      case 'd':
      case 'D':
        for (i = 0; i < size; i++) {
          buffer.append(m_vMsgList.toDouble(i));
          buffer.append(m_sMTX);
        }
        break;
      case 's':
      case 'S':
        for (i = 0; i < size; i++) {
          buffer.append(m_vMsgList.toString(i));
          buffer.append(m_sMTX);
        }
        break;
      case 'l':
        for (i = 0; i < size; i++) {
          SubMessage = (CMessage) m_vMsgList.elementAt(i);
          buffer.append(SubMessage.ToMessageRecursive());
        }
        break;
      case 'n':
        break;
      default:
        return "Error in ToMessage";
    }

    return buffer.toString();
  }

  /* GetMessageInfo : Get Name/Type/ValueList, use parsing SQL*/
  boolean GetMessageInfo(MsgVector NameList, MsgVector TypeList,
                         MsgVector ValueList) {
    boolean bReturn;

    // reset
    NameList.removeAllElements();
    TypeList.removeAllElements();
    ValueList.removeAllElements();

    bReturn = GetMessageInfoRecursive(NameList, TypeList, ValueList, "");
    if (bReturn == false) {
      ErrorLog("GetMessageInfo", "");
    }
    return bReturn;
  }

  boolean GetMessageInfoRecursive(MsgVector NameList, MsgVector TypeList,
                                  MsgVector ValueList, String Name) {
    CMessage SubMessage;
    String sNewName = "";
    int i, size;

    size = m_vMsgList.size();
    for (i = 0; i < size; i++) {
      SubMessage = (CMessage) m_vMsgList.elementAt(i);

      sNewName = Name + "." + SubMessage.m_sName;
      switch (SubMessage.m_cType) {
        case 'b':
        case 'i':
        case 'd':
        case 's':
          NameList.add(sNewName);
          TypeList.add(new StringBuffer().append(SubMessage.m_cType).toString());
          ValueList.add(SubMessage.m_vMsgList.elementAt(0));
          break;
        case 'B':
        case 'I':
        case 'D':
        case 'S':
          NameList.add(sNewName);
          TypeList.add(new StringBuffer().append(SubMessage.m_cType).toString());
          break;
        case 'l':
          if (SubMessage.GetMessageInfoRecursive(NameList, TypeList, ValueList,
                                                 sNewName) == false) {
            return false;
          }
          break;
        case 'n':
        default:
          break;
      }
    }
    return true;
  }

  /* SetMessageMove : Copy CMessage */
  boolean SetMessageMove(CMessage Message) {
    boolean bReturn;

    bReturn = Message.SetMessageMove(m_sName, m_cType, m_vMsgList);
    m_vMsgList.removeAllElements();
    return bReturn;
  }

  boolean SetMessageMove(String Name, char Type, MsgVector MoveList) {
    int i;

    // reset
    m_vMsgList.removeAllElements();

    m_sName = Name;
    m_cType = Type;
    for (i = 0; i < MoveList.size(); i++) {
      m_vMsgList.add( (CMessage) MoveList.elementAt(i));

    }
    return true;
  }

  /* SetMessage : Make CMessage by Receiver side*/
  boolean SetMessage(String MessageString) {
    String MessageContent;
    boolean bReturn;
    int Loc;
    Vector RemainStringList = new Vector();

    Loc = MessageString.indexOf(m_sSTX);
    if (Loc == -1) {
      return false;
    }
    MessageString = MessageString.substring(Loc + 1);

    Loc = MessageString.indexOf(m_sETX);
    if (Loc == -1) {
      return false;
    }
    MessageContent = MessageString.substring(0, Loc);
    bReturn = SetMessageRecursive(MessageContent, RemainStringList);

    return bReturn;
  }

  boolean SetMessageRecursive(String MessageString, Vector RemainStringList) {
    int Loc, i, Count;
    String subString;
    CMessage SubMessage;

    Boolean bVal;
    Integer iVal;
    Double dVal;
    String sVal;

    // reset
    RemainStringList.removeAllElements();
    m_vMsgList.removeAllElements();

    Loc = MessageString.indexOf(m_sMTX);
    if (Loc == -1) {
      return false;
    }

    m_sName = MessageString.substring(0, Loc);
    MessageString = MessageString.substring(Loc + 1);
    m_cType = MessageString.charAt(0);
    Loc = MessageString.indexOf(m_sMTX);
    if (Loc == -1) {
      return false;
    }

    subString = MessageString.substring(1, Loc);
    Count = new Integer(subString).intValue();
    MessageString = MessageString.substring(Loc + 1);

    switch (m_cType) {
      case 'b':
      case 'B':
        for (i = 0; i < Count; i++) {
          Loc = MessageString.indexOf(m_sMTX);
          if (Loc == -1) {
            return false;
          }
          subString = MessageString.substring(0, Loc);
          bVal = new Boolean(subString);
          m_vMsgList.add(bVal);
          MessageString = MessageString.substring(Loc + 1);
        }
        break;
      case 'i':
      case 'I':
        for (i = 0; i < Count; i++) {
          Loc = MessageString.indexOf(m_sMTX);
          if (Loc == -1) {
            return false;
          }
          subString = MessageString.substring(0, Loc);
          iVal = new Integer(subString);
          m_vMsgList.add(iVal);
          MessageString = MessageString.substring(Loc + 1);
        }
        break;
      case 'd':
      case 'D':
        for (i = 0; i < Count; i++) {
          Loc = MessageString.indexOf(m_sMTX);
          if (Loc == -1) {
            return false;
          }
          subString = MessageString.substring(0, Loc);
          dVal = new Double(subString);
          m_vMsgList.add(dVal);
          MessageString = MessageString.substring(Loc + 1);
        }
        break;
      case 's':
      case 'S':
        for (i = 0; i < Count; i++) {
          Loc = MessageString.indexOf(m_sMTX);
          if (Loc == -1) {
            return false;
          }
          subString = MessageString.substring(0, Loc);
          m_vMsgList.add(subString);
          MessageString = MessageString.substring(Loc + 1);
        }
        break;
      case 'l':
        for (i = 0; i < Count; i++) {
          SubMessage = new CMessage();
          if (SubMessage.SetMessageRecursive(MessageString, RemainStringList) == false) {
            return false;
          }
          m_vMsgList.add(SubMessage);
          MessageString = (String) RemainStringList.elementAt(0);
        }
        break;
      case 'n':
      default:
        break;
    }

    RemainStringList.add(MessageString);
    return true;
  }

  /* ErrorLog */
  void ErrorLog(String Command, String Name) {
    StringBuffer Buffer = new StringBuffer();
    StringBuffer FilePathName = new StringBuffer();

    // get Current Directory
    String Path = System.getProperty("user.dir");
    String Separator = System.getProperty("file.separator");
    FilePathName.append(Path).append(Separator).append("MESSAGECLASS.ERROR.TXT");

    try {
      // Format the current time.
      SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
      Date curTime = new Date();
      String dateString = format.format(curTime);

      Buffer.append(dateString).append(" ").append(Command).append(" ").append(
          Name).append(" ").append(ToMessage());

      BufferedWriter out = new BufferedWriter(new FileWriter(FilePathName.
          toString(), true));
      out.newLine();
      out.write(Buffer.toString());
      out.close();
    }
    catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
      return;
    }
  }
}

///////////////////////////////////////////////////////////////////////////////
// utilLog class : write Logfile
class utilLog {
  String m_sPath = "";
  int m_nDeleteLogDay = -1;

  //2006.09.14 by N.Y.K
  boolean m_bLogFlag = true;

  //2006.07.03
  public Vector m_pFileList = new Vector();

  // 수정
  // 2005. 6. 7. 정정주
  // Log Delete를 위한 DeleteLogDay를 argument로 넘김
  utilLog(String sPath, int nDeleteLogDay) {
    m_sPath = sPath;
    m_nDeleteLogDay = nDeleteLogDay;
  }

  utilLog(String sPath) {
    m_sPath = sPath;
  }

  utilLog() {
  }

  void SetLogPath(String strPath) {
    m_sPath = strPath;
  }

  void SetLogFlag(boolean bLogFlag) {
    m_bLogFlag = bLogFlag;
  }

  synchronized String DisplayException(Exception e)
  {
	StringBuffer sb = new StringBuffer();
	StackTraceElement[] trace = e.getStackTrace();

	for (int i=0; i<trace.length; i++)
		sb.append("\n " + trace[i]);
	return sb.toString();
  }

  //--------------------------------------------------------------------------
  // make log string
  synchronized void WriteReturnLog(String filename, String log1, String log2,
                                   boolean bHourly) {
    if (m_bLogFlag == false) {
      return;
    }
    String Path, Separator, dateString, timeFormat, filePathName;
    MsgVector fileformat = new MsgVector();

    if (log2 != null) {
      log1 += (" " + log2);
    }

    //2006.07.03 by N.Y.K
    CLogItem pLogItem = null;
    int k;
    String strName;
    for (k = 0; k < m_pFileList.size(); k++) {
      pLogItem = (CLogItem) m_pFileList.elementAt(k);
      strName = pLogItem.m_strAliasName;
      if (strName.equals(filename) == true) {
        break;
      }
    }

    //2006.07.03 by N.Y.K
    if (k >= m_pFileList.size()) {
      pLogItem = new CLogItem();
      pLogItem.m_strAliasName = filename;
      m_pFileList.addElement(pLogItem);
    }

    // get time
    if (bHourly == true) {
      timeFormat = "yyyyMMddHH";
    }
    else {
      timeFormat = "yyyyMMdd";

    }
    SimpleDateFormat format = new SimpleDateFormat(timeFormat);
    Date curTime = new Date();
    dateString = format.format(curTime);

    if ( (m_sPath == null) || (m_sPath.equals("") == true)) {
      // get Current Directory
      Path = System.getProperty("user.dir");
      Separator = System.getProperty("file.separator");

      // Path+화일
      fileformat.add(Path);
      fileformat.add(Separator);
      fileformat.add(Separator);
      fileformat.add(filename);
      fileformat.add(dateString);
      filePathName = fileformat.printf("%s%slog%s%s%s.log");
    }
    else {
      // get Current Directory
      Path = m_sPath;
      Separator = System.getProperty("file.separator");

      // Path+화일
      fileformat.add(Path);
      fileformat.add(Separator);
      fileformat.add(filename);
      fileformat.add(dateString);
      filePathName = fileformat.printf("%s%s%s%s.log");
    }

    try {
      //Log 날짜와 이름이 같지 않으면 새로 생성
      if (pLogItem.m_strFileName.equals(filePathName) == false) {
        pLogItem.m_strFileName = filePathName;

        //기존거 제거
        if (pLogItem.m_fwFile != null) {
          pLogItem.m_fwFile.close();

          //새로 생성
        }
        File file = new File(filePathName);
        FileWriter out = new FileWriter(file, true);
        pLogItem.m_fwFile = out;
      }

      // 2005. 8. 21. 정정주 수정 --------------
      // Millisecond의 경우 3자리 이하는 앞에 0을 붙여서 표현하도록 한다.
      Format formatter1 = new SimpleDateFormat("HH:mm:ss:");
      Format formatter2 = new SimpleDateFormat("S");
      String strCurrTime1 = formatter1.format(curTime);
      String strCurrTime2 = formatter2.format(curTime);
      if (strCurrTime2.length() < 3) {
        strCurrTime2 = "0" + strCurrTime2;
      }
      String strCurrTime = strCurrTime1 + strCurrTime2 + " ";

      log1 = strCurrTime + log1;

      //2006.07.03 by N.Y.K
      BufferedWriter out = new BufferedWriter(pLogItem.m_fwFile);
      out.write(log1);
      out.newLine();
      out.flush();
      //out.close();

    }
    catch (IOException ex) {
      System.out.println("IOException: " + ex.getMessage());
      return;
    }
  }

//  synchronized void WriteReturnLog(String filename, String log1, String log2,
//                                   boolean bHourly)
//  {
//    String Path, Separator, dateString, timeFormat, filePathName;
//    MsgVector fileformat = new MsgVector();
//
//    if (log2 != null)
//    {
//      log1 += (" " + log2);
//    }
//
//    // get time
//    if (bHourly == true)
//      timeFormat = "yyyyMMddHH";
//    else
//      timeFormat = "yyyyMMdd";
//
//    SimpleDateFormat format = new SimpleDateFormat(timeFormat);
//    Date curTime = new Date();
//    dateString = format.format(curTime);
//
//    if ( (m_sPath == null) || (m_sPath.equals("") == true))
//    {
//      // get Current Directory
//      Path = System.getProperty("user.dir");
//      Separator = System.getProperty("file.separator");
//
//      // Path+화일
//      fileformat.add(Path);
//      fileformat.add(Separator);
//      fileformat.add(Separator);
//      fileformat.add(filename);
//      fileformat.add(dateString);
//      filePathName = fileformat.printf("%s%slog%s%s%s.log");
//    }
//    else
//    {
//      // get Current Directory
//      Path = m_sPath;
//      Separator = System.getProperty("file.separator");
//
//      // Path+화일
//      fileformat.add(Path);
//      fileformat.add(Separator);
//      fileformat.add(filename);
//      fileformat.add(dateString);
//      filePathName = fileformat.printf("%s%s%s%s.log");
//    }
//
//    // 2005. 8. 21. 정정주 수정 --------------
//    // Millisecond의 경우 3자리 이하는 앞에 0을 붙여서 표현하도록 한다.
//    Format formatter1 = new SimpleDateFormat("HH:mm:ss:");
//    Format formatter2 = new SimpleDateFormat("S");
//    String strCurrTime1 = formatter1.format(curTime);
//    String strCurrTime2 = formatter2.format(curTime);
//    if (strCurrTime2.length() < 3)
//    {
//      strCurrTime2 = "0" + strCurrTime2;
//    }
//    String strCurrTime = strCurrTime1 + strCurrTime2 + " ";
//
//    log1 = strCurrTime + log1;
//    OutputLog(filePathName, log1);
//
//  }

  //--------------------------------------------------------------------------
  // write log file
  synchronized void OutputLog(String filePathName, String log) {
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(filePathName, true));

      //out.newLine();
      out.write(log);
      out.newLine();
      out.close();
    }
    catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
      return;
    }
  }

  //2006.07.03 by N.Y.K
  synchronized void OutputLog(String filePathName, FileWriter fwFile,
                              String log) {
    try {
      BufferedWriter out = new BufferedWriter(fwFile);
      //out.newLine();
      out.write(log);
      out.newLine();
      out.close();
    }
    catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
      return;
    }
  }

  //--------------------------------------------------------------------------
  // Delete all log file before n days
  synchronized void DeleteAutoLogFiles(int DaysBefore) {
    // 수정
    // 2005. 6. 7. 정정주
    // DeleteLogDay가 0보다 작은 값인 경우 무시함.
    if (m_nDeleteLogDay < 0) {
      return;
    }

    long updatedtime, difftime;
    String Path, Separator, strDir;

    // 수정
    // 2005. 6. 7. 정정주
    // log path가 별도로 지정되어 있는 경우 해당 경로를 log 경로로 지정
    if ( (m_sPath == null) || (m_sPath.equals("") == true)) {
      Path = System.getProperty("user.dir");
      Separator = System.getProperty("file.separator");
      strDir = Path + Separator + "log";
    }
    else {
      strDir = m_sPath;
    }

    File dir = new File(strDir);

    String[] children = dir.list();
    if (children != null) {
      for (int i = 0; i < children.length; i++) {
        File f = new File(dir, children[i]);
        updatedtime = f.lastModified();
        difftime = (System.currentTimeMillis() - updatedtime) / 1000 / 24 /
            3600;
        if (difftime > DaysBefore) {
          f.delete();
        }
      }
    }
  }

  //--------------------------------------------------------------------------
  // System Time 변경
  // Windows sTime = "14:31:30";
  // Unix    sTime = "1431.30";
  synchronized void ChangeSystemTime(String sTime) {
    String OS = System.getProperty("os.name").toLowerCase();
    if (OS.indexOf("windows") > -1) {
      try {
        Runtime.getRuntime().exec("cmd.exe /c time " + sTime);
      }
      catch (Exception e) {
        System.out.println("Can't change the time");
      }
    }
    else {
      try {
        Runtime.getRuntime().exec("date " + sTime);
      }
      catch (Exception e) {
        System.out.println("Can't change the time");
      }
    }
  }

  //--------------------------------------------------------------------------
  // get current datetime string
  synchronized String GetCurrentDateTime(int YearDigit) {
    SimpleDateFormat format;
    Date curTime;
    String sDate = "";
    if (YearDigit == 2) {
      format = new SimpleDateFormat("yyMMddHHmmss");
      curTime = new Date();
      sDate = format.format(curTime);
    }
    else if (YearDigit == 4) {
      format = new SimpleDateFormat("yyyyMMddHHmmss");
      curTime = new Date();
      sDate = format.format(curTime);
    }
    else if (YearDigit == 0) {
      format = new SimpleDateFormat("HH:mm:ss");
      curTime = new Date();
      sDate = format.format(curTime);
    }
    else if (YearDigit == -1) {
      format = new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss");
      curTime = new Date();
      sDate = format.format(curTime);
    }

    return sDate;
  }

  //--------------------------------------------------------------------------
  // get current datetime string
  // Java(1899/12/30)와 C++ Builder(1980/12/30)의 기준날짜 차이 보정(25569.374994213)
  synchronized double getCBuilderDay() {
    double dTime;
    dTime = (double) (System.currentTimeMillis() / 3600000.0 / 24.0 +
                      25569.374994213);
    return dTime;
  }

// make Thread Hang Check String
  synchronized void ThreadHangCheckLog(String strFilename, String log) {
    String strPath, strSeparator, strFilePathName;

    MsgVector fileformat = new MsgVector();

    // get Current Directory

    strPath = System.getProperty("user.dir");

    strSeparator = System.getProperty("file.separator");

    // Path+화일

    fileformat.add(strPath);

    fileformat.add(strSeparator);

    fileformat.add(strFilename);

    strFilePathName = fileformat.printf("%s%s%s");

    OutputLog(strFilePathName, log);
  }

  // Version history 기록 함수
  synchronized void WriteVersionHistory(String filename, String versionid) {
    String Path, Separator, filePathName;
    MsgVector fileformat = new MsgVector();

    // get Current Directory
    Path = System.getProperty("user.dir");
    Separator = System.getProperty("file.separator");

    // Path+화일
    fileformat.add(Path);
    fileformat.add(Separator);
    fileformat.add(filename);
    fileformat.add("_Ver");
    filePathName = fileformat.printf("%s%s%s%s.txt");

    Date curTime = new Date();
    Format formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss ");
    String strCurrTime = formatter.format(curTime);
    versionid = strCurrTime + versionid;

    OutputLog(filePathName, versionid);
  }
}

class CACSTime {
  int m_nAccelFactor;

  CACSTime(int Factor) {
    m_nAccelFactor = Factor;
  }

  //---------------------------------------------------------------------------
  // SetAccelerationFactor
  synchronized boolean SetAccelerationFactor(int Factor) {
    m_nAccelFactor = Factor;
    return true;
  }

  //---------------------------------------------------------------------------
  // GetAccelarationFactor
  synchronized int GetAccelerationFactor() {
    return m_nAccelFactor;
  }

  //---------------------------------------------------------------------------
  // return CurrentTime in mm sec unit
  synchronized long GetCurrentTime() {
    return System.currentTimeMillis(); // in mm sec
  }

  //---------------------------------------------------------------------------
  // return BasicTimeSpan between TimeTo and TimeFrom
  synchronized long GetBasicTimeSpan(long TimeTo, long TimeFrom) {
    long diff;
    if (TimeTo > TimeFrom) {
      diff = TimeTo - TimeFrom;
    }
    else {
      diff = 0;

    }
    return diff;
  }

  //---------------------------------------------------------------------------
  // return TimeSpan between Current Time and TimeFrom
  synchronized long GetCurrentTimeSpan(long TimeFrom) {
    return (System.currentTimeMillis() - TimeFrom);
  }

  //---------------------------------------------------------------------------
  // return real TimeSpan between TimeTo and TimeFrom considering Accelaration Factor
  synchronized long GetAccelTimeSpan(long TimeTo, long TimeFrom) {
    long diff;

    diff = GetBasicTimeSpan(TimeTo, TimeFrom);
    return (diff * m_nAccelFactor);
  }

  //---------------------------------------------------------------------------
  // return real TimeSpan between Current Time and TimeFrom considering Accelaration Factor
  synchronized long GetAccelCurrentTimeSpan(long TimeFrom) {
    long diff;

    diff = GetCurrentTimeSpan(TimeFrom);
    return (diff * m_nAccelFactor);
  }
}

/* *****************************************************************************
   Class and Operation for the LogFile Writing. (programmed by I.K.Y.)
 ***************************************************************************** */
//class CLogItem
//{
//  private String m_FileName = "";
//
//  public String getName()
//  {
//    return m_FileName;
//  }
//
//  public void setName(String filename)
//  {
//    m_FileName = filename;
//  }
//}

// 2006.07.03 by N.Y.K
class CLogItem {
  public String m_strFileName = "";
  public String m_strAliasName = "";
  public FileWriter m_fwFile = null;
  public String getName() {
    return m_strFileName;
  }

  public void setName(String filename) {
    m_strFileName = filename;
  }
}

class CLogManager {
  private boolean m_bSaveLog = true;
  private String m_LogPath = ".\\";
  private Vector m_LogFileLists = new Vector();
  private String m_LmLogName = "LogManagerErrLog.log";

  CLogManager() {
  }

  // Save SystemErrorLog
  synchronized public boolean SaveLogManagerErrLog(String sErrLog) {
    try {
      File lmLogFile = new File(m_LmLogName);
      FileWriter sysout = new FileWriter(lmLogFile, true);

      sErrLog += "\r\n";
      sysout.write(sErrLog);
      sysout.flush();
      sysout.close();

      return true;
    }
    catch (IOException e) {
      return false;
    }
  }

  // Delete LogFileName in the LogFileLists
  synchronized public boolean DeleteFileList(String filename) {
    int i = 0;
    CLogItem tempLog;
    if (!filename.equals("")) {
      for (i = 0; i < m_LogFileLists.size(); i++) {
        tempLog = (CLogItem) m_LogFileLists.get(i);
        if (tempLog.getName().equals(filename)) {
          m_LogFileLists.removeElementAt(i);
          return true;
        }
      }
      return false;
    }
    else {
      for (i = 0; i < m_LogFileLists.size(); i++) {
        m_LogFileLists.removeElementAt(0);
      }
      return true;
    }
  }

  // Set LogFileName into the LogFileLists
  synchronized public boolean SetFileList(String filename) {
    CLogItem tempLog = new CLogItem();
    tempLog.setName(filename);
    m_LogFileLists.addElement(tempLog);
    return true;
  }

  // Get LogFileName with given index from the LogFileLists
  synchronized public String GetFileList(int index) {
    int i = 0;
    CLogItem tempLog;
    if (index >= m_LogFileLists.size()) {
      return null;
    }

    tempLog = (CLogItem) m_LogFileLists.elementAt(index);
    return tempLog.getName();
  }

  // Set LogFilePath
  synchronized public boolean SetLogFilePath(String sLogFilePath) {
    m_LogPath = sLogFilePath + "log\\"; ;
    return true;
  }

  // Time + Log
  synchronized public boolean WriteReturnTimeLog(String logfilename,
                                                 String logstring1,
                                                 String logstring2,
                                                 boolean bHourly) {
    int Pos;
    String Name1;
    String Text1;
    Date currTime = new Date();
    SimpleDateFormat dateFormat;
    SimpleDateFormat timeFormat;
    String dateString, timeString;

    if (bHourly == false) {
      dateFormat = new SimpleDateFormat("yyyyMMdd");
    }
    else {
      dateFormat = new SimpleDateFormat("yyyyMMddHH");
    }
    dateString = dateFormat.format(currTime);

    Name1 = m_LogPath + logfilename;
    Name1 = Name1 + dateString + ".log";

    // 2007.11.28 By MYM 조건 수정
    //if (Name1.equals == "")
    if (Name1.equals("") == true) {
      return false;
    }

    try {
      File file = new File(Name1);
      FileWriter out = new FileWriter(file, true);

      timeFormat = new SimpleDateFormat("HH:mm:ss ");
      timeString = timeFormat.format(currTime);

      if (logstring2 == null) {
        logstring2 = "";

      }
      Pos = logstring1.indexOf("\r\n");
      if (Pos > 0) {
        logstring1 = logstring1.substring(0, Pos);
      }
      Pos = logstring2.indexOf("\r\n");
      if (Pos > 0) {
        logstring2 = logstring2.substring(0, Pos);

      }
      Text1 = timeString + logstring1 + "  " + logstring2 + "\r\n";
      out.write(Text1);
      out.flush();
      out.close();
      return true;
    }
    catch (IOException e) {
      SaveLogManagerErrLog(e.getMessage());
    }
    return false;
  }

  // Log
  synchronized public boolean WriteReturnLog(String logfilename,
                                             String logstring1,
                                             String logstring2, boolean bHourly) {
    int Pos;
    String Name1;
    String Text1;
    Date currTime = new Date();
    SimpleDateFormat dateFormat;
    SimpleDateFormat timeFormat;
    String dateString, timeString;

    if (bHourly == false) {
      dateFormat = new SimpleDateFormat("yyyyMMdd");
    }
    else {
      dateFormat = new SimpleDateFormat("yyyyMMddHH");
    }
    dateString = dateFormat.format(currTime);

    Name1 = m_LogPath + logfilename;
    Name1 = Name1 + dateString + ".log";

    // 2007.11.28 By MYM 조건 수정
    //if (Name1.equals == "")
    if (Name1.equals("") == true) {
      return false;
    }

    try {
      File file = new File(Name1);
      FileWriter out = new FileWriter(file, true);

      if (logstring2 == null) {
        logstring2 = "";

      }
      Pos = logstring1.indexOf("\r\n");
      if (Pos > 0) {
        logstring1 = logstring1.substring(0, Pos - 1);
      }
      Pos = logstring2.indexOf("\r\n");
      if (Pos > 0) {
        logstring2 = logstring2.substring(0, Pos - 1);

      }
      Text1 = logstring1 + "  " + logstring2 + "\r\n";
      out.write(Text1);
      out.flush();
      out.close();
      return true;
    }
    catch (IOException e) {
      SaveLogManagerErrLog(e.getMessage());
    }
    return false;
  }

  // LogFile AutoDelete
  synchronized public boolean AutoDeleteLogFiles(int nBeforeDays) {
    String sDir = m_LogPath;
    String[] sFileList;
    File fileDir = new File(sDir);
    long ltimeLimit;

    ltimeLimit = (nBeforeDays * 24 * 3600 * 1000);

    sFileList = fileDir.list();
    try {
      if (sFileList.length == 0) {
        SaveLogManagerErrLog("There's nothing in the Log Directory.");
        return false;
      }

      for (int i = 0; i < sFileList.length; i++) {
        File logfile = new File(sDir + sFileList[i]);
        Date curTime = new Date();

        if ( (curTime.getTime() - logfile.lastModified()) > ltimeLimit) {
          logfile.delete();
        }
      }
      return true;
    }
    catch (Exception e) {
      SaveLogManagerErrLog("Exception: " + e.getMessage());
      return false;
    }
  }

  //--------------------------------------------------------------------------
  // get current datetime string
  synchronized String GetCurrentDateTime(int YearDigit) {
    SimpleDateFormat format;
    Date curTime;
    String sDate = "";
    if (YearDigit == 2) {
      format = new SimpleDateFormat("yyMMddHHmmss");
      curTime = new Date();
      sDate = format.format(curTime);
    }
    else if (YearDigit == 4) {
      format = new SimpleDateFormat("yyyyMMddHHmmss");
      curTime = new Date();
      sDate = format.format(curTime);
    }
    else if (YearDigit == 0) {
      format = new SimpleDateFormat("HH:mm:ss");
      curTime = new Date();
      sDate = format.format(curTime);
    }
    else if (YearDigit == -1) {
      format = new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss");
      curTime = new Date();
      sDate = format.format(curTime);
    }

    return sDate;
  }

  //--------------------------------------------------------------------------
}
