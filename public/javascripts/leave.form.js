$(function(){
    
    $("#navProfile").addClass("active open");
    $("#navDashboard").addClass("active");
    
    $('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
			
	//show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
    
	// To date can not earlier than From date
	$("#tdat").datepicker("setStartDate", $("#fdat").val());
	
	// Future request date setting
	if (!futurerequest) {
		$("#fdat").datepicker("setEndDate", cutoffdate);
		$("#tdat").datepicker("setEndDate", cutoffdate);
	};
	
    // Disable empty date field after selecting current date
    // http://stackoverflow.com/questions/24981072/bootstrap-datepicker-empties-field-after-selecting-current-date
    let stickyDate;
	$("#fdat, #tdat").on("show", function(e){
    	$(this).data("stickyDate", e.date);
    });
    
    $("#tdat").on("hide", function(e){
    	stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        }
        setApplyBtn();
    });
    
    $("#fdat").on("hide", function(e){
    	stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        } else {
    		$("#tdat").datepicker("setStartDate", $(this).val());
    		$("#tdat").datepicker("update", $(this).val());
        }
        setApplyBtn();
    });
    
    
    // Setup input file field
    $('#file').ace_file_input({
    	maxSize: 1000000, //1 MB
        allowExt:  ['jpg', 'jpeg', 'tif', 'tiff', 'gif', 'bmp', 'png', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'csv', 'txt']
    }).on('file.error.ace', function(event, info) {
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
        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Over 1 MB file size limit.</label>");
        	$("#file-error").removeClass("hidden");
    	};
    	
    	//if you do this
    	event.preventDefault();
    	//it will reset (empty) file input, i.e. no files selected
     }); 
        
    // Bind leave type field 
    $("#lt").change(function() {
    	let selectedLT = $( "#lt option:selected" ).val();
    	if (selectedLT != "") {
    		$.ajax({
    			url: "/leavepolicy/getdaytype/" + encodeURIComponent(selectedLT),
    			dataType: "json",
    			success: function(data){
    				if (data.daytype == "Full day only") {
    					$("#dt").attr("disabled", "disabled");
    					$("#dt").val("Full day");
    					$("#tdat").removeAttr("disabled");
    				} else {
    					$("#dt").removeAttr("disabled");
    				}
    				setApplyBtn();
    			},
    			error: function(xhr, status, error){
    				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
    			},
    		});	
    	} else {
    		setApplyBtn();
    	};
    });
    
	// Bind date type field 
	$(document).on('change', '#dt', function(e) {
		let seldatetype = this.options[this.selectedIndex].value;
		if (seldatetype=="1st half" || seldatetype=="2nd half") {
			$("#tdat").attr("disabled", "disabled");
			$("#tdat").val($("#fdat").val());
			$("#tdat").datepicker("update", $("#fdat").val());
		} else {
			$("#tdat").removeAttr("disabled");
		}
		setApplyBtn();
	});
	
	// Binder on supporting document field
	$(document).on('change', '#file', function(e) {
		
		let formfile = new FormData(), // Create a new FormData object for file upload.
			file = e.target.files[0]; // Get upload file
		
		// Add the file to the request.
		formfile.append("file", file, file.name);
		
		// Upload using AJAX
	    $.ajax({
	        url: "/leavefile/insert?p_lk=" + $("#docnum").val(),
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
				if (!($("#btnApply").text()=="Apply for 0 day" || $("#btnApply").text()=="Conflict with other leave application  Please select to other date")) { $("#btnApply").attr("disabled", "disabled"); };
			},
	        success: function(data, textStatus, jqXHR){
		        if (data.status == "exceed file size limit") {
		        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Over 1 MB file size limit.</label>");
		        	$("#file-loader").addClass("hidden");
		        	$("#file-input-control").removeClass("hidden");
		        	$("#file-error").removeClass("hidden");
		        } else {
					$("#file-loader").addClass("hidden");
					$("#file-view").html("<a href='/leavefile/viewByLK?p_lk=" + $("#docnum").val() + "' target='_blank'>" + file.name + "</a> &nbsp <a class='remove' href=javascript:onDelete('" + $("#docnum").val() + "') title='Delete'><i class='ace-icon fa fa-trash'></i></a>");
					$("#file-view").removeClass("hidden");
					if (!($("#btnApply").text()=="Apply for 0 day" || $("#btnApply").text()=="Conflict with other leave application  Please select to other date")) { $("#btnApply").removeAttr("disabled"); };
		        }; 
	        },
	        error: function(jqXHR, textStatus, errorThrown){
	        	$("#file-loader").addClass("hidden");
	        	$("#file-view").removeClass("file-input-control");
	        	$("#btnApply").removeAttr("disabled");
	        	if ($("#btnApply").text() != "Apply for 0 day" ) { $("#btnApply").removeAttr("disabled"); };
	        	alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
	        }
	    });	
		
	});
	
	// Show calendar	
	CALENDAR.initCalendar();
	CALENDAR.showCompanyHoliday(false);
	CALENDAR.showEvent(false);
	CALENDAR.showMyLeave(false);
	CALENDAR.showOtherLeave(false);
	
});

