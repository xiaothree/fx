package com.latupa.stock;

import java.text.DecimalFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 交易策略
      买入：
	1. 均线支撑 &&（贴布林线上轨 || macd红线变长）
      卖出：
	1. 是否多头
		1）是，则等两次死叉，第一次死叉出一半；然后再死叉（或者变长再变短），出另一半
		2）否，macd一重顶出一半，二重顶再出另一半，如果直接死叉则全部出
 * @author latupa
 * TODO
 * 
 * 上证指数000001复盘
 * 		1. 出场的两次死叉要提防单边下跌，可以考虑增加跌破bbi或者布林线中轨来快速出场(done，采用跌破bbi)
 * 		2. 增加底部两次金叉的入场
 *      3. 对于出场的判断，可以考虑出场的那个点是否还是多头，如果不是，则直接全部出
 * 个股600104复盘
 * 		1. 波动比较大，导致来不及出场，或者出场太急，后续又会往上走
 */
public class BTCTransStrategy1 implements BTCTransStrategy {
	
	public static final Log log = LogFactory.getLog(BTCTransStrategy1.class);
	
	public enum STATUS {
		READY,	//待买，即空仓
		BUYIN,	//买入
		BULL,	//多头（均线支撑，且贴布林线上轨或者macd红线变长
		HALF;	//半仓
	};
	
	//当前状态
	public STATUS curt_status = STATUS.READY;
	
	//当前价格
	public double curt_price;
	
	//金叉、死叉
	public boolean is_dead_cross	= false;
	public boolean is_gold_cross	= false;
	
	//多头
	public boolean is_bull	= false;
	
	//贴上轨
	public boolean is_boll_up	= false;
	public boolean is_boll_bbi_down	= false;
	public boolean is_boll_mid_down	= false;
	
	//macd顶、底
	public boolean is_macd_top		= false;	//macd>0 顶
	public boolean is_macd_bottom	= false;	//macd<0 底
	public boolean is_macd_up		= false;	//macd>0 变短再变长
	public boolean is_macd_down		= false;	//macd<0 变短再变长
	
	public BTCTransStrategy1() {
		this.InitPoint();
	}
	
	public void InitPoint() {
		this.is_dead_cross	= false;
		this.is_gold_cross	= false;
		this.is_boll_up		= false;
		this.is_bull		= false;
		this.is_macd_top	= false;
		this.is_macd_bottom	= false;
		this.is_macd_up		= false;
		this.is_macd_down	= false;
		this.is_boll_bbi_down	= false;
		this.is_boll_mid_down	= false;
	}
	
	public void CheckPoint(double buy_price, BTCData btc_data, String sDateTime) {
		BTCTotalRecord record	= btc_data.BTCRecordOptGetByCycle(0, null);
		BTCTotalRecord record_1cycle_before	= btc_data.BTCRecordOptGetByCycle(1, null);
		BTCTotalRecord record_2cycle_before	= btc_data.BTCRecordOptGetByCycle(2, null);
		
		this.curt_price	= record.close;
		
		//多头
//		if ((record.ma_record.ma5 > record.ma_record.ma10 && 
//				record.ma_record.ma10 > record.ma_record.ma20 &&
//				record.ma_record.ma20 > record.ma_record.ma30 &&
//				record.ma_record.ma30 > record.ma_record.ma60)) {
		if ((record.ma_record.ma5 > record.ma_record.ma10 && 
				record.ma_record.ma10 > record.ma_record.ma20 &&
				record.ma_record.ma20 > record.ma_record.ma30)) {
			this.is_bull	= true;
		}
		
		//贴上轨
		if (record.close > record.boll_record.upper) {
			this.is_boll_up	= true;
		}
		
		//死叉
		if ((record.macd_record.diff < record.macd_record.dea) &&
				(record_1cycle_before.macd_record.diff > record_1cycle_before.macd_record.dea)) {
			this.is_dead_cross	= true;
		}
		
		//金叉
		if ((record.macd_record.diff > record.macd_record.dea) &&
				(record_1cycle_before.macd_record.diff < record_1cycle_before.macd_record.dea)) {
			this.is_gold_cross	= true;
		}
		
		//macd红线变短再变长
		if (record.macd_record.macd > 0 &&
				record_2cycle_before.macd_record.macd > record_1cycle_before.macd_record.macd &&
				record.macd_record.macd > record_1cycle_before.macd_record.macd) {
			this.is_macd_up	= true;
		}
		
		//macd绿线变短再变长
		if (record.macd_record.macd < 0 &&
				record_2cycle_before.macd_record.macd < record_1cycle_before.macd_record.macd &&
				record.macd_record.macd < record_1cycle_before.macd_record.macd) {
			this.is_macd_down	= true;
		}
		//macd顶
		if (record.macd_record.macd > 0 &&
				record_2cycle_before.macd_record.macd < record_1cycle_before.macd_record.macd &&
				record.macd_record.macd < record_1cycle_before.macd_record.macd) {
			this.is_macd_top	= true;
		}
		
		//跌破bbi
		if (record.close < record.boll_record.bbi) {
			this.is_boll_bbi_down	= true;
		}
		
		//跌破布林线中轨
		if (record.close < record.boll_record.mid) {
			this.is_boll_mid_down	= true;
		}
	}
	
