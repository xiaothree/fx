package com.latupa.stock;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 布林线的返回结果
 * @author latupa
 *
 */
class BollRet {
        double upper;
        double mid;
        double lower;
        double bbi;
        
        public BollRet() {
        }
        
        public BollRet(BollRet bollret) {
                if (bollret != null) {
                        this.upper        = bollret.upper;
                        this.mid        = bollret.mid;
                        this.lower        = bollret.lower;
                        this.bbi        = bollret.bbi;
                }
        }
}

/**
 * macd的返回结果
 * @author latupa
 *
 */
class MacdRet {
        double diff;
        double dea;
        double macd;
        double ema13;
        double ema26;
        
        public MacdRet() {
        }
        
        public MacdRet(MacdRet macdret) {
                if (macdret != null) {
                        this.diff        = macdret.diff;
                        this.dea        = macdret.dea;
                        this.macd        = macdret.macd;
                        this.ema13        = macdret.ema13;
                        this.ema26        = macdret.ema26;
                }
        }
}

/**
 * 均线的通用存储结构
 * @author latupa
 *
 */
class MaRet {
        double ma5;
        double ma10;
        double ma20;
        double ma30;
        double ma60;
        double ma120;
        
        public MaRet() {
                
        }
        
        public MaRet(MaRet maret) {
                if (maret != null) {
                        this.ma5 = maret.ma5;
                        this.ma10 = maret.ma10;
                        this.ma20 = maret.ma20;
                        this.ma30 = maret.ma30;
                        this.ma60 = maret.ma60;
                        this.ma120 = maret.ma120;
                }
        }
}

/**
 * BTC价格计算公式
 * @author latupa
 */
public class BTCFunc {
	
	private static final Log log = LogFactory.getLog(BTCFunc.class);
	
	/**
	 * 标准差，为计算布林线使用
	 * @param record_map 数据映射
	 * @param p_time 指定某个时间的标准差
	 * @return
	 */
	public double std(TreeMap<String, BTCTotalRecord> record_map, String p_time, int count, double average) {
		
		double sum = 0;
		int i = 0;
		for (String time : record_map.headMap(p_time, true).descendingKeySet().toArray(new String[0])) {
			sum += (record_map.get(time).close - average) * (record_map.get(time).close - average);
			i++;
			if (i == count) {
				break;
			}
		}
		
		sum /= (count - 1);
		
		return Math.sqrt(sum);
	}
	
	/**
	 * 计算布林线相关指标
	 * @param record_map 数据映射
	 * @param p_time 指定某个时间的布林线
	 * @return
	 */
	public BollRet boll(TreeMap<String, BTCTotalRecord> record_map, String p_time) {
		final int n = 26;
		final int p = 2;
		
		BollRet br = new BollRet();
		TreeMap<Integer, Double> maret_map;
		
		//先计算该周期的均值
		ArrayList<Integer> mas = new ArrayList<Integer>();
		mas.add(new Integer(n));
		maret_map = ma(record_map, p_time, mas, 0);
		br.mid = maret_map.get(new Integer(n)).doubleValue();
		if (br.mid == 0) {
			log.debug("return null for mid is 0");
			return null;
		}
		
		br.upper = br.mid + p * std(record_map, p_time, n, br.mid);
		br.lower = br.mid - p * std(record_map, p_time, n, br.mid);
		
		mas.clear();
		mas.add(new Integer(3));
		mas.add(new Integer(6));
		mas.add(new Integer(12));
		mas.add(new Integer(24));
		maret_map = ma(record_map, p_time, mas, 0);
		
		br.bbi = (maret_map.get(new Integer(3)).doubleValue() +
				maret_map.get(new Integer(6)).doubleValue() +
				maret_map.get(new Integer(12)).doubleValue() +
				maret_map.get(new Integer(24)).doubleValue()) / 4;
		
		log.debug("boll, mid:" + br.mid + ", upper:" + br.upper + ", lower:" + br.lower + ", bbi:" + br.bbi);
		
		return br;
	}
	
	/**
	 * ema 每个交易日计算，因此只需要前一日的ema即可
	 * @param curt_close
	 * @param last_ema 上一个交易日的ema
	 * @param size ema的周期
	 * @return
	 */
	private double ema2(double curt_close,  double last_ema, int size) {
		return ((2 * curt_close + (size - 1) * last_ema) / (size + 1));
	}
	
