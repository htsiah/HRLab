$(function(){
	
    $("#navReports").addClass("active open");
    $("#navMyTeamLeaveRequest").addClass("active");
				
	$("#grid-table").jqGrid({
	   	url:"/report/myteamleaverequestJSON",
		datatype: 'json',
		colNames:['Name','','DocNum', 'Leave Type', 'Day Type', 'Date From', 'Date To', 'Utilized', "Carry Forward Utilized", 'Status', 'Approver'],
	   	colModel:[
	   		{name:'name',index:'name',width:100},
	   		{name:'lock',index:'lock',width:20},
			{name:'docnum',index:'docnum', width:70, sorttype:'int'},
			{name:'lt',index:'lt',width:130},
			{name:'dt',index:'dt',width:70},
			{name:'fdat',index:'fdat',width:70,sortable:false},
			{name:'tdat',index:'tdat',width:70,sortable:false},
			{name:'uti',index:'uti',width:70, sorttype:"int"},
			{name:'cfuti',index:'cfuti',width:130, sorttype:"int"},
			{name:'wf_s',index:'wf_s',width:130},
			{name:'wf_aprn',index:'wf_aprn',width:130}
		],
	   	rowNum:30,
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
   			groupText : ['<b>&nbsp{0} - {1} Leave Request(s)</b>'],
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