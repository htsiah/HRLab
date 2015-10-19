$(function(){

	$("#navReports").addClass("active open");
	
	switch(path) {
    case "/report/myteamleaveprofile":
    	$("#navMyTeamLeaveProfile").addClass("active");
        break;
    case "/report/allstaffleaveprofile":
    	$("#navAllStaffLeaveProfile").addClass("active");
        break;
	};
		
});