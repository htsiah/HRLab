$(function(){

    $("#navProfile").addClass("active open");
    $("#navMyProfile").addClass("active");
    
	$('#adj').ace_spinner({
		value:0,
		min:-100,
		max:100,
		step:1, 
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});

	$('#adj,#e1,#e2,#e3,#e4,#e5,#e1_cf,#e2_cf,#e3_cf,#e4_cf,#e5_cf').ace_spinner({
		value:0,
		min:0,
		max:100,
		step:1, 
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});

	$('#e1_s,#e2_s,#e3_s,#e4_s,#e5_s').ace_spinner({
		value:0,
		min:0,
		max:600,
		step:1, 
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});
	
	$( "#lt" ).change(function() {
		var selectedLT = $( "#lt option:selected" ).text();
		$.ajax({
			url: "/leavepolicy/getLeaveEntitlement/" + selectedLT + "/" + p_position,
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				// $('#loader-overlay').show()
			},
			success: function(data){
				$( "#e1_s" ).val(data.e1_s);
				$( "#e1" ).val(data.e1);
				$( "#e1_cf" ).val(data.e1_cf);
				$( "#e2_s" ).val(data.e2_s);
				$( "#e2" ).val(data.e2);
				$( "#e2_cf" ).val(data.e2_cf);
				$( "#e3_s" ).val(data.e3_s);
				$( "#e3" ).val(data.e3);
				$( "#e3_cf" ).val(data.e3_cf);
				$( "#e4_s" ).val(data.e4_s);
				$( "#e4" ).val(data.e4);
				$( "#e4_cf" ).val(data.e4_cf);
				$( "#e5_s" ).val(data.e5_s);
				$( "#e5" ).val(data.e5);
				$( "#e5_cf" ).val(data.e5_cf);
				// $('#loader-overlay').hide();
			},
			error: function(xhr, status, error){
				$( "#e1_s" ).val("0");
				$( "#e1" ).val("0");
				$( "#e1_cf" ).val("0");
				$( "#e2_s" ).val("0");
				$( "#e2" ).val("0");
				$( "#e2_cf" ).val("0");
				$( "#e3_s" ).val("0");
				$( "#e3" ).val("0");
				$( "#e3_cf" ).val("0");
				$( "#e4_s" ).val("0");
				$( "#e4" ).val("0");
				$( "#e4_cf" ).val("0");
				$( "#e5_s" ).val("0");
				$( "#e5" ).val("0");
				$( "#e5_cf" ).val("0");
				$('#loader-overlay').hide();
			},
		});
	});
		
	$("#leaveprofileform").validate({
		onkeyup: false,
		rules: {
			"lt": "required"
		},
		messages: {
			"lt": "Please select leave type."
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});