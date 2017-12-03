$(function(){

	$("#navCompany").addClass("active open");
    $("#navCalendar").addClass("active");
            
	CALENDAR.initCalendar();
	CALENDAR.showCompanyHoliday(true, "company");
	CALENDAR.showEvent(true, "company");
	CALENDAR.showMyLeave(true, "company");
	CALENDAR.showOtherLeave(true, "company");
	
	$(document).on('change', '#calendardisplaytypes', function(e) {
		let selcalendardisplaytypes = this.options[this.selectedIndex].value;
		if (selcalendardisplaytypes=="Company Calendar"){
			CALENDAR.removeEvent("showMyLeave");
			CALENDAR.removeEvent("showOtherLeave");
			CALENDAR.removeEvent("showDeptLeave");
			CALENDAR.showMyLeave(true, "company");
			CALENDAR.showOtherLeave(true, "company");
		} else if (selcalendardisplaytypes=="My Calendar"){
			CALENDAR.removeEvent("showMyLeave");
			CALENDAR.removeEvent("showOtherLeave");
			CALENDAR.removeEvent("showDeptLeave");
			CALENDAR.showMyLeave(true, "company");
		} else {
			CALENDAR.removeEvent("showMyLeave");
			CALENDAR.removeEvent("showOtherLeave");
			CALENDAR.removeEvent("showDeptLeave");
			CALENDAR.showDeptLeave(selcalendardisplaytypes);
		}
	});
    
});