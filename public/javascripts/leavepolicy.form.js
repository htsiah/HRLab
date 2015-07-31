$(function(){

	$("#navAdmin").addClass("active open");
    $("#navLeaveTypeSetting").addClass("active");
    	
	$('#e1,#e2,#e3,#e4,#e5,#e1_cf,#e2_cf,#e3_cf,#e4_cf,#e5_cf').ace_spinner({
		value:0,
		min:0,
		max:100,
		step:1, 
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});

	$('#e1_s,#e2_s,#e3_s,#e4_s,#e5_s').ace_spinner({
		value:0,
		min:0,
		max:999,
		step:1,
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});
	
	$("#leavepolicyform").validate({
		onkeyup: false,
		rules: {
			"lt": "required",
			"pt": "required"
		},
		messages: {
			"lt": "Please select leave type.",
			"pt": "Please select position type."
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});

function onUpdateSubmit() {

	$( "#dialog-message" ).removeClass('hide').dialog({
		resizable: false,
		modal: true,
		title: "<div class='widget-header'><h4 class='smaller'><i class='ace-icon fa fa-question-circle'></i> Question</h4></div>",
		title_html: true,
		buttons: [
			{
				html: "<i class='ace-icon fa fa-check bigger-110'></i>&nbsp; Yes",
				"class" : "btn btn-mini",
				click: function() {
					$( this ).dialog( "close" );
				}
			},
			{
				html: "<i class='ace-icon fa fa-times bigger-110'></i>&nbsp; No",
				"class" : "btn btn-mini",
				click: function() {
					$( this ).dialog( "close" );
				}
			}
		]
	});

}