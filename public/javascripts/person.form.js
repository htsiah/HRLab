$(function(){

	$("#navCompany").addClass("active open");
	$("#navStaff").addClass("active");
	
	$('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
	
	//show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
		
    // Disable empty date field after selecting current date
    // http://stackoverflow.com/questions/24981072/bootstrap-datepicker-empties-field-after-selecting-current-date
    $("#p_edat").on("show", function(e){
    	$(this).data("stickyDate", e.date);
    });

    $("#p_edat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        }
    });
    
	$("#personform").validate({
		onkeyup: false,
		rules: {
			"p.fn": "required",
			"p.ln": "required",
			"p.em": {
				checkEmail: true,
				email: true,
				remote: {
					url: "/signup/checkemail",
			        type: "get",
			        cache: false,
			        data: {
			        	p_email: function() {
			        		return $( "#p_em" ).val();
			        	}
			        }
				}
			},
			"p.mgrid": "required",
			"p.smgrid" : {
				checkSManagerid: true
			},
			"p.g": "required",
			"p.ms": "required",
			"p.dpm": "required",
			"p.off": "required",
			"p.edat": {
				required: true,
				customDate: true
			}
		},
		messages: {
			"p.fn": "Please enter first name.",
			"p.ln": "Please enter last name.",
			"p.em": {
				checkEmail: "Please enter an email address.",
				email: "Please enter a valid email address.",
				remote: "Someone already used this email. Try another one?"
			},
			"p.mgrid": "Please select manager.",
			"p.smgrid": "Substitute Manager can not same with Manager.",
			"p.g": "Please select gender.",
			"p.ms": "Please select marital status.",
			"p.dpm": "Please select department.",
			"p.off": "Please select Office.",
			"p.edat": {
				required: "Please enter employment start date.",
				customDate: "Please enter a valid date format d-mmm-yyyy."
			}
		},		 
		submitHandler: function(form) {
			$("#p_em").removeAttr("disabled");
			form.submit();
		 }
	});
	
	$.validator.addMethod(
		"checkEmail",
		function(value,element){
			var nemail = $("#p_nem").is(":checked");
	
			if (nemail==false && value=="") {
				return false;
			} else {
				return true;
			};
		},
		"Please enter an email address."	
	);
	
	$.validator.addMethod(
		"customDate", 
		function(value, element) {
			return this.optional(element) || value.match(/^\d\d?-\w\w\w-\d\d\d\d/);
		}, 
		"Please enter a valid date format d-mmm-yyyy."
	);
	
	$.validator.addMethod(
		"checkSManagerid",
		function(value,element){
			var managerid = $("#p_mgrid").val();
			if (value == managerid) {
				return false;
			} else {
				return true;
			}
		},
		"Substitute Manager can not same with Manager."
	);
		
});

$("#p_nem").click(function(){
	if(this.checked){
		// Disable email field
		$("#p_em").val("");
		$("#p_em-error").hide();
		$("#p_em").attr("disabled", "disabled");
		
		// Disable admin roles checkbox
		$('#rl_admin').attr('checked', false);
		$("#rl_admin").attr("disabled", "disabled");
		$("#p_rl").val("");
	} else {
		$("#p_em").removeAttr("disabled");
		$("#rl_admin").removeAttr("disabled");
	}
});

$("#rl_admin").click(function(){
	if(this.checked){
		$("#p_rl").val("Admin");
	}else{
		$("#p_rl").val("");
	}
});