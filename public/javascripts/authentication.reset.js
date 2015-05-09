$(function(){

	$("#resetform").validate({
		onkeyup: false,
		rules: {
			email: {
				required: true,
				email: true
			}
		},
		messages: {
			email: {
				required: "Please enter your email address.",
				email: "Please enter a valid email address."
			}
		},
		 submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});