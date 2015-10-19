$(function(){
   
	$("#navReports").addClass("active open");
		
	switch(path) {
    case "/report/myleaverequest":
    	$("#navMyLeaveRequest").addClass("active");
        break;
    case "/report/myteamleaverequest":
    	$("#navMyTeamLeaveRequest").addClass("active");
        break;
    case "/report/allstaffleaverequest":
    	$("#navAllStaffLeaveRequest").addClass("active");
        break;
	};
	
});