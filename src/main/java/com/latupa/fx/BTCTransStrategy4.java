package com.latupa.stock;

import java.text.DecimalFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 交易策略
 * 在TransStrategy2的基础上进行更新
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
 * 1. 针对低位macd红线变长，优化入场时机

 */

public class BTCTransStrategy4 implements BTCTransStrategy {

	public static final Log log = LogFactory.getLog(BTCTransStrategy4.class);
	
	public enum STATUS {
		READY,	//待买，即空仓
		BUYIN,	//买入，满仓
		BULL,	//多头（均线支撑，且贴布林线上轨或者macd红线变长
		HALF;	//半仓
	};
	
	public final static int CONTIDION_MULTI_BOTTOM	= 1<<1;  //多重底
	public final static int CONTIDION_DOUBLE_CROSS	= 1<<2;  //两次金叉
	public final static int CONTIDION_MA_MACD_UP	= 1<<3;  //均线支撑&&macd红线变长
	public final static int CONTIDION_MA_BOLL_UP	= 1<<4;  //均线支撑&&贴boll上轨
	public final static int CONTIDION_MID_DOWN_MACD_UP = 1<<5; //低位macd红线变长
	public final static int CONTIDION_MACD_TO_RED   = 1<<6;  //macd变红
	public final static int CONTIDION_BULL_MACD_UP  = 1<<7; //多头macd红线变长
	
	public final static int CONTIDION_BREAK_UP_MA60 = 1<<8;  //突破60日均线
	public final static int CONTIDION_MA_BULL_ARRANGE = 1<<9;  //均线多头支撑
	
	//买入原因
	public int buy_reason = 0;
	
	//当前状态
	public STATUS curt_status = STATUS.READY;
	
	//当前价格
	public double curt_price;
	
	//二次金叉的中间状态
	class StatusDoubleCross {
		public boolean is_down_cross = false;	//低位金叉
		public double down_cross_value	= 0;	//低位金叉的值
		public boolean is_up_boll_mid = false;  //两次金叉之间是否突破过布林线中轨
		
		public StatusDoubleCross() {
			Init();
		}
		
		public void Init() {
			this.is_down_cross	= false;
			this.is_up_boll_mid	= false;
			this.down_cross_value	= 0;
		}
		
		public String toString() {
			String str = "DoubleCross { is_down_cross:" + this.is_down_cross + 
					", down_cross_value:" + this.down_cross_value +
					", is_up_boll_mid:" + this.is_up_boll_mid + "}";
			return str;
		}
	}
	public StatusDoubleCross status_double_cross = new StatusDoubleCross();
	
	//多重底的中间状态
	class StatusMultiBottom {
		public final int DOWN_MACD_COUNT = 2;	//多重底的数量
		public int down_macd_count;  //本次macd底次数
		public boolean is_boll_lower; //是否贴布林线下轨
		
		public StatusMultiBottom() {
			Init();
		}
		
		public void Init() {
			this.down_macd_count	= 0;
			this.is_boll_lower		= false;
		}
		
		public String toString() {
			String str = "MultiBottom { down_macd_count:" + this.down_macd_count + 
					", is_boll_lower:" + this.is_boll_lower + "}";
			return str;
		}
		
