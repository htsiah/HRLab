let CALENDAR = (function(){
	
	// Private	
	let companyHolidaySource = {
			url: '',
			type: 'GET',
			cache: true,
			error: function() {
				alert('There was an error while fetching company holiday!');
			},
			className: 'label-success'
		},
		eventSource = {
			url: '',
			type: 'GET',
			cache: true,
			error: function() {
				alert('There was an error while fetching event!');
			},
			className: 'label-success'
		},
		myLeaveSource = {
			url: '',
			type: 'GET',
			cache: true,
			error: function() {
				alert('There was an error while fetching your leave!');
			},
			color: 'blue',   // a non-ajax option
			textColor: 'white' // a non-ajax option
		},
		otherLeaveSource = {
			url: '',
			type: 'GET',
			cache: true,
			error: function() {
				alert('There was an error while fetching leave!');
			},
			color: 'blue',   // a non-ajax option
			textColor: 'white' // a non-ajax option
		},
		deptLeaveSource = {
			url: '',
			type: 'GET',
			cache: true,
			error: function() {
				alert('There was an error while fetching leave!');
			},
			color: 'blue',   // a non-ajax option
			textColor: 'white' // a non-ajax option
		};
		
	// Public
	return {
		initCalendar: function(){
			$('#calendar').fullCalendar({
			     eventRender: function(event, element) {
			    	 $(element).tooltip({title: event.tip});
			     }
			});	
		},
		
		removeEvent: function(p_source){
			switch (p_source) {
				case "showCompanyHoliday" : $('#calendar').fullCalendar('removeEventSource', companyHolidaySource.url); break;
				case "showEvent" : $('#calendar').fullCalendar('removeEventSource', eventSource.url); break;
				case "showMyLeave" : $('#calendar').fullCalendar('removeEventSource', myLeaveSource.url); break;
				case "showOtherLeave" : $('#calendar').fullCalendar('removeEventSource', otherLeaveSource.url); break;
				case "showDeptLeave" : $('#calendar').fullCalendar('removeEventSource', deptLeaveSource.url); break;
			};			
		},
		
		showCompanyHoliday: function(p_link, p_page){
			p_link = p_link || true;
			p_page = p_page || "";
			companyHolidaySource.url = p_link ? "/companyholiday/getcompanyholiday/y" : "/companyholiday/getcompanyholiday/n";
			companyHolidaySource.url += p_page==="" ? "" : "?p_page=" + p_page;
			$('#calendar').fullCalendar('addEventSource', companyHolidaySource);
		},
		
		showEvent: function(p_link, p_page){
			p_link = p_link || true;
			p_page = p_page || "";
			eventSource.url = p_link ? "/event/getevent/y" : "/event/getevent/n";
			eventSource.url += p_page==="" ? "" : "?p_page=" + p_page;
			$('#calendar').fullCalendar('addEventSource', eventSource);
		},
		
		showMyLeave: function(p_link, p_page){
			p_link = p_link || true;
			p_page = p_page || "";
			myLeaveSource.url = p_link ? "/leave/getapprovedleave/my/y" : "/leave/getapprovedleave/my/n";
			myLeaveSource.url += p_page==="" ? "" : "?p_page=" + p_page;
			$('#calendar').fullCalendar('addEventSource', myLeaveSource);
		},
		
		showOtherLeave: function(p_link, p_page){
			p_link = p_link || true;
			p_page = p_page || "";
			otherLeaveSource.url = p_link ? "/leave/getapprovedleave/allexceptmy/y" : "/leave/getapprovedleave/allexceptmy/n";
			otherLeaveSource.url += p_page==="" ? "" : "?p_page=" + p_page;
			$('#calendar').fullCalendar('addEventSource', otherLeaveSource);
		},
		
		showDeptLeave: function(p_dept){
			deptLeaveSource.url = "/leave/getapprovedleave/" + p_dept + "/y?p_page=company";
			$('#calendar').fullCalendar('addEventSource', deptLeaveSource);
		}
	}
	
})();