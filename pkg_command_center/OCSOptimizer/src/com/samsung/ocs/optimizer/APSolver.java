package com.samsung.ocs.optimizer;

/**
 * APSolver Class, OCS 3.0 for Unified FAB
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

public class APSolver {
	private final int MAX_SIZE = 1000;
	private final double MAX_COST = 10E20;

	/**
	 * Constructor of APSolver class.
	 */
	public APSolver() {
	}

	/**
	 * Solve Assignment Problem
	 * 
	 * @param size int
	 * @param cost double[][]
	 * @param rowSol int[]
	 * @return true if successfully solved
	 */
	public boolean solveAssignment(int size, double[][] cost, int[] rowSol) {
		boolean result = true;
		int i, j;
		double totalCost = 0.0, tempCost = 0.0;
		double sum = 0.0, min = 10E20, mean = 0.0, max = 0.0, range = 0.0;

		if (size <= 0)
			return false;

		// Phase-I
		if (solveHungarianMethod(size, cost, rowSol) == false)
			return false;

		// Phase-II
		// adjust cost by mean of current opt. sol & range of all cost
		for (i=0; i<size; i++) {
			j = rowSol[i];

			// 2007.05.14 by PTE
			// 반드시 잡아야 하는 Job을 할당하기 위하여 해당 Job의 모든 Cost에 9999.0을 Job Assign에서 빼준다
			// 단 하지 말아야 할 작업은 빼지 말것!!!!
			// 하지만 이값으로 Phase-II의 AP를 풀면 분산이 큰 Job이 선정될 수 있어
			// 다시 원래의값으로 하여 평균을 구해야 함. Range는 영향 없음
			if (cost[i][j] != 0.0) {
				tempCost = cost[i][j];
				if (tempCost < 0.0)
					tempCost += 9999.0;

				sum += tempCost;
			}
		}

		// cal. mean of current opt. sol
		mean = sum/size;

		// cal. min & max
		for (i=0; i<size; i++) {
			for (j=0; j<size; j++) {
				if (cost[i][j] < min)
					min = cost[i][j];
				if (cost[i][j] > max)
					max = cost[i][j];
			}
		}

		// cal. Range of all cost
		range = max - min;

		if (range != 0.0) {
			// Adjust cost
			// 2007.05.14 by PTE
			// Cost를 변경하는 부분은 원래의 Cost에서 평균을 빼야 한다.
			// 그렇지 않으면 Job Swapping이 되지 않음

			for (i=0; i<size; i++)
				for (j=0; j<size; j++) {
					tempCost = cost[i][j];
					if (tempCost < 0.0)
						tempCost += 9999.0;

					cost[i][j] = cost[i][j] + Math.pow(tempCost-mean, 2.0)/Math.pow(range, 2.0);
				}
			// resolve AP
			if (solveHungarianMethod(size, cost, rowSol) == false)
				return false;
		}
		return result;
	}

	/**
	 * Solve AP (e.g. Hungarian Method)
	 */
	private boolean solveHungarianMethod(int size, double[][] cost, int[] rowSol) {
		boolean isUnassignedFound;
		int min, numFree = 0, previousNumFree, low, up, rowFree;
		int i, i1, j, j1, j2=0, f, k, endOfPath=0, last=0, loopCount = 0;
		int[] pred;
		int[] free;
		int[] colsol;
		int[] collist;
		int[] matches;
		double dMin=0.0, dUmin, dUsubmin, h, v2;
		double[] d;
		double[] u;
		double[] v;

		free = new int[size];       // unassigned rows lists.
		colsol = new int[size];     // row assigned to column
		collist = new int[size];
		matches = new int[size];
		pred = new int[size];
		u = new double[size];       // dual variables, row reduction numbers
		v = new double[size];       // dual variables, column reduction numbers
		d = new double[size];       // cost-distance

		// 초기화
		for (i=0; i<size; i++)
			matches[i] = 0;

		// Column Reduction
		for (j=size-1; j>=0; j--) {
			// find minimum cost
			dMin = cost[0][j];
			min = 0;
			for (i=1; i<size; i++) {
				if (cost[i][j] < dMin) {
					dMin = cost[i][j];
					min = i;
				}
			}
			v[j] = dMin;

			if (++matches[min] == 1) {
				rowSol[min] = j;
				colsol[j] = min;
			} else
				colsol[j] = -1;
		}

		// Reduction Transfer
		for (i=0; i<size; i++) {
			if (matches[i] == 0)     // fill list of unassigned 'free' rows.
				free[numFree++] = i;
			else {
				if (matches[i] == 1) {
					j1 = rowSol[i];
					dMin = MAX_COST;
					for (j=0; j<size; j++) {
						if (j != j1) {
							if (cost[i][j] - v[j] < dMin)
								dMin = cost[i][j] - v[j];
						}
					}
					v[j1] = v[j1] - dMin;
				}
			}
		}

		// Augmenting Reduction
		do {
			loopCount++;

			// scan all free rows.
			k = 0;
			previousNumFree = numFree;
			numFree = 0;
			while (k < previousNumFree) {
				i = free[k];
				k++;

				// find minimum and second minimum reduced cost over columns.
				dUmin = cost[i][0] - v[0];
				j1 = 0;
				dUsubmin = MAX_COST;
				for (j=1; j<size; j++) {
					h = cost[i][j] - v[j];
					if (h < dUsubmin) {
						if (h >= dUmin) {
							dUsubmin = h;
							j2 = j;
						} else {
							dUsubmin = dUmin;
							dUmin = h;
							j2 = j1;
							j1 = j;
						}
					}
				}

				i1 = colsol[j1];
				if (dUmin < dUsubmin) {
					// reduced cost in the row to the subminimum.
					v[j1] = v[j1] - (dUsubmin - dUmin);
				} else {
					// minimum and subminimum equal. 
					if (i1 >= 0) {
						// swap columns j1 and j2
						j1 = j2;
						i1 = colsol[j2];
					}
				}

				rowSol[i] = j1;
				colsol[j1] = i;

				if (i1 >= 0) {
					// 2006.11.20. by P.T.E
					//if ((dUmin < dUsubmin) && (Math.abs(dUmin-dUsubmin) >= 0.0001))
					// 2007.03.16 revised by P.T.E
					if ((dUmin < dUsubmin) && ((dUmin-dUsubmin) >= 0.0001)) {
						free[--k] = i1;
					} else {
						free[numFree++] = i1;
					}
				}
			}
		}
		while (loopCount < 2);

		// Augment solution for each free row.
		for (f=0; f<numFree; f++) {
			rowFree = free[f];

			// Dijkstra shortest path algorithm.
			for (j=0; j<size; j++) {
				d[j] = cost[rowFree][j] - v[j];
				pred[j] = rowFree;
				collist[j] = j;
			}

			low = 0;
			up = 0;

			isUnassignedFound = false;
			do {
				if (up == low) {
					last = low - 1;
					dMin = d[collist[up++]];

					for (k=up; k<size; k++) {
						j = collist[k];
						h = d[j];
						if (h <= dMin) {
							if (h < dMin) {
								// new minimum.
								up = low;
								dMin = h;
							}
							collist[k] = collist[up];
							collist[up++] = j;
						}
					}

					// check if any of the minimum columns happens to be unassigned.
					for (k=low; k<up; k++) {
						if (colsol[collist[k]] < 0) {
							endOfPath = collist[k];
							isUnassignedFound = true;
							break;
						}
					}
				}

				if (!isUnassignedFound) {
					j1 = collist[low];
					low++;
					i = colsol[j1];
					h = cost[i][j1] - v[j1] - dMin;

					for (k=up; k<size; k++) {
						j = collist[k];
						v2 = cost[i][j] - v[j] - h;
						if (v2 < d[j]) {
							pred[j] = i;
							if (v2 == dMin) {
								if (colsol[j] < 0) {
									// if unassigned, shortest augmenting path is complete.
									endOfPath = j;
									isUnassignedFound = true;
									break;
								} else {
									collist[k] = collist[up];
									collist[up++] = j;
								}
							}
							d[j] = v2;
						}
					}
				}
			}
			while (!isUnassignedFound);

			// update column costs.
			for (k=0; k<=last; k++) {
				j1 = collist[k];
				v[j1] = v[j1] + d[j1] - dMin;
			}

			// reset row and column assignments along the alternating path.
			do {
				i = pred[endOfPath];
				colsol[endOfPath] = i;
				j1 = endOfPath;
				endOfPath = rowSol[i];
				rowSol[i] = j1;
			}
			while (i != rowFree);
		}

		// calculate optimal cost.
		double totalCost = 0.0;
		for (i=0; i<size; i++) {
			j = rowSol[i];
			u[i] = cost[i][j] - v[j];
			totalCost = totalCost + cost[i][j];
		}
		return true;
	}
}

