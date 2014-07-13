package com.latupa.fx;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 数据抓取线程
 * @author latupa
 *
 */
public class BTCUpdateThread extends Thread {
	
	public static final Log log = LogFactory.getLog(BTCUpdateThread.class);
	
	public BTCUpdateSystem btc_update_sys;
	
	public BTCUpdateThread(BTCUpdateSystem btc_update_sys) {
		this.btc_update_sys	= btc_update_sys;
	}
	
	private boolean CheckDataUpdate(HashMap<String, Ticker> last_ticker_map, HashMap<String, Ticker> ticker_map) {
		for (String pair : last_ticker_map.keySet()) {
			if (!last_ticker_map.get(pair).time.equals(ticker_map.get(pair).time)) {
				return true;
			}
		}
		
		log.info("time is same.");
		
		return false;
	}

	public void run() {
		
		log.info("BTCUpdateThread start");
		
		long stamp_millis = System.currentTimeMillis();
		long stamp_sec = stamp_millis / 1000;
		
		//同步时间到分钟整点
		while (stamp_sec % 60 != 0) {
			log.info("sync for minute, now is " + stamp_sec);
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stamp_millis = System.currentTimeMillis();
			stamp_sec = stamp_millis / 1000;
		}
		
		long last_stamp_sec = 0;
		HashMap<String, Ticker> last_ticker_map = null;
		
		while (true) {
			
			stamp_millis = System.currentTimeMillis();
			stamp_sec = stamp_millis / 1000;
			if (stamp_sec >= (last_stamp_sec + this.btc_update_sys.fetch_cycle)) {
				log.info("fetch:" + stamp_sec);
				
				HashMap<String, Ticker> ticker_map = this.btc_update_sys.btc_api.ApiTicker();
				if (ticker_map == null ||
						(last_ticker_map != null && CheckDataUpdate(last_ticker_map, ticker_map) == false)) {
					log.info("sleep 30 sec");
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						log.error("sleep exception", e);
					}
				}
				else {
					log.info("update prices");
					for (int data_cycle : this.btc_update_sys.data_map.keySet()) {
						BTCData btc_data = this.btc_update_sys.data_map.get(data_cycle);
						btc_data.BTCSliceRecordUpdate(ticker_map);
					}
				}
				
				last_stamp_sec = stamp_sec;
				last_ticker_map = ticker_map;
				log.info("last_stamp_sec:" + last_stamp_sec);
			}
			else {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
