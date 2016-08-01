$(function(){

	$("#navCompany").addClass("active open");
	$("#navStaff").addClass("active");
	
	$('#adj').ace_spinner({
		value:0,
		min:-100,
		max:100,
		step:0.5, 
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});

	$('#e1,#e2,#e3,#e4,#e5,#e6,#e7,#e8,#e9,#e10,#e1_cf,#e2_cf,#e3_cf,#e4_cf,#e5_cf,#e6_cf,#e7_cf,#e8_cf,#e9_cf,#e10_cf').ace_spinner({
		value:0,
		min:0,
		max:120,
		step:1, 
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});

	$('#e1_s,#e2_s,#e3_s,#e4_s,#e5_s,#e6_s,#e7_s,#e8_s,#e9_s,#e10_s').ace_spinner({
		value:0,
		min:0,
		max:999,
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
			url: "/leaveprofile/getvaluesonleavetypechangejson/" + p_pid + "/" + selectedLT,
			contentType: "application/json; charset=utf-8",
			beforeSend: function(){
				loader.on();
			},
			success: function(data){
				$("#e1_s").ace_spinner('value', data.e1_s);
				$("#e1").ace_spinner('value', data.e1);
				$("#e1_cf").ace_spinner('value', data.e1_cf);
				$("#e2_s").ace_spinner('value', data.e2_s);
				$("#e2").ace_spinner('value', data.e2);
				$("#e2_cf").ace_spinner('value', data.e2_cf);
				$("#e3_s").ace_spinner('value', data.e3_s);
				$("#e3").ace_spinner('value', data.e3);
				$("#e3_cf").ace_spinner('value', data.e3_cf);
				$("#e4_s").ace_spinner('value', data.e4_s);
				$("#e4").ace_spinner('value', data.e4);
				$("#e4_cf").ace_spinner('value', data.e4_cf);
				$("#e5_s").ace_spinner('value', data.e5_s);
				$("#e5").ace_spinner('value', data.e5);
				$("#e5_cf").ace_spinner('value', data.e5_cf);
				$( "#v_earned" ).text(data.earned);
				$( "#v_ent" ).text(data.ent);
				$( "#v_bal" ).text(data.bal);
				$( "#v_cbal" ).text(data.cbal);
				$( "#m_jan" ).text(data.m_jan);
				$( "#m_feb" ).text(data.m_feb);
				$( "#m_mar" ).text(data.m_mar);
				$( "#m_apr" ).text(data.m_apr);
				$( "#m_may" ).text(data.m_may);
				$( "#m_jun" ).text(data.m_jun);
				$( "#m_jul" ).text(data.m_jul);
				$( "#m_aug" ).text(data.m_aug);
				$( "#m_sep" ).text(data.m_sep);
				$( "#m_oct" ).text(data.m_oct);
				$( "#m_nov" ).text(data.m_nov);
				$( "#m_dec" ).text(data.m_dec);
				loader.off();
			},
			error: function(xhr, status, error){
				$("#e1_s").ace_spinner('value', 0);
				$("#e1").ace_spinner('value', 0);
				$("#e1_cf").ace_spinner('value', 0);
				$("#e2_s").ace_spinner('value', 0);
				$("#e2").ace_spinner('value', 0);
				$("#e2_cf").ace_spinner('value', 0);
				$("#e3_s").ace_spinner('value', 0);
				$("#e3").ace_spinner('value', 0);
				$("#e3_cf").ace_spinner('value', 0);
				$("#e4_s").ace_spinner('value', 0);
				$("#e4").ace_spinner('value', 0);
				$("#e4_cf").ace_spinner('value', 0);
				$("#e5_s").ace_spinner('value', 0);
				$("#e5").ace_spinner('value', 0);
				$("#e5_cf").ace_spinner('value', 0);
				$( "#v_earned" ).text("0");
				$( "#v_ent" ).text("0");
				$( "#v_bal" ).text("0");
				$( "#v_cbal" ).text("0");
				$( "#m_jan" ).text("0");
				$( "#m_feb" ).text("0");
				$( "#m_mar" ).text("0");
				$( "#m_apr" ).text("0");
				$( "#m_may" ).text("0");
				$( "#m_jun" ).text("0");
				$( "#m_jul" ).text("0");
				$( "#m_aug" ).text("0");
				$( "#m_sep" ).text("0");
				$( "#m_oct" ).text("0");
				$( "#m_nov" ).text("0");
				$( "#m_dec" ).text("0");
				loader.off();
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