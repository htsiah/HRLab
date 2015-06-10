
var Calendar = {

	companyholidaysource:{
		url: '/companyholiday/getcompanyholidayjson',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching company holiday!');
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
	
	deptleavesurl:{},
	
	deptleavessource:{},
	
	initCalendar:function(){
		var date = new Date();
		var d = date.getDate();
		var m = date.getMonth();
		var y = date.getFullYear();
		$('#calendar').fullCalendar({});	
	},

	removeEvents:function(p_source){
		$('#calendar').fullCalendar( 'removeEventSource', p_source )
	},

	showMyCalendar:function(){
		$('#calendar').fullCalendar('addEventSource',this.companyholidaysource);
	},
	
	showMyLeave:function(){
		$('#calendar').fullCalendar('addEventSource',this.myapprovedleavessource);
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

$(function(){

	$("#navCompany").addClass("active open");
    $("#navCalendar").addClass("active");
        
	Calendar.initCalendar();
	Calendar.showMyCalendar();
	Calendar.showMyLeave();
	
	$(document).on('change', '#calendardisplaytypes', function(e) {
		var selcalendardisplaytypes = this.options[this.selectedIndex].value;
		if (selcalendardisplaytypes=="My Calendar"){
			Calendar.removeEvents(Calendar.deptleavesurl);
			Calendar.showMyCalendar();
			Calendar.showMyLeave();
		} else {
			Calendar.removeEvents(Calendar.deptleavesurl);
			Calendar.removeEvents("/companyholiday/getcompanyholidayjson");
			Calendar.removeEvents("/leave/getapprovedleaveforcompanyviewjson/my");
			Calendar.deptleavesurl = "/leave/getapprovedleavejson/" + selcalendardisplaytypes;
			Calendar.showDeptCalendar();
			
		}
	})
});