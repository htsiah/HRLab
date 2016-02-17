$(function(){
    
    $("#navProfile").addClass("active open");
    $("#navDashboard").addClass("active");
    
    $('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
		
	// show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
    
	// To date can not earlier than From date
	$("#tdat").datepicker("setStartDate", $("#fdat").val());
	
    // Disable empty date field after selecting current date
    // http://stackoverflow.com/questions/24981072/bootstrap-datepicker-empties-field-after-selecting-current-date
    $("#fdat, #tdat").on("show", function(e){
    	$(this).data("stickyDate", e.date);
    });
    
    $("#fdat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        } else {   		
    		// This bit only process if value change
    		if (e.date.toLocaleDateString() !== stickyDate.toLocaleDateString()) {
    	    	
        		$("#tdat").datepicker("setStartDate", $(this).val());
        		$("#tdat").datepicker("update", $(this).val());
        		
    			var selperson = $( "#pid option:selected" ).val();
    	    	var sellt = $( "#lt option:selected" ).val();
    			var seldt = $( "#dt option:selected" ).val();
    			var fillfdat = $( "#fdat" ).val();
    			var filltdat = $( "#tdat" ).val();
    			        		
        		$.ajax({
        			url: "/leave/getapplieddurationjson/" + selperson + "/" + sellt + "/" + seldt + "/" + fillfdat + "/" + filltdat,
        			dataType: "json",
        			beforeSend: function(){
        				loader.on();
        			},
        			success: function(data){
        				if (parseFloat(data.balance) < 0 ){
        					$("#btn-apply").html("Apply for " + data.applied + " day.<br>No enough leave balance.");
        					$("#btn-apply").attr("disabled", "disabled");
        				} else {
        					$("#btn-apply").text("Apply for " + data.applied + " day");
        				}
        				loader.off();
        			},
        			error: function(xhr, status, error){
        				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
        				loader.off();
        			},
        		});
    		};
        }
    });
    
    $("#tdat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        } else {
        	// This bit only process if value change
    		if (e.date.toLocaleDateString() !== stickyDate.toLocaleDateString()) {
    			var selperson = $( "#pid option:selected" ).val();
    	    	var sellt = $( "#lt option:selected" ).val();
    			var seldt = $( "#dt option:selected" ).val();
    			var fillfdat = $( "#fdat" ).val();
    			var filltdat = $( "#tdat" ).val();
    			        		
        		$.ajax({
        			url: "/leave/getapplieddurationjson/" + selperson + "/" + sellt + "/" + seldt + "/" + fillfdat + "/" + filltdat,
        			dataType: "json",
        			beforeSend: function(){
        				loader.on();
        			},
        			success: function(data){
        				if (parseFloat(data.balance) < 0 ){
        					$("#btn-apply").html("Apply for " + data.applied + " day.<br>No enough leave balance.");
        					$("#btn-apply").attr("disabled", "disabled");
        				} else {
        					$("#btn-apply").text("Apply for " + data.applied + " day");
        				}
        				loader.off();
        			},
        			error: function(xhr, status, error){
        				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
        				loader.off();
        			},
        		});
    		};
        }
    });
        	
    // Set new form
    resetForm();
    
    // Bind Applicant field 
    $(document).on('change', '#pid', function(e) {
    	var selperson = this.options[this.selectedIndex].value;
    	if (selperson == "") {
			resetForm("applicant");
    	} else {
    		$.ajax({
    			url: "/leaveprofile/getleaveprofile/" + selperson,
    			dataType: "json",
    			beforeSend: function(){
    				loader.on();
    			},
    			success: function(data){
    				$("#lt").removeAttr("disabled");
    				$('#lt option').remove();
    				$('#lt').append( new Option("Please select","") );
    				$.each(data, function(key, val) {
    					$('#lt').append( new Option(val,key) );
    				});
    				loader.off();
    			},
    			error: function(xhr, status, error){
    				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.")
    				loader.off();
    			}
    			
    		})
    	}
    });
    
    // Bind leave type field 
    $(document).on('change', '#lt', function(e) {
    	var selperson = $( "#pid option:selected" ).val();
    	var sellt = $( "#lt option:selected" ).val();
    	if (sellt == "") {
    		resetForm("leave type - reset");
    	} else {
    		$.ajax({
    			url: "/leave/getdaytypeandapplieddurationjson/" + selperson + "/" + sellt,
    			dataType: "json",
    			beforeSend: function(){
    				loader.on();
    			},
    			success: function(data){
    				if (data.daytype == "Full day only") {
    					resetForm("leave type - full day only");
    				} else {
    					resetForm("leave type - all");
    				};
    				if (parseFloat(data.balance) < 0 ){
    					$("#btn-apply").html("Apply for " + data.applied + " day.<br>No enough leave balance.");
    					$("#btn-apply").attr("disabled", "disabled");
    				} else {
    					$("#btn-apply").text("Apply for " + data.applied + " day");
    				}
    				loader.off();
    			},
    			error: function(xhr, status, error){
    				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
    				loader.off();
    			},
    		});
    	}
    });
    
	// Bind day type field 
	$(document).on('change', '#dt', function(e) {
    	var selperson = $( "#pid option:selected" ).val();
    	var sellt = $( "#lt option:selected" ).val();
		var seldt = this.options[this.selectedIndex].value;
		if (seldt=="1st half" || seldt=="2nd half") {
			resetForm("day type - half day");
		} else {
			resetForm("day type - full day");
		}
		var fillfdat = $( "#fdat" ).val();
		var filltdat = $( "#tdat" ).val();
		$.ajax({
			url: "/leave/getapplieddurationjson/" + selperson + "/" + sellt + "/" + seldt + "/" + fillfdat + "/" + filltdat,
			dataType: "json",
			beforeSend: function(){
				loader.on();
			},
			success: function(data){
				if (parseFloat(data.balance) < 0 ){
					$("#btn-apply").html("Apply for " + data.applied + " day.<br>No enough leave balance.");
					$("#btn-apply").attr("disabled", "disabled");
				} else {
					$("#btn-apply").text("Apply for " + data.applied + " day");
				}
				loader.off();
			},
			error: function(xhr, status, error){
				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
				loader.off();
			},
		});
	})
    		
	// Validation for form
	$.validator.addMethod(
		"customDate", 
		function(value, element) {
			return this.optional(element) || value.match(/^\d\d?-\w\w\w-\d\d\d\d/);
		}, 
		"Please enter a valid date format d-mmm-yyyy."
	);
		
	$.validator.addMethod(
		"checkDate",
		function(value,element){
			var fdat = new Date($("#fdat").val());
			var tdat = new Date($("#tdat").val());
			
			if (fdat>tdat) {
				return false;
			} else {
				return true
			};
		},
		"Date to should greater than date from."
	);
		
	$("#leaveonbehalfform").validate({
		debug: false,
		onkeyup: false,
		rules: {
			pid: "required",
			lt: "required",
			fdat: {
				required: true,
				date: true
			},
			tdat: {
				required: true,
				date: true,
				checkDate: true
			}
		},
		messages: {
			pid: "Please select applicant",
			lt: "Please select a leave type",
			fdat: {
				required: "Please enter the date from.",
				customDate: "Please enter a valid date format d-mmm-yyyy."
			},
			tdat: {
				required: "Please enter the date to.",
				customDate: "Please enter a valid date format d-mmm-yyyy.",
				checkDate: "Date to should greater than date from."
			}
		},
		submitHandler: function(form) {
			$("#dt").removeAttr("disabled");
		 	$("#tdat").removeAttr("disabled");
		 	form.submit();
		}
	});
	
});

