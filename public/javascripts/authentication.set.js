$(function(){

	$("#setform").validate({
		onkeyup: false,
		rules: {
			npassword: {
				required: true,
				minlength: 8
			},
			cpassword: {
				required: true,
				minlength: 8,
				equalTo: "#npassword"
			}
		},
		messages: {
		
			npassword: {
				required: "Please enter your new password.",
				minlength: "Your password must be at least 8 characters long."
			},
			cpassword: {
				required: "Please re-enter your new password.",
				minlength: "Your password must be at least 8 characters long.",
				equalTo: "Please enter the same password as above."
			}
		},
		 submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});