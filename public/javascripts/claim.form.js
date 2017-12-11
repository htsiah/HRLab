$(function(){
    
    $("#navDashboard").addClass("active");
    
	$('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
	
	//show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
	
	let stickyDate;
	$("#ed_rdat").on("show", function(e){
    	$(this).data("stickyDate", e.date);
    });

    $("#ed_rdat").on("hide", function(e){
        stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        }
    });
	
    // wysiwyg
    $('.wysiwyg-editor').ace_wysiwyg({
    	toolbar: [
			{name:'bold', className:'btn-info'},
			{name:'italic', className:'btn-info'},
			{name:'strikethrough', className:'btn-info'},
			{name:'underline', className:'btn-info'},
    		null,
			{name:'insertunorderedlist', className:'btn-success'},
			{name:'insertorderedlist', className:'btn-success'},
			{name:'outdent', className:'btn-purple'},
			{name:'indent', className:'btn-purple'},
    		null,
			{name:'justifyleft', className:'btn-primary'},
			{name:'justifycenter', className:'btn-primary'},
			{name:'justifyright', className:'btn-primary'},
			{name:'justifyfull', className:'btn-inverse'},
    		null,
    		'foreColor',
    		null,
    		{name:'viewSource', className:'btn-grey'}
    	]
    });
    
    // load Category 
    CATEGORY.init();
    
	$("#ed_cat").change( "click", function() {
		CATEGORY.change();
	});
	
	// Set approve amount
	$("#ed_amt_amt").change( "click", function() {
		setApproveAmount();
	});
	
	$("#ed_er").change( "click", function() {
		setApproveAmount();
	});
	
	$("#ed_amt_ccy").change( "click", function() {		
		if($("#ed_amt_ccy").val() == $("#ed_aamt_ccy").val()){
			$("#btnCallCompanyTaxModal").show(500);
			$('#ed_gstamt_tamt_amt').removeAttr('readonly');
			$("#ed_er").val("1.0");
			$('#ed_er').attr('readonly', true);
			setApproveAmount();
		} else {
			$('#ed_er').removeAttr('readonly');
			$("#btnCallCompanyTaxModal").hide(500);
			$("#ed_gstamt_tamt_amt").val("0.0");
			$('#ed_gstamt_tamt_amt').attr('readonly', true);		
			$("#ed_gstamt_cn").val("");
			$("#ed_gstamt_crnum").val("");
			$("#ed_gstamt_tnum").val("");
		}
	});
	
	// Binder on receipt upload field	
	$('#file').ace_file_input({
		style: 'well',
		btn_choose: 'Drop file here or click to choose',
		btn_change: null,
		no_icon: 'ace-icon fa fa-cloud-upload',
		droppable: true,
    	maxSize: 5000000, // 5 MB
        allowExt:  ['jpg', 'jpeg', 'tif', 'tiff', 'gif', 'bmp', 'png', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'csv', 'txt']
	}).on('change', function(e) {
					
 		let formfile = new FormData(), // Create a new FormData object for file upload.
 		files = $(this).data('ace_input_files');
 		
 		// Don't process invalid file selected.
 		if(files != undefined){
 			
 	 		let file = files[0]; // Get upload file
 	 	 	
 	 		// Add the file to the request.
 	 		formfile.append("file", file, file.name);
 	 		
 	 		// Upload using AJAX
 	 	    $.ajax({
 	 	        url: "/claimfile/insert?p_lk=" + $("#docnum").val(),
 	 	        type: "POST",
 	 	        data: formfile,
 	 	        cache: false,
 	 	        dataType: "json",
 	 	        processData: false, // Don't process the files
 	 	        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
 	 			beforeSend: function(){
 	 				$("#file-input-control").addClass("hidden");
 	 				$("#file-error").addClass("hidden");
 	 				$("#file-loader").removeClass("hidden");
 	 			},
 	 	        success: function(data, textStatus, jqXHR){
 	 		        if (data.status == "exceed file size limit") {
 	 		        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Over 5 MB file size limit.</label>");
 	 		        	$("#file-loader").addClass("hidden");
 	 		        	$("#file-input-control").removeClass("hidden");
 	 		        	$("#file-error").removeClass("hidden");
 	 		        } else if (data.status == "error update metadata" || data.status == "error upload file") {
 	 		        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>There was an error while uploading data to server. Do not proceed! Please contact support@hrsifu.com.</label>");
 	 		        	$("#file-loader").addClass("hidden");
 	 		        	$("#file-input-control").removeClass("hidden");
 	 		        	$("#file-error").removeClass("hidden");
 	 		        } else {
 	 					$("#file-loader").addClass("hidden");
 	 					$('#file').ace_file_input('reset_input');
 	 					$("#file-input-control").removeClass("hidden");
 	 					$("#file-view").append("<p id=" + data.id + "><a href='/claimfile/view?p_id=" + data.id + "' target='_blank'>" + file.name + "</a> &nbsp <a class='remove' href=javascript:onDelete('" + data.id + "') title='Delete'><i class='ace-icon fa fa-trash'></i></a></p>");
 	 		        };
 	 	        },
 	 	        error: function(jqXHR, textStatus, errorThrown){
 	 	        	$("#file-loader").addClass("hidden");
 	 	        	$("#file-view").removeClass("file-input-control");
 	 	        	alert("There was an error while uploading data to server. Do not proceed! Please contact support@hrsifu.com.");
 	 	        }
 	 	    });	
 			
 		}

 	});
	
	$('#file').on('file.error.ace', function(event, info) {
    	//info.file_count > number of files selected
    	//info.invalid_count > number of invalid files
    	//info.error_count['ext'] > number of files with invalid extension (only if allowExt or denyExt is set)
    	//info.error_count['mime'] > number of files with invalid mime type (only if allowMime or denyMime is set)
    	//info.error_count['size'] > number of files with invalid size (only if maxSize option is set)
    	//info.error_list['ext'] > list of file names with invalid extension
    	//info.error_list['mime'] > ...
    	//info.error_list['size'] > ...
    	//info.dropped > true if files have been selected by drag & drop
    	
    	if (info.error_count['ext'] > 0) {
        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Invalid file extension. Allowed file extensions are jpg, jpeg, tif, tiff, gif, bmp, png, pdf, doc, docx, xls, xlsx, ppt, pptx, csv and txt.</label>");
        	$("#file-error").removeClass("hidden");
    	} else if (info.error_count['size'] > 0) {
        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Upload aborted! Over 5 MB file size limit.</label>");
        	$("#file-error").removeClass("hidden");
    	};
    	
    	//if you do this
    	event.preventDefault();
    	//it will reset (empty) file input, i.e. no files selected
     });
	
    // form validation
	$("#claimform").validate({
		onkeyup: false,
		rules: {
			"ed.cat": "required",
			"ed.amt.amt": {
				required: true,
				checkTransactionLimit: [$("#ed_aamt_amt").val()],
				currency: ['$', false]
			},
			"ed.er": {
				required: true,
				number: true
			},
			"ed.gstamt.tamt.amt": {
				required: true,
				currency: ['$', false]
			}
		},
		messages: {
			"ed.cat": "Please select category.",
			"ed.amt.amt": {
				required: "Please enter claim amount.",
				checkTransactionLimit: function(){ return "Over " + $("#ed_aamt_ccy").val() + " " + CATEGORY.getTransactionLimit() + " transaction limit in Approve Amount." }
			},
			"ed.er": {
				required: "Please enter Exchange Rate."
			},
			"ed.gstamt.tamt.amt": {
				required: "Please enter GST / VAT."
			}
		},		 
		submitHandler: function(form) {
			$("#ed_d").val($("#d-editor").html());
			form.submit();
		 }
	});
	
	$.validator.addMethod(
		"checkTransactionLimit",
		function(value,element,params){
			let aamount = $("#ed_aamt_amt").val(),
				transactionlimit = parseFloat(CATEGORY.getTransactionLimit());

			if (transactionlimit!=0 && aamount>transactionlimit) {
				return false;
			} else {
				return true;
			};
		},
		"Over transaction limit."
	);
	
});

