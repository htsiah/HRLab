$(function(){

    $("#navProfile").addClass("active open");
    $("#navDashboard").addClass("active");
    
    $('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
	
	//show datepicker when clicking on the icon
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

    $("#tdat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        }
    });
    
    $("#fdat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        } else {
    		$("#tdat").datepicker("setStartDate", $(this).val());
    		$("#tdat").datepicker("update", $(this).val());
        }
    });
        
	$("#companyholidayform").validate({
		onkeyup: false,
		rules: {
			"n": "required",
			"fdat": {
				required: true,
				date: true
			},
			"tdat": {
				required: true,
				date: true,
				checkDate: true
			}
		},
		messages: {
			"n": "Please enter holiday name.",
			"fdat": {
				required: "Please enter date from (start).",
				customDate: "Please enter a valid date format d-mmm-yyyy."
			},
			"tdat": {
				required: "Please enter date to (end).",
				customDate: "Please enter a valid date format d-mmm-yyyy.",
				checkDate: "Date to (end) should greater than date from (start)."
			}
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
	$.validator.addMethod(
		"customDate", 
		function(value, element) {
			return this.optional(element) || value.match(/^\d\d?-\w\w\w-\d\d\d\d/);
		}, 
		"Please enter a valid date format d-mmm-yyyy."
	);
	
	$.validator.addMethod("checkDate", 
		function(value) {
			var fdat = new Date($("#fdat").val());
			var tdat = new Date($("#tdat").val());
			if (fdat>tdat) {
				return false;
			} else {
				return true;
			}
		}, 
		'Date to (end) should greater than date from (start).'
	);
	
});

// Form submit function
var handleSubmit = function() {
	
	var selectedStates = "";
	var count = $("input:checkbox.state").length;
	
	for(var i=0; i<count; i++){
		st=$("input:checkbox:checked#st" + i).val();
		if (st!=undefined) {
			if (selectedStates=="") {
				selectedStates = st;
			} else {
				selectedStates = selectedStates + "," + st;
			}
		}
	};
	
	$("#st").val(selectedStates)
	$('#companyholidayform').submit();	

}