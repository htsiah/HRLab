$(function(){

	$("#navCompany").addClass("active open");
    $("#navComProfile").addClass("active");

	$("#company-name-pencil").click(function(){
		$("#company-name-view").hide();
		$("#company-name-edit").show();
	});

	$("#company-name-times").click(function(){
		$("#company-name-edit").hide();
		$("#company-name-view").show();
	});
			
	$("#company-name-check").click(function(){
		$.ajax({
			url: "/company/updatecompanyname/" + $("#company-name-field").val().trim(),
			type: 'GET',
			dataType: "json",
			success: function(data){
				$("#company-name-value").text(data.name);
				$("#company-name-edit").hide();
				$("#company-name-field").val(data.name);
				$("#company-name-view").show();
				$("#header-company-name").fadeOut("slow", function(){
					$("#header-company-name").text(data.name);
					$("#header-company-name").fadeIn("slow");
				});
				document.title = data.name + ' | HRLab.my';
            },
            error: function(xhr, textStatus, errorThrown) {
            	alert("Update company name failed!!!");
			}
		});
	});
	
});

function onDelete(p_officename) {

	$.ajax({
		url: "/office/isUsedJSON/" + p_officename,
		type: 'GET',
		dataType: "json",
		success: function(data){
			if(data.status){
				onDeleteAbort()
			} else {
				onDeleteProceed(p_officename)
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

function onDeleteProceed(p_officename){
	
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
					window.location = "/office/delete/" + p_officename;
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