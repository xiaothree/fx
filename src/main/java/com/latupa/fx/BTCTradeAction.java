package com.latupa.stock;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 交易操作
 * @author latupa
 *
 * TODO
 * 1. 部分成交的处理方式，取决于部分成交是否会成为委托的最终状态，暂不处理
 */

public class BTCTradeAction {
	
	public static final Log log = LogFactory.getLog(BTCTradeAction.class);
	
	BTCApi btc_api;
	
	public BTCTradeAction() {
		this.btc_api	= new BTCApi();
	}
	
	/**
	 * 获取账户信息
	 * @return
	 * @throws InterruptedException
	 */
	public UserInfo DoUserInfo() throws InterruptedException {
		int count = 0;
		UserInfo user_info	= btc_api.ApiUserInfo();
		while (user_info == null) {
			count++;
			if (count == 5) {
				break;
			}
			Thread.sleep(3000);
			user_info = btc_api.ApiUserInfo();
		}
		Thread.sleep(3000);
		return user_info;
	}
	
	/**
	 * 获取当前行情
	 * @return
	 * @throws InterruptedException 
	 */
	public Ticker DoTicker() throws InterruptedException {
		int count = 0;
		Ticker ticker = btc_api.ApiTicker();
		while (ticker == null) {
			count++;
			if (count == 5) {
				break;
			}
			Thread.sleep(5000);
			ticker = btc_api.ApiTicker();
		}
		return ticker;
	}
	
	/**
	 * 撤销委托
	 * @param order_id
	 * @return
	 * @throws InterruptedException 
	 */
	public boolean DoCancelOrder(String order_id) throws InterruptedException {
		int count = 0;
		boolean ret	= btc_api.ApiCancelOrder(order_id);
		while (ret != true) {
			count++;
			if (count == 5) {
				break;
			}
			Thread.sleep(5000);
			ret	= btc_api.ApiCancelOrder(order_id);
		}
		
		return ret;
	}
	
	/**
	 * 委托买卖
	 * @param type
	 * @param price
	 * @param quantity
	 * @return
	 * @throws InterruptedException
	 */
	public String DoTrade(String type, double price, double quantity) throws InterruptedException {
		int count = 0;
		String order_id = btc_api.ApiTrade(type, price, quantity);
		while (order_id == null) {
			count++;
			if (count == 5) {
				break;
			}
			
			Thread.sleep(5000);
			order_id = btc_api.ApiTrade(type, price, quantity);
		}
		return order_id;
	}
	
	/**
	 * 获取订单状态，立即返回
	 * @param order_id
	 * @return
	 * @throws InterruptedException
	 */
	public TradeRet DoGetOrderNow(String order_id) throws InterruptedException {
		int count = 0;
		TradeRet trade_ret = btc_api.ApiGetOrder(order_id);
		while (trade_ret == null) {
			count++;
			if (count == 5) {
				break;
			}
			
			Thread.sleep(10000);
			trade_ret = btc_api.ApiGetOrder(order_id);
		}
		
		return trade_ret;
	}
	
	/**
	 * 获取订单状态
	 * @param order_id
	 * @return
	 * @throws InterruptedException
	 */
	public TradeRet DoGetOrder(String order_id) throws InterruptedException {
		int count = 0;
		TradeRet trade_ret = btc_api.ApiGetOrder(order_id);
		while (trade_ret == null || 
				(trade_ret != null && trade_ret.status != TradeRet.STATUS.TOTAL)) {
			count++;
			if (count == 5) {
				break;
			}
			
			Thread.sleep(10000);
			trade_ret = btc_api.ApiGetOrder(order_id);
		}
		
		return trade_ret;
	}
	
