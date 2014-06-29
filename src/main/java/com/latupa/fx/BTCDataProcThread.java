package com.latupa.stock;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 针对抓取到的数据构造K线，同时计算指标值，更新到内存和数据库中
 * @author latupa
 *
 */
public class BTCDataProcThread extends Thread {
	public static final Log log = LogFactory.getLog(BTCDataProcThread.class);
	
	public BTCData btc_data;
	public int data_cycle;
	
	public BTCFunc btc_func = new BTCFunc();
	
	public BTCDataProcThread(BTCData btc_data, int data_cycle) {
		this.btc_data	= btc_data;
		this.data_cycle	= data_cycle;
	}
	
	/**
	 * 补历史数据用
	 * @param time_s 起始时间
	 * @param time_s 结束时间
	 */
	public void ModifyHistoryData(String time_s, String time_e) {
		//加载数据库中的历史数据到内存中
		log.info("load data from db for cycle " + this.data_cycle);
		this.btc_data.BTCDataLoadFromDB(0, null);
		log.info("load finish");
		
		for (String time : this.btc_data.b_record_map.keySet().toArray(new String[0])) {
			if (time.compareTo(time_s) >= 0 && time.compareTo(time_e) <= 0) {
				log.info("proc " + time);
				CalcFunc(time);
			}
			
			if (time.compareTo(time_e) > 0) {
				break;
			}
		}
		
		log.info("modify finish");
	}
	
	/**
	 * 每条更新的价格记录，计算指标
	 * @param sDateTime
	 */
	private void CalcFunc(String sDateTime) {
		
		//计算均线
		MaRet ma_ret = this.btc_data.BTCCalcMa(this.btc_func, sDateTime);
		this.btc_data.BTCMaRetMemUpdate(sDateTime, ma_ret);
		this.btc_data.BTCMaRetDBUpdate(sDateTime);
		
		//计算布林线
		BollRet boll_ret = this.btc_data.BTCCalcBoll(this.btc_func, sDateTime);
		this.btc_data.BTCBollRetMemUpdate(sDateTime, boll_ret);
		this.btc_data.BTCBollRetDBUpdate(sDateTime);
		
		//计算Macd值
		try {
			MacdRet macd_ret = this.btc_data.BTCCalcMacd(this.btc_func, sDateTime, data_cycle);
			this.btc_data.BTCMacdRetMemUpdate(sDateTime, macd_ret);
			this.btc_data.BTCMacdRetDBUpdate(sDateTime);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.btc_data.BTCDBComplete(sDateTime);
	}
	
	public void run() {
		
		log.info("data proc for " + this.data_cycle);
		
		DateFormat df	= new SimpleDateFormat("yyyyMMddHHmmss"); 
		
		long stamp_millis;
		long stamp_sec;
		
		long last_stamp_sec = 0;
		
		while (true) {
			
			stamp_millis = System.currentTimeMillis();
			stamp_sec = stamp_millis / 1000;
			
			//防止同一秒钟内处理多次
			if (stamp_sec == last_stamp_sec) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			
			if (stamp_sec % (24 * 60 * 60) == 0) {	//每天清理一次内存中的历史数据
				log.info("clean cycle " + this.data_cycle);
				
				this.btc_data.BTCDataMemoryClean(2);
			}
			
			if (stamp_sec % this.data_cycle == 0) {	
				
				//等待1s，让数据抓取完成该周期最后的一次数据获取
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				log.info("trigger cycle " + this.data_cycle);
				
				BTCDSliceRecord slice_record = this.btc_data.btc_s_record;
				
				if (slice_record.init_flag == false) {
				
					log.info("update cycle " + this.data_cycle);
					slice_record.Show();

					int curt_k_num = btc_data.b_record_map.size();
					
					log.info("new k(" + this.data_cycle + "):" + curt_k_num);
					
					Date cur_date = new Date(stamp_millis);
					String sDateTime = df.format(cur_date); 
					
					//更新基础数值
					this.btc_data.BTCRecordMemInsert(sDateTime);
					this.btc_data.BTCRecordDBInsert(sDateTime);
					this.btc_data.BTCSliceRecordInit();
					//this.btc_trans_sys.btc_data.BTCRecordMemShow();
					
					CalcFunc(sDateTime);
				}
			}
			
			last_stamp_sec = stamp_sec;
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//处理更新历史数据
		if (args.length != 3) {
			System.out.println("usage: time_s(yyyymmddHHMMSS) time_e(yyyymmddHHMMSS) data_cycle");
			System.exit(0);
		}
		
		String time_s = args[0];
		String time_e = args[1];
		int data_cycle = Integer.parseInt(args[2]);
		
		System.out.println("time_s:" + time_s + ", time_e:" + time_e + ", data_cycle:" + data_cycle);
		
		BTCData btc_data = new BTCData(data_cycle);
		BTCDataProcThread btc_data_proc = new BTCDataProcThread(btc_data, data_cycle);
		btc_data_proc.ModifyHistoryData(time_s, time_e);
	}
}
