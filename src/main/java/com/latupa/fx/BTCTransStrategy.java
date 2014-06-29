package com.latupa.stock;

public interface BTCTransStrategy {
	
	/**
	 * 是否买入
	 * 先简化，如果需要买，则全部资金买入，返回买入原因int，如果为0则表示不买
	 * @return
	 */
	public int IsBuy(String sDateTime);
	
	/**
	 * 是否卖出
	 * 如果是卖出则返回1-10，表示卖出的比例；否则返回0
	 * @return
	 */
	public int IsSell(String sDateTime);
	
	/**
	 * check状态是否变化
	 * @param btc_data
	 */
	public void CheckPoint(double buy_price, BTCData btc_data, String sDateTime);
	
	/**
	 * 初始化相关值
	 */
	public void InitPoint();
}