		public boolean IsMultiBottom(BTCData btc_data) {
				
			BTCTotalRecord record	= btc_data.BTCRecordOptGetByCycle(0, null);
			BTCTotalRecord record_1cycle_before	= btc_data.BTCRecordOptGetByCycle(1, null);
			BTCTotalRecord record_2cycle_before	= btc_data.BTCRecordOptGetByCycle(2, null);
			
			if (record.macd_record.macd < 0) {
				
				//阴线贴布林线下轨
				if ((record.open > record.boll_record.lower) &&
						(record.close < record.boll_record.lower)) {
					log.info("TransProcess: boll_lower");
					this.is_boll_lower = true; 
				}
				
				//macd绿线变短
				if (record_2cycle_before.macd_record.macd >= record_1cycle_before.macd_record.macd &&
						record.macd_record.macd > record_1cycle_before.macd_record.macd) {
					this.down_macd_count++;
					
					//对于一个多重底群，只入场前两个底，避免低迷状态时频繁入场
					if ((this.is_boll_lower == false) &&
							(this.down_macd_count >= DOWN_MACD_COUNT) &&
							(this.down_macd_count < (DOWN_MACD_COUNT + 1))) {
						return true;
					}
				}
			}
			else {	//如果macd>=0
				if (this.down_macd_count >= (this.DOWN_MACD_COUNT + 2) ||   //头两次入场机会都失败了
						((this.down_macd_count >= this.DOWN_MACD_COUNT) && (this.is_boll_lower == true))) {   //贴布林线下轨且本次多重底群多于两个底
					log.info("TransProcess: macd_count(" + this.down_macd_count + "), boll_lower(" + this.is_boll_lower + ")");
					this.Init();
					return true;
				}
				
				this.Init();
			}
			
			return false;
		}
	}
	
	public StatusMultiBottom status_multi_bottom = new StatusMultiBottom();
	
	//二次金叉
	public boolean is_double_gold_cross	= false;
	
	//多重底
	public boolean is_multi_macd_bottom	= false;
	
	//死叉
	public boolean is_dead_cross	= false;
	
	//均线
	public boolean is_ma_support	= false;	//均线支撑
	public boolean is_ma_bull_arrange	= false;	//均线多头排列
	public boolean is_ma_up			= false;    //均线上升
	public boolean is_ma_smooth		= false;    //均线平稳
	
	//贴上轨
	public boolean is_boll_up	= false;
	public boolean is_boll_bbi_down	= false;
	public boolean is_boll_mid_down	= false;
	public boolean is_last_boll_mid_down = false; //上一条K线低于布林线中轨
	public boolean is_boll_midsell_position_down	= false;
	public boolean is_boll_bbi_or_mid_down = false;
	public boolean is_boll_narrow = false; //布林线上下轨开始收窄
	
	//macd顶、底
	public boolean is_macd_top		= false;	//macd>0 顶
	public boolean is_macd_bottom	= false;	//macd<0 底
	public boolean is_macd_up		= false;	//macd>0 变短再变长
	public boolean is_macd_down		= false;	//macd<0 变短再变长
	public boolean is_macd_larger	= false;	//macd变长
	
	//入场后阴线数
	public int num_first_yin = 0;
	
	//阴线上影线
	public boolean is_up_yin = false;
	
	//跌破昨日开盘价
	public boolean is_down_last_open = false;
	
	//止损
	public boolean is_zhisun = false;
	
	//突破60日均线
	public boolean is_break_up_ma60 = false;
	public int num_below_ma60 = 0;//低于60日线的K线数
	
	//高于布林线中轨
	public boolean is_up_boll_mid = false;
	public boolean is_macd_changeto_red = false;
	
	public BTCTransStrategy4() {
	}
	
