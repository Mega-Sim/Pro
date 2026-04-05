package com.samsung.ocs.common.message;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Message Class, OCS 3.0 for Unified FAB
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

public class Message {
	private final String DELIMITER = ".";
	private final int STX = 0x02;
	private final int ETX = 0x03;
	private final int MTX = 0x04;
	private String m_sSTX;
	private String m_sETX;
	private String m_sMTX;

	private String name;
	private char type;
	private MsgVector msgList;
	private String messageString;
	
	private final String COMMA = ",";

	/**
	 * Constructor of Message class.
	 */
	public Message() {
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
		name = "";
		type = 'n';
		msgList = new MsgVector();
	}

	/**
	 * Constructor of Message class.
	 */
	public Message(String Name)
	{
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
		name = Name;
		type = 'n';
		msgList = new MsgVector();
	}

	/**
	 * Set Message Name
	 * 
	 * @param name String
	 * @return true
	 */
	public boolean setMessageName(String name) {
		this.name = name;
		return true;
	}

	/**
	 * Get Message Name
	 */
	public String getMessageName() {
		return name;
	}

	/**
	 * Reset
	 */
	public boolean reset() {
		msgList.removeAllElements();
		msgList.clear();
		return true;
	}

	/* Set Message Item : bool*/
	boolean setMessageItem(String Name, boolean Value, boolean AddArray) {
		boolean bReturn;
		bReturn = setMessageItemRecursive(Name, Value, AddArray);
		if (bReturn == false)
			errorLog("SetMessageItem", Name);
		return bReturn;
	}

	boolean setMessageItemRecursive(String Name, boolean Value, boolean AddArray) {
		Message SubMessage;
		Boolean bValue;
		int i, Find, size;
		String SubName;

		size = msgList.size();
		Find = Name.indexOf(DELIMITER);
		if (Find == -1) {
			if (type == 'n')
				type = 'l';
			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(Name) == 0) {
						if (SubMessage.type == 'b') {
							if (AddArray) {
								SubMessage.type = 'B';
								bValue = new Boolean(Value);
								SubMessage.msgList.add(bValue);
							} else {
								bValue = new Boolean(Value);
								SubMessage.msgList.set(0, bValue);
							}
							return true;
						} else if (SubMessage.type == 'B') {
							if (AddArray) {
								bValue = new Boolean(Value);
								SubMessage.msgList.add(bValue);
								return true;
							}
						}
						return false;
					}
				}

