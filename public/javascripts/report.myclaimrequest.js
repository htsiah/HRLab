$(function(){
	
    $("#navClaimReports").addClass("active open");
    $("#navMyClaimRequest").addClass("active");
				
	$("#grid-table").jqGrid({
	   	url:"/report/myclaimrequest",
		datatype: 'json',
	   	colNames:['DocNum', 'Receipt Date', 'Category', 'Amount', 'Exchange Rate', 'Approve Amount', 'GST / VAT', 'Status', 'Pending Approver(s)',''],
	   	colModel:[
			{name:'docnum',index:'docnum', width:50, sorttype:'int'},
			{name:'rdat',index:'rdat',width:70,sortable:false},
			{name:'cat',index:'cat',width:70},
			{name:'amt',index:'amt',width:70},
			{name:'er',index:'er',width:70},
			{name:'aamt',index:'aamt',width:70},
			{name:'tamt',index:'tamt',width:70},
			{name:'s',index:'s',width:70},
			{name:'papr',index:'papr',width:130},
			{name:'v_link',index:'v_link',width:30,sortable:false}
		],
	   	rowNum: 250,
	   	rowList: [],
	   	loadonce: true,
	   	pager: '#grid-pager',
	   	altRows: true,
	   	height: 'auto',
	   	autowidth: true,
	   	forceFit: true,
	   	sortname: 'docnum',
	   	sortorder: "desc",
	    viewrecords: true,
	    gridview: true, // Only apply when not use treeGrid, subGrid, or the afterInsertRow event.
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
        	// window.open("/report/myleaverequestcsv");
        	alert("Coming soon.");
        } 
    });
	
	//trigger window resize to make the grid get the correct size
	$(window).triggerHandler('resize.jqGrid');
		
});

//replace icons with FontAwesome icons like above
function updatePagerIcons(table) {
	const replacement = 
	{
		'ui-icon-seek-first' : 'ace-icon fa fa-angle-double-left bigger-140',
		'ui-icon-seek-prev' : 'ace-icon fa fa-angle-left bigger-140',
		'ui-icon-seek-next' : 'ace-icon fa fa-angle-right bigger-140',
		'ui-icon-seek-end' : 'ace-icon fa fa-angle-double-right bigger-140'
	};
	let	icon,
		$class;
	
	$('.ui-pg-table:not(.navtable) > tbody > tr > .ui-pg-button > .ui-icon').each(function(){
		icon = $(this);
		$class = $.trim(icon.attr('class').replace('ui-icon', ''));
		
		if($class in replacement) icon.attr('class', 'ui-icon '+replacement[$class]);
	})
}

function enableTooltips(table) {
	$('.navtable .ui-pg-button').tooltip({container:'body'});
	$(table).find('.ui-pg-div').tooltip({container:'body'});
}