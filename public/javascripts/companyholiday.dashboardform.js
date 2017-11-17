$(function(){

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
	let stickyDate;
	$("#fdat, #tdat").on("show", function(e){
    	$(this).data("stickyDate", e.date);
    });

    $("#tdat").on("hide", function(e){
        stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        }
    });
    
    $("#fdat").on("hide", function(e){
        stickyDate = $(this).data("stickyDate");
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
			"n": "required"
		},
		messages: {
			"n": "Please enter holiday name."
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});

// Form submit function
var handleSubmit = function() {
	
	let selectedOffices = "";
	
	for(let i=0, count = $("input:checkbox.office").length; i<count; i++){
		off=$("input:checkbox:checked#off" + i).val();
		if (off!=undefined) {
			if (selectedOffices=="") {
				selectedOffices = off;
			} else {
				selectedOffices = selectedOffices + "," + off;
			}
		}
	};
	
	$("#off").val(selectedOffices)
	$('#companyholidayform').submit();	

}