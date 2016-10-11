$(function(){
    $("#navProfile").addClass("active open");
    $("#navDashboard").addClass("active");
    
    Calendar.initCalendar();
	Calendar.showMyCalendar();
	Calendar.showMyLeave();
	Calendar.showOtherLeave();
});

var Calendar = {
	companyholidaysource:{
		url: '/companyholiday/getcompanyholiday/y?p_page=myprofile',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching company holiday!');
		},
		className: 'label-success'
	},
	
	eventsource:{
		url: '/event/getevent/y?p_page=myprofile',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching event!');
		},
		className: 'label-success'
	},
		
	myapprovedleavessource:{
		url: '/leave/getapprovedleave/my/y',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
		
	otherapprovedleavessource:{
		url: '/leave/getapprovedleave/allexceptmy/y',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
		
	initCalendar:function(){
		var date = new Date();
		var d = date.getDate();
		var m = date.getMonth();
		var y = date.getFullYear();
		$('#calendar').fullCalendar({
		     eventRender: function(event, element) {
		    	 $(element).tooltip({title: event.tip});
		     }
		});	
	},

	removeEvents:function(){
		$('#calendar').fullCalendar('removeEvents').fullCalendar('removeEventSources');
	},

	showMyCalendar:function(){
		$('#calendar').fullCalendar('addEventSource',this.companyholidaysource);
		$('#calendar').fullCalendar('addEventSource',this.eventsource);
	},
		
	showMyLeave:function(){
		$('#calendar').fullCalendar('addEventSource',this.myapprovedleavessource);
	},
		
	showOtherLeave:function(){
		$('#calendar').fullCalendar('addEventSource',this.otherapprovedleavessource);
	},

};

function dismissTask(p_lk){
	$.getJSON("/task/dismiss/" + p_lk, function(data){
		if(data.status == true){
			$("#div" + p_lk).hide("slow");
		}else{
			("Error dismissing action");
		}
	});
}