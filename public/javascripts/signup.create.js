$(function(){

	$("#signupform").validate({
		onkeyup: false,
		rules: {
			fname: "required",
			lname: "required",
			email: {
				required: true,
				email: true,
				remote: {
					url: "/signup/checkemail",
			        type: "get",
			        cache: false,
			        data: {
			        	p_email: function() {
			        		return $( "#email" ).val();
			        	}
			        }
				}
			},
			gender: "required",
			marital: "required",
			company: "required",
			country: "required"
		},
		messages: {
			fname: "Please enter your first name.",
			lname: "Please enter your last name.",
			email: {
				required: "Please enter an email address.",
				email: "Please enter a valid email address.",
				remote: "Someone already used this email. Try another one?"
			},
			gender: "Please select your gender.",
			marital: "Please select your marital status.",
			company: "Please enter your company.",
			country: "Please select your country."
		},
		 submitHandler: function(form) {
			 gtag_report_conversion ('https://app.hrsifu.com');
			 form.submit();
		 }
	});
	
});
