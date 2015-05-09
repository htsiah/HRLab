$(function(){

	$("#loginform").validate({
		onkeyup: false,
		rules: {
			email: {
				required: true,
				email: true
			},
			password: "required"
		},
		messages: {
			email: {
				required: "Please enter your email address.",
				email: "Please enter a valid email address."
			},
			password: "Please enter your password.",
		},
		 submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});