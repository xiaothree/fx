package com.latupa.stock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class BTCBasicRecord {
	public static final Log log = LogFactory.getLog(BTCBasicRecord.class);
	
	double high;
	double low;
	double open;
	double close;
	
	public BTCBasicRecord() {
		
	}
	
	public BTCBasicRecord(BTCBasicRecord record) {
		this.high	= record.high;
		this.low	= record.low;
		this.open	= record.open;
		this.close	= record.close;
	}
	
	public void Show() {
		DecimalFormat df = new DecimalFormat("#0.00");
		log.info("open:" + this.open + ", " +
				"close:" + this.close +", " +
				"high:" + this.high + ", " +
				"^" + String.valueOf(df.format((this.high - this.open) / this.open * 100)) + "%, " +
				"low:" + this.low + ", " +
				"V" + String.valueOf(df.format((this.open - this.low) / this.open * 100)) + "%,"
				);
	}
}

class BTCDSliceRecord extends BTCBasicRecord {
	boolean init_flag;
}


class BTCTotalRecord extends BTCBasicRecord {
	MaRet ma_record		= new MaRet();
	BollRet boll_record	= new BollRet();
	MacdRet macd_record	= new MacdRet();
	
	public BTCTotalRecord(BTCBasicRecord record) {
		this.high	= record.high;
		this.low	= record.low;
		this.open	= record.open;
		this.close	= record.close;
	}
	
	public BTCTotalRecord() {
		
	}
	
	public String toString() {
		String str = "TotalRecord {" + 
				"BasicRecord { " + "high:" + this.high + ", low:" + this.low + ", open:" + this.open + ", close:" + this.close + "}, " +
				"MaRet {" + "ma5:" + this.ma_record.ma5 + ", ma10:" + this.ma_record.ma10 + ", ma20:" + this.ma_record.ma20 + ", ma30:" + this.ma_record.ma30 + ", ma60:" + this.ma_record.ma60 + ", ma120:" + this.ma_record.ma120 + "}, " +
				"BollRet {" + "upper:" + this.boll_record.upper + ", mid:" + this.boll_record.mid + ", lower:" + this.boll_record.lower + ", bbi:" + this.boll_record.bbi + "}, " +
				"MACDRet {" + "diff:" + this.macd_record.diff + ", dea:" + this.macd_record.dea + ", macd:" + this.macd_record.macd + "}" +
				"}";
		return str;
	}
}
	
/**
 * 所有交易数据相关操作
 * @author latupa
 *
 */
public class BTCData {
	
	public static final Log log = LogFactory.getLog(BTCData.class);
	
	//记录最近一个K线周期的数值
	public BTCDSliceRecord btc_s_record = new BTCDSliceRecord();
	
	//记录运行时间内的所有K线周期数据
	public TreeMap<String, BTCTotalRecord> b_record_map = new TreeMap<String, BTCTotalRecord>();
	
	//BTC行情接口
//	public static final String URL_PRICE = "https://www.okcoin.com/api/ticker.do?symbol=ltc_cny";
	public static final String URL_PRICE = "https://www.okcoin.com/api/ticker.do";
	
	//数据库连接
	public DBInst dbInst;  
	
	//数据库配置文件
	public static final String FLAG_FILE_DIR = "src/main/resources/";
	public static final String dbconf_file = "db.flag";
	
	public static final String BTC_PRICE_TABLE = "btc_price";
	public static final String BTC_TRANS_TABLE = "btc_trans";
	
	public int data_cycle;
	
	//用于生成伪随机值
	public double price_mock;
	 
	//用于记录从数据库mock的数据, <day, close>
	public TreeMap<Integer, Double> update_mock_map = new TreeMap<Integer, Double>();
	public Iterator<Integer> update_mock_it;
	
	//用于记录从数据库BTC mock的数据，<String, BTCTotalRecord>
	public TreeMap<String, BTCTotalRecord> btc_mock_map = new TreeMap<String, BTCTotalRecord>();
	public Iterator<String> btc_mock_it;
	
	public BTCData(int data_cycle) {
		this.data_cycle = data_cycle;
		this.dbInst	= ConnectDB();
		DBInit();
		BTCSliceRecordInit();
		this.price_mock = 0;
	}
	
