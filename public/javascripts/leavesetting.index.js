$(function(){

	$("#navAdmin").addClass("active open");
    $("#navLeaveTypeSetting").addClass("active");
    
	// Setup jquery dataTable
	$('#leavepoliciestable').dataTable({
		"aoColumnDefs": [
	    	{ "bSortable": false, "sClass":"text-center", "aTargets": [ 3 ] }
	    ]
	});
	
	// Mouse over help
	$('[data-rel=popover]').popover({container:'body'});
	
	// Leave cut off
	$("#leave-cut-off-pencil").click(function(){
		$("#leave-cut-off-view").hide();
		$("#leave-cut-off-edit").show();
	});

	$("#leave-cut-off-times").click(function(){
		$("#leave-cut-off-edit").hide();
		$("#leave-cut-off-view").show();
	});
	
	$("#leave-cut-off-check").click(function(){
		$( "#dialog-cutoff-change-confirm" ).removeClass('hide').dialog({
			resizable: false,
			modal: true,
			title: "<div class='widget-header'><h4 class='smaller'>Confirmation</h4></div>",
			title_html: true,
			buttons: [
				{
					html: "Yes",
					"class" : "btn btn-primary btn-mini",
					click: function() {
						$.ajax({
							url: "/leavesetting/updatecfm/" + $("#leave-cut-off-field").val(),
							type: 'GET',
							dataType: "json",
							success: function(result){
								switch ($("#leave-cut-off-field").val()) {
									case "1": 
										$('#leave-cut-off-value').text("1st January");
										break;
									case "2": 
										$('#leave-cut-off-value').text("1st February");
										break;
									case "3": 
										$('#leave-cut-off-value').text("1st March");
										break;
									case "4": 
										$('#leave-cut-off-value').text("1st April");
										break;
									case "5": 
										$('#leave-cut-off-value').text("1st May");
										break;
									case "6": 
										$('#leave-cut-off-value').text("1st June");
										break;
									case "7": 
										$('#leave-cut-off-value').text("1st July");
										break;
									case "8": 
										$('#leave-cut-off-value').text("1st August");
										break;
									case "9": 
										$('#leave-cut-off-value').text("1st September");
										break;
									case "10": 
										$('#leave-cut-off-value').text("1st October");
										break;
									case "11": 
										$('#leave-cut-off-value').text("1st November");
										break;
									case "12": 
										$('#leave-cut-off-value').text("1st December");
										break;
								}
								$("#leave-cut-off-edit").hide();
				            	$("#leave-cut-off-view").show();
				            },
				            error: function(xhr, textStatus, errorThrown) {
				            	alert("Update leave cut off failed!!!");
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
	});
	
	// Leave approval method
	$("#approval-method-pencil").click(function(){
		$("#approval-method-view").hide();
		$("#approval-method-edit").show();
	});

	$("#approval-method-times").click(function(){
		$("#approval-method-edit").hide();
		$("#approval-method-view").show();
	});
	
	$("#approval-method-check").click(function(){
		$.ajax({
			url: "/leavesetting/updateaprmthd/" + $("#approval-method-field").val(),
			type: 'GET',
			dataType: "json",
			success: function(result){
				$('#approval-method-value').text($("#approval-method-field").val());
				$("#approval-method-edit").hide();
            	$("#approval-method-view").show();
            },
            error: function(xhr, textStatus, errorThrown) {
            	alert("Update approval method failed!!!");
			}
		});
	});
	
	// Future Leave Request
	$("#future-request-pencil").click(function(){
		$("#future-request-view").hide();
		$("#future-request-edit").show();
	});
	
	$("#future-request-times").click(function(){
		$("#future-request-edit").hide();
		$("#future-request-view").show();
	});
	
	$("#future-request-check").click(function(){
		$.ajax({
			url: "/leavesetting/updatefuturerequest/" + $("#future-request-field").val(),
			type: 'GET',
			dataType: "json",
			success: function(result){
				$('#future-request-value').text($("#future-request-field").val());
				$("#future-request-edit").hide();
            	$("#future-request-view").show();
            },
            error: function(xhr, textStatus, errorThrown) {
            	alert("Update future request failed!!!");
			}
		});
	});
	
});

function onDelete(p_id, p_lt) {

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
					window.location = "/leavepolicy/delete/" + p_id + "/" + p_lt;
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