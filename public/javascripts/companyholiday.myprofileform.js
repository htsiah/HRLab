$(function(){

    $("#navProfile").addClass("active open");
    $("#navDashboard").addClass("active");
    
    $('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
	
	//show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
	
	// Bind on start date
	$(document).on('change', '#fdat', function(e) {
		$("#tdat").val($("#fdat").val());
	})
    
	$("#companyholidayform").validate({
		onkeyup: false,
		rules: {
			"n": "required",
			"fdat": {
				required: true,
				date: true
			},
			"tdat": {
				required: true,
				date: true,
				checkDate: true
			}
		},
		messages: {
			"n": "Please enter holiday name.",
			"fdat": {
				required: "Please enter date from (start).",
				date: "Please enter a valid date format yyyy-mm-dd."
			},
			"tdat": {
				required: "Please enter date to (end).",
				date: "Please enter a valid date format yyyy-mm-dd.",
				checkDate: "Date to (end) should greater than date from (start)."
			}
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
	$.validator.addMethod(
		"date", 
		function(value, element) {
			return this.optional(element) || value.match(/^\d{4}-((0\d)|(1[012]))-(([012]\d)|3[01])$/);
		}, 
		"Please enter a valid date format yyyy-mm-dd."
	);
	
	$.validator.addMethod("checkDate", 
		function(value) {
			var fdat = new Date($("#fdat").val());
			var tdat = new Date($("#tdat").val());
			if (fdat>tdat) {
				return false;
			} else {
				return true;
			}
		}, 
		'Date to (end) should greater than date from (start).'
	);
	
});

// Form submit function
var handleSubmit = function() {
	
	var selectedStates = "";
	var count = $("input:checkbox.state").length;
	
	for(var i=0; i<count; i++){
		st=$("input:checkbox:checked#st" + i).val();
		if (st!=undefined) {
			if (selectedStates=="") {
				selectedStates = st;
			} else {
				selectedStates = selectedStates + "," + st;
			}
		}
	};
	
	$("#st").val(selectedStates)
	$('#companyholidayform').submit();	

}