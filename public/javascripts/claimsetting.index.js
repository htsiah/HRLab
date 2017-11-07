$(function(){

	$("#navAdmin").addClass("active open");
    $("#navClaimSetting").addClass("active");
        
	// Setup jquery dataTable
	$('#claimcategorytable').dataTable({
		"aoColumnDefs": [
	    	{ "bSortable": false, "sClass":"text-center", "aTargets": [ 3 ] }
	    ]
	});
	
	// Setup jquery dataTable
	$('#claimworkflowtable').dataTable({
		"aoColumnDefs": [
	    	{ "bSortable": false, "sClass":"text-center", "aTargets": [ 2 ] }
	    ]
	});
	
	// Mouse over help
	$('[data-rel=popover]').popover({container:'body'});
	
	// Disable User Request
	$("#disable-request-pencil").click(function(){
		$("#disable-request-view").hide();
		$("#disable-request-edit").show();
	});

	$("#disable-request-times").click(function(){
		$("#disable-request-edit").hide();
		$("#disable-request-field").val(disableuserrequestvalue);
		$("#disable-request-view").show();
	});
	
	$("#disable-request-check").click(function(){
		disableuserrequestvalue = $("#disable-request-field").val();
		$.ajax({
			url: "/claimsetting/updatedisablerequest/" + disableuserrequestvalue,
			type: 'GET',
			dataType: "json",
			success: function(result){
				$("#disable-request-edit").hide();
				$('#disable-request-value').text(disableuserrequestvalue);
				$("#disable-request-view").show();
            },
            error: function(xhr, textStatus, errorThrown) {
            	alert("Failed update org chart setting!!!");
			}
		});
	});
	
});

let disableuserrequestvalue = $("#disable-request-field").val();

function onDeleteCategory(p_id){
	
	$( "#dialog-delete-category" ).removeClass('hide').dialog({
		resizable: false,
		modal: true,
		title: "<div class='widget-header'><h4 class='smaller'><i class='ace-icon fa fa-warning red'></i> Delete the document?</h4></div>",
		title_html: true,
		buttons: [
			{
				html: "<i class='ace-icon fa fa-trash-o bigger-110'></i>&nbsp; Delete",
				"class" : "btn btn-danger btn-mini",
				click: function() {
					window.location = "/claimcategory/delete/" + p_id;
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
	
};

function onDeleteWorkflow(p_id){
	
	$( "#dialog-delete-workflow" ).removeClass('hide').dialog({
		resizable: false,
		modal: true,
		title: "<div class='widget-header'><h4 class='smaller'><i class='ace-icon fa fa-warning red'></i> Delete the document?</h4></div>",
		title_html: true,
		buttons: [
			{
				html: "<i class='ace-icon fa fa-trash-o bigger-110'></i>&nbsp; Delete",
				"class" : "btn btn-danger btn-mini",
				click: function() {
					window.location = "/claimworkflow/delete/" + p_id;
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
	
};