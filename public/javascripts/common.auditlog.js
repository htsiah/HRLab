
function displayAuditLog(p_lk) {
	
	$.ajax({
		url: "/auditlog/" + p_lk,
		dataType: "json",
		cache: true,
		success: function(data){
			bootbox.dialog({
		        title: "<div class='blue'><i class='ace-icon fa fa-history'></i> Audit Log</div>",
		        title_html: true,
		        message: data.auditlog,
		        buttons: {
		            cancel: {
		                label: " Close ",
		                className: "btn"
		            }
		        }
		    });
		},
		error: function(xhr, status, error){
			alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
		},
	});	
		
};