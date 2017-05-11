$(function(){
	
	$("#navCompany").addClass("active open");
	$("#navOrgChart").addClass("active");
	
	$.ajax({
		url: "/orgchart/getchart",
		dataType: "json"
	}).done(function( data ) {
		for(let i=0, x= data.ids.length; i<x; i++){
			$("#chart-container").append("<div id='chart-container-" + i + "'></div>");
			$('#chart-container-' + i).orgchart({
				'data' : "/orgchart/getchartstructure/" + data.ids[i],
				'verticalDepth': data.verticalDepth,
				'nodeContent': 'title',
				'zoom': true,
				'zoominLimit': 1,
				'zoomoutLimit': 0.5,
				'chartClass': 'OrgChart-' + data.ids[i]
			});
		};
		$('.orgchart').width('95%');
	}).fail(function(xhr, status, error) {
		alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
	});
	
});