$(function(){

	$("#navCompany").addClass("active open");
    $("#navCalendar").addClass("active");
        
	Calendar.initCalendar();
	Calendar.showMyCalendar();
	Calendar.showMyLeave();
	Calendar.showOtherLeave();
	
	$(document).on('change', '#calendardisplaytypes', function(e) {
		var selcalendardisplaytypes = this.options[this.selectedIndex].value;
		if (selcalendardisplaytypes=="Company Calendar"){
			Calendar.removeEvents("/leave/getapprovedleaveforcompanyviewjson/my");
			Calendar.removeEvents("/leave/getapprovedleaveforcompanyviewjson/allexceptmy");
			Calendar.removeEvents(Calendar.deptleavesurl);
			Calendar.showMyLeave();
			Calendar.showOtherLeave();
		} else if (selcalendardisplaytypes=="My Calendar"){
			Calendar.removeEvents("/leave/getapprovedleaveforcompanyviewjson/my");
			Calendar.removeEvents("/leave/getapprovedleaveforcompanyviewjson/allexceptmy");
			Calendar.removeEvents(Calendar.deptleavesurl);
			Calendar.showMyLeave();
		} else {
			Calendar.removeEvents("/leave/getapprovedleaveforcompanyviewjson/my");
			Calendar.removeEvents("/leave/getapprovedleaveforcompanyviewjson/allexceptmy");
			Calendar.removeEvents(Calendar.deptleavesurl);
			Calendar.deptleavesurl = "/leave/getapprovedleaveforcompanyviewjson/" + selcalendardisplaytypes;
			Calendar.showDeptCalendar();
		}
	})
	
});

var Calendar = {
	companyholidaysource:{
		url: '/companyholiday/getcompanyholidayjson/y',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching company holiday!');
		},
		className: 'label-success'
	},
	
	eventsource:{
		url: '/event/getevent/y?p_page=company',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching event!');
		},
		className: 'label-success'
	},
	
	myapprovedleavessource:{
		url: '/leave/getapprovedleaveforcompanyviewjson/my',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
	
	otherapprovedleavessource:{
		url: '/leave/getapprovedleaveforcompanyviewjson/allexceptmy',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
		
	deptleavesurl:{},
		
	deptleavessource:{},
		
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

	removeEvents:function(p_source){
		$('#calendar').fullCalendar( 'removeEventSource', p_source )
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
	
	showDeptCalendar:function(){
		this.deptleavessource = {
			url: this.deptleavesurl,
			type: 'GET',
			cache: false,
			error: function() {
				alert('There was an error while fetching department leave!');
			},
			color: 'blue',   // a non-ajax option
			textColor: 'white' // a non-ajax option
		}
		$('#calendar').fullCalendar('addEventSource',this.deptleavessource);
	}

}