	/**
	 * 
	 * @param invest_position
	 * @param price 当前价格
	 * @param data_cycle K线周期
	 * @return
	 * @throws InterruptedException
	 */
	public ArrayList<TradeRet> DoBuy(int invest_position, double price, int data_cycle) throws InterruptedException {
		
		long start_stamp_sec = System.currentTimeMillis() / 1000;
		
		ArrayList<TradeRet> tr_list = new ArrayList<TradeRet>();
		
		int buy_count = 0;//尝试交易次数
		
		//获取账户中的金额
		UserInfo user_info = DoUserInfo();
		if (user_info == null) {
			log.error("get account info failed!");
			return null;
		}
		user_info.Show();
		
		double invest_cny = (invest_position == 10) ? user_info.cny : (user_info.cny * invest_position / 10);
		log.info("invest_cny:" + invest_cny);
		
		double buy_price_max = price * 1.003;
		
		while (true) {
			
			long curt_stamp_sec = System.currentTimeMillis() / 1000;
			if ((curt_stamp_sec - start_stamp_sec) > (data_cycle * 0.8)) {//整个买入时间消耗不能多于K线周期，确保有时间处理下一条K线
				if (tr_list.size() > 0) {//如果之前分批买入有成功的，那么也返回成功
					log.info("cost " + (curt_stamp_sec - start_stamp_sec) + "secs, has buyed buy_total_quantity and over " + buy_count + " times, finish");
					return tr_list;
				}
				else {
					log.error("force buy failed!");
					return null;
				}
			}
			
			buy_count++;
			log.info("buy for " + buy_count + " times");
			
			//获取当前买一价
			Ticker ticker = DoTicker();
			if (ticker == null) {
				log.error("get curt price failed!");
				return null;
			}
			ticker.Show();
			
			//计算委托价格
			double buy_price	= (ticker.buy + ticker.sell) / 2;
			
			if (buy_price > buy_price_max) {//超过买入预期最高价
				Thread.sleep(3000);
				continue;
			}
				
//			double buy_price	= (ticker.buy + ticker.sell) / 2 + BTCApi.TRADE_DIFF;
//			double buy_price	= ticker.buy + BTCApi.TRADE_DIFF;
			
			double buy_quantity	= invest_cny / (buy_price + BTCApi.TRADE_DIFF);
			
			//委托买单
			log.info("buy total cny:" + invest_cny + ", price:" + buy_price + ", quantify:" + buy_quantity + ", buy1:" + ticker.buy + ", sell1:" + ticker.sell);
			String order_id	= DoTrade("buy", buy_price, buy_quantity);
			if (order_id == null) {
				log.error("trade for buy failed!");
				return null;
			}
			log.info("order_id:" + order_id);
			
			//避免委托买单和查单过于频繁
			Thread.sleep(10000);

			//获取委托的结果
			TradeRet trade = DoGetOrder(order_id);
			if (trade == null) {
				log.error("get order result failed!");
				return null;
			}
			else {
				trade.Show();
				
				//有交易成功的部分
				if (trade.deal_amount > 0) {
					
					tr_list.add(trade);
					
					invest_cny -= trade.avg_price * trade.deal_amount;
					
					log.info("remain cny:" + invest_cny);
					
					if (invest_cny <= 0) {
						log.info("buy reach invest_cny, finish");
						return tr_list;
					}
				}
				
				if (trade.status == TradeRet.STATUS.TOTAL) {
					log.info("order return total, finish");
					return tr_list;
				}
				else if (trade.status == TradeRet.STATUS.PARTER) {//如果是部分成交
					log.info("order return parter");
					//如果剩余部分小于最小买入份额，就返回
					if (trade.amount - trade.deal_amount < 0.01) {
						log.info("need buy(" + trade.amount + "), deal(" + trade.deal_amount + "), finish");
						return tr_list;
					}
				}
			}
			
			//撤销委托
			boolean ret = DoCancelOrder(order_id);
			if (ret != true) {
				log.error("cancel trade failed!");
				return null;
			}
		}
	}
	
