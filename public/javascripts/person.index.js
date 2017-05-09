$(function(){
	$("#navCompany").addClass("active open");
	$("#navStaff").addClass("active");

	// Setup keyword index table using jquery table
	$('#persontable').dataTable({
		"lengthMenu": [[25, 50, 100, -1], [25, 50, 100, "All"]],
		"aoColumnDefs": [
	    	{ "bSortable": false, "sClass":"text-center", "aTargets": [ 5 ] }
	    ]
	});
});

function onDelete(p_id, p_email) {

	$.ajax({
		url: "/person/getemploymenttypejson/" + p_id,
		type: 'GET',
		dataType: "json",
		success: function(data){
			if(data.type == "staff"){
				onDeleteProceed(p_id, p_email)
			} else {
				onDeleteAbort()
			}		
        },
        error: function(xhr, textStatus, errorThrown) {
        	alert("System error!!!");
		}
	});

}

function onDeleteAbort(){

	$( "#dialog-abort" ).removeClass('hide').dialog({
		resizable: false,
		modal: true,
		title: "<div class='widget-header'><h4 class='smaller'><i class='ace-icon fa fa-warning red'></i> Abort</h4></div>",
		title_html: true,
		buttons: [
			{
				html: "Got it",
				"class" : "btn btn-mini",
				click: function() {
					$( this ).dialog( "close" );
				}
			}
		]
	});
	
}

function onDeleteProceed(p_id, p_email){
	
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
					window.location = "/person/delete?p_id=" + p_id + "&p_email=" + p_email;
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