	public String toString() {
		String str = this.status_multi_bottom.toString() + ", " + 
				this.status_double_cross.toString() + ", " +
				"is_double_gold_cross:" + this.is_double_gold_cross + ", " +
				"is_multi_macd_bottom:" + this.is_multi_macd_bottom + ", " +
				"is_dead_cross:" + this.is_dead_cross + ", " +
				"is_ma_support:" + this.is_ma_support + ", " +
				"is_ma_bull_arrange:" + this.is_ma_bull_arrange + ", " +
				"is_ma_up:" + this.is_ma_up + ", " +
				"is_ma_smooth:" + this.is_ma_smooth + ", " +
				"is_boll_up:" + this.is_boll_up + ", " +
				"is_boll_bbi_down:" + this.is_boll_bbi_down + ", " +
				"is_boll_mid_down:" + this.is_boll_mid_down + ", " +
				"is_last_boll_mid_down:" + this.is_last_boll_mid_down + ", " +
				"is_boll_bbi_or_mid_down:" + this.is_boll_bbi_or_mid_down + ", " +
				"is_boll_narrow:" + this.is_boll_narrow + ", " +
				"is_macd_top:" + this.is_macd_top + ", " +
				"is_macd_bottom:" + this.is_macd_bottom + ", " +
				"is_macd_up:" + this.is_macd_up + ", " +
				"is_macd_down:" + this.is_macd_down + ", " +
				"is_macd_larger:" + this.is_macd_larger + ", " +
				"is_up_yin:" + this.is_up_yin + ", " +
				"is_down_last_open:" + this.is_down_last_open + ", " +
				"num_first_yin:" + this.num_first_yin + ", " +
				"is_zhisun:" + this.is_zhisun + ", " +
				"is_break_up_ma60:" + this.is_break_up_ma60 + ", " +
				"num_below_ma60:" + this.num_below_ma60 + ", " +
				"is_up_boll_mid:" + this.is_up_boll_mid + ", " +
				"is_macd_changeto_red:" + this.is_macd_changeto_red;
		return str;
	}
	
	public void CheckPoint(double buy_price, BTCData btc_data, String sDateTime) {
		
		BTCTotalRecord record	= btc_data.BTCRecordOptGetByCycle(0, null);
		BTCTotalRecord record_1cycle_before	= btc_data.BTCRecordOptGetByCycle(1, null);
		BTCTotalRecord record_2cycle_before	= btc_data.BTCRecordOptGetByCycle(2, null);
		BTCTotalRecord record_13cycle_before	= btc_data.BTCRecordOptGetByCycle(13, null);
		
		this.curt_price	= record.close;
		
		log.info("record:" + record.toString());
		log.info("record1:" + record_1cycle_before.toString());
		log.info("record2:" + record_2cycle_before.toString());
		log.info("record5:" + record_2cycle_before.toString());
		
		//记录小于60日均线的连续K线数
		if (record.close <= record.ma_record.ma60) {
			this.num_below_ma60++;
		}
		
		//突破60日均线
		if (record.close > record.ma_record.ma60 &&
				record.open < record.ma_record.ma60 &&
				this.num_below_ma60 >= 21) {
			this.is_break_up_ma60 = true;
		}
		
		//初始化
		if (record.close > record.ma_record.ma60) {
			this.num_below_ma60 = 0;
		}
		
		//均线支撑
		if (record.ma_record.ma5 >= record.ma_record.ma10 && 
				record.ma_record.ma10 >= record.ma_record.ma20) {
			this.is_ma_support	= true;
			//均线多头排列
			if (record.ma_record.ma20 > record.ma_record.ma30 &&
					record.ma_record.ma30 > record.ma_record.ma60) {
				this.is_ma_bull_arrange	= true;
			}
		}
		
		//均线上升
		if (record.ma_record.ma5 >= record_1cycle_before.ma_record.ma5 &&
				record.ma_record.ma10 >= record_1cycle_before.ma_record.ma10 &&
				record.ma_record.ma20 >= record_1cycle_before.ma_record.ma20 &&
				record.ma_record.ma30 >= record_1cycle_before.ma_record.ma30 &&
				record.ma_record.ma60 >= record_1cycle_before.ma_record.ma60) {
			this.is_ma_up = true;
		}
		
		
		if (this.is_ma_bull_arrange &&
				this.is_ma_up) {
			if (this.curt_status == STATUS.BUYIN) {
				StatusChange(STATUS.BULL, "ma bull arrange && ma up", sDateTime, 0);
			}
		}
		
		//近期涨幅不大，防止多头尾部才介入
//		if (((record.open - record.ma_record.ma20) / record.ma_record.ma20 <= 0.005) &&
//				record.open >= record.ma_record.ma20) {
		if (Math.abs(record.close - record_13cycle_before.close) / record_13cycle_before.close <= 0.01) {
			this.is_ma_smooth = true;
		}
		
		
		//贴上轨
		if (record.close > record.boll_record.upper) {
			this.is_boll_up	= true;
		}
		
		if (record.macd_record.macd < 0) {
			//macd绿线变长
			if (record_2cycle_before.macd_record.macd <= record_1cycle_before.macd_record.macd &&
					record.macd_record.macd < record_1cycle_before.macd_record.macd) {
				is_macd_down = true;
			}
			
			//macd绿线变短
			if (record_2cycle_before.macd_record.macd >= record_1cycle_before.macd_record.macd &&
					record.macd_record.macd > record_1cycle_before.macd_record.macd) {
				is_macd_bottom = true;
			}
		}
		
		if (record.macd_record.macd > 0) {
			//macd红线变短再变长
			if (record_2cycle_before.macd_record.macd >= record_1cycle_before.macd_record.macd &&
					record.macd_record.macd > record_1cycle_before.macd_record.macd &&
					record_1cycle_before.macd_record.macd >= 0) {
				this.is_macd_up	= true;
			}
			//macd顶
			else if (record_2cycle_before.macd_record.macd <= record_1cycle_before.macd_record.macd &&
					record.macd_record.macd < record_1cycle_before.macd_record.macd) {
				this.is_macd_top = true;
			}
		}
		
		//macd变长
		if (record.macd_record.macd > record_1cycle_before.macd_record.macd) {
			this.is_macd_larger = true;
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
				if ((this.status_double_cross.is_down_cross == true) &&
						(record.macd_record.diff > this.status_double_cross.down_cross_value) &&
						(this.status_double_cross.is_up_boll_mid == true)) {
					this.is_double_gold_cross	= true;
				}
				
				this.status_double_cross.Init();
				this.status_double_cross.is_down_cross		= true;
				this.status_double_cross.down_cross_value	= record.macd_record.diff;
			}
		}
		else {	//如果diff>=0，则上次低位金叉失效
			this.status_double_cross.Init();
		}
		
