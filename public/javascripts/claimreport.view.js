$(function(){
   
	$("#navClaimReports").addClass("active open");
		
	switch(path) {
    case "/report/myclaimrequest":
    	$("#navMyClaimRequest").addClass("active");
        break;
    case "/report/allstaffclaimrequest":
    	$("#navAllStaffClaimRequest").addClass("active");
        break;
	};
	
});