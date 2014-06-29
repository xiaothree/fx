package com.latupa.stock;

import java.text.DecimalFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 交易策略
      买入：
	                         多重底&&低于BBI                                   
	                                  低位二次金叉                                             均线多头排列                      
	READY----------------------------------->BUYIN----------------->BULL
     ^    均线支撑&&（贴上轨||macd红线变长）       |                      |  
     |                                        |                      |              
     |----------------------------------------|                      |                        
     |                 死叉                                         |                      |
     |----------------------------------------|                      |死叉
     |           顶&&低于BBI                  |                      |
     |                                        |顶                                           |	               
	 |             低于BBI                    V                      | 
	 |---------------------------------------HALF <------------------|    
	     死叉||顶（macd红线变短，或者macd绿线变长）                                                 |
	 ^                                                               |
	 |---------------------------------------------------------------|
	                                                                   低于BBI
	
	备注：
	1. 多重底：macd绿线多次变短
	2. 均线支撑：5>10>20>30
	3. 均线多头排列:5>10>20>30>60>120
	4. 一旦出场，中间状态全部清空
	5. 多重底，一旦macd变红，则之前的多重底取消，重新计算
	6. 低位二次金叉，DIFF<0, 后一次比前一次要高
	
	
 * @author latupa
 * TODO
 * 1. 卖出时机要加快，可以考虑利用当前K线，而不是等K线完整
 * 2. 多重底还要仔细考虑，多重底是否连续？入场条件是否再严格？
 *    2.1 多重底要考虑macd线的长度，底越深相对上弹的可能性和幅度就越大，比如很多小底是否可以过滤？
 *    2.2 分割的多重底考虑连起来
 * 3. 底部两次金叉入场，可否等同于BULL处理，金叉的过期要review
 */

public class BTCTransStrategy2 implements BTCTransStrategy {

public static final Log log = LogFactory.getLog(BTCTransStrategy2.class);
	
	public enum STATUS {
		READY,	//待买，即空仓
		BUYIN,	//买入
		BULL,	//多头（均线支撑，且贴布林线上轨或者macd红线变长
		HALF;	//半仓
	};
	
	public final static int CONTIDION_MULTI_BOTTOM	= 0x1;  //多重底
	public final static int CONTIDION_DOUBLE_CROSS	= 0x2;  //两次金叉
	public final static int CONTIDION_MA_MACD_UP	= 0x4;  //均线支撑&&macd红线变长
	public final static int CONTIDION_MA_BOLL_UP	= 0x8;  //均线支撑&&贴boll上轨
	
	//当前状态
	public STATUS curt_status = STATUS.READY;
	
	//当前价格
	public double curt_price;
	
	//中间状态
	public boolean status_is_down_cross = false;	//低位金叉
	public double status_down_cross_value	= 0;	//低位金叉的值
	
	public int status_down_macd_count	= 0;		//macd底次数
	
	public final static int DOWN_MACD_COUNT = 2;	//多重底的数量
	
	//二次金叉
	public boolean is_double_gold_cross	= false;
	
	//多重底
	public boolean is_multi_macd_bottom	= false;
	
	//死叉
	public boolean is_dead_cross	= false;
	
	//均线
	public boolean is_ma_support	= false;	//均线支撑
	public boolean is_ma_bull_arrange	= false;	//均线多头排列
	
	//贴上轨
	public boolean is_boll_up	= false;
	public boolean is_boll_bbi_down	= false;
	public boolean is_boll_mid_down	= false;
	
	//macd顶、底
	public boolean is_macd_top		= false;	//macd>0 顶
	public boolean is_macd_bottom	= false;	//macd<0 底
	public boolean is_macd_up		= false;	//macd>0 变短再变长
	public boolean is_macd_down		= false;	//macd<0 变短再变长
	
	public BTCTransStrategy2() {
		
	}
	