	public synchronized void BTCSliceRecordInit() {
		this.btc_s_record.high	= 0;
		this.btc_s_record.low	= 0;
		this.btc_s_record.open	= 0;
		this.btc_s_record.close	= 0;
		this.btc_s_record.init_flag	= true;
	}
	
	public void DBInit() {
		String sql = "create table if not exists " + BTC_PRICE_TABLE + "__" + this.data_cycle + 
			"(`time` DATETIME not null default '0000-00-00 00:00:00', " +
				"`open` double NOT NULL default '0', " +
				"`close` double NOT NULL default '0', " +
				"`high` double NOT NULL default '0', " +
				"`low` double NOT NULL default '0', " +
				"`ma5` double NOT NULL default '0', " +
				"`ma10` double NOT NULL default '0', " +
				"`ma20` double NOT NULL default '0', " +
				"`ma30` double NOT NULL default '0', " +
				"`ma60` double NOT NULL default '0', " +
				"`ma120` double NOT NULL default '0', " +
				"`upper` double NOT NULL default '0', " +
				"`mid` double NOT NULL default '0', " +
				"`lower` double NOT NULL default '0', " +
				"`bbi` double NOT NULL default '0', " +
				"`ema13` double NOT NULL default '0', " +
				"`ema26` double NOT NULL default '0', " +
				"`diff` double NOT NULL default '0', " +
				"`dea` double NOT NULL default '0', " +
				"`macd` double NOT NULL default '0', " +
				"PRIMARY KEY (`time`)" +
				") ENGINE=InnoDB DEFAULT CHARSET=utf8";	
		
		dbInst.updateSQL(sql);
		
	}
	
	/**
	 * 对BTCTotalRecord映射表的操作：获取指定时间的record
	 * @param time
	 * @return
	 */
	public BTCTotalRecord BTCRecordOptGetByTime(String time) {
		if (this.b_record_map.containsKey(time)) {
			return this.b_record_map.get(time);
		}
		else {
			return null;
		}
	}
	
	/**
	 * 对BTCTotalRecord映射表的操作：获取指定周期的record
	 * @param cycle 1表示1个周期前record，以此类推，0表示当前最新周期record
	 * @param p_time 指定某个时间点，如果为null，则表示当前
	 * @return
	 */
	public BTCTotalRecord BTCRecordOptGetByCycle(int cycle, String p_time) {
		
		BTCTotalRecord last_record = null;
		
		if (p_time == null) {
			p_time = "99991231235959";
		}
		
		for (String time : this.b_record_map.headMap(p_time, true).descendingKeySet().toArray(new String[0])) {
			last_record	= this.b_record_map.get(time);
			if (cycle == 0) {
				return last_record;
			}
			cycle--;
		}
		
		return last_record;
	}
	
	/**
	 * 清理之前内存中的历史数据
	 * @param pre_days
	 */
	public void BTCDataMemoryClean(int pre_days) {
		
		DateFormat df	= new SimpleDateFormat("yyyyMMddHHmmss"); 
		
		long stamp_millis = System.currentTimeMillis();
		long stamp_sec = stamp_millis / 1000;
		
		long pre_stamp_sec = stamp_sec - 24 * 60 * 60 * pre_days;
		
		Date cur_date = new Date(pre_stamp_sec * 1000);
		String sDateTime = df.format(cur_date); 
		
		for (String time : this.b_record_map.headMap(sDateTime, true).keySet().toArray(new String[0])) {
			this.b_record_map.remove(time);
		}
	}
	
