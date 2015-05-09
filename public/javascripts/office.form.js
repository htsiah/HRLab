$(function(){

	$("#navCompany").addClass("active open");
    $("#navComProfile").addClass("active");
        
	$("#officeform").validate({
		onkeyup: false,
		rules: {
			"n": {
				required: true,
				checkExistOfficeName: true
			},
			"st": "required"
		},
		messages: {
			"n": {
				required: "Please enter office name.",
				checkExistOfficeName: "Office name already been used."
			},
			"st": "Please select state."
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});