	public void CheckPoint(double buy_price, BTCData btc_data, String sDateTime) {
		
		BTCTotalRecord record	= btc_data.BTCRecordOptGetByCycle(0, null);
		BTCTotalRecord record_1cycle_before	= btc_data.BTCRecordOptGetByCycle(1, null);
		BTCTotalRecord record_2cycle_before	= btc_data.BTCRecordOptGetByCycle(2, null);
		
		this.curt_price	= record.close;
		
		//均线支撑
		if (record.ma_record.ma5 > record.ma_record.ma10 && 
				record.ma_record.ma10 > record.ma_record.ma20 &&
				record.ma_record.ma20 > record.ma_record.ma30) {
			this.is_ma_support	= true;
			//均线多头排列
			if (record.ma_record.ma30 > record.ma_record.ma60 &&
					record.ma_record.ma60 > record.ma_record.ma120) {
				this.is_ma_bull_arrange	= true;
				
				if (this.curt_status == STATUS.BUYIN) {
					this.curt_status = STATUS.BULL;
					log.info("TransProcess: status from " + STATUS.BUYIN + " to " + STATUS.BULL);
				}
			}
		}
		
		//贴上轨
		if (record.close > record.boll_record.upper) {
			this.is_boll_up	= true;
		}
		
		//死叉
		if ((record.macd_record.diff < record.macd_record.dea) &&
				(record_1cycle_before.macd_record.diff >= record_1cycle_before.macd_record.dea)) {
			this.is_dead_cross	= true;
		}
		
		//低位二次金叉
		if (record.macd_record.diff < 0) {
			if (record.macd_record.diff > record.macd_record.dea &&
					record_1cycle_before.macd_record.diff <= record_1cycle_before.macd_record.dea) {
				//之前金叉过
				if (this.status_is_down_cross == true &&
						record.macd_record.diff > this.status_down_cross_value) {
					this.is_double_gold_cross	= true;
				}
				
				this.status_is_down_cross	= true;
				this.status_down_cross_value	= record.macd_record.diff;
			}
		}
		else {	//如果diff>=0，则上次低位金叉失效
			this.status_is_down_cross	= false;
		}
		
		//macd多重底
		if (record.macd_record.macd < 0) {
			
			//macd绿线变长
			if (record_2cycle_before.macd_record.macd <= record_1cycle_before.macd_record.macd &&
					record.macd_record.macd < record_1cycle_before.macd_record.macd) {
				this.is_macd_down	= true;
				this.is_multi_macd_bottom	= false;
			}
			//macd绿线变短
			else if (record_2cycle_before.macd_record.macd >= record_1cycle_before.macd_record.macd &&
					record.macd_record.macd > record_1cycle_before.macd_record.macd) {
				this.is_macd_bottom	= true;
				this.status_down_macd_count++;
				
				//之前有过底
				if (this.status_down_macd_count >= DOWN_MACD_COUNT) {
					this.is_multi_macd_bottom	= true;
				}
			}
		}
		else {	//如果macd>=0，则上次低位底失效
			this.status_down_macd_count	= 0;
		}
		
		
		if (record.macd_record.macd > 0) {
			//macd红线变短再变长
			if (record_2cycle_before.macd_record.macd >= record_1cycle_before.macd_record.macd &&
					record.macd_record.macd > record_1cycle_before.macd_record.macd) {
				this.is_macd_up	= true;
			}
			//macd顶
			else if (record_2cycle_before.macd_record.macd <= record_1cycle_before.macd_record.macd &&
					record.macd_record.macd < record_1cycle_before.macd_record.macd) {
				this.is_macd_top	= true;
			}
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
		
		boolean is_buy = false;
		int ret = 0;
		DecimalFormat df1 = new DecimalFormat("#0.00");
		
		//低位二次金叉
		if (this.is_double_gold_cross) {
			ret = CONTIDION_DOUBLE_CROSS;
			is_buy	= true;
			this.curt_status	= STATUS.BUYIN;
			log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", buy for double_gold_cross, status from " + STATUS.READY + " to " + STATUS.BUYIN);
		}
		
		//均线支撑
		if (this.is_ma_support) {
			if (this.is_macd_up) {
				ret = CONTIDION_MA_MACD_UP;
				is_buy	= true;
				this.curt_status	= STATUS.BUYIN;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", buy for ma_support && macd_up, status from " + STATUS.READY + " to " + STATUS.BUYIN);
			}
			
			if (this.is_boll_up) {
				ret = CONTIDION_MA_BOLL_UP;
				is_buy	= true;
				this.curt_status	= STATUS.BUYIN;
				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", buy for ma_support && boll_up, status from " + STATUS.READY + " to " + STATUS.BUYIN);
			}
		}
		
		//多重底
		if (this.is_multi_macd_bottom) {
			ret = CONTIDION_MULTI_BOTTOM;
			is_buy	= true;
			this.curt_status = STATUS.BUYIN;
			log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", buy for multi_macd_bottom, status from " + STATUS.READY + " to " + STATUS.BUYIN);
		}
		
		if (is_buy) {
			if (this.is_ma_bull_arrange) {
				this.curt_status	= STATUS.BULL;
				log.info("TransProcess: status from " + STATUS.BUYIN + " to " + STATUS.BULL);
			}
		}
		
		return ret;
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
			if (this.is_macd_top || this.is_macd_down) {
				if (this.is_boll_bbi_down) {
					this.curt_status	= STATUS.READY;
					log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for (macd_top || macd_down) && bool_bbi_down in buy, status from " + STATUS.BUYIN + " to " + STATUS.READY);
					return 10;
				}
				else {
					this.curt_status	= STATUS.HALF;
					log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for macd_top || macd_down in buy, status from " + STATUS.BUYIN + " to " + STATUS.HALF);
					return 5;	
				}
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
	
	public void CleanStatus() {
		this.status_down_macd_count	= 0;
	}

	public void InitPoint() {
		// TODO Auto-generated method stub
		//二次金叉
		is_double_gold_cross	= false;
		
		//多重底
		is_multi_macd_bottom	= false;
		
		//死叉
		is_dead_cross	= false;
		
		//均线
		is_ma_support	= false;	//均线支撑
		is_ma_bull_arrange	= false;	//均线多头排列
		
		//贴上轨
		is_boll_up	= false;
		is_boll_bbi_down	= false;
		is_boll_mid_down	= false;
		
		//macd顶、底
		is_macd_top		= false;	//macd>0 顶
		is_macd_bottom	= false;	//macd<0 底
		is_macd_up		= false;	//macd>0 变短再变长
		is_macd_down	= false;	//macd<0 变短再变长
	}
	
}
