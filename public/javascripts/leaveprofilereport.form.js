$(function(){

	$("#navReports").addClass("active open");
	
	switch(path) {
    case "/report/myteamleaveprofile":
    	$("#navMyTeamLeaveProfile").addClass("active");
        break;
	};
	
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

	$('#e1,#e2,#e3,#e4,#e5,#e1_cf,#e2_cf,#e3_cf,#e4_cf,#e5_cf').ace_spinner({
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
		max:999,
		step:1, 
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});
	
});