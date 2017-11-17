$(function(){

    $("#navDashboard").addClass("active");
    
    // Date time field
	$(".datetime-picker").datetimepicker({
    	format: 'D-MMM-YYYY h:mm A',
    	defaultDate: moment().startOf('day'),
    	showClose: true,
    	icons: {
    		time: 'fa fa-clock-o',
    		date: 'fa fa-calendar',
    		up: 'fa fa-chevron-up',
    		down: 'fa fa-chevron-down',
    		previous: 'fa fa-chevron-left',
    		next: 'fa fa-chevron-right',
    		today: 'fa fa-arrows ',
    		clear: 'fa fa-trash',
    		close: 'fa fa-times'
    	}
    }).next().on(ace.click_event, function(){
    	$(this).prev().focus();
    });
    
	$("#t_fdat").data("DateTimePicker").date($("#fdat").data("DateTimePicker").date());
	$("#t_tdat").data("DateTimePicker").date($("#tdat").data("DateTimePicker").date());
	
    if($("#aday").is(":checked")) {
    	$("#t_fdat").data("DateTimePicker").format("D-MMM-YYYY");
		$("#t_tdat").data("DateTimePicker").format("D-MMM-YYYY");
    };
            
    // To date can not earlier than From date
    $("#t_tdat").data("DateTimePicker").minDate($("#t_fdat").data("DateTimePicker").date());
    
    // Disable empty date field after selecting current date  
    let stickyDate;
    $("#t_fdat").on("dp.show", function(e){
    	$(this).data("stickyDate", $("#t_fdat").data("DateTimePicker").date());
    });

    $("#t_fdat").on("dp.hide", function(e){
        stickyDate = $(this).data("stickyDate");
        if ( !$("#t_fdat").data("DateTimePicker").date()) {
        	$(this).data("DateTimePicker").date(stickyDate);
        } else {
        	$("#t_tdat").data("DateTimePicker").minDate($("#t_fdat").data("DateTimePicker").date());
        	$("#t_tdat").data("DateTimePicker").date($("#t_fdat").data("DateTimePicker").date());
        }
        $(this).data("stickyDate", null);
    });

    $("#t_tdat").on("dp.show", function(e){
    	$(this).data("stickyDate", $("#t_tdat").data("DateTimePicker").date());
    });

    $("#t_tdat").on("dp.hide", function(e){
        stickyDate = $(this).data("stickyDate");
        if ( !$("#t_tdat").data("DateTimePicker").date() ) {
        	$(this).data("DateTimePicker").date(stickyDate);
        };
        $(this).data("stickyDate", null);
    });
    
    // Choosen field
    $(".chosen-select").chosen({
    	placeholder_text_multiple: "Please select",
    	width: "100%",
    	display_selected_options: false
    });
    
    $('ul[class="chosen-choices"]').addClass( "form-control" );
    
    // Initial colour picker
    $("#c").ace_colorpicker();

	$("#eventform").validate({
		onkeyup: false,
		rules: {
			"n": "required"
		},
		messages: {
			"n": "Please enter event name."
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
    
});

$("#aday").click(function(){
	if(this.checked){
		$("#t_fdat").data("DateTimePicker").format("D-MMM-YYYY");
		$("#t_tdat").data("DateTimePicker").format("D-MMM-YYYY");
	} else {
		$("#t_fdat").data("DateTimePicker").format("D-MMM-YYYY h:mm A");
		$("#t_tdat").data("DateTimePicker").format("D-MMM-YYYY h:mm A");
	}
});

// Form submit function
var handleSubmit = function() {
	
	if ($("#aday").is(":checked")) {
		$("#fdat").data("DateTimePicker").date(moment($("#t_fdat").data("DateTimePicker").date()).startOf("day"));
		$("#tdat").data("DateTimePicker").date(moment($("#t_tdat").data("DateTimePicker").date()).startOf("day"));
	} else {
		$("#fdat").data("DateTimePicker").date($("#t_fdat").data("DateTimePicker").date());
		$("#tdat").data("DateTimePicker").date($("#t_tdat").data("DateTimePicker").date());
	};
	
	$('#eventform').submit();
	
}