package com.latupa.stock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 交易记录
 * @author latupa
 *
 */
public class BTCTransRecord {
	
	public static final Log log = LogFactory.getLog(BTCTransRecord.class);
	
	public enum OPT {
		OPT_BUY,
		OPT_SELL
	};
	
	//数据库连接
	public DBInst dbInst;  
	
	public static final String BTC_TRANS_TABLE = "btc_trans";
	public static final String BTC_TRANS_DETAIL_TABLE = "btc_trans_detail";
	
	public BTCTransRecord(DBInst dbInst) {
		this.dbInst	= dbInst;
	}
	
	public void InitTable(String postfix) {
		String table_name = BTC_TRANS_TABLE + "__" + postfix;
		String sql = "create table if not exists " + table_name + 
				"(`time` DATETIME not null default '0000-00-00 00:00:00', " +   //卖出时间
				"`start_amount` double NOT NULL default '0', " +   //买入时总资金
				"`end_amount` double NOT NULL default '0', " +     //卖出时总资金
				"`profit` double NOT NULL default '0', " +    	   //利润
				"`reason` int NOT NULL default '0', " +            //买入原因
				"`start_price` double NOT NULL default '0', " +    //买入价格
				"`end_price` double NOT NULL default '0', " +      //卖出价格
				"`start_time` DATETIME not null default '0000-00-00 00:00:00', " +   //买入时间
				"PRIMARY KEY (`time`)" +
				") ENGINE=InnoDB DEFAULT CHARSET=utf8";	
		
		dbInst.updateSQL(sql);
		
		sql = "truncate table " + table_name;
		dbInst.updateSQL(sql);
		
		table_name = BTC_TRANS_DETAIL_TABLE + "__" + postfix;
		sql = "create table if not exists " + table_name + 
				"(`time` DATETIME not null default '0000-00-00 00:00:00', " +
				"`opt` int NOT NULL default '0', " +
				"`quantity` double NOT NULL default '0', " +
				"`price` double NOT NULL default '0', " +
				"`amount` double NOT NULL default '0', " +
				"PRIMARY KEY (`time`)" +
				") ENGINE=InnoDB DEFAULT CHARSET=utf8";	

		dbInst.updateSQL(sql);
		
		sql = "truncate table " + table_name;
		dbInst.updateSQL(sql);
	}
	
	public void InsertTransDetail(String postfix, String time, OPT opt, double quantity, double price) {
		String table_name = BTC_TRANS_DETAIL_TABLE + "__" + postfix;
		String sql = "insert ignore into " + table_name + 
				"(`time`, `opt`, `quantity`, `price`, `amount`) values ('" +
				time + "', " +
				opt.ordinal() + ", " +
				quantity + ", " +
				price + ", " +
				(quantity * price) + ")";
		log.debug(sql);

		dbInst.updateSQL(sql);
	}
	
	public void InsertTrans(String postfix, String time, double start_amount, double end_amount, double profit, int reason, double start_price, double end_price, String start_time) {
		
		String table_name = BTC_TRANS_TABLE + "__" + postfix;
		String sql = "insert ignore into " + table_name + 
				"(`time`, `start_amount`, `end_amount`, `profit`, `reason`, `start_price`, `end_price`, `start_time`) values ('" +
				time + "', " +
				start_amount + ", " +
				end_amount + ", " +
				profit + ", " +
				reason + ", " +
				start_price + ", " +
				end_price + ", '" +
				start_time + "')";
		log.debug(sql);
		
		dbInst.updateSQL(sql);
	}

}
