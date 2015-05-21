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
    
    // Bind leave type field 
    $("#lt").change(function() {
    	var selectedLT = $( "#lt option:selected" ).text();
		$.ajax({
			url: "/leavepolicy/getdaytypejson/" + selectedLT,
			contentType: "application/json; charset=utf-8",
			success: function(data){
				if (data.daytype == "Full day only") {
					$("#dt").attr("disabled", "disabled");
					$("#dt").val("Full day");
					$("#tdat").removeAttr("disabled");
				} else {
					$("#dt").removeAttr("disabled");
				}
			},
			error: function(xhr, status, error){
				alert("There was an error while fetching your leave setting. Do not proceed! Please contact support@hrlab.my.")
			},
		});
    });
    
	// Bind date type field 
	$(document).on('change', '#dt', function(e) {
		var seldatetype = this.options[this.selectedIndex].value;
		if (seldatetype=="1st half" || seldatetype=="2nd half") {
			$("#tdat").attr("disabled", "disabled");
			$("#tdat").val($("#fdat").val());
		} else {
			$("#tdat").removeAttr("disabled");
		}
	})
	
	// Bind start date field 
	$(document).on('change', '#fdat', function(e) {
		$("#tdat").val($("#fdat").val());
	})
	
	$.validator.addMethod(
		"date", 
		function(value, element) {
			return this.optional(element) || value.match(/^\d{4}-((0\d)|(1[012]))-(([012]\d)|3[01])$/);
		}, 
		"Please enter a valid date format yyyy-mm-dd."
	);
	
	$.validator.addMethod(
		"checkDate",
		function(value,element){
			var fdat = new Date($("#fdat").val());
			var tdat = new Date($("#tdat").val());
			
			if (fdat>tdat) {
				return false;
			} else {
				return true
			};
		},
		"Date to should greater than date from."
	);
	
	// Validation for form
	$("#leaveform").validate({
		debug: false,
		onkeyup: false,
		rules: {
			lt: "required",
			fdat: {
				required: true,
				date: true
			},
			tdat: {
				required: true,
				date: true,
				checkDate: true
			}
		},
		messages: {
			lt: "Please select a leave type",
			fdat: {
				required: "Please enter the date from.",
				date: "Please enter a valid date format yyyy-mm-dd."
			},
			tdat: {
				required: "Please enter the date to.",
				date: "Please enter a valid date format yyyy-mm-dd.",
				checkDate: "Date to should greater than date from."
			}
		},
		submitHandler: function(form) {
			$("#dt").removeAttr("disabled");
		 	$("#tdat").removeAttr("disabled");
		 	form.submit();
		}
	});
	
});

// Form submit function
var handleSubmit = function() {
	$('#leaveform').submit();	
}