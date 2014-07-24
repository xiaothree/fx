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

		$sql = "select time, open, close, high, low, truncate(macd, 2) as macd, truncate(diff, 2) as diff, truncate(dea, 2) as dea, truncate(upper, 2) as upper, truncate(mid, 2) as mid, truncate(lower, 2) as lower, truncate(bbi, 2) as bbi from ".$table_name." where time >= '".$start."'";
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
