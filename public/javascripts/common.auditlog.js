
function displayAuditLog(p_lk) {
	
	bootbox.dialog({
        title: "<div class='blue'><i class='ace-icon fa fa-history'></i>&nbsp;&nbsp;Audit Log</div>",
        title_html: true,
        message: '<table class="table  table-bordered table-hover"><tr><th>Date</th><th>By</th><th>Message</th></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr>' +
        	'<tr><td>Date</td><td>By</td><td>Message</td></tr></table>',
        buttons: {
            cancel: {
                label: " Close ",
                className: "btn"
            }
        }
    });
	
}