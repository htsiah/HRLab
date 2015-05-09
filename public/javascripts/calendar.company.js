
var Calendar = {

	companyholidaysource:{
		url: '/companyholiday/getcompanyholidayjson',
		type: 'GET',
		error: function() {
			alert('There was an error while fetching company holiday!');
		},
		className: 'label-success'
	},
	
	myapprovedleavessource:{
		url: '/leave/getapprovedleaveforcompanyviewjson/my',
		type: 'GET',
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
	
	deptleavessource:{},
	
	initCalendar:function(){
		var date = new Date();
		var d = date.getDate();
		var m = date.getMonth();
		var y = date.getFullYear();
		$('#calendar').fullCalendar({});	
	},

	removeEvents:function(){
		$('#calendar').fullCalendar('removeEvents').fullCalendar('removeEventSources');
	},

	showMyCalendar:function(){
		$('#calendar').fullCalendar('addEventSource',this.companyholidaysource);
	},
	
	showMyLeave:function(){
		$('#calendar').fullCalendar('addEventSource',this.myapprovedleavessource);
	},
	
	showDeptCalendar:function(p_dept){
		this.deptleavessource = {
			url: '/leave/getapprovedleavejson/' + p_dept,
			type: 'GET',
			error: function() {
				alert('There was an error while fetching department leave!');
			},
			color: 'blue',   // a non-ajax option
			textColor: 'white' // a non-ajax option
		}
		$('#calendar').fullCalendar('addEventSource',this.deptleavessource);
	}

}

$(function(){

	$("#navCompany").addClass("active open");
    $("#navCalendar").addClass("active");
        
	Calendar.initCalendar();
	Calendar.showMyCalendar();
	Calendar.showMyLeave();
	
	$(document).on('change', '#calendardisplaytypes', function(e) {
		var selcalendardisplaytypes = this.options[this.selectedIndex].value;
		if (selcalendardisplaytypes=="My Calendar"){
			Calendar.removeEvents();
			Calendar.showMyCalendar();
			Calendar.showMyLeave();
		} else {
			Calendar.removeEvents();
			Calendar.showDeptCalendar(selcalendardisplaytypes);
		}
	})
});