	/**
	 * ema recursive
	 * @param x
	 * @param n
	 * @return
	 */
//	private double ema(ArrayList<Double> x, int n, int size) {
//		log.debug("n:" + n + "->" + x.get(n - 1));
//		if (n == 1) {
//			return x.get(n - 1);
//		}
//		else {
//			return (2 * x.get(n - 1) + (size - 1) * ema(x, n - 1, size)) / (size + 1);
//		}
//	}
	
	
	/**
	 * 截取整个map的一部分
	 * @param record_map 数据映射
	 * @param p_time 指定某个时间的布林线
	 * @param pre_cycles 获取pre_cycles的数据
	 * @return
	 */
//	private ArrayList<BTCTotalRecord> GetEMAPriceArray(TreeMap<String, BTCTotalRecord> record_map, String p_time, int pre_cycles) {
//		ArrayList<BTCTotalRecord> cut_list = new ArrayList<BTCTotalRecord>();
//		 
//		BTCTotalRecord last_record	= null;
//		int count = 0;
//		//先以时间降序写入到数组中
//		for (String time : record_map.headMap(p_time, true).descendingKeySet().toArray(new String[0])) {
//			BTCTotalRecord record = record_map.get(time);
//			
//			cut_list.add(count, record);
//			count++;
//			last_record = record;
//			if (count == pre_cycles) {
//				break;
//			}
//		}
//
//		if (last_record.macd_record == null) {
//			last_record.macd_record.ema13	= last_record.close;
//			last_record.macd_record.ema26	= last_record.close;
//		}
//		
//		while (count < pre_cycles) {
//			cut_list.add(count, last_record);
//			count++;
//		}
//		
//		//转换成时间和数组位置成正序
//		Collections.reverse(cut_list);
//		
//		return cut_list;
//	}
	
	/**
	 * MACD计算公式
	 * y=ema(x,n), y=[2*x+(n-1)y']/(n+1),其中y'表示上一周期y的值
	 * @param btc_data 数据
	 * @param p_time 指定某个时间的macd
	 * @return
	 * @throws ParseException
	 */
	public MacdRet macd(BTCData btc_data, String p_time) throws ParseException {
		int p_short = 13;
		int p_long = 26;
		int p_m = 9;
		
		BTCTotalRecord record	= btc_data.BTCRecordOptGetByCycle(0, p_time);
		BTCTotalRecord record_1cycle_before	= btc_data.BTCRecordOptGetByCycle(1, p_time);
		
		double ema13;
		double ema26;
		double diff_ema9;
		if (record_1cycle_before.macd_record == null) {
			ema13	= record.close;
			ema26	= record.close;
			diff_ema9	= ema13 - ema26;
		}
		else {
			ema13	= ema2(record.close, record_1cycle_before.macd_record.ema13, p_short);
			ema26	= ema2(record.close, record_1cycle_before.macd_record.ema26, p_long);
			diff_ema9	= ema2(ema13 - ema26, record_1cycle_before.macd_record.dea, p_m);
		}
		
		MacdRet mr = new MacdRet();
		
		mr.ema13	= ema13;
		mr.ema26	= ema26;
		mr.diff		= ema13 - ema26;
		mr.dea		= diff_ema9;
		mr.macd		= 2 * (mr.diff - mr.dea);
		log.debug("macd, diff:" + mr.diff + ", dea:" + mr.dea + ", macd:" + mr.macd + ", ema13:" + ema13 + ", ema26:" + ema26);
		
		return mr;
	}
	
	
	/**
	 * 均线
	 * @param record_map 数据映射
	 * @param p_time 指定某个时间的均线
	 * @param cycles 均线周期列表（按照从小到大排序）
	 * @param pre_cycles 计算pre_cycles之前的数据，如果为0，表示time当时
	 * @return
	 */
	public TreeMap<Integer, Double> ma(TreeMap<String, BTCTotalRecord> record_map, String p_time, ArrayList<Integer> cycles, int pre_cycles) {
		
		double sum = 0;
		int count = 0;
		int i = 0;  //处理到的均线坐标
		
		TreeMap<Integer, Double> maret_map = new TreeMap<Integer, Double>();
		
		for (String time : record_map.headMap(p_time, true).descendingKeySet().toArray(new String[0])) {
			
			if (pre_cycles > 0) {
				pre_cycles--;
				continue;
			}
			
			BTCTotalRecord record = record_map.get(time);
			sum += record.close;
			count++;
			
			// 循环处理每个均线周期
			if (count == cycles.get(i)) {
				maret_map.put(count, sum / count);
				i++;
				// 所有给定周期已经计算完，则退出
				if (i == cycles.size()) {
					break;
				}
			}
		}
		
		//补全时间不足的返回结果
		while (i < cycles.size()) {
			maret_map.put(cycles.get(i), 0.0);
			i++;
		}
		
		for (int cycle : maret_map.keySet()) {
			log.debug(cycle + "->" + maret_map.get(cycle));
		}
		
		return maret_map;
	}
}