// Set Form
function resetForm(opt){
	switch (opt) {
	case "applicant":
		$("#lt option").remove();
		$("#lt").append( new Option("Please select","") );
		$("#dt").val("Full day");
		$("#fdat").datepicker("setDate", new Date());
		$("#tdat").datepicker("setStartDate", new Date());
		$("#tdat").datepicker("setDate", new Date());
		$("#lt").attr("disabled", "disabled");
	    $("#dt").attr("disabled", "disabled");
	    $("#fdat").attr("disabled", "disabled");
	    $("#tdat").attr("disabled", "disabled");
	    $("#btn-apply").text("Apply for 0 day");
	    $("#btn-apply").attr("disabled", "disabled");
	    break;
	case "leave type - reset":
		$("#dt").val("Full day");
		$("#fdat").datepicker("setDate", new Date());
		$("#tdat").datepicker("setStartDate", new Date());
		$("#tdat").datepicker("setDate", new Date());
	    $("#dt").attr("disabled", "disabled");
	    $("#fdat").attr("disabled", "disabled");
	    $("#tdat").attr("disabled", "disabled");
	    $("#btn-apply").text("Apply for 0 day");
	    $("#btn-apply").attr("disabled", "disabled");
	    break;
	case "leave type - all":
		$("#dt").removeAttr("disabled");
		$("#fdat").removeAttr("disabled");
		$("#tdat").removeAttr("disabled");
		$("#btn-apply").removeAttr("disabled");
		$("#dt").val("Full day");
		$("#fdat").datepicker("setDate", new Date());
		$("#tdat").datepicker("setStartDate", new Date());
		$("#tdat").datepicker("setDate", new Date());
	    break;
	case "leave type - full day only":
		$("#fdat").removeAttr("disabled");
		$("#tdat").removeAttr("disabled");
		$("#btn-apply").removeAttr("disabled");
		$("#dt").val("Full day");
		$("#fdat").datepicker("setDate", new Date());
		$("#tdat").datepicker("setStartDate", new Date());
		$("#tdat").datepicker("setDate", new Date());
	    $("#dt").attr("disabled", "disabled");
	    break;
	case "day type - half day":
		$("#tdat").removeAttr("disabled");
		$("#fdat").datepicker("setDate", new Date());
		$("#tdat").datepicker("setStartDate", new Date());
		$("#tdat").datepicker("setDate", new Date());
	    $("#tdat").attr("disabled", "disabled");
	    break;
	case "day type - full day":
		$("#tdat").removeAttr("disabled");
		$("#fdat").datepicker("setDate", new Date());
		$("#tdat").datepicker("setStartDate", new Date());
		$("#tdat").datepicker("setDate", new Date());
	    break;
	default: // New form
		$("#lt").attr("disabled", "disabled");
		$("#dt").attr("disabled", "disabled");
		$("#fdat").attr("disabled", "disabled");
		$("#tdat").attr("disabled", "disabled");
	}
}

// Form submit function
var handleSubmit = function() {
	$('#leaveonbehalfform').submit();	
}