	/**
	 * 卖出操作
	 * @param position 仓位比例
	 * @return
	 * @throws InterruptedException
	 */
	public ArrayList<TradeRet> DoSell(int position) throws InterruptedException {
		
		ArrayList<TradeRet> tr_list = new ArrayList<TradeRet>();
		
		//获取账户中的数量
		UserInfo user_info = DoUserInfo();
		if (user_info == null) {
			log.error("get account info failed!");
			return null;
		}
		user_info.Show();
		
		//如果没有持有中的数量，则直接返回
		if (user_info.btc == 0) {
			log.info("no quantity to sell");
			return tr_list;
		}
		
		//计算卖出数量
		double sell_quantity;
		
		int sell_count = 0;//尝试交易次数
		if (position == 10) {
			sell_quantity	= user_info.btc;
			
			while (sell_quantity > 0) {
				
				sell_count++;
				log.info("sell for " + sell_count + " times");
				
				//获取当前卖一价
				Ticker ticker = DoTicker();
				if (ticker == null) {
					log.error("get curt price failed!");
					return null;
				}
				ticker.Show();
				
				//计算卖出委托价
				double sell_price = (ticker.sell + ticker.buy) / 2;
//				double sell_price = (ticker.sell + ticker.buy) / 2 - BTCApi.TRADE_DIFF * sell_count;
				//double sell_price	= ticker.sell - BTCApi.TRADE_DIFF * sell_count;
				
				//委托卖单
				log.info("sell price:" + sell_price + ", quantity:" + sell_quantity + ", buy1:" + ticker.buy + ", sell1:" + ticker.sell);
				String order_id	= DoTrade("sell", sell_price, sell_quantity);
				if (order_id == null) {
					log.error("trade for sell failed!");
					return null;
				}
				log.info("order_id:" + order_id);
				
				//避免委托卖单和查单过于频繁
				Thread.sleep(10000);
				
				//获取委托的结果
				TradeRet trade = DoGetOrder(order_id);
				if (trade == null) {
					log.error("get order result failed!");
					return null;
				}
				else {
					trade.Show();
					
					if (trade.status == TradeRet.STATUS.TOTAL) {
						tr_list.add(trade);
						return tr_list;
					}
				}
				
				//撤销委托
				boolean ret = DoCancelOrder(order_id);
				if (ret != true) {
					log.error("cancel trade failed!");
					return null;
				}
				
				Thread.sleep(5000);
				
				//获取委托的结果
				trade = DoGetOrderNow(order_id);
				if (trade == null) {
					log.error("get order result failed!");
					return null;
				}
				else {
					log.info("update get order " + order_id);
					trade.Show();
					
					if (trade.deal_amount > 0) {
						tr_list.add(trade);
					}
					
					if (trade.status == TradeRet.STATUS.TOTAL) {
						return tr_list;
					}
				}
				
				Thread.sleep(5000);
				
				//获取账户中的数量
				user_info = DoUserInfo();
				if (user_info == null) {
					log.error("get account info failed!");
					return null;
				}
				user_info.Show();
				
				sell_quantity = user_info.btc;
				
				Thread.sleep(5000);
			}
		}
		else {
			sell_quantity	= user_info.btc * position / 10;
			
			while (sell_quantity > 0) {
				
				sell_count++;
				log.info("sell for " + sell_count + " times");
				
				//获取当前卖一价
				Ticker ticker = DoTicker();
				if (ticker == null) {
					log.error("get curt price failed!");
					return null;
				}
				ticker.Show();
				
				//计算卖出委托价
				double sell_price = (ticker.sell + ticker.buy) / 2;
//				double sell_price = (ticker.sell + ticker.buy) / 2 - BTCApi.TRADE_DIFF * sell_count;
				//double sell_price	= ticker.sell - BTCApi.TRADE_DIFF * sell_count;
				
				//委托卖单
				log.info("sell price:" + sell_price + ", quantity:" + sell_quantity + ", buy1:" + ticker.buy + ", sell1:" + ticker.sell);
				String order_id	= DoTrade("sell", sell_price, sell_quantity);
				if (order_id == null) {
					log.error("trade for sell failed!");
					return null;
				}
				log.info("order_id:" + order_id);
				
				//避免委托卖单和查单过于频繁
				Thread.sleep(10000);
				
				//获取委托的结果
				TradeRet trade = DoGetOrder(order_id);
				if (trade == null) {
					log.error("get order result failed!");
					return null;
				}
				else {
					trade.Show();
					
					if (trade.deal_amount > 0) {
						tr_list.add(trade);
					}
					
					if (trade.status == TradeRet.STATUS.TOTAL) {
						return tr_list;
					}
					else if (trade.status == TradeRet.STATUS.PARTER) {//如果是部分成交，则继续卖出剩余的部分
						sell_quantity -= trade.deal_amount;
					}
				}
				
				//撤销委托
				boolean ret = DoCancelOrder(order_id);
				if (ret != true) {
					log.error("cancel trade failed!");
					return null;
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BTCTradeAction btc_ta = new BTCTradeAction();
		try {
			btc_ta.DoSell(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