// Modal on Company Tax
function callCompanyTaxModal() {
	$('#dialog_gstamt_cn').val($("#ed_gstamt_cn").val());
	$('#dialog_gstamt_crnum').val($("#ed_gstamt_crnum").val());
	$('#dialog_gstamt_tnum').val($("#ed_gstamt_tnum").val());
	
	$( "#dialog-companytax" ).removeClass('hide').dialog({
		resizable: false,
		modal: true,
		title: "<div class='widget-header'><h4 class='smaller'> Tax Company Details</h4></div>",
		title_html: true,
		buttons: [
			{
				html: "<i class='ace-icon fa fa-floppy-o bigger-110'></i>&nbsp; Save",
				"class" : "btn btn-primary btn-mini",
				click: function() {
					$('#ed_gstamt_cn').val($("#dialog_gstamt_cn").val());
					$('#ed_gstamt_crnum').val($("#dialog_gstamt_crnum").val());
					$('#ed_gstamt_tnum').val($("#dialog_gstamt_tnum").val());
					$( this ).dialog( "close" );
				}
			},
			{
				html: "Cancel",
				"class" : "btn btn-mini",
				click: function() {
					$( this ).dialog( "close" );
				}
			}
		]
	});
}

// Calculate Approve Amount
function setApproveAmount(){
	let amount = parseFloat($("#ed_amt_amt").val()),
		exchangerate = parseFloat($("#ed_er").val()),
		aamount;
		
	if(isNaN(amount) || isNaN(exchangerate)){
		$("#aamt").html("0.0");
		$("#ed_aamt_amt").val("0.0");
	} else {
		aamount =  (amount * exchangerate).toFixed(2);
		$("#aamt").html(aamount);
		$("#ed_aamt_amt").val(aamount);
	};
}

//On delete file
var onDelete = function(p_id) {

    $.ajax({
        url: "/claimfile/delete?p_id=" + p_id,
        dataType: "json",
        cache: false,
        success: function(data, textStatus, jqXHR){
        	$("#" + p_id).remove();
        },
        error: function(jqXHR, textStatus, errorThrown){
        	alert("There was an error from server. Do not proceed! Please contact support@hrsifu.com.");
        }
    });
    
} 

let CATEGORY = (function(){
	
	// Private	
	let wfcategoryJSON = {},
		selectedCategory = ""
		transactionlimit = 0;
	
	// Public
	return {

		init: function(){
			$.ajax({
    			url: "/claimcategory/getdetails",
    			dataType: "json",
    			async: false,
    			success: function(data){
    				wfcategoryJSON = data;
    			},
    			error: function(xhr, status, error){
    				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
    			},
    		});	
		},
	
		change: function(){			
			selectedCategory = $("#ed_cat").val();
			for(let i=0, totalcategory=wfcategoryJSON["data"].length; i<totalcategory; i++){
				let categorydetail = wfcategoryJSON["data"][i];
				if (categorydetail.c==selectedCategory) {
					$("#d-editor").html(categorydetail.h);
					$("#ed_glc").val(categorydetail.glc);
					transactionlimit = categorydetail.l;
					break;
				}
			}
		},
		
		getTransactionLimit: function(){
			return transactionlimit;
		}
		
	}
	
})()