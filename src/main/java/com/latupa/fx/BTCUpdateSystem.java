package com.latupa.stock;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BTCUpdateSystem {
	public static final Log log = LogFactory.getLog(BTCUpdateSystem.class);
	
	//每个时间间隔分别对应BTCData<seconds, BTCData>
	public HashMap<Integer, BTCData> data_map = new HashMap<Integer, BTCData>();
	
	//数据采集周期(s)
	public int fetch_cycle;
	
	public BTCUpdateSystem(ArrayList<Integer> data_cycle_list, int fetch_cycle) {
		for (int data_cycle : data_cycle_list) {
			this.data_map.put(data_cycle, new BTCData(data_cycle));
		}
		
		this.fetch_cycle = fetch_cycle;
	}
	
	public void Route() {
		
		BTCUpdateThread update_thread = new BTCUpdateThread(this);
		update_thread.start();
		
		//每个K线周期独立线程更新
		for (int data_cycle : this.data_map.keySet()) {
			BTCData btc_data = this.data_map.get(data_cycle);
			
			BTCDataProcThread data_proc_thread = new BTCDataProcThread(btc_data, data_cycle);
			//加载数据库中的历史数据到内存中
			log.info("load data from db for cycle " + data_cycle);
			data_proc_thread.btc_data.BTCDataLoadFromDB(300, null);
			log.info("load finish");
			data_proc_thread.start();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArrayList<Integer> alist = new ArrayList<Integer>();
		alist.add(60);
		alist.add(120);
		alist.add(300);
		alist.add(600);
		
		BTCUpdateSystem btc_us = new BTCUpdateSystem(alist, 10);
		btc_us.Route();
	}

}