	/**
	 * 从数据库加载历史数据
	 * @param last_days 最近天数的数据，如果为0，则表示加载所有
	 * @param time_s 起始时间，如果为null，则表示从当前时间往前加载
	 */
	public void BTCDataLoadFromDB(int last_days, String time_s) {
		log.info("load history data from db(" + this.data_cycle + ") for " + last_days + " days");
		
		this.b_record_map.clear();
		
		String sql = "select floor(time + 0) as time, open, close, high, low, ma5, ma10, ma20, ma30, ma60, ma120, upper, mid, lower, bbi, ema13, ema26, diff, dea, macd from  " + BTC_PRICE_TABLE + "__" + this.data_cycle + " where data_complete = 1";
//		String sql = "select floor(time + 0) as time, round(open, 2) as open, round(close, 2) as close, round(high, 2) as high, round(low, 2) as low, round(ma5, 2) as ma5, round(ma10, 2) as ma10, round(ma20, 2) as ma20, round(ma30, 2) as ma30, round(ma60, 2) as ma60, round(ma120, 2) as ma120, round(upper, 2) as upper, round(mid, 2) as mid, round(lower, 2) as lower, round(bbi, 2) as bbi, round(ema13, 2) as ema13, round(ema26, 2) as ema26, round(diff, 2) as diff, round(dea, 2) as dea, round(macd, 2) as macd from  " + BTC_PRICE_TABLE + "__" + this.data_cycle + " where data_complete = 1";
		if (time_s != null) {
			sql += " where time < '" + time_s + "'";
		}
		
		if (last_days != 0) {
			sql += " order by time desc limit " + last_days;
		}
		
		ResultSet rs = null;
		rs = dbInst.selectSQL(sql);
		try {
			while (rs.next()) {
				
				String time	= rs.getString("time");
				
				BTCTotalRecord record = new BTCTotalRecord();
				
				record.open		= rs.getDouble("open");
				record.close	= rs.getDouble("close");
				record.high		= rs.getDouble("high");
				record.low		= rs.getDouble("low");
				
				record.ma_record.ma5	= rs.getDouble("ma5");
				record.ma_record.ma10	= rs.getDouble("ma10");
				record.ma_record.ma20	= rs.getDouble("ma20");
				record.ma_record.ma30	= rs.getDouble("ma30");
				record.ma_record.ma60	= rs.getDouble("ma60");
				record.ma_record.ma120	= rs.getDouble("ma120");
				
				record.boll_record.upper	= rs.getDouble("upper");
				record.boll_record.mid		= rs.getDouble("mid");
				record.boll_record.lower	= rs.getDouble("lower");
				record.boll_record.bbi		= rs.getDouble("bbi");

				record.macd_record.ema13	= rs.getDouble("ema13");
				record.macd_record.ema26	= rs.getDouble("ema26");
				record.macd_record.diff		= rs.getDouble("diff");
				record.macd_record.dea		= rs.getDouble("dea");
				record.macd_record.macd		= rs.getDouble("macd");
				
				this.b_record_map.put(time, record);
			}
			
			dbInst.closeSQL(rs);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 更新基础价格信息到DB
	 * @param time
	 */
	public void BTCRecordDBInsert(String time) {
		
		String sql = "insert ignore into " + BTC_PRICE_TABLE + "__" + this.data_cycle + 
				"(`time`, `open`, `close`, `high`, `low`) values ('" +
				time + "', " +
				this.btc_s_record.open + ", " +
				this.btc_s_record.close + ", " +
				this.btc_s_record.high + ", " +
				this.btc_s_record.low + ")" +
				"ON DUPLICATE KEY UPDATE " +
				"`open` = " + this.btc_s_record.open + ", " +
				"`close` = " + this.btc_s_record.close + ", " +
				"`high` = " + this.btc_s_record.high + ", " +
				"`low` = " + this.btc_s_record.low;

		dbInst.updateSQL(sql);
	}
	
	
	/**
	 * 更新Ma数据到DB
	 * @param time
	 */
	public void BTCMaRetDBUpdate(String time) {
		if (this.b_record_map.containsKey(time)) {
			BTCTotalRecord record = this.b_record_map.get(time);
			String sql = "update " + BTC_PRICE_TABLE + "__" + this.data_cycle + " set " +
					"ma5=" + record.ma_record.ma5 + ", " +
					"ma10=" + record.ma_record.ma10 + ", " +
					"ma20=" + record.ma_record.ma20 + ", " +
					"ma30=" + record.ma_record.ma30 + ", " +
					"ma60=" + record.ma_record.ma60 + ", " +
					"ma120=" + record.ma_record.ma120 + 
					" where time = '" + time + "'";
			
			dbInst.updateSQL(sql);
		}
		else {
			log.error("time " + time + "is not in db");
			System.exit(1);
		}
	}
	
	/**
	 * 更新Boll数据到DB
	 * @param time
	 */
	public void BTCBollRetDBUpdate(String time) {
		if (this.b_record_map.containsKey(time)) {
			BTCTotalRecord record = this.b_record_map.get(time);
			String sql = "update " + BTC_PRICE_TABLE + "__" + this.data_cycle + " set " +
					"upper=" + record.boll_record.upper + ", " +
					"mid=" + record.boll_record.mid + ", " +
					"lower=" + record.boll_record.lower + ", " +
					"bbi=" + record.boll_record.bbi + 
					" where time = '" + time + "'";
			
			dbInst.updateSQL(sql);
		}
		else {
			log.error("time " + time + "is not in db");
			System.exit(1);
		}
	}
	
	/**
	 * 更新Macd数据到DB
	 * @param time
	 */
	public void BTCMacdRetDBUpdate(String time) {
		if (this.b_record_map.containsKey(time)) {
			BTCTotalRecord record = this.b_record_map.get(time);
			String sql = "update " + BTC_PRICE_TABLE + "__" + this.data_cycle + " set " +
					"ema13=" + record.macd_record.ema13 + ", " +
					"ema26=" + record.macd_record.ema26 + ", " +
					"diff=" + record.macd_record.diff + ", " +
					"dea=" + record.macd_record.dea + ", " +
					"macd=" + record.macd_record.macd + 
					" where time = '" + time + "'";
			
			dbInst.updateSQL(sql);
		}
		else {
			log.error("time " + time + "is not in db");
			System.exit(1);
		}
	}
	
	/**
	 * 数据库记录更新完成
	 * @param time
	 */
	public void BTCDBComplete(String time) {
		if (this.b_record_map.containsKey(time)) {
			
			String sql = "update " + BTC_PRICE_TABLE + "__" + this.data_cycle + " set " +
					"data_complete=1" + 
					" where time = '" + time + "'";
			
			dbInst.updateSQL(sql);
		}
		else {
			log.error("time " + time + "is not in db");
			System.exit(1);
		}
	}
	
	
	/**
	 * 计算Macd
	 * @param btc_func
	 * @param time
	 * @param cycle_data
	 * @return
	 * @throws ParseException 
	 */
	public MacdRet BTCCalcMacd(BTCFunc btc_func, String time, int cycle_data) throws ParseException {
		return btc_func.macd(this, time);
	}
	
	/**
	 * 计算Boll
	 * @param btc_func
	 * @param time
	 * @return
	 */
	public BollRet BTCCalcBoll(BTCFunc btc_func, String time) {
		return btc_func.boll(this.b_record_map, time);
	}
	
	/**
	 * 计算均线
	 * @param btc_func
	 * @param time
	 * @return
	 */
	public MaRet BTCCalcMa(BTCFunc btc_func, String time) {
		ArrayList<Integer> mas = new ArrayList<Integer>();
		mas.add(new Integer(5));
		mas.add(new Integer(10));
		mas.add(new Integer(20));
		mas.add(new Integer(30));
		mas.add(new Integer(60));
		mas.add(new Integer(120));
		
		TreeMap<Integer, Double> ret = btc_func.ma(this.b_record_map, time, mas, 0);
		
		MaRet maret = new MaRet();
		maret.ma5	= ret.get(5);
		maret.ma10	= ret.get(10);
		maret.ma20	= ret.get(20);
		maret.ma30	= ret.get(30);
		maret.ma60	= ret.get(60);
		maret.ma120	= ret.get(120);
		
		return maret;
	}
	
	
	/**
	 * 更新基础价格信息到内存映射表中
	 * @param time
	 */
	public void BTCRecordMemInsert(String time) {
		if (this.btc_s_record.init_flag == false) {
			BTCTotalRecord record = new BTCTotalRecord(this.btc_s_record);
			this.b_record_map.put(time, record);
		}
	}
	
	/**
	 * 把均线值更新到内存中
	 * @param time
	 * @param ma_ret
	 */
	public void BTCMaRetMemUpdate(String time, MaRet ma_ret) {
		if (this.b_record_map.containsKey(time)) {
			BTCTotalRecord record = this.b_record_map.get(time);
			record.ma_record = new MaRet(ma_ret);
			this.b_record_map.put(time, record);
		}
		else {
			log.error("time " + time + " is not in mem");
			System.exit(1);
		}
	}
	
	/**
	 * 把Boll线值更新到内存中
	 * @param time
	 * @param boll_ret
	 */
	public void BTCBollRetMemUpdate(String time, BollRet boll_ret) {
		if (this.b_record_map.containsKey(time)) {
			BTCTotalRecord record = this.b_record_map.get(time);
			record.boll_record = new BollRet(boll_ret);
			this.b_record_map.put(time, record);
		}
		else {
			log.error("time " + time + " is not in mem");
			System.exit(1);
		}
	}
	
	/**
	 * 把Macd值更新到内存中
	 * @param time
	 * @param boll_ret
	 */
	public void BTCMacdRetMemUpdate(String time, MacdRet macd_ret) {
		if (this.b_record_map.containsKey(time)) {
			BTCTotalRecord record = this.b_record_map.get(time);
			record.macd_record = new MacdRet(macd_ret);
			this.b_record_map.put(time, record);
		}
		else {
			log.error("time " + time + "is not in mem");
			System.exit(1);
		}
	}
	
	public void BTCRecordMemShow() {
		for (String time : this.b_record_map.keySet().toArray(new String[0])) {
			BTCBasicRecord record = this.b_record_map.get(time);
			log.info("time:" + time);
			record.Show();
		}
		log.info(b_record_map.size() + " records");
	}
	
	/**
	 * 更新BTCSliceRecord的值
	 * @throws IOException 
	 */
	public synchronized void BTCSliceRecordUpdate(double last) {
		
//		double last = FetchMock();
//		double last = FetchRT();
//		double last = FetchRTWeb();
		
		if (this.btc_s_record.init_flag == true) {
			this.btc_s_record.high	= last;
			this.btc_s_record.low	= last;
			this.btc_s_record.open	= last;
			this.btc_s_record.close	= last;
			this.btc_s_record.init_flag	= false;
		}
		else {
			this.btc_s_record.high	= (last > this.btc_s_record.high) ? last : this.btc_s_record.high;
			this.btc_s_record.low	= (last < this.btc_s_record.low) ? last : this.btc_s_record.low;
			this.btc_s_record.close	= last;
		}
		
		return;
	}
	
	/**
	 * 从数据库获取Mock数据
	 */
	public void UpdateMockInit() {
		ResultSet rs = null;
		
		String sql	= "select day + 0 as day, close as close from stock_price__sh where code = '000001' and is_holiday != 1";
//		String sql	= "select day + 0 as day, close as close from stock_price__sh where code = '000001' and is_holiday != 1 and day >= '20090101'";
//		String sql	= "select day + 0 as day, close as close from stock_price__sz where code = '399001' and is_holiday != 1";

		rs = dbInst.selectSQL(sql);
		try {
			while (rs.next()) {

				int day	= rs.getInt("day");
				double close	= rs.getDouble("close");
				this.update_mock_map.put(day, close);
			}
			
			dbInst.closeSQL(rs);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.update_mock_it	= this.update_mock_map.keySet().iterator();
	}
	
	/**
	 * 基于数据库获取的mock数据，每次更新一条
	 * @return
	 */
	public boolean UpdateMockGet() {
		if (this.update_mock_it.hasNext()) {
			int day	= this.update_mock_it.next();
			double close	= this.update_mock_map.get(day);
			
			log.debug("mock from db, day:" + day + ", close:" + close);
			this.btc_s_record.close	= close;
			this.btc_s_record.open	= day;
			this.btc_s_record.init_flag	= false;
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * 从数据库获取BTC的Mock数据
	 */
	public void BTCMockInit(String time_s, String time_e) {
		
//		String sql = "select floor(time + 0) as time, " +
//				"round(open, 2) as open, " +
//				"round(close, 2) as close, " +
//				"round(high, 2) as high, " +
//				"round(low, 2) as low, " +
//				"round(ma5, 2) as ma5, " +
//				"round(ma10, 2) as ma10, " +
//				"round(ma20, 2) as ma20, " +
//				"round(ma30, 2) as ma30, " +
//				"round(ma60, 2) as ma60, " +
//				"round(ma120, 2) as ma120, " +
//				"round(upper, 2) as upper, " +
//				"round(mid, 2) as mid, " +
//				"round(lower, 2) as lower, " +
//				"round(bbi, 2) as bbi, " +
//				"round(ema13, 2) as ema13, " +
//				"round(ema26, 2) as ema26, " +
//				"round(diff, 2) as diff, " +
//				"round(dea, 2) as dea, " +
//				"round(macd, 2) as macd " +
//				"from  " + BTC_PRICE_TABLE + "__" + this.data_cycle + 
//				" where data_complete = 1";
		String sql = "select floor(time + 0) as time, open, close, high, low, ma5, ma10, ma20, ma30, ma60, ma120, upper, mid, lower, bbi, ema13, ema26, diff, dea, macd from  " + BTC_PRICE_TABLE + "__" + this.data_cycle + " where data_complete = 1";
		if (time_s != null) {
			sql += " and time >= '" + time_s + "'";
		}
		if (time_e != null) {
			sql += " and time <= '" + time_e + "'";
		}
		sql += " order by time asc";
		
		log.info(sql);
		
		ResultSet rs = null;
		rs = dbInst.selectSQL(sql);
		try {
			while (rs.next()) {
				
				String time	= rs.getString("time");
				
				BTCTotalRecord record = new BTCTotalRecord();
				
				record.open		= rs.getDouble("open");
				record.close	= rs.getDouble("close");
				record.high		= rs.getDouble("high");
				record.low		= rs.getDouble("low");
				
				record.ma_record.ma5	= rs.getDouble("ma5");
				record.ma_record.ma10	= rs.getDouble("ma10");
				record.ma_record.ma20	= rs.getDouble("ma20");
				record.ma_record.ma30	= rs.getDouble("ma30");
				record.ma_record.ma60	= rs.getDouble("ma60");
				record.ma_record.ma120	= rs.getDouble("ma120");
				
				record.boll_record.upper	= rs.getDouble("upper");
				record.boll_record.mid		= rs.getDouble("mid");
				record.boll_record.lower	= rs.getDouble("lower");
				record.boll_record.bbi		= rs.getDouble("bbi");

				record.macd_record.ema13	= rs.getDouble("ema13");
				record.macd_record.ema26	= rs.getDouble("ema26");
				record.macd_record.diff		= rs.getDouble("diff");
				record.macd_record.dea		= rs.getDouble("dea");
				record.macd_record.macd		= rs.getDouble("macd");
				
				this.btc_mock_map.put(time, record);
			}
			
			dbInst.closeSQL(rs);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.btc_mock_it = this.btc_mock_map.keySet().iterator();
	}
	
	
	/**
	 * 基于数据库获取的BTC mock数据，每次更新一条
	 * @return
	 */
	public BTCTotalRecord BTCMockGet() {
		if (this.btc_mock_it.hasNext()) {
			String time	= this.btc_mock_it.next();
			BTCTotalRecord record = this.btc_mock_map.get(time);
			
			log.debug("mock from db, time:" + time);
			
			return record;
		}
		else {
			return null;
		}
	}
	
	/**
	 * 从配置文件获取mysql连接
	 */
	public DBInst ConnectDB() {
		
//		String dbfile = FLAG_FILE_DIR + dbconf_file;
		String dbfile = dbconf_file;
		try {
			//FileInputStream fis		= new FileInputStream(dbfile);
			InputStream fis			= BTCData.class.getClassLoader().getResourceAsStream(dbfile);
	        InputStreamReader isr	= new InputStreamReader(fis, "utf8");
	        BufferedReader br		= new BufferedReader(isr);
	        
	        String line = br.readLine();
	        
	        br.close();
	        isr.close();
	        fis.close();
	        
	        if (line != null) {
	        	String arrs[] = line.split(" ");
	        	
	        	String host = arrs[0];
	        	String port = arrs[1];
	        	String db	= arrs[2];
	        	String user = arrs[3];
	        	String passwd = arrs[4];
	        	
	        	log.info("read db conf, host:" + host + ", port:" + port + ", db:" + db + ", user:" + user + ", passwd:" + passwd);
	        	DBInst dbInst = new DBInst("jdbc:mysql://" + host + ":" + port + "/" + db, user, passwd);
	        	return dbInst;
	        }
	        else {
	        	log.error("read " + dbfile + " is null!");
	        	return null;
	        }
		}
		catch (Exception e) {
			log.error("read " + dbfile + " failed!", e);
			return null;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}

}
