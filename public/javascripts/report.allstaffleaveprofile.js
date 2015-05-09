$(function(){
	
    $("#navReports").addClass("active open");
    $("#navAllStaffLeaveProfile").addClass("active");
				
	$("#grid-table").jqGrid({
	   	url:"/report/allstaffleaveprofileJSON",
		datatype: 'json',
	   	colNames:['Name','Leave Type','Entitlement','Earned','Adjustment','Carry Forward','Total Utilised','Total Expired','Balance'],
	   	colModel:[
			{name:'name',index:'name',width:100},
			{name:'lt',index:'lt',width:100},
			{name:'ent',index:'ent',width:100,sorttype:"int"},
			{name:'ear',index:'ear',width:100,sorttype:"float"},
			{name:'adj',index:'adj',width:100,sorttype:"float"},
			{name:'cf',index:'cf',width:100,sorttype:"float"},
			{name:'tuti',index:'tuti',width:100,sorttype:"float"},
			{name:'texp',index:'texp',width:100,sorttype:"float"},
			{name:'bal',index:'bal',width:100,sorttype:"float"}
		],
	   	rowNum:30,
	   	rowList:[],
	   	pager: '#grid-pager',
	   	altRows: true,
	   	height: 'auto',
	   	autowidth: true,
	   	sortname: 'lt',
	    viewrecords: true,
	    grouping:true,
   		groupingView : {
   			groupField : ['name'],
   			groupDataSorted : true,
   			groupColumnShow : [false],
   			groupText : ['<b>&nbsp{0} - {1} Leave Profile(s)</b>'],
			plusicon : 'fa fa-plus-square-o bigger-110',
			minusicon : 'fa fa-minus-square-o bigger-110'
   		},
	    caption:"",
	    loadComplete : function() {
			var table = this;
			//setTimeout is for webkit only to give time for DOM changes and then redraw!!!
			setTimeout(function(){
				updatePagerIcons(table);
				enableTooltips(table);
			}, 0);			
		}
	    
	});
	
	$("#grid-table").jqGrid('navGrid','#grid-pager',{
		edit:false,
		add:false,
		del:false,
		search: false,
		refresh: false,
	});
	
	$("#grid-table").jqGrid('navButtonAdd','#grid-pager',{
		title:"Export to CSV",
		caption:"",
        buttonicon:"ace-icon fa fa-download bigger-140", 
        onClickButton : function () { 
        	alert("This feature is no available yet.");
            // alert("Call server to generate CSV. Example: http://www.trirand.net/documentation/php/_32h0wow2v.htm");
        } 
    });
	
	//trigger window resize to make the grid get the correct size
	$(window).triggerHandler('resize.jqGrid');
		
});

//replace icons with FontAwesome icons like above
function updatePagerIcons(table) {
	var replacement = 
	{
		'ui-icon-seek-first' : 'ace-icon fa fa-angle-double-left bigger-140',
		'ui-icon-seek-prev' : 'ace-icon fa fa-angle-left bigger-140',
		'ui-icon-seek-next' : 'ace-icon fa fa-angle-right bigger-140',
		'ui-icon-seek-end' : 'ace-icon fa fa-angle-double-right bigger-140'
	};
	$('.ui-pg-table:not(.navtable) > tbody > tr > .ui-pg-button > .ui-icon').each(function(){
		var icon = $(this);
		var $class = $.trim(icon.attr('class').replace('ui-icon', ''));
		
		if($class in replacement) icon.attr('class', 'ui-icon '+replacement[$class]);
	})
}

function enableTooltips(table) {
	$('.navtable .ui-pg-button').tooltip({container:'body'});
	$(table).find('.ui-pg-div').tooltip({container:'body'});
}