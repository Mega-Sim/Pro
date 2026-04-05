package com.samsung.ocs.common.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.constant.OcsConstant;

public class CarrierTypeConfig {
	private static final String ITEM = "ITEM";
	private static final String MATERIAL = "MATERIAL";
	private static final String CARRIER_TYPE = "CARRIER_TYPE";
	
	private static final String FOUP = "Foup";
	private static final String RETICLE = "Reticle";
	private static final String FOSB = "Fosb";
	private static final String MAC = "Mac";
	
	private static final String CST = "CST";
	private static final String MZ = "MZ";
	private static final String SSD = "SSD";
	private static final String TRAY = "TRAY";
	
	private String homePath = "";
	private String fileSeparator = File.separator;
	
	private static CarrierTypeConfig config = null;
	private HashMap<String, Integer> carrierTypeMap = new HashMap<String, Integer>();
	private HashMap<Integer, String> materialTypeMap = new HashMap<Integer, String>();

	private CarrierTypeConfig() throws JDOMException, IOException {
		homePath = System.getProperty(OcsConstant.HOMEDIR);
		fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		loadCarrierTypeConfig();
	}
	
	public synchronized static CarrierTypeConfig getInstance() {
		if (config == null) {
			try {
				config = new CarrierTypeConfig();
			} catch (JDOMException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return config;
	}
	
	public void loadCarrierTypeConfig() {
		String configFile = homePath + fileSeparator + "CarrierTypeConfig.xml";
		
		// file reading from xml
		String material;
		String carrierType;
		
		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		
		try {
			carrierTypeMap.clear();
			materialTypeMap.clear();
			
			doc = saxb.build(configFile);
			Element root = doc.getRootElement();
			@SuppressWarnings("unchecked")
			List<Element> list = root.getChildren(ITEM);
			for (Element element : list) {
				material = element.getChildTextTrim(MATERIAL);
				carrierType = element.getChildTextTrim(CARRIER_TYPE);
				if (material != null && material.length() > 0 
						&& carrierType != null && carrierType.length() > 0) {
					try {
						carrierTypeMap.put(material, new Integer(carrierType));
						materialTypeMap.put(new Integer(carrierType), material);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public int getCarrierType(String material) {
		Integer carrierType = carrierTypeMap.get(material);
		if (carrierType != null) {
			return carrierType.intValue();
		} else {
			// 0 = FOUP, 1 = POD, 3 = MAC, 4 = FOSB 
			if (FOUP.equalsIgnoreCase(material)) {
				return 0;
			} else if (RETICLE.equalsIgnoreCase(material)) {
				return 1;
			} else if (MAC.equalsIgnoreCase(material)) {
				return 3;
			} else if (FOSB.equalsIgnoreCase(material)) {
				return 4;
			} else if (CST.equalsIgnoreCase(material)) {
				return 6;
			} else if (MZ.equalsIgnoreCase(material)) {
				return 7;
			} else if (SSD.equalsIgnoreCase(material)) {
				return 8;
			} else if (TRAY.equalsIgnoreCase(material)) {
				return 9;
			}
		}
		return 100;
	}
	
	public String getMaterialType(int carrierType) {
		String material = materialTypeMap.get(carrierType);
		if (material != null) {
			return material;
		} else {
			// 0 = FOUP, 1 = RETICLE, 3 = MAC, 4 = FOSB 
			if (0 == carrierType) {
				return "Foup";
			} else if (1 == carrierType) {
				return "Reticle";
			} else if (3 == carrierType) {
				return "Mac";
			} else if (4 == carrierType) {
				return "Fosb";
			} else if (6 == carrierType) {
				return "CST";
			} else if (7 == carrierType) {
				return "MZ";
			} else if (8 == carrierType) {
				return "SSD";
			} else if (9 == carrierType) {
				return "TRAY";
			}
			return ""; 
		}
	}
}
