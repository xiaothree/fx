<?php

	define ("MYSQL_HOST", "127.0.0.1");
	define ("MYSQL_NAME", "latupa");
	define ("MYSQL_PASSWORD", "latupa");
	define ("MYSQL_DB", "oanda");
	define ("TABLE_PRE", "oanda_price_");

	function query_summary_daily($pair, $table_postfix, $start, $end) {

		$db = mysql_connect(MYSQL_HOST, MYSQL_NAME, MYSQL_PASSWORD);
		if (!$db) {
			die('Could not connect: '.mysql_error());
		}
		//echo 'Connected successfully';

		$table_name = TABLE_PRE.$pair."__".$table_postfix;

		mysql_query("use ".MYSQL_DB);
		mysql_query("set names utf8");

		$sql = "select time, truncate(open, 2) as open, truncate(close, 2) as close, truncate(high, 2) as high, truncate(low, 2) as low, truncate(macd, 2) as macd, truncate(diff, 2) as diff, truncate(dea, 2) as dea, truncate(upper, 2) as upper, truncate(mid, 2) as mid, truncate(lower, 2) as lower, truncate(bbi, 2) as bbi, truncate(ma5, 2) as ma5, truncate(ma10, 2) as ma10, truncate(ma20, 2) as ma20, truncate(ma30, 2) as ma30, truncate(ma60, 2) as ma60, truncate(ma120, 2) as ma120 from ".$table_name." where time >= '".$start."'";
		if ($end != "") {
			$sql .=  " and time <= '".$end."'";
		}
		$sql .= " order by time desc";

		//$sql = "select time, open, close, high, low from ".$table_name." limit 2700,10";
		//echo $sql."\n";
		$result = mysql_query($sql);
		if (!$result) {
		    die('Invalid query: '.mysql_error());
		}

		$k_daily = array();
		while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
			$k_daily[] = $row;
		}

		//必须是正序
		$k_daily_asc = array_reverse($k_daily);

		mysql_close($db);
		
		return $k_daily_asc;
	}
?>
