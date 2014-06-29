package com.latupa.stock;

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
	
	public BTCApi btc_api = new BTCApi();
	
	public BTCUpdateThread(BTCUpdateSystem btc_update_sys) {
		this.btc_update_sys	= btc_update_sys;
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
		
		while (true) {
			
			stamp_millis = System.currentTimeMillis();
			stamp_sec = stamp_millis / 1000;
			if (stamp_sec >= (last_stamp_sec + this.btc_update_sys.fetch_cycle)) {
				log.info("fetch:" + stamp_sec);
				
				Ticker ticker = btc_api.ApiTicker();
				if (ticker == null) {
					log.info("sleep 30 sec");
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					double last = ticker.last;
					for (int data_cycle : this.btc_update_sys.data_map.keySet()) {
						BTCData btc_data = this.btc_update_sys.data_map.get(data_cycle);
						btc_data.BTCSliceRecordUpdate(last);
					}
				}
				
				last_stamp_sec = stamp_sec;
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
