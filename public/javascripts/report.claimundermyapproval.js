$(function(){
	
    $("#navClaimReports").addClass("active open");
    $("#navClaimUnderMyApproval").addClass("active");
				
    $.ajax({
    	url: "/report/claimundermyapproval",
		dataType: "json",
		success: function(data){
			setupJqGrid(data);
		},
		error: function(xhr, status, error){
			alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
		}
	});
	
});

//Failed to load pagination with group using json. There is a suggestion to disable sorting, but still not workable.
//http://stackoverflow.com/questions/10977583/jqgrid-grouping-deactivating-client-side-sorting-on-page-navigation
//Workaround is using local data type.
//Future enhancement should do paging on server side.
//http://stackoverflow.com/questions/29851892/server-side-pagination-in-jqgrid-no-grid-pager-parameters
function setupJqGrid(data){
	
	$("#grid-table").jqGrid({
		data: data,
		datatype: "local",
	   	colNames:['Name', 'DocNum', 'Receipt Date', 'Category', 'Amount', 'Exchange Rate', 'Approve Amount', 'GST / VAT', 'Status', 'Pending Approver(s)',''],
	   	colModel:[
	   		{name:'name',index:'name',width:100},
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
	   	rowNum:250,
	   	rowList:[],
	   	pager: '#grid-pager',
	   	altRows: true,
	   	height: 'auto',
	   	autowidth: true,
	   	forceFit: true,
	   	sortname: 'docnum',
	   	sortorder: "desc",
	    viewrecords: true,
	    grouping:true,
   		groupingView : {
   			groupField : ['name'],
   			groupDataSorted : true,
   			groupColumnShow : [false],
   			groupText : ['<b>&nbsp{0} - {1} Claim Request(s)</b>'],
			plusicon : 'fa fa-plus-square-o bigger-110',
			minusicon : 'fa fa-minus-square-o bigger-110',
			groupCollapse : true
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
        	window.open("/report/claimundermyapprovalcsv");
        } 
    });
	
	//trigger window resize to make the grid get the correct size
	$(window).triggerHandler('resize.jqGrid');
	
}

//replace icons with FontAwesome icons like above
function updatePagerIcons(table) {
	const replacement = 
	{
		'ui-icon-seek-first' : 'ace-icon fa fa-angle-double-left bigger-140',
		'ui-icon-seek-prev' : 'ace-icon fa fa-angle-left bigger-140',
		'ui-icon-seek-next' : 'ace-icon fa fa-angle-right bigger-140',
		'ui-icon-seek-end' : 'ace-icon fa fa-angle-double-right bigger-140'
	};
	let icon,
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