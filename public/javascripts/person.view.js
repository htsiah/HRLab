$(function(){
	
	$("#navCompany").addClass("active open");
	$("#navStaff").addClass("active");

});

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
					window.location = "/leaveprofile/delete/" + p_id + "/" + encodeURIComponent(p_lt) + "/" + p_pid;
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

function sendWelcomeEmail(p_email) {
	
	$( "#dialog-send-welcome-email" ).removeClass('hide').dialog({
		resizable: false,
		modal: true,
		title: "<div class='widget-header'><h4 class='smaller'>Confirmation</h4></div>",
		title_html: true,
		buttons: [
			{
				html: "Yes, send now",
				"class" : "btn btn-primary btn-mini",
				click: function() {
					$.ajax({
						url: "/person/sendwelcomeemail",
						type: 'GET',
						data: {p_email : p_email},
						dataType: "json",
						success: function(result){
							if (result == "false") {
								alert("Send welcome email failed!!!");
							};
			            },
			            error: function(xhr, textStatus, errorThrown) {
			            	alert("Send welcome email failed!!!");
						}
					});
					$( this ).dialog( "close" );
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