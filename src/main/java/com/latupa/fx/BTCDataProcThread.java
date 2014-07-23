package com.latupa.fx;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

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
	 * @throws ParseException 
	 * @throws UnsupportedEncodingException 
	 * @throws InterruptedException 
	 */
	public void HistoryData(String time_s, String time_e, String cycle) throws ParseException, UnsupportedEncodingException, InterruptedException {
		
		log.info("start:" + time_s + ", end:" + time_e + ", cycle:" + cycle);
		
		SimpleDateFormat sdf_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf_1.setTimeZone(TimeZone.getTimeZone("GMT+8"));
		
		SimpleDateFormat sdf_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		sdf_2.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		
		Date date_s = new Date();
		date_s = sdf_1.parse(time_s);
		
		Date date_e = new Date();
		date_e = sdf_1.parse(time_e);
		
		long stamp = date_s.getTime() / 1000 + 7 * 24 * 60 * 60;
        Date date_tmp = new Date(stamp * 1000);  //原始时间
        
        Date date_last = date_s;
        while (date_tmp.before(date_e)) {
        	
        	log.info("start:" + sdf_1.format(date_last) + ", end:" + sdf_1.format(date_tmp));
        	
        	//call URLEncoder.encode(sdf_2.format(date_last) URLEncoder.encode(sdf_2.format(date_tmp), "utf-8")
        	date_last = date_tmp;
        	
        	stamp = date_tmp.getTime() / 1000 + 7 * 24 * 60 * 60;
        	date_tmp = new Date(stamp * 1000);
        	
        	Thread.sleep(3000);
        }
        
        log.info("start:" + sdf_1.format(date_last) + ", end:" + sdf_1.format(date_e));
        //call URLEncoder.encode(sdf_2.format(date_last) URLEncoder.encode(sdf_2.format(date_e), "utf-8")
        
		//加载数据库中的历史数据到内存中
//		log.info("load data from db for cycle " + this.data_cycle);
//		this.btc_data.BTCDataLoadFromDB(0, null);
//		log.info("load finish");
//		
//		for (String pair : this.btc_data.b_record_map_map.keySet()) {
//			
//			for (String time : this.btc_data.b_record_map_map.get(pair).keySet().toArray(new String[0])) {
//				if (time.compareTo(time_s) >= 0 && time.compareTo(time_e) <= 0) {
//					log.info("proc " + time);
//					CalcFunc(time, pair);
//				}
//				
//				if (time.compareTo(time_e) > 0) {
//					break;
//				}
//			}
//		}
//		
//		log.info("modify finish");
	}
	
	/**
	 * 每条更新的价格记录，计算指标
	 * @param sDateTime
	 */
	private void CalcFunc(String sDateTime, String pair) {
		
		//计算均线
		MaRet ma_ret = this.btc_data.BTCCalcMa(this.btc_func, sDateTime, pair);
		this.btc_data.BTCMaRetMemUpdate(sDateTime, ma_ret, pair);
		this.btc_data.BTCMaRetDBUpdate(sDateTime, pair);
		
		//计算布林线
		BollRet boll_ret = this.btc_data.BTCCalcBoll(this.btc_func, sDateTime, pair);
		this.btc_data.BTCBollRetMemUpdate(sDateTime, boll_ret, pair);
		this.btc_data.BTCBollRetDBUpdate(sDateTime, pair);
		
		//计算Macd值
		try {
			MacdRet macd_ret = this.btc_data.BTCCalcMacd(this.btc_func, sDateTime, data_cycle, pair);
			this.btc_data.BTCMacdRetMemUpdate(sDateTime, macd_ret, pair);
			this.btc_data.BTCMacdRetDBUpdate(sDateTime, pair);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.btc_data.BTCDBComplete(sDateTime, pair);
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
				
				
				for (String pair : this.btc_data.btc_s_record_map.keySet()) {
					log.info("clean cycle " + this.data_cycle + ", pair:" + pair);
					this.btc_data.BTCDataMemoryClean(2, pair);
				}
			}
			
			if (stamp_sec % this.data_cycle == 0) {	
				
				//等待1s，让数据抓取完成该周期最后的一次数据获取
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				for (String pair : this.btc_data.btc_s_record_map.keySet()) {
					
					log.info("trigger cycle:" + this.data_cycle + ", pair:" + pair);
					
					BTCDSliceRecord slice_record = this.btc_data.btc_s_record_map.get(pair);
				
					if (slice_record.init_flag == false) {
					
						log.info("update cycle " + this.data_cycle + ", pair:" + pair);
						slice_record.Show();
		
						int curt_k_num = this.btc_data.b_record_map_map.get(pair).size();
						
						log.info("new k(" + this.data_cycle + "):" + curt_k_num);
						
						Date cur_date = new Date(stamp_millis);
						String sDateTime = df.format(cur_date); 
						
						//更新基础数值
						this.btc_data.BTCRecordMemInsert(sDateTime, pair);
						this.btc_data.BTCRecordDBInsert(sDateTime, pair);
						this.btc_data.BTCSliceRecordInit(pair);
						//this.btc_trans_sys.btc_data.BTCRecordMemShow();
						
						CalcFunc(sDateTime, pair);
					}
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
			System.out.println("usage: time_s(2014-07-01 10:10:00) time_e(2014-07-19 10:50:00) M5");
			System.exit(0);
		}
		
		String time_s = args[0];
		String time_e = args[1];
		String cycle = args[2];
		
		System.out.println("time_s:" + time_s + ", time_e:" + time_e + ", data_cycle:" + cycle);
		ArrayList<String> pairs_list = new ArrayList<String>();
		pairs_list.add("EUR_USD");
		BTCData btc_data = new BTCData(300, pairs_list);
		BTCDataProcThread btc_data_proc = new BTCDataProcThread(btc_data, 300);
		try {
			btc_data_proc.HistoryData(time_s, time_e, cycle);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
