

function delApp() {
	
	bootbox.dialog({
        title: "<i class='ace-icon fa fa-warning red'></i> Delete Application (" + $("#header-company-name").text() + ")",
        title_html: true,
        message: '<span class="help-block">Deleting your HR Application is irreversible. Enter your company name below to confirm you want to permanently delete it.</span>' +
        '<input id="company" name="company" type="text" class="form-control">',
        buttons: {
            danger: {
                label: "<i class='ace-icon fa fa-trash-o bigger-110'></i>&nbsp; Delete",
                className: "btn-danger",
                callback: function () {
                    var name = $('#company').val();
                    $.post("/deleteapp/delete", {company: name}, function(data){
                    	window.location.replace(data.url);
                    });
                }
            },
            cancel: {
                label: "Cancel",
                className: "btn btn-mini"
            }
        }
    });
	
}