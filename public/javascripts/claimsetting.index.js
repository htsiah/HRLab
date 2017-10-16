$(function(){

	$("#navAdmin").addClass("active open");
    $("#navClaimSetting").addClass("active");
        
	// Setup jquery dataTable
	$('#claimcategorytable').dataTable({
		"aoColumnDefs": [
	    	{ "bSortable": false, "sClass":"text-center", "aTargets": [ 3 ] }
	    ]
	});
	
});

function onDelete(p_id){
	
	$( "#dialog-delete" ).removeClass('hide').dialog({
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
	
}