		//记录是否突破过布林线中轨（即有没有出现洞），为低位二次金叉提供条件
		if ((this.status_double_cross.is_down_cross == true) &&
//				(record.close > record.boll_record.mid)) {
				(record.close > record.boll_record.bbi)) {
			this.status_double_cross.is_up_boll_mid = true;
		}
		
		//macd多重底
		if (true == this.status_multi_bottom.IsMultiBottom(btc_data)) {
			this.is_multi_macd_bottom = true;
		}
		
		//跌破bbi
		if (record.close < record.boll_record.bbi) {
			this.is_boll_bbi_down	= true;
		}
		
		//跌破布林线中轨
		if (record.close < record.boll_record.mid) {
			this.is_boll_mid_down	= true;
		}
		
		//上一条K线跌破布林线中轨
		if (record_1cycle_before.close < record_1cycle_before.boll_record.mid) {
			this.is_last_boll_mid_down	= true;
		}
		
		//跌破bbi和布林线中轨的低点
		if (((record.boll_record.mid < record.boll_record.bbi) && (record.close < record.boll_record.mid)) ||
				((record.boll_record.bbi < record.boll_record.mid) && (record.close < record.boll_record.bbi))) {
			this.is_boll_bbi_or_mid_down = true;
		}
		
		//布林线开始收窄
		if ((record_2cycle_before.boll_record.upper > record_1cycle_before.boll_record.upper) && 
				(record_1cycle_before.boll_record.upper > record.boll_record.upper) &&
				(record_2cycle_before.boll_record.lower < record_1cycle_before.boll_record.lower) &&
				(record_1cycle_before.boll_record.lower < record.boll_record.lower)) {
			this.is_boll_narrow = true;
		}
		
		//入场后阴线数
		if (record.close < record.open) {
			this.num_first_yin++;
		}
		
