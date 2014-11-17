package com.latupa.fx;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
	public int data_cycle;//单位分钟
	public BTCApi btc_api = new BTCApi();
	
	public BTCFunc btc_func = new BTCFunc();
	
	public BTCDataProcThread(BTCData btc_data, int data_cycle) {
		this.btc_data	= btc_data;
		this.data_cycle	= data_cycle;
	}
	
	/**
	 * 通过数据文件补股票的历史数据
	 * @param file
	 * @param time_s
	 * @param cycle
	 * @param is_clean
	 * @param pair
	 * @throws ParseException
	 * @throws UnsupportedEncodingException
	 * @throws InterruptedException
	 */
	public void LoadHistoryDataForStock(String file, String time_s, String cycle, int cycle_int, boolean is_clean, String pair) throws ParseException, UnsupportedEncodingException, InterruptedException {
		
		log.info("file:" + file + ", start:" + time_s + ", cycle:" + cycle + ", cycle_int:" + cycle_int + ", is_clean:" + is_clean + ", pair:" + pair);
		
		if (is_clean == true) {
			this.btc_data.BTCDataCleanDB(pair);
		}
		else {
			this.btc_data.BTCDataLoadFromDB(1000, time_s);
		}
		
		try {
			InputStream fis			= BTCApi.class.getClassLoader().getResourceAsStream(file);
	        InputStreamReader isr	= new InputStreamReader(fis, "utf8");
	        BufferedReader br		= new BufferedReader(isr);
	        
	        //数据文件中的起始时间
	        SimpleDateFormat file_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        Date date_s = new Date();
    		date_s = file_sdf.parse(time_s);
    		long stamp = date_s.getTime() / 1000;
    		
    		//上午开始时间0930，上午结束时间1130，下午开始时间1300，下午结束时间1500
    		final String morning_open	= "09:30";
    		final String morning_close	= "11:30";
    		final String afternoon_open	= "13:00";
    		final String afternoon_close	= "15:00";
	        
	        String line = null;
	        
			while ((line = br.readLine()) != null) {
				
	        	String items[] = line.split(",");
	        		
	        	String date = items[0].replace("/", "-");
	        	String time = items[1];
	        	
//	        	System.out.println(date + "," +time);
	        	
	        	BTCBasicRecord cycle_5_k = new BTCBasicRecord();
	        	cycle_5_k.open = Double.parseDouble(items[2]);
	        	cycle_5_k.high = Double.parseDouble(items[3]);
	        	cycle_5_k.low	= Double.parseDouble(items[4]);
	        	cycle_5_k.close	= Double.parseDouble(items[5]);
	        	
	        	String date_str = date + " " + time + ":00";
	        	Date date_5_k = new Date();
	    		date_5_k = file_sdf.parse(date_str);
	    		long stamp_5_k = date_5_k.getTime() / 1000;
	    		
	    		HashMap<String, BTCBasicRecord> pair_map = new HashMap<String, BTCBasicRecord>();
				pair_map.put(pair, cycle_5_k);
	    		this.btc_data.BTCSliceHistoryStockUpdate(pair_map);
	    		
//	    		System.out.println(stamp_5_k + ", " + stamp + ", " + (stamp_5_k - stamp));
	    		
	    		//如果有几天的数据跳空，那么在这里时间窗口向后移动
	    		while (stamp_5_k - stamp > 24 * 60 * 60) {
	    			stamp += 24 * 60 * 60;
	    		}
	    		
	    		//指定的周期到了
	    		if ((stamp_5_k - stamp) == cycle_int * 60) {
	    			
	    			String sDateTime = date_str; 
	    			log.info("load " + sDateTime);
	    			
	    			//更新基础数值
	    			this.btc_data.BTCRecordMemInsert(sDateTime, pair);
	    			this.btc_data.BTCRecordDBInsert(sDateTime, pair);
	    			this.btc_data.BTCSliceRecordInit(pair);
	    			//this.btc_trans_sys.btc_data.BTCRecordMemShow();
	    			
	    			CalcFunc(sDateTime, pair);
	    			
	    			if (time.equals(morning_close)) {
	    	        	Date date_tmp = new Date();
	    	    		date_tmp = file_sdf.parse(date + " " + afternoon_open + ":00");
	    	    		stamp = date_tmp.getTime() / 1000;
	    			}
	    			else if (time.equals(afternoon_close)) {
	    				Date date_tmp = new Date();
	    	    		date_tmp = file_sdf.parse(date + " " + morning_open + ":00");
	    	    		stamp = date_tmp.getTime() / 1000 + 24 * 60 * 60;
	    			}
	    			else {
	    				stamp = stamp_5_k;
	    			}
	    		}
	        }
	        
	        br.close();
	        isr.close();
	        fis.close();
		}
		catch (Exception e) {
			log.error("read " + file + " failed!", e);
			return;
		}
        
		log.info("load finish");
	}
	
	/**
	 * 补外汇的历史数据用
	 * @param time_s 起始时间
	 * @param time_s 结束时间
	 * @throws ParseException 
	 * @throws UnsupportedEncodingException 
	 * @throws InterruptedException 
	 */
	public void LoadHistoryData(String time_s, String time_e, String cycle, boolean is_clean, String pair) throws ParseException, UnsupportedEncodingException, InterruptedException {
		
		log.info("start:" + time_s + ", end:" + time_e + ", cycle:" + cycle + ", is_clean:" + is_clean + ", pair:" + pair);
		
		if (is_clean == true) {
			this.btc_data.BTCDataCleanDB(pair);
		}
		else {
			this.btc_data.BTCDataLoadFromDB(1000, time_s);
		}
		
		SimpleDateFormat sdf_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf_1.setTimeZone(TimeZone.getTimeZone("GMT+8"));
		
		Date date_s = new Date();
		date_s = sdf_1.parse(time_s);
		
		Date date_e = new Date();
		date_e = sdf_1.parse(time_e);
		
		long stamp = date_s.getTime() / 1000 + 7 * 24 * 60 * 60;
        Date date_tmp = new Date(stamp * 1000);  //原始时间
        
        Date date_last = date_s;
        while (date_tmp.before(date_e)) {
        	
        	log.info("start:" + sdf_1.format(date_last) + ", end:" + sdf_1.format(date_tmp));
        	
        	//call URLEncoder.encode(sdf_2.format(date_last), "utf-8") URLEncoder.encode(sdf_2.format(date_tmp), "utf-8")
        	UpdateHistoryData(cycle, pair, date_last, date_tmp);
        	date_last = date_tmp;
        	
        	stamp = date_tmp.getTime() / 1000 + 7 * 24 * 60 * 60;
        	date_tmp = new Date(stamp * 1000);
        	
        	Thread.sleep(3000);
        }
        
        log.info("start:" + sdf_1.format(date_last) + ", end:" + sdf_1.format(date_e));
        UpdateHistoryData(cycle, pair, date_last, date_e);
        

		log.info("load finish");
	}

	/**
	 * 通过外汇接口获取历史数据
	 * @param cycle
	 * @param pair
	 * @param date_s
	 * @param date_e
	 * @throws ParseException
	 * @throws UnsupportedEncodingException
	 */
	private void UpdateHistoryData(String cycle, String pair,
			Date date_s, Date date_e)
			throws ParseException, UnsupportedEncodingException {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		
		ArrayList<HistoryTicker> ticker_list = this.btc_api.ApiHistoryTicker(pair, 
				URLEncoder.encode(sdf.format(date_s), "utf-8"), 
				URLEncoder.encode(sdf.format(date_e), "utf-8"), 
				cycle);
		
		for (int i = 0; i < ticker_list.size(); i++) {
			HistoryTicker ticker = ticker_list.get(i);
			HashMap<String, HistoryTicker> pair_map = new HashMap<String, HistoryTicker>();
			pair_map.put(pair, ticker);
			
			this.btc_data.BTCSliceHistoryRecordUpdate(pair_map);
			
			String sDateTime = ticker.time; 
			
			//更新基础数值
			this.btc_data.BTCRecordMemInsert(sDateTime, pair);
			this.btc_data.BTCRecordDBInsert(sDateTime, pair);
			this.btc_data.BTCSliceRecordInit(pair);
			//this.btc_trans_sys.btc_data.BTCRecordMemShow();
			
			CalcFunc(sDateTime, pair);
		}
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
			MacdRet macd_ret = this.btc_data.BTCCalcMacd(this.btc_func, sDateTime, pair);
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

		//K线周期（分钟）
		HashMap<String, Integer> cycle_to_int = new HashMap<String, Integer>();
		cycle_to_int.put("M5", 5);
		cycle_to_int.put("M10", 10);
		cycle_to_int.put("H1", 60);
		cycle_to_int.put("H2", 120);
		cycle_to_int.put("D1", 330);  //A股一天开盘4个小时，加上中间休息的1个半小时，总共5个半小时
		
		//处理更新历史数据
		if (args.length != 5) {
			System.out.println("usage: file time_s(2014-07-01 09:30:00) H1 clean pair(market_code)");
			System.exit(0);
		}
		
		String file		= args[0];
		String time_s	= args[1];
		String cycle	= args[2];
		boolean is_clean	= Boolean.parseBoolean(args[3]);
		String pair		= args[4];
		
		System.out.println("file:" + file + ", time_s:" + time_s + ", data_cycle:" + cycle + ", is_clean:" + is_clean + ", pair:" + pair);
		ArrayList<String> pairs_list = new ArrayList<String>();
		pairs_list.add(pair);
		BTCData btc_data = new BTCData(cycle, pairs_list);
		BTCDataProcThread btc_data_proc = new BTCDataProcThread(btc_data, cycle_to_int.get(cycle));
		try {
			btc_data_proc.LoadHistoryDataForStock(file, time_s, cycle, cycle_to_int.get(cycle), is_clean, pair);
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
