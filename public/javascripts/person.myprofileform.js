$(function(){

    $("#navProfile").addClass("active open");
    $("#navMyProfile").addClass("active");
	
	$('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
	
	//show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
	
	$("#personform").validate({
		onkeyup: false,
		rules: {
			"p.fn": "required",
			"p.ln": "required",
			"p.em": {
				required: true,
				email: true,
				checkEmailExist: true
			},
			"p.pt": "required",
			"p.mgrid": "required",
			"p.g": "required",
			"p.ms": "required",
			"p.dpm": "required",
			"p.off": "required",
			"p.edat": {
				required: true,
				date: true
			}
		},
		messages: {
			"p.fn": "Please enter first name.",
			"p.ln": "Please enter last name.",
			"p.em": {
				required: "Please enter an email address.",
				email: "Please enter a valid email address.",
				checkEmailExist: "Someone already used this email. Try another one?"
			},
			"p.pt": "Please select position.",
			"p.mgrid": "Please select manager.",
			"p.g": "Please select gender.",
			"p.ms": "Please select marital status.",
			"p.dpm": "Please select department.",
			"p.off": "Please select Office.",
			"p.edat": {
				required: "Please enter employment start date.",
				date: "Please enter a valid date format yyyy-mm-dd."
			}
		},		 
		submitHandler: function(form) {
			$( "#dialog-save-confirm" ).removeClass('hide').dialog({
				resizable: false,
				modal: true,
				title: "<div class='widget-header'><h4 class='smaller'>Confirmation</h4></div>",
				title_html: true,
				buttons: [
					{
						html: "Yes",
						"class" : "btn btn-primary btn-mini",
						click: function() {
							form.submit();
						}
					},
					{
						html: "Cancel",
						"class" : "btn btn-mini",
						click: function() {
							$( this ).dialog( "close" );
						}
					}
				]
			});
		 }
	});
	
	$.validator.addMethod(
		"date", 
		function(value, element) {
			return this.optional(element) || value.match(/^\d{4}-((0\d)|(1[012]))-(([012]\d)|3[01])$/);
		}, 
		"Please enter a valid date format yyyy-mm-dd."
	);
	
	$.validator.addMethod(
		"checkEmailExist",
		function(value) {
			var result = true
			var request = $.ajax({url:'/signup/checkemailexistjson?email='+value, type: 'GET', async: false, dataType: 'json'});
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

$("#rl_admin").click(function(){
	if(this.checked){
		$("#p_rl").val("Admin");
	}else{
		$("#p_rl").val("");
	}
});