function setApplyBtn() {
	
	let selPerson = $("#pid").val(), 
	    selLT = $("#lt").val(), 
	    selDT = $("#dt").val(), 
	    selFDat = $("#fdat").val(), 
	    selTDat = $("#tdat").val();
		
	if (selPerson=="" || selLT=="" || selDT=="" || selFDat=="" || selTDat=="") {
		$("#btnApply").text("Apply for 0 day");
		$("#btnApply").attr("disabled", "disabled");
	} else {
		
		$.ajax({
			url: "/leave/getapplyday/" + selPerson + "/" + encodeURIComponent(selLT) + "/" + selDT + "/" + selFDat + "/" + selTDat,
			dataType: "json",
			success: function(data){
				if (data.msg == "overlap") {
					$("#btnApply").html("Conflict with other leave application <br /> Please select to other date");
					$("#btnApply").attr("disabled", "disabled");
				} else if (data.msg == "restricted on event") {
					$("#btnApply").html("Restricted leave application on event day <br /> Please select to other date");
					$("#btnApply").attr("disabled", "disabled");
				} else if (data.a <= 0) {
					$("#btnApply").text("Apply for 0 day");
					$("#btnApply").attr("disabled", "disabled");
				} else if (data.b < 0) {
					$("#btnApply").html("Apply for " + data.a + " day(s) <br /> No enough leave balance");
					$("#btnApply").attr("disabled", "disabled");
				} else {
					$("#btnApply").html("Apply for " + data.a + " day(s) <br />" + data.b + " day(s) remaining balance");
					$("#btnApply").removeAttr("disabled");
				};
			},
			error: function(xhr, status, error){
				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
			}
		});	

	}
};

// Form submit function
var handleSubmit = function() {
	
	// Check on mandatory upload supporting document
	$.ajax({
		url: "/leavepolicy/getsupportingdocument/" + encodeURIComponent($( "#lt option:selected" ).val()),
		dataType: "json",
		success: function(data){
			if (data.supportingdocument == false) {
				$("#dt").removeAttr("disabled");
			 	$("#tdat").removeAttr("disabled");
				$('#leaveform').submit();
			} else {
				if ($("#file").val() == "" && $("#file-view").hasClass("hidden")) {
		        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Please upload a supporting document.</label>");
		        	$("#file-loader").addClass("hidden");
		        	$("#file-input-control").removeClass("hidden");
		        	$("#file-error").removeClass("hidden");
				} else {
					$("#dt").removeAttr("disabled");
				 	$("#tdat").removeAttr("disabled");
					$('#leaveform').submit();
				}
			};
		},
		error: function(xhr, status, error){
			alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
		},
	});	
	
};

// On delete file
var onDelete = function(p_lk) {

    $.ajax({
        url: "/leavefile/deleteByLK?p_lk=" + $("#docnum").val(),
        dataType: "json",
        cache: false,
        success: function(data, textStatus, jqXHR){
        	$('#file').ace_file_input('reset_input');
        	$("#file-view").addClass("hidden");
        	$("#file-input-control").removeClass("hidden");
        },
        error: function(jqXHR, textStatus, errorThrown){
        	alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.com.");
        }
    });
    
} 