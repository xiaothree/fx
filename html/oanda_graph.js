    var chart;
    var chartData = [];
    //var data_cycle;

    AmCharts.ready(function () {
	alert("start");
	// generate some random data first
	generateChartData();

	// SERIAL CHART    
	chart = new AmCharts.AmStockChart();
	chart.pathToImages = "amstockcharts/amcharts/images/";
	chart.balloon.horizontalPadding = 13;

	// As we have minutely data, we should set minPeriod to "mm"
	var categoryAxesSettings = new AmCharts.CategoryAxesSettings();
	categoryAxesSettings.minPeriod = "mm"; //指定数据的最小时间间隔是分钟
	categoryAxesSettings.groupToPeriods = ["mm"];//指定按照分钟聚合，也就是说不聚合
	categoryAxesSettings.position = "bottom"; 
	//底部时间轴静态显示格式
	categoryAxesSettings.dateFormats = [{period:'fff',format:'JJ:NN:SS'},
		{period:'ss',format:'JJ:NN:SS'},
		{period:'mm',format:'DD JJ:NN'},
		{period:'hh',format:'JJ:NN'},
		{period:'DD',format:'MMM DD'},
		{period:'WW',format:'MMM DD'},
		{period:'MM',format:'MMM'},
		{period:'YYYY',format:'YYYY'}];
	chart.categoryAxesSettings = categoryAxesSettings;

	// DATASET //////////////////////////////////////////
	var dataSet = new AmCharts.DataSet();
	dataSet.fieldMappings = [{
		fromField: "open",
		toField: "open"
	}, {
		fromField: "close",
		toField: "close"
	}, {
		fromField: "high",
		toField: "high"
	}, {
		fromField: "low",
		toField: "low"
	}, {
		fromField: "macd",
		toField: "macd"
	}, {
		fromField: "diff",
		toField: "diff"
	}, {
		fromField: "dea",
		toField: "dea"
	}, {
		fromField: "upper",
		toField: "upper"
	}, {
		fromField: "mid",
		toField: "mid"
	}, {
		fromField: "lower",
		toField: "lower"
	}, {
		fromField: "bbi",
		toField: "bbi"
	}, {
		fromField: "ma5",
		toField: "ma5"
	}, {
		fromField: "ma10",
		toField: "ma10"
	}, {
		fromField: "ma20",
		toField: "ma20"
	}, {
		fromField: "ma30",
		toField: "ma30"
	}, {
		fromField: "ma60",
		toField: "ma60"
	}, {
		fromField: "ma120",
		toField: "ma120"
	}];
	dataSet.color = "#7f8da9";
	dataSet.dataProvider = chartData;
	dataSet.categoryField = "date";

        chart.dataSets = [dataSet];

	// PANELS ///////////////////////////////////////////
	// K线的控制面板
	stockPanel = new AmCharts.StockPanel();
	stockPanel.title = "K lines";
	stockPanel.percentHeight = 70;

	// graph of first stock panel
	var graph = new AmCharts.StockGraph();
	graph.type = "candlestick";
	graph.openField = "open";
	graph.closeField = "close";
	graph.highField = "high";
	graph.lowField = "low";
	graph.valueField = "close";
	graph.lineColor = "#7f8da9";
	graph.fillColors = "#7f8da9";
	graph.negativeLineColor = "#db4c3c";
	graph.negativeFillColors = "#db4c3c";
	graph.fillAlphas = 1;
	graph.balloonText = "open:<b>[[open]]</b><br>close:<b>[[close]]</b><br>low:<b>[[low]]</b><br>high:<b>[[high]]</b>";
	graph.legendAlpha = 1;
	graph.legendColor = "#FCF930";
	graph.visibleInLegend = true;
	graph.legendValueText = "close:[[close]]";
	graph.useDataSetColors = false;
	stockPanel.addStockGraph(graph);

	// boll upper graph
	var graph4 = new AmCharts.StockGraph();
	graph4.type = "line";
	graph4.valueField = "upper";
	graph4.lineColor = "#FCF930";
	graph4.lineThickness = 1;
	graph4.fillColors = "#FCF930";
	graph4.showBalloon = false;
	//graph4.balloonText = "diff:<b>[[diff]]</b>";
	graph4.legendAlpha = 1;
	graph4.legendColor = "#FCF930";
	graph4.visibleInLegend = true;
	graph4.legendValueText = "upper:[[upper]]";
	graph4.useDataSetColors = false;
	stockPanel.addStockGraph(graph4);

	// boll mid graph
	var graph5 = new AmCharts.StockGraph();
	graph5.type = "line";
	graph5.valueField = "mid";
	graph5.lineColor = "#FFFFFF";
	graph5.lineThickness = 1;
	graph5.fillColors = "#FFFFFF";
	graph5.showBalloon = false;
	//graph5.balloonText = "diff:<b>[[diff]]</b>";
	graph5.legendAlpha = 1;
	graph5.legendColor = "#FFFFFF";
	graph5.visibleInLegend = true;
	graph5.legendValueText = "mid:[[mid]]";
	graph5.useDataSetColors = false;
	stockPanel.addStockGraph(graph5);

	// boll lower graph
	var graph6 = new AmCharts.StockGraph();
	graph6.type = "line";
	graph6.valueField = "lower";
	graph6.lineColor = "#E60CD4";
	graph6.lineThickness = 1;
	graph6.fillColors = "#E60CD4";
	graph6.showBalloon = false;
	//graph6.balloonText = "diff:<b>[[diff]]</b>";
	graph6.legendAlpha = 1;
	graph6.legendColor = "#E60CD4";
	graph6.visibleInLegend = true;
	graph6.legendValueText = "lower:[[lower]]";
	graph6.useDataSetColors = false;
	stockPanel.addStockGraph(graph6);

	// boll bbi graph
	var graph7 = new AmCharts.StockGraph();
	graph7.type = "line";
	graph7.valueField = "bbi";
	graph7.lineColor = "#12EB16";
	graph7.lineThickness = 1;
	graph7.fillColors = "#12EB16";
	graph7.showBalloon = false;
	//graph7.balloonText = "diff:<b>[[diff]]</b>";
	graph7.legendAlpha = 1;
	graph7.legendColor = "#12EB16";
	graph7.visibleInLegend = true;
	graph7.legendValueText = "bbi:[[bbi]]";
	graph7.useDataSetColors = false;
	stockPanel.addStockGraph(graph7);

	// ma ma5 graph
	var graph8 = new AmCharts.StockGraph();
	graph8.type = "line";
	graph8.valueField = "ma5";
	graph8.lineColor = "#FFFFFF";
	graph8.lineThickness = 1;
	graph8.fillColors = "#FFFFFF";
	graph8.showBalloon = false;
	//graph8.balloonText = "diff:<b>[[diff]]</b>";
	graph8.legendAlpha = 1;
	graph8.legendColor = "#FFFFFF";
	graph8.visibleInLegend = true;
	graph8.legendValueText = "ma5:[[ma5]]";
	graph8.useDataSetColors = false;
	stockPanel.addStockGraph(graph8);
	stockPanel.hideGraph(graph8);

	// ma ma10 graph
	var graph9 = new AmCharts.StockGraph();
	graph9.type = "line";
	graph9.valueField = "ma10";
	graph9.lineColor = "#FCF930";
	graph9.lineThickness = 1;
	graph9.fillColors = "#FCF930";
	graph9.showBalloon = false;
	//graph8.balloonText = "diff:<b>[[diff]]</b>";
	graph9.legendAlpha = 1;
	graph9.legendColor = "#FCF930";
	graph9.visibleInLegend = true;
	graph9.legendValueText = "ma10:[[ma10]]";
	graph9.useDataSetColors = false;
	stockPanel.addStockGraph(graph9);
	stockPanel.hideGraph(graph9);

	// ma ma20 graph
	var graph10 = new AmCharts.StockGraph();
	graph10.type = "line";
	graph10.valueField = "ma20";
	graph10.lineColor = "#E60CD4";
	graph10.lineThickness = 1;
	graph10.fillColors = "#E60CD4";
	graph10.showBalloon = false;
	//graph8.balloonText = "diff:<b>[[diff]]</b>";
	graph10.legendAlpha = 1;
	graph10.legendColor = "#E60CD4";
	graph10.visibleInLegend = true;
	graph10.legendValueText = "ma20:[[ma20]]";
	graph10.useDataSetColors = false;
	stockPanel.addStockGraph(graph10);
	stockPanel.hideGraph(graph10);

	// ma ma30 graph
	var graph11 = new AmCharts.StockGraph();
	graph11.type = "line";
	graph11.valueField = "ma30";
	graph11.lineColor = "#12EB16";
	graph11.lineThickness = 1;
	graph11.fillColors = "#12EB16";
	graph11.showBalloon = false;
	//graph8.balloonText = "diff:<b>[[diff]]</b>";
	graph11.legendAlpha = 1;
	graph11.legendColor = "#12EB16";
	graph11.visibleInLegend = true;
	graph11.legendValueText = "ma30:[[ma30]]";
	graph11.useDataSetColors = false;
	stockPanel.addStockGraph(graph11);
	stockPanel.hideGraph(graph11);

	// ma ma60 graph
	var graph12 = new AmCharts.StockGraph();
	graph12.type = "line";
	graph12.valueField = "ma60";
	graph12.lineColor = "#FFFFFF";
	graph12.lineThickness = 2;
	graph12.fillColors = "#FFFFFF";
	graph12.showBalloon = false;
	//graph8.balloonText = "diff:<b>[[diff]]</b>";
	graph12.legendAlpha = 1;
	graph12.legendColor = "#FFFFFF";
	graph12.visibleInLegend = true;
	graph12.legendValueText = "ma60:[[ma60]]";
	graph12.useDataSetColors = false;
	stockPanel.addStockGraph(graph12);
	stockPanel.hideGraph(graph12);

	// ma ma120 graph
	var graph13 = new AmCharts.StockGraph();
	graph13.type = "line";
	graph13.valueField = "ma120";
	graph13.lineColor = "#0033FF";
	graph13.lineThickness = 1;
	graph13.fillColors = "#0033FF";
	graph13.showBalloon = false;
	//graph8.balloonText = "diff:<b>[[diff]]</b>";
	graph13.legendAlpha = 1;
	graph13.legendColor = "#0033FF";
	graph13.visibleInLegend = true;
	graph13.legendValueText = "ma120:[[ma120]]";
	graph13.useDataSetColors = false;
	stockPanel.addStockGraph(graph13);
	stockPanel.hideGraph(graph13);

	var stockLegend = new AmCharts.StockLegend();
	stockLegend.markerType = "none";
	stockLegend.markerSize = 0;
	stockLegend.valueTextRegular = undefined;
	stockLegend.valueWidth = 250;
	stockPanel.stockLegend = stockLegend;

	chart.panels = [stockPanel];

	// MACD的控制面板
	newstockPanel = new AmCharts.StockPanel();
	newstockPanel.title = "MACD";
	newstockPanel.percentHeight = 30;

	var newstockLegend = new AmCharts.StockLegend();
	newstockLegend.markerType = "none";
	newstockLegend.markerSize = 0;
	newstockLegend.valueTextRegular = undefined;
	newstockLegend.valueWidth = 250;
	newstockPanel.stockLegend = newstockLegend;

	// diff graph
	var graph1 = new AmCharts.StockGraph();
	graph1.type = "line";
	graph1.valueField = "diff";
	graph1.lineColor = "#FFFFFF";
	graph1.lineThickness = 2;
	graph1.fillColors = "#FFFFFF";
	graph1.showBalloon = false;
	//graph1.balloonText = "diff:<b>[[diff]]</b>";
	graph1.legendAlpha = 1;
	graph1.legendColor = "#FFFFFF";
	graph1.visibleInLegend = true;
	graph1.legendValueText = "diff:[[diff]]";
	graph1.useDataSetColors = false;
	newstockPanel.addStockGraph(graph1);

	// dea graph
	var graph2 = new AmCharts.StockGraph();
	graph2.type = "line";
	graph2.valueField = "dea";
	graph2.lineColor = "#FCF930";
	graph2.lineThickness = 2;
	graph2.fillColors = "#FCF930";
	graph2.showBalloon = false;
	//graph2.balloonText = "dea:<b>[[dea]]</b>";
	graph2.legendAlpha = 1;
	graph2.legendColor = "#FCF930";
	graph2.visibleInLegend = true;
	graph2.legendValueText = "dea:[[dea]]";
	graph2.useDataSetColors = false;
	newstockPanel.addStockGraph(graph2);

	// macd graph
	var graph3 = new AmCharts.StockGraph();
	graph3.type = "column";
	graph3.valueField = "macd";

	//填充柱子的颜色并隐藏它们的边框
	graph3.lineAlpha = 0;
	graph3.fillAlphas = 0.8;

	graph3.lineColor = "#D71244";
	graph3.lineThickness = 2;
	graph3.fillColors = "#D71244";
	graph3.negativeLineColor = "#197E0E";
	graph3.negativeFillColors = "#197E0E";
	graph3.showBalloon = false;
	//graph3.balloonText = "macd1:<b>[[macd]]</b>";
	graph3.legendAlpha = 1;
	graph3.legendColor = "#D71244";
	graph3.visibleInLegend = true;
	graph3.legendValueText = "macd:[[macd]]";
	graph3.useDataSetColors = false;
	newstockPanel.addStockGraph(graph3);

	chart.addPanelAt(newstockPanel, 1);

	// OTHER SETTINGS ////////////////////////////////////
	var sbsettings = new AmCharts.ChartScrollbarSettings();
	sbsettings.graph = graph;
	sbsettings.graphType = "line";
	sbsettings.usePeriod = "mm"; //聚合周期，指定为分钟，即不聚合
	chart.chartScrollbarSettings = sbsettings;

	// Enable pan events
	var panelsSettings = new AmCharts.PanelsSettings();
	panelsSettings.panEventsEnabled = true;
	chart.panelsSettings = panelsSettings;

	// CURSOR
	var cursorSettings = new AmCharts.ChartCursorSettings();
	cursorSettings.valueBalloonsEnabled = true;
	chart.chartCursorSettings = cursorSettings;

	// PERIOD SELECTOR ///////////////////////////////////
	var periodSelector = new AmCharts.PeriodSelector();
	periodSelector.position = "bottom";   //在底部显示时间周期控件

	//设置时间窗口的大小和显示格式
	periodSelector.dateFormat = "YYYY-MM-DD JJ:NN";
	periodSelector.inputFieldWidth = 150;

	periodSelector.periods = [{
		period: "mm",
		selected: true,
		count: 1,
		label: "1 minute"
	}];
	chart.periodSelector = periodSelector;

	chart.write('chartdiv');
	
    });

    // generate some random data, quite different range
    function generateChartData() {
	
	var json=document.getElementById('show').value;
	var obj = eval(json);  

	data_cycle=document.getElementById('data_cycle').value;

	var count = 0;

	//必须是正序
	for(var i=0;i<obj.length;i++) {  
		count++;
		if (count % 100 == 0) {
			//alert(count);
		}
		var time = obj[i].time;
		var open = obj[i].open;
		var close= obj[i].close;
		var high = obj[i].high;
		var low  = obj[i].low;
		var diff = obj[i].diff;
		var macd = obj[i].macd;
		var dea  = obj[i].dea;
		var upper= obj[i].upper;
		var mid  = obj[i].mid;
		var lower= obj[i].lower;
		var bbi  = obj[i].bbi;
		var ma5  = obj[i].ma5;
		var ma10 = obj[i].ma10;
		var ma20 = obj[i].ma20;
		var ma30 = obj[i].ma30;
		var ma60 = obj[i].ma60;
		var ma120= obj[i].ma120;

		var items = time.split(" ");
		var time_day  = items[0];
		var time_time = items[1];

		var obj_date = new Date(time_day);

		var items = time_time.split(":");
		obj_date.setHours(items[0], items[1]);
		//obj_date.setMinutes(items[1]);
		//alert(obj_date);
		
		chartData.push({
		date: obj_date,
		open: open,
		close: close,
		high: high,
		low: low,
		diff: diff,
		macd: macd,
		dea: dea, 
		upper: upper,
		mid: mid, 
		lower: lower, 
		bbi: bbi,
		ma5: ma5,
		ma10: ma10,
		ma20: ma20,
		ma30: ma30,
		ma60: ma60,
		ma120: ma120
		});
	}  
	//alert(count);
    }

    // this method is called when chart is first inited as we listen for "dataUpdated" event
    function zoomChart() {
	// different zoom methods can be used - zoomToIndexes, zoomToDates, zoomToCategoryValues
	chart.zoomToIndexes(10, 20);
    }