	public int IsBuy(String sDateTime) {
		
//		if (this.is_gold_cross) {
//			this.curt_status	= STATUS.READY;
//			log.info("TransProcess: buy for gold_cross, status from " + STATUS.READY + " to " + STATUS.BUYIN);
//			return true;
//		}
		
		DecimalFormat df1 = new DecimalFormat("#0.00");
		
		if (this.is_bull) {
			if (this.is_macd_up) {
				this.curt_status	= STATUS.BULL;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", buy for bull && macd_up, status from " + STATUS.READY + " to " + STATUS.BULL);
				return 1;
			}
			
			if (this.is_boll_up) {
				this.curt_status	= STATUS.BULL;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", buy for bull && boll_up, status from " + STATUS.READY + " to " + STATUS.BULL);
				return 2;
			}
		}
		
		return 0;
	}
	
	public int IsSell(String sDateTime) {
		
		DecimalFormat df1 = new DecimalFormat("#0.00");
		
		if (this.curt_status == STATUS.BULL) {
			if (this.is_dead_cross) {
				if (this.is_boll_bbi_down) {
					this.curt_status	= STATUS.READY;
					log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for dead_cross && bbi_down in bull, status from " + STATUS.BULL + " to " + STATUS.READY);
					return 10;
				}
				else {
					this.curt_status	= STATUS.HALF;
					log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for dead_cross in bull, status from " + STATUS.BULL + " to " + STATUS.HALF);
					return 5;
				}
			}
		}
		else if (this.curt_status == STATUS.BUYIN) {
			if (this.is_dead_cross) {
				this.curt_status	= STATUS.READY;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for dead_cross in buy, status from " + STATUS.BUYIN + " to " + STATUS.READY);
				return 10;
			}
			if (this.is_macd_top) {
				this.curt_status	= STATUS.HALF;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for macd_top in buy, status from " + STATUS.BUYIN + " to " + STATUS.HALF);
				return 5;
			}
		}
		else if (this.curt_status == STATUS.HALF) {
			if (this.is_dead_cross) {
				this.curt_status	= STATUS.READY;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for dead_cross in half, status from " + STATUS.HALF + " to " + STATUS.READY);
				return 10;
			}
			
			if (this.is_boll_bbi_down) {
				this.curt_status	= STATUS.READY;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for bbi_down in half, status from " + STATUS.HALF + " to " + STATUS.READY);
				return 10;
			}
			
			if (this.is_macd_top) {
				this.curt_status	= STATUS.READY;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for macd_top in half, status from " + STATUS.HALF + " to " + STATUS.READY);
				return 10;
			}
			
			if (this.is_macd_down) {
				this.curt_status	= STATUS.READY;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for macd_down in half, status from " + STATUS.HALF + " to " + STATUS.READY);
				return 10;
			}
		}
		
		return 0;
	}
}
