package com.samsung.ocs.manager.impl.model;

import java.util.HashSet;

import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;

/**
 * Link Class, OCS 3.0 for Unified FAB
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

public class Link {
	protected String linkId;
	protected String type;
	protected Node fromNode;
	protected Node toNode;
	protected String theta1;
	protected String theta2;
	protected String direction;
	protected String align;
	protected boolean enabled;
	protected Section section;
	protected char steerPosition;
	// 2015.07.08 by KYK
	protected double speed;
	protected double distance;
	
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public String getLinkId() {
		return linkId;
	}
	public void setLinkId(String linkId) {
		this.linkId = linkId;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Node getFromNode() {
		return fromNode;
	}
	public void setFromNode(Node fromNode) {
		this.fromNode = fromNode;
	}
	public Node getToNode() {
		return toNode;
	}
	public void setToNode(Node toNode) {
		this.toNode = toNode;
	}
	public String getTheta1() {
		return theta1;
	}
	public void setTheta1(String theta1) {
		this.theta1 = theta1;
	}
	public String getTheta2() {
		return theta2;
	}
	public void setTheta2(String theta2) {
		this.theta2 = theta2;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	public String getAlign() {
		return align;
	}
	public void setAlign(String align) {
		this.align = align;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public Section getSection() {
		return section;
	}
	public void setSection(Section section) {
		this.section = section;
	}
	public char getSteerPosition() {
		return steerPosition;
	}
	public void setSteerPosition(char steerPosition) {
		this.steerPosition = steerPosition;
	}
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 */
	public void checkRepathSearch() {
		toNode.checkRepathSearch(fromNode);
	}
	/**
	 * 2014.10.11 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public void releaseAbnormalSection() {
		// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
//		section.removeAbnormalItem(this);
		HashSet<Section> detourSectionSet = new HashSet<Section>();
		detourSectionSet.addAll(section.getDetourSectionSet());
		for (Section section : detourSectionSet) {
			section.removeAbnormalItem(this);
		}
	}
	/**
	 * 2014.10.11 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public void setAbnormalSection() {
		// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
//		section.addAbnormalItem(this, true, DETOUR_REASON.LINK_DISABLED);
		HashSet<Section> detourSectionSet = new HashSet<Section>();
		detourSectionSet.addAll(section.getDetourSectionSet());
		for (Section section : detourSectionSet) {
			section.addAbnormalItem(this, DETOUR_REASON.LINK_DISABLED);
		}
	}
	public String toString() {
		return fromNode.getNodeId() + "_" + toNode.getNodeId();
	}
}
