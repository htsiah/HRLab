$(function(){

	$("#navAdmin").addClass("active open");
    $("#navExchangeRate").addClass("active");
        
	$("#exchangerateform").validate({
		onkeyup: false,
		rules: {
			"fct": "required",
			"fccy": "required",
			"tct": "required",
			"tccy": "required",
			"er": "required"
		},
		messages: {
			"fct": "Please select From Country.",
			"fccy": "Please select From Currency.",
			"tct": "Please select To Country.",
			"tccy": "Please select To Currency.",
			"er": "Please select Exchange Rate."
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});