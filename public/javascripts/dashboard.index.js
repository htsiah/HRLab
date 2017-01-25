$(function(){
    $("#navProfile").addClass("active open");
    $("#navDashboard").addClass("active");
    	
	CALENDAR.initCalendar();
	CALENDAR.showCompanyHoliday(true, "myprofile");
	CALENDAR.showEvent(true, "myprofile");
	CALENDAR.showMyLeave(true, "myprofile");
	CALENDAR.showOtherLeave(true, "myprofile");
});

function dismissTask(p_lk){
	$.getJSON("/task/dismiss/" + p_lk, function(data){
		if(data.status == true){
			$("#div" + p_lk).hide("slow");
		}else{
			("Error dismissing action");
		}
	});
};