$(function(){
	
    $("#navReports").addClass("active open");
    $("#navAllStaffLeaveProfile").addClass("active");
							
    $.ajax({
    	url: "/report/allstaffleaveprofile",
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
	   	colNames:['Name','Leave Type','Entitlement','Earned','Adjustment','Carry Forward','Total Utilised','Total Expired','Pending Approval','Balance','Closing Balance',''],
	   	colModel:[
			{name:'name',index:'name',width:100},
			{name:'lt',index:'lt',width:100},
			{name:'ent',index:'ent',width:100,sorttype:"int"},
			{name:'ear',index:'ear',width:100,sorttype:"float"},
			{name:'adj',index:'adj',width:100,sorttype:"float"},
			{name:'cf',index:'cf',width:100,sorttype:"float"},
			{name:'tuti',index:'tuti',width:100,sorttype:"float"},
			{name:'texp',index:'texp',width:100,sorttype:"float"},
			{name:'papr',index:'papr',width:120,sorttype:"float"},
			{name:'bal',index:'bal',width:100,sorttype:"float"},
			{name:'cbal',index:'cbal',width:120,sorttype:"float"},
			{name:'a_link',index:'a_link',width:100,sortable:false}
		],
	   	rowNum:250,
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
        	window.open("/report/allstaffleaveprofilecsv");
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

function onDeleteLeaveProfile(p_id, p_lt, p_pid) {

	$( "#dialog-message" ).removeClass('hide').dialog({
		resizable: false,
		modal: true,
		title: "<div class='widget-header'><h4 class='smaller'><i class='ace-icon fa fa-warning red'></i> Delete the document?</h4></div>",
		title_html: true,
		buttons: [
			{
				html: "<i class='ace-icon fa fa-trash-o bigger-110'></i>&nbsp; Delete",
				"class" : "btn btn-danger btn-mini",
				click: function() {
					window.location = "/leaveprofilereport/delete/" + p_id + "/" + p_lt + "/" + p_pid;
				}
			},
			{
				html: "Cancel",
				"class" : "btn btn-mini",
				click: function() {
					$( this ).dialog( "close" );
				}
			}
		]
	});

}