				SubMessage = new Message();
				SubMessage.name = Name;
				SubMessage.type = 'b';
				bValue = new Boolean(Value);
				SubMessage.msgList.add(bValue);
				msgList.add(SubMessage);
				return true;
			}
		} else {
			SubName = Name.substring(0, Find);
			Name = Name.substring(Find + 1);
			if (type == 'n')
				type = 'l';
			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(SubName) == 0) {
						return SubMessage.setMessageItem(Name, Value, AddArray);
					}
				}
				if (Name != null && Name.length() > 0) {
					SubMessage = new Message();
					SubMessage.name = SubName;
					msgList.add(SubMessage);
					return SubMessage.setMessageItem(Name, Value, AddArray);
				}
			}
		}
		return false;
	}

	/**
	 * Set Message Item : int
	 */
	public boolean setMessageItem(String name, int value, boolean isAddArray) {
		boolean bReturn;
		bReturn = setMessageItemRecursive(name, value, isAddArray);
		if (bReturn == false)
			errorLog("SetMessageItem", name);
		return bReturn;
	}

	/**
	 * 
	 * @param Name
	 * @param Value
	 * @param AddArray
	 * @return
	 */
	boolean setMessageItemRecursive(String Name, int Value, boolean AddArray) {
		Message SubMessage;
		Integer iValue;
		int i, find, size;
		String SubName;

		size = msgList.size();
		find = Name.indexOf(DELIMITER);
		if (find == -1) {
			if (type == 'n')
				type = 'l';
			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(Name) == 0) {
						if (SubMessage.type == 'i') {
							if (AddArray) {
								SubMessage.type = 'I';
								iValue = new Integer(Value);
								SubMessage.msgList.add(iValue);
							} else {
								iValue = new Integer(Value);
								SubMessage.msgList.set(0, iValue);
							}
							return true;
						} else if (SubMessage.type == 'I') {
							if (AddArray) {
								iValue = new Integer(Value);
								SubMessage.msgList.add(iValue);
								return true;
							}
						}
						return false;
					}
				}

				SubMessage = new Message();
				SubMessage.name = Name;
				SubMessage.type = 'i';
				iValue = new Integer(Value);
				SubMessage.msgList.add(iValue);
				msgList.add(SubMessage);
				return true;
			}
		} else {
			SubName = Name.substring(0, find);
			Name = Name.substring(find + 1);
			if (type == 'n')
				type = 'l';
			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(SubName) == 0) {
						return SubMessage.setMessageItem(Name, Value, AddArray);
					}
				}
				if (Name != null && Name.length() > 0) {
					SubMessage = new Message();
					SubMessage.name = SubName;
					msgList.add(SubMessage);
					return SubMessage.setMessageItem(Name, Value, AddArray);
				}
			}
		}
		return false;
	}

	/**
	 * Set Message Item : double
	 */
	public boolean setMessageItem(String Name, double Value, boolean AddArray) {
		boolean bReturn;
		bReturn = setMessageItemRecursive(Name, Value, AddArray);
		if (bReturn == false)
			errorLog("SetMessageItem", Name);
		return bReturn;
	}

	boolean setMessageItemRecursive(String Name, double Value, boolean AddArray) {
		Message SubMessage;
		Double dValue;
		int i, Find, size;
		String SubName;

		size = msgList.size();
		Find = Name.indexOf(DELIMITER);
		if (Find == -1) {
			if (type == 'n')
				type = 'l';
			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(Name) == 0) {
						if (SubMessage.type == 'd') {
							if (AddArray) {
								SubMessage.type = 'D';
								dValue = new Double(Value);
								SubMessage.msgList.add(dValue);
							} else {
								dValue = new Double(Value);
								SubMessage.msgList.set(0, dValue);
							}
							return true;
						} else if (SubMessage.type == 'D') {
							if (AddArray) {
								dValue = new Double(Value);
								SubMessage.msgList.add(dValue);
								return true;
							}
						}
						return false;
					}
				}

				SubMessage = new Message();
				SubMessage.name = Name;
				SubMessage.type = 'd';
				dValue = new Double(Value);
				SubMessage.msgList.add(dValue);
				msgList.add(SubMessage);
				return true;
			}
		} else {
			SubName = Name.substring(0, Find);
			Name = Name.substring(Find + 1);
			if (type == 'n')
				type = 'l';
			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(SubName) == 0) {
						return SubMessage.setMessageItem(Name, Value, AddArray);
					}
				}
				if (Name != null && Name.length() > 0) {
					SubMessage = new Message();
					SubMessage.name = SubName;
					msgList.add(SubMessage);
					return SubMessage.setMessageItem(Name, Value, AddArray);
				}
			}
		}
		return false;
	}

	/**
	 * Set Message Item : String
	 */
	public boolean setMessageItem(String Name, String Value, boolean AddArray) {
		boolean bReturn;
		bReturn = setMessageItemRecursive(Name, Value, AddArray);
		if (bReturn == false)
			errorLog("SetMessageItem", Name);
		return bReturn;
	}

	boolean setMessageItemRecursive(String Name, String Value, boolean AddArray) {
		Message SubMessage;
		int i, Find, size;
		String SubName;

		size = msgList.size();
		Find = Name.indexOf(DELIMITER);
		if (Find == -1) {
			if (type == 'n')
				type = 'l';
			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(Name) == 0) {
						if (SubMessage.type == 's') {
							if (AddArray) {
								SubMessage.type = 'S';
								SubMessage.msgList.add(Value);
							} else {
								SubMessage.msgList.set(0, Value);
							}
							return true;
						} else if (SubMessage.type == 'S') {
							if (AddArray) {
								SubMessage.msgList.add(Value);
								return true;
							}
						}
						return false;
					}
				}

				SubMessage = new Message();
				SubMessage.name = Name;
				SubMessage.type = 's';
				SubMessage.msgList.add(Value);
				msgList.add(SubMessage);
				return true;
			}
		} else {
			SubName = Name.substring(0, Find);
			Name = Name.substring(Find + 1);
			if (type == 'n')
				type = 'l';
			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(SubName) == 0) {
						return SubMessage.setMessageItem(Name, Value, AddArray);
					}
				}
				if (Name != null && Name.length() > 0) {
					SubMessage = new Message();
					SubMessage.name = SubName;
					msgList.add(SubMessage);
					return SubMessage.setMessageItem(Name, Value, AddArray);
				}
			}
		}
		return false;
	}

	/**
	 * Get Message Item
	 */
	public boolean getMessageItem(String Name, MsgVector ValueList, int Index, boolean ErrorLog) {
		boolean bReturn;

		// reset
		ValueList.removeAllElements();

		bReturn = getMessageItemRecursive(Name, ValueList, Index);
		if ((bReturn == false) && (ErrorLog == true))
			errorLog("GetMessageItem", Name);
		return bReturn;
	}
	
	/**
	 * Get Message Item Recursive
	 * @param Name
	 * @param ValueList
	 * @param Index
	 * @return
	 */
	public boolean getMessageItemRecursive(String Name, MsgVector ValueList, int Index) {
		Message SubMessage;
		int i, Find, size, size_s;
		String SubName;
		Boolean bVal;
		Integer iVal;
		Double dVal;
		String sVal;

		size = msgList.size();
		Find = Name.indexOf(DELIMITER);
		if (Find == -1) {
			if ((name.compareTo(Name) == 0) && (type != 'l')) {
				switch (type) {
					case 'b':
						bVal = (Boolean) msgList.elementAt(0);
						ValueList.add(bVal);
						return true;
					case 'B':
						if (Index == -1) {
							for (i = 0; i < size; i++) {
								bVal = (Boolean) msgList.elementAt(i);
								ValueList.add(bVal);
							}
						} else {
							bVal = (Boolean) msgList.elementAt(Index);
							ValueList.add(bVal);
						}
						return true;
					case 'i':
						iVal = (Integer) msgList.elementAt(0);
						ValueList.add(iVal);
						return true;
					case 'I':
						if (Index == -1) {
							for (i = 0; i < size; i++) {
								iVal = (Integer) msgList.elementAt(i);
								ValueList.add(iVal);
							}
						} else {
							iVal = (Integer) msgList.elementAt(Index);
							ValueList.add(iVal);
						}
						return true;
					case 'd':
						dVal = (Double) msgList.elementAt(0);
						ValueList.add(dVal);
						return true;
					case 'D':
						if (Index == -1) {
							for (i = 0; i < size; i++) {
								dVal = (Double) msgList.elementAt(i);
								ValueList.add(dVal);
							}
						} else {
							dVal = (Double) msgList.elementAt(Index);
							ValueList.add(dVal);
						}
						return true;
					case 's':
						sVal = (String) msgList.elementAt(0);
						ValueList.add(sVal);
						return true;
					case 'S':
						if (Index == -1) {
							for (i = 0; i < size; i++) {
								sVal = (String) msgList.elementAt(i);
								ValueList.add(sVal);
							}
						} else {
							sVal = (String) msgList.elementAt(Index);
							ValueList.add(sVal);
						}
						return true;
					default:
						return false;
				}
			} else if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(Name) == 0) {
						size_s = SubMessage.msgList.size();
						switch (SubMessage.type) {
							case 'b':
								bVal = (Boolean) SubMessage.msgList.elementAt(0);
								ValueList.add(bVal);
								return true;
							case 'B':
								if (Index == -1) {
									for (i = 0; i < size_s; i++) {
										bVal = (Boolean) SubMessage.msgList.elementAt(i);
										ValueList.add(bVal);
									}
								} else {
									bVal = (Boolean) SubMessage.msgList.elementAt(Index);
									ValueList.add(bVal);
								}
								return true;
							case 'i':
								iVal = (Integer) SubMessage.msgList.elementAt(0);
								ValueList.add(iVal);
								return true;
							case 'I':
								if (Index == -1) {
									for (i = 0; i < size_s; i++) {
										iVal = (Integer) SubMessage.msgList.elementAt(i);
										ValueList.add(iVal);
									}
								} else {
									iVal = (Integer) SubMessage.msgList.elementAt(Index);
									ValueList.add(iVal);
								}
								return true;
							case 'd':
								dVal = (Double) SubMessage.msgList.elementAt(0);
								ValueList.add(dVal);
								return true;
							case 'D':
								if (Index == -1) {
									for (i = 0; i < size_s; i++) {
										dVal = (Double) SubMessage.msgList.elementAt(i);
										ValueList.add(dVal);
									}
								} else {
									dVal = (Double) SubMessage.msgList.elementAt(Index);
									ValueList.add(dVal);
								}
								return true;
							case 's':
								sVal = (String) SubMessage.msgList.elementAt(0);
								ValueList.add(sVal);
								return true;
							case 'S':
								if (Index == -1) {
									for (i = 0; i < size_s; i++) {
										sVal = (String) SubMessage.msgList.elementAt(i);
										ValueList.add(sVal);
									}
								} else {
									sVal = (String) SubMessage.msgList.elementAt(Index);
									ValueList.add(sVal);
								}
								return true;
							default:
								return false;
						}
					}
				}
			}
		} else {
			SubName = Name.substring(0, Find);
			Name = Name.substring(Find + 1);

			if (type == 'l') {
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					if (SubMessage.name.compareTo(SubName) == 0) {
						return SubMessage.getMessageItem(Name, ValueList, Index, true);
					}
				}
			}
		}
		return false;
	}

	/**
	 * ToMessage : Make MessageString by Sender side
	 */
	public String toMessage() {
		String sReturn = "";

		sReturn = m_sSTX + toMessageRecursive() + m_sETX;
		return sReturn;
	}

	/**
	 * 
	 * @return
	 */
	String toMessageRecursive() {
		Message SubMessage;
		int i, size;

		StringBuffer buffer = new StringBuffer();

		size = msgList.size();

		buffer.append(name);
		buffer.append(m_sMTX);
		buffer.append(type);
		buffer.append(size);
		buffer.append(m_sMTX);

		switch (type) {
			case 'b':
			case 'B':
				for (i = 0; i < size; i++) {
					buffer.append(msgList.toBool(i));
					buffer.append(m_sMTX);
				}
				break;
			case 'i':
			case 'I':
				for (i = 0; i < size; i++) {
					buffer.append(msgList.toInt(i));
					buffer.append(m_sMTX);
				}
				break;
			case 'd':
			case 'D':
				for (i = 0; i < size; i++) {
					buffer.append(msgList.toDouble(i));
					buffer.append(m_sMTX);
				}
				break;
			case 's':
			case 'S':
				for (i = 0; i < size; i++) {
					buffer.append(msgList.toString(i));
					buffer.append(m_sMTX);
				}
				break;
			case 'l':
				for (i = 0; i < size; i++) {
					SubMessage = (Message) msgList.elementAt(i);
					buffer.append(SubMessage.toMessageRecursive());
				}
				break;
			case 'n':
				break;
			default:
				return "Error in ToMessage";
		}

		return buffer.toString();
	}
	
	/**
	 * ToString : Make String by Sender side
	 */
	public String toString() {
		int i;
		int size = msgList.size();
		Message SubMessage;
		StringBuffer buffer = new StringBuffer();
		
		switch (type) {
			case 'b':
			case 'B':
				for (i = 0; i < size; i++) {
					if (i > 0) {
						buffer.append(COMMA);
					}
					buffer.append(msgList.toBool(i));
				}
				break;
			case 'i':
			case 'I':
				for (i = 0; i < size; i++) {
					if (i > 0) {
						buffer.append(COMMA);
					}
					buffer.append(msgList.toInt(i));
				}
				break;
			case 'd':
			case 'D':
				for (i = 0; i < size; i++) {
					if (i > 0) {
						buffer.append(COMMA);
					}
					buffer.append(msgList.toDouble(i));
				}
				break;
			case 's':
			case 'S':
				for (i = 0; i < size; i++) {
					if (i > 0) {
						buffer.append(COMMA);
					}
					buffer.append(msgList.toString(i));
				}
				break;
			case 'l':
				for (i = 0; i < size; i++) {
					if (i > 0) {
						buffer.append(COMMA);
					}
					SubMessage = (Message) msgList.elementAt(i);
					buffer.append(SubMessage.toString());
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
	boolean getMessageInfo(MsgVector NameList, MsgVector TypeList, MsgVector ValueList) {
		boolean bReturn;

		// reset
		NameList.removeAllElements();
		TypeList.removeAllElements();
		ValueList.removeAllElements();

		bReturn = getMessageInfoRecursive(NameList, TypeList, ValueList, "");
		if (bReturn == false)
			errorLog("GetMessageInfo", "");
		return bReturn;
	}

	boolean getMessageInfoRecursive(MsgVector NameList, MsgVector TypeList, MsgVector ValueList, String Name) {
		Message SubMessage;
		String sNewName = "";
		int i, size;

		size = msgList.size();
		for (i = 0; i < size; i++) {
			SubMessage = (Message) msgList.elementAt(i);

			sNewName = Name + "." + SubMessage.name;
			switch (SubMessage.type) {
				case 'b':
				case 'i':
				case 'd':
				case 's':
					NameList.add(sNewName);
					TypeList.add(new StringBuffer().append(SubMessage.type).toString());
					ValueList.add(SubMessage.msgList.elementAt(0));
					break;
				case 'B':
				case 'I':
				case 'D':
				case 'S':
					NameList.add(sNewName);
					TypeList.add(new StringBuffer().append(SubMessage.type).toString());
					break;
				case 'l':
					if (SubMessage.getMessageInfoRecursive(NameList, TypeList, ValueList, sNewName) == false) {
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

	/* SetMessageMove : Copy Message */
	boolean setMessageMove(Message Message) {
		boolean bReturn;

		bReturn = Message.setMessageMove(name, type, msgList);
		msgList.removeAllElements();
		return bReturn;
	}

	boolean setMessageMove(String Name, char Type, MsgVector MoveList) {
		int i;

		// reset
		msgList.removeAllElements();

		name = Name;
		type = Type;
		for (i = 0; i < MoveList.size(); i++)
			msgList.add( (Message) MoveList.elementAt(i));

		return true;
	}

	/**
	 * SetMessage : Make Message by Receiver side
	 */
	public boolean setMessage(String MessageString) {
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
		bReturn = setMessageRecursive(MessageContent, RemainStringList);

		return bReturn;
	}

	boolean setMessageRecursive(String MessageString, Vector RemainStringList) {
		int Loc, i, Count;
		String subString;
		Message SubMessage;

		Boolean bVal;
		Integer iVal;
		Double dVal;
		String sVal;

		// reset
		RemainStringList.removeAllElements();
		msgList.removeAllElements();

		Loc = MessageString.indexOf(m_sMTX);
		if (Loc == -1)
			return false;

		name = MessageString.substring(0, Loc);
		MessageString = MessageString.substring(Loc + 1);
		type = MessageString.charAt(0);
		Loc = MessageString.indexOf(m_sMTX);
		if (Loc == -1)
			return false;

		subString = MessageString.substring(1, Loc);
		Count = new Integer(subString).intValue();
		MessageString = MessageString.substring(Loc + 1);

		switch (type) {
			case 'b':
			case 'B':
				for (i = 0; i < Count; i++) {
					Loc = MessageString.indexOf(m_sMTX);
					if (Loc == -1)
						return false;
					subString = MessageString.substring(0, Loc);
					bVal = new Boolean(subString);
					msgList.add(bVal);
					MessageString = MessageString.substring(Loc + 1);
				}
				break;
			case 'i':
			case 'I':
				for (i = 0; i < Count; i++) {
					Loc = MessageString.indexOf(m_sMTX);
					if (Loc == -1)
						return false;
					subString = MessageString.substring(0, Loc);
					iVal = new Integer(subString);
					msgList.add(iVal);
					MessageString = MessageString.substring(Loc + 1);
				}
				break;
			case 'd':
			case 'D':
				for (i = 0; i < Count; i++) {
					Loc = MessageString.indexOf(m_sMTX);
					if (Loc == -1)
						return false;
					subString = MessageString.substring(0, Loc);
					dVal = new Double(subString);
					msgList.add(dVal);
					MessageString = MessageString.substring(Loc + 1);
				}
				break;
			case 's':
			case 'S':
				for (i = 0; i < Count; i++) {
					Loc = MessageString.indexOf(m_sMTX);
					if (Loc == -1)
						return false;
					subString = MessageString.substring(0, Loc);
					msgList.add(subString);
					MessageString = MessageString.substring(Loc + 1);
				}
				break;
			case 'l':
				for (i = 0; i < Count; i++) {
					SubMessage = new Message();
					if (SubMessage.setMessageRecursive(MessageString, RemainStringList) == false)
						return false;
					msgList.add(SubMessage);
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

	/**
	 * ErrorLog
	 */
	void errorLog(String Command, String Name) {
		StringBuffer Buffer = new StringBuffer();
		StringBuffer FilePathName = new StringBuffer();

		// get Current Directory
		String Path = System.getProperty("user.dir");
		String Separator = System.getProperty("file.separator");
		FilePathName.append(Path).append(Separator).append("MESSAGECLASS.ERROR.TXT");

		BufferedWriter out = null;

		try {
			// Format the current time.
			SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
			Date curTime = new Date();
			String dateString = format.format(curTime);

			Buffer.append(dateString).append(" ").append(Command).append(" ").append(Name).append(" ").append(toMessage());

			out = new BufferedWriter(new FileWriter(FilePathName.toString(), true));
			out.newLine();
			out.write(Buffer.toString());
			out.flush();
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
		// 2009.12.10 by MYM : File close를 반드시 하도록 finally로 처리
		finally {
			try {
				if (out != null) {
					out.close();
					out = null;
				}
			} catch (Exception e) {}
		}
	}
}
