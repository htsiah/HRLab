$(function(){
	
	$( "#addholidayfromlistbtn" ).on( "click", function() {
		IMPORTHOLIDAYS.init();
	});
	
	$( "#ct" ).change( "click", function() {
		IMPORTHOLIDAYS.changeCountry();
	});
	
	$( "#yr" ).change( "click", function() {
		IMPORTHOLIDAYS.changeYear();
	});
	
});

// Form submit function
let handleSubmit = function() {
	$('#importholidaysform').submit();
};

let IMPORTHOLIDAYS = (function(){
	
	// Private	
	let configholidaysJSON = {}, 
		selectedCounty = "",
		selectedYear = "";
	
	// Public
	return {
		
		init: function(){
			if ( sessionStorage.configholidays === null || sessionStorage.configholidays === undefined ) { 
				sessionStorage.configholidays = '{"Malaysia":{"2017":[{"name":"Chinese New Year 1","dat":"2017-02-13","day":"Monday"},{"name":"Chinese New Year 2","dat":"2017-02-14","day":"Tuesday"},{"name":"Chinese New Year 3","dat":"2017-02-15","day":"Wednesday"},{"name":"Chinese New Year 4","dat":"2017-02-16","day":"Thursday"}],"2018":[{"name":"Chinese New Year ","dat":"2018-02-13","day":"Monday"},{"name":"Chinese New Year ","dat":"2018-02-14","day":"Monday"}]},"Singapore":{"2019":[{"name":"Chinese New Year ","dat":"2017-02-13","day":"Monday"},{"name":"Chinese New Year ","dat":"2017-02-14","day":"Monday"}],"2020":[{"name":"Chinese New Year ","dat":"2018-02-13","day":"Monday"},{"name":"Chinese New Year ","dat":"2018-02-14","day":"Monday"}]}}' 
			};
				
			configholidaysJSON =  JSON.parse(sessionStorage.configholidays);
			
			// Append country options
			$('#ct option').remove();
			let CTOptfragment = document.createDocumentFragment();
			CTOptfragment.appendChild( new Option("Please select","") );
			for (ct in configholidaysJSON) {
				CTOptfragment.appendChild( new Option(ct, ct) );
			};
			$('#ct').append( CTOptfragment );
			
			this.changeCountry();
			
			$("#importholidaysmodal").modal("show");
		},
		changeCountry: function(){
			selectedCounty = $( "#ct option:selected" ).val();
			
			// Append year options
			$('#yr option').remove();
			let YROptfragment = document.createDocumentFragment();
			YROptfragment.appendChild( new Option("Please select","") );
			if (selectedCounty !== "") {
				for (yr in configholidaysJSON[selectedCounty]) {
					YROptfragment.appendChild( new Option(yr, yr) );
				};
			};
			$('#yr').append( YROptfragment );
			
			this.changeYear();
		},
		changeYear: function(){
			selectedYear = $( "#yr option:selected" ).val();
			
			// Draw holiday
			$('#holidays').empty();
			let Holidayfragment = document.createDocumentFragment();
			if ( selectedYear != "") {
				let totalholiday = configholidaysJSON[selectedCounty][selectedYear].length;
				for(let i=0; i<totalholiday; i++){
					let holidaydetail = configholidaysJSON[selectedCounty][selectedYear][i];
					let $holiday = $('.holiday_template').children().clone();
					$holiday.find(".cbx").attr("name","cb" + i);
					$holiday.find(".cbx").attr("value",i);
					$holiday.find(".datx").text(holidaydetail.dat);
					$holiday.find(".datx").attr("id", "date" + i);
					$holiday.find(".dayx").text(holidaydetail.day);
					$holiday.find(".dayx").attr("id", "day" + i);
					$holiday.find(".holidayx").text(holidaydetail.name);
					$holiday.find(".holidayx").attr("id", "holiday" + i);
					$holiday.find(".obsx").attr("name", "obs" + i);
					$holiday.find(".obsx").attr("id", "obs" + i);
					
					// Append to fragment
					let div = document.createElement('div');
					div.innerHTML = $holiday.get(0).outerHTML;
					Holidayfragment.appendChild( div.firstChild );
				};
			} else {
				// Append to fragment
				let div = document.createElement('div');
				div.innerHTML = `<div class="row"><div class="col-md-12 alert alert-warning">Please select country and year to display holidays.</div></div>`;
				Holidayfragment.appendChild( div.firstChild );
			};
			$('#holidays').append( Holidayfragment );
			
		    $('.date-picker').datepicker({
				autoclose: true,
				todayHighlight: true
			})
					
			//show datepicker when clicking on the icon
			.next().on(ace.click_event, function(){
				$(this).prev().focus();
			});
			
		}, 
		add: function(){
			let importholidaysObject = {};
			
			if ($('.cbx:checkbox:checked').length !==0) {
				importholidaysObject.offices = [];
				importholidaysObject.holidays = [];
				$('.office:checkbox:checked').each(function () {
					importholidaysObject.offices.push(this.checked ? $(this).val() : "");
				});
				$('.cbx:checkbox:checked').each(function (n) {
					let selectedNumber = $(this).val();
					let selectedName = $("#holiday"+selectedNumber).text();
					let selectedDate = $("#obs"+selectedNumber).val() !== "" ? $("#obs"+selectedNumber).val() : $("#date"+selectedNumber).text();
					importholidaysObject.holidays.push({"name" : selectedName, "date": selectedDate})
				});
				// $.post( "/companyholiday/importholidays", JSON.stringify({ "name": "John", "time": "2pm" }) );
				// var d = { 'filter': "John Portella" };
				$.ajax({
				    type :  "POST",
				    dataType: 'json',
				    data: JSON.stringify({ "name": "John", "time": "2pm" }),
				    contentType: "application/json; charset=utf-8",
				    url  :  "/companyholiday/importholidays",
				        success: function(data){
				            console.log(data);
				        }
				});
				
				
				alert(importholidaysObject);
			} else {
				alert("No holiday selected!");
			};
		}
		
	}
	
})();