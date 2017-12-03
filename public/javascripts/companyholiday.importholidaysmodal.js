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
	
	$( "#selection" ).on("click", function() {
		if ($("#selection").is(':checked')) {
			$('.cbxactive:checkbox').each(function() {
				$(this).prop('checked', true);			
			})
		} else {
			$('.cbxactive:checkbox').each(function() {
				$(this).prop('checked', false);			
			})
		}
	});
	
});

let IMPORTHOLIDAYS = (function(){
	
	// Private	
	let configholidaysJSON = {}, 
		selectedCounty = "",
		selectedYear = "";
	
	// Public
	return {
		
		init: function(){
			if ( sessionStorage.configholidays === null || sessionStorage.configholidays === undefined ) { 
	    		$.ajax({
	    			url: "/companyholiday/getconfigholidays",
	    			dataType: "json",
	    			async: false,
	    			success: function(data){
	    				sessionStorage.configholidays = JSON.stringify(data);
	    			},
	    			error: function(xhr, status, error){
	    				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
	    			},
	    		});	
			};
				
			configholidaysJSON =  JSON.parse(sessionStorage.configholidays);

			// Remove selection value
			$("#selection:checkbox").prop('checked', false); 
			
			// Remove offices value
			$(".office:checkbox:checked").prop('checked', false); 
			
			// Append country options
			$('#ct option').remove();
			let CTOptfragment = document.createDocumentFragment();
			CTOptfragment.appendChild( new Option("Please select","") );			
			for(let i=0, totalcountry=configholidaysJSON["data"].length; i<totalcountry; i++){
				for (ct in configholidaysJSON["data"][i]) {
					CTOptfragment.appendChild( new Option(ct, ct) );
				}
			}
			$('#ct').append( CTOptfragment );
			
			this.changeCountry();
			
			$("#importholidaysmodal").modal("show");
		},
		changeCountry: function(){
			this.selectedCounty = $( "#ct option:selected" ).val();
			
			// Append year options
			$('#yr option').remove();
			let YROptfragment = document.createDocumentFragment();
			YROptfragment.appendChild( new Option("Please select","") );
			if (this.selectedCounty !== "") {				
				for(let i=0, totalcountry=configholidaysJSON["data"].length; i<totalcountry; i++){
					for (ct in configholidaysJSON["data"][i]) {
						if (ct==this.selectedCounty){
							for(let j=0, totalyear=configholidaysJSON["data"][i][this.selectedCounty].length; j<totalyear; j++){
								for (yr in configholidaysJSON["data"][i][this.selectedCounty][j]) {
									YROptfragment.appendChild( new Option(yr, yr) );
								};					
							};
							break;
						}
					}
				}
			};
			$('#yr').append( YROptfragment );
			
			this.changeYear();
		},
		changeYear: function(){
			this.selectedYear = $( "#yr option:selected" ).val();
			
			// Draw holiday
			$('#holidays').empty();
			let Holidayfragment = document.createDocumentFragment();
			if ( this.selectedYear != "") {
				for(let i=0, totalcountry=configholidaysJSON["data"].length; i<totalcountry; i++){
					for (ct in configholidaysJSON["data"][i]) {
						if (ct==this.selectedCounty){
							for(let j=0, totalyear=configholidaysJSON["data"][i][this.selectedCounty].length; j<totalyear; j++){
								for (yr in configholidaysJSON["data"][i][this.selectedCounty][j]) {
									if (yr == this.selectedYear) {
										let totalholiday = configholidaysJSON["data"][i][this.selectedCounty][j][this.selectedYear].length;
										for(let k=0; k<totalholiday; k++){
											let holidaydetail = configholidaysJSON["data"][i][this.selectedCounty][j][this.selectedYear][k];
											let $holiday = $('.holiday_template').children().clone();
											$holiday.find(".cbx").attr("name","cb" + k);
											$holiday.find(".cbx").attr("value",k);
											$holiday.find(".cbx").addClass("cbxactive");
											$holiday.find(".datx").text(holidaydetail.dt);
											$holiday.find(".datx").attr("id", "date" + k);
											$holiday.find(".dayx").text(holidaydetail.dy);
											$holiday.find(".dayx").attr("id", "day" + k);
											$holiday.find(".holidayx").text(holidaydetail.n);
											$holiday.find(".holidayx").attr("id", "holiday" + k);
											$holiday.find(".obsx").attr("name", "obs" + k);
											$holiday.find(".obsx").attr("id", "obs" + k);
											
											// Append to fragment
											let div = document.createElement('div');
											div.innerHTML = $holiday.get(0).outerHTML;
											Holidayfragment.appendChild( div.firstChild );
										};
										break;
									};
								};					
							};
							break;
						}
					}
				}				
				
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
		    
			// Remove selection value
			$("#selection:checkbox").prop('checked', false); 
			
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
				$.ajax({
				    type :  "POST",
				    dataType: 'json',
				    data: JSON.stringify(importholidaysObject),
				    contentType: "application/json; charset=utf-8",
				    url  :  "/companyholiday/importholidays",
				    success: function(data){
				    	CALENDAR.removeEvent("showCompanyHoliday");
				    	CALENDAR.showCompanyHoliday(true, "company");
				    	$("#importholidaysmodal").modal("hide");
				    },
	    			error: function(xhr, status, error){
	    				alert("There was an error while adding holidays. Do not proceed! Please contact support@hrsifu.com.");
	    			},
				});
			} else {
				alert("No holiday selected!");
			};
		}
		
	}
	
})();