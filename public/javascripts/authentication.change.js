$(function(){

	$("#changeform").validate({
		onkeyup: false,
		rules: {
			password: {
				required: true
			},
			npassword: {
				required: true,
				minlength: 8
			},
			cpassword: {
				required: true,
				minlength: 8
				//equalTo: "#npassword"
			}
		},
		messages: {
			password: {
				required: "Please enter your old password."
			},
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
	
    $('#updpasswd').on('click', function(e) { 
        e.preventDefault();     // block the default anchor behavior
        $('#changeform').submit();  // submit the form
    });
    
	
});