		//阴线上影线
		if ((record.close < record.open) && 
				((record.high - record.open) / (record.high - record.low) >= 0.25) &&  //上影线占据空间大于1/4
				(record.open - record.close) / record.open >= 0.003) {  //跌幅超过0.3%
			this.is_up_yin = true;  
		}
		
		//跌破昨日开盘价
		if ((record_1cycle_before.close > record_1cycle_before.open) &&
				(record.close < record.open) &&
				(record.close < record_1cycle_before.open)) {
			this.is_down_last_open = true;
		}
		
		//macd线变红
		if (record_1cycle_before.macd_record.macd <= 0 &&
				record.macd_record.macd > 0) {
			this.is_macd_changeto_red = true;
		}
		
		//收盘价高于布林线中轨
		if (record.close > record.boll_record.mid) {
			this.is_up_boll_mid = true;
		}
		
		//触达止损，即2%
		if ((buy_price > 0) && (record.close - buy_price) / buy_price < -0.02) {
			this.is_zhisun = true;
		}
		
		log.info("checkpoint:" + this.toString());
	}
	
	public void BuyReset() {
		log.info("TransProcess: ret buy status to " + STATUS.READY);
		this.curt_status = STATUS.READY;
	}
	
	public int IsBuy(String sDateTime) {
		
		boolean is_buy = false;
		int ret = 0;//买入原因
		
//		if (this.curt_status == STATUS.READY) {
//			//突破60日线
//			if (this.is_break_up_ma60 &&
//					this.is_ma_up) {
//				StatusChange(STATUS.BUYIN, "break up ma60", sDateTime, 2);
//				ret = CONTIDION_BREAK_UP_MA60;
//				is_buy = true;
//			}
//		}
		
		if (this.curt_status == STATUS.READY) {
			//多头
			if (this.is_ma_bull_arrange &&
					this.is_ma_up &&
					this.is_macd_larger) {// &&
					//this.is_ma_smooth) {
				StatusChange(STATUS.BUYIN, "ma bull arrange && ma up", sDateTime, 2);
				ret = CONTIDION_MA_BULL_ARRANGE;
				is_buy = true;
			}
		}
		
		if (is_buy) {
			if (this.is_ma_bull_arrange) {
				StatusChange(STATUS.BULL, "ma bull arrange", sDateTime, 0);
			}
			
			//依赖于入场的状态设置
			this.num_first_yin = 0;
		}
		
		this.buy_reason = ret;
		
		return ret;
	}
	
	/**
	 * 状态改变
	 * @param to_status, 目标状态
	 * @param reason, 原因
	 * @param sDateTime, 时间
	 * @param opt, 操作，0-无操作，1-卖出， 2-买入
	 */
	private void StatusChange(STATUS to_status, String reason, String sDateTime, int opt) {
		DecimalFormat df1 = new DecimalFormat("#0.00");
		String opt_str = "";
		if (opt == 2) {
			opt_str = "buy for";
		}
		else if (opt == 1) {
			opt_str = "sell for";
		}
		
		log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", " + opt_str + " " + reason + ", status from " + this.curt_status + " to " + to_status);
		this.curt_status = to_status;
	}
	
	public int IsSell(String sDateTime) {
		
		int sell_position = 0;
		
		if (this.curt_status == STATUS.BULL) {
			if (this.is_dead_cross) {
				if (this.is_boll_bbi_down) {
					StatusChange(STATUS.READY, "dead_cross && bbi_down in bull", sDateTime, 1);
					sell_position = 10;
				}
				else {
					StatusChange(STATUS.HALF, "dead_cross in bull", sDateTime, 1);
					sell_position = 5;
				}
			}
			if (this.is_macd_down) {
				StatusChange(STATUS.READY, "macd down", sDateTime, 1);
				sell_position = 10;
			}
			else if (this.buy_reason == CONTIDION_MA_BOLL_UP && 
					this.num_first_yin == 1 &&
					this.is_up_yin) {
				StatusChange(STATUS.HALF, "up yin in bull", sDateTime, 1);
				sell_position = 5;
			}
			else if (this.buy_reason == CONTIDION_MA_BOLL_UP && 
					this.num_first_yin == 1 &&
					this.is_down_last_open) {
				StatusChange(STATUS.HALF, "down last open in bull", sDateTime, 1);
				sell_position = 5;
			}
		}
		else if (this.curt_status == STATUS.BUYIN) {
			if (this.is_dead_cross) {
				StatusChange(STATUS.READY, "dead_cross in buy", sDateTime, 1);
				sell_position = 10;
			}
			else if (this.is_macd_top || this.is_macd_down) {
				if (this.is_boll_bbi_down) {
					StatusChange(STATUS.READY, "(macd_top || macd_down) && bool_bbi_down in buy", sDateTime, 1);
					sell_position = 10;
				}
				else {
					StatusChange(STATUS.HALF, "macd_top || macd_down in buy", sDateTime, 1);
					sell_position = 5;	
				}
			}
			else if (this.buy_reason == CONTIDION_MA_BOLL_UP && 
					this.num_first_yin == 1 &&
					this.is_up_yin) {
				StatusChange(STATUS.HALF, "up yin in buy", sDateTime, 1);
				sell_position = 5;
			}
			else if (this.buy_reason == CONTIDION_MA_BOLL_UP && 
					this.num_first_yin == 1 &&
					this.is_down_last_open) {
				StatusChange(STATUS.HALF, "down last open in buy", sDateTime, 1);
				sell_position = 5;
			}
		}
		else if (this.curt_status == STATUS.HALF) {
			if (this.is_dead_cross) {
				StatusChange(STATUS.READY, "dead_cross in half", sDateTime, 1);
				sell_position = 10;
			}
			
//			if (this.is_boll_bbi_down) {
//				this.curt_status	= STATUS.READY;
//				log.info("TransProcess: time:" + sDateTime + ", price:" + df1.format(this.curt_price) + ", sell for bbi_down in half, status from " + STATUS.HALF + " to " + STATUS.READY);
//				sell_position = 10;
//			}
		
			else if (this.is_boll_bbi_or_mid_down) {
				StatusChange(STATUS.READY, "bbi_or_mid_down in half", sDateTime, 1);
				sell_position = 10;
			}
			
			else if (this.is_macd_top) {
				StatusChange(STATUS.READY, "macd_top in half", sDateTime, 1);
				sell_position = 10;
			}
			
			else if (this.is_macd_down) {
				StatusChange(STATUS.READY, "macd_down in half", sDateTime, 1);
				sell_position = 10;
			}
		}
		
		//止损
		if (this.is_zhisun == true) {
			StatusChange(STATUS.READY, "zhisun", sDateTime, 1);
			sell_position = 10;
		}
		
		if (sell_position > 0) {
			this.buy_reason = 0;
		}
		
		return sell_position;
	}
	
	public void CleanStatus() {
		return;
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
		is_ma_up = false;
		is_ma_smooth = false;
		
		//贴上轨
		is_boll_up	= false;
		is_boll_bbi_down	= false;
		is_boll_mid_down	= false;
		is_last_boll_mid_down = false;
		is_boll_bbi_or_mid_down = false;
		is_boll_narrow = false;
		
		//macd顶、底
		is_macd_top		= false;	//macd>0 顶
		is_macd_bottom	= false;	//macd<0 底
		is_macd_up		= false;	//macd>0 变短再变长
		is_macd_down	= false;	//macd<0 变短再变长
		is_macd_larger	= false;	//macd变长
		
		is_up_yin = false;
		is_down_last_open = false;
		
		is_zhisun = false;
		
		is_break_up_ma60 = false;
		
		
		is_up_boll_mid = false;
		is_macd_changeto_red = false;
	}
	
}
