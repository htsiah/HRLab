$(function(){

	$("#signupform").validate({
		onkeyup: false,
		rules: {
			fname: "required",
			lname: "required",
			email: {
				required: true,
				email: true,
				checkEmailExist: true
			},
			gender: "required",
			marital: "required",
			company: "required"
		},
		messages: {
			fname: "Please enter your first name.",
			lname: "Please enter your last name.",
			email: {
				required: "Please enter an email address.",
				email: "Please enter a valid email address.",
				checkEmailExist: "Someone already used this email. Try another one?"
			},
			gender: "Please select your gender.",
			marital: "Please select your marital status.",
			company: "Please enter your company."
		},
		 submitHandler: function(form) {
		   form.submit();
		 }
	});
	
	$.validator.addMethod(
		"checkEmailExist",
		function(value) {
			var result = true
			var request = $.ajax({url:'/signup/checkemailexistjson?p_email='+value, type: 'GET', async: false, dataType: 'json'});
			request.done(function(data) {
				if(data.status==true){
					result = false;
				}else{
					result = true;
				}
		    });
		    request.fail(function(jqXHR, textStatus) {
		    	console.log("fail " + textStatus);
			  	result = true;
			});
			return result
		},
		"Someone already used this email. Try another one?"
	);

});