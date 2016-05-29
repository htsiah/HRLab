$(function(){
    
    $("#navProfile").addClass("active open");
    $("#navDashboard").addClass("active");
    
    $('.date-picker').datepicker({
		autoclose: true,
		todayHighlight: true
	})
		
	// Show datepicker when clicking on the icon
	.next().on(ace.click_event, function(){
		$(this).prev().focus();
	});
    
	// To date can not earlier than From date
	$("#tdat").datepicker("setStartDate", $("#fdat").val());
	
    // Disable empty date field after selecting current date
    // http://stackoverflow.com/questions/24981072/bootstrap-datepicker-empties-field-after-selecting-current-date
    $("#fdat, #tdat").on("show", function(e){
    	$(this).data("stickyDate", e.date);
    });

    $("#tdat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        };
        setApplyBtn(true);
    });
    
    $("#fdat").on("hide", function(e){
        var stickyDate = $(this).data("stickyDate");
        if ( !e.date && stickyDate ) {
        	$(this).datepicker("setDate", stickyDate);
            $(this).data("stickyDate", null);
        } else {
    		$("#tdat").datepicker("setStartDate", $(this).val());
    		$("#tdat").datepicker("update", $(this).val());
        };
        setApplyBtn(true);
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
    
    // Bind Applicant field 
    $(document).on('change', '#pid', function(e) {
    	var selperson = this.options[this.selectedIndex].value;
    	if (selperson == "") {
			$('#lt option').remove();
			$('#lt').append( new Option("Please select","") );
    		$("#dt").removeAttr("disabled");
    	} else {
    		$.ajax({
    			url: "/leaveprofile/getleaveprofile/" + selperson,
    			dataType: "json",
    			beforeSend: function(){
    				loader.on();
    			},
    			success: function(data){
    				$('#lt option').remove();
    				$('#lt').append( new Option("Please select","") );
    				$.each(data, function(key, val) {
    					$('#lt').append( new Option(val,key) );
    				});
    				$("#dt").removeAttr("disabled");
    				loader.off();
    			},
    			error: function(xhr, status, error){
    				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.")
    				loader.off();
    			}
    			
    		})
    	};
    	setApplyBtn(true);
    })

    // Bind leave type field 
    $("#lt").change(function() {
    	var selectedLT = $( "#lt option:selected" ).val();
    	if (selectedLT != "") {
    		$.ajax({
    			url: "/leavepolicy/getdaytype/" + selectedLT,
    			dataType: "json",
    			beforeSend: function(){
    				loader.on();
    			},
    			success: function(data){
    				if (data.daytype == "Full day only") {
    					$("#dt").attr("disabled", "disabled");
    					$("#dt").val("Full day");
    					$("#tdat").removeAttr("disabled");
    				} else {
    					$("#dt").removeAttr("disabled");
    				}
    				setApplyBtn(false);
    				loader.off();
    			},
    			error: function(xhr, status, error){
    				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
    				loader.off();
    			},
    		});	
    	} else {
    		setApplyBtn(true);
    	};
    });
    
	// Bind day type field 
	$(document).on('change', '#dt', function(e) {
		var seldatetype = this.options[this.selectedIndex].value;
		if (seldatetype=="1st half" || seldatetype=="2nd half") {
			$("#tdat").attr("disabled", "disabled");
			$("#tdat").datepicker("update", $("#fdat").val());
		} else {
			$("#tdat").removeAttr("disabled");
		}
		setApplyBtn(true);
	});
	
	// Binder on supporting document field
	$(document).on('change', '#file', function(e) {
		
		// Create a new FormData object for file upload.
		var formfile = new FormData();
		
		// Get upload file
		var file = e.target.files[0];
		
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
				if ($("#btnApply").text() != "Apply for 0 day" ) { $("#btnApply").attr("disabled", "disabled"); };
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
					if ($("#btnApply").text() != "Apply for 0 day" ) { $("#btnApply").removeAttr("disabled"); };
		        }; 
	        },
	        error: function(jqXHR, textStatus, errorThrown){
	        	$("#file-loader").addClass("hidden");
	        	$("#file-view").removeClass("file-input-control");
	        	if ($("#btnApply").text() != "Apply for 0 day" ) { $("#btnApply").removeAttr("disabled"); };
	        	alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
	        }
	    });	
		
	});
	
	// Show calendar
	Calendar.initCalendar();
	Calendar.showCompanyHoliday();
	Calendar.showMyLeave();
	Calendar.showOtherLeave();
	
});

var Calendar = {
	companyholidaysource:{
		url: '/companyholiday/getcompanyholidayjson/n',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching company holiday!');
		},
		className: 'label-success'
	},
			
	myapprovedleavessource:{
		url: '/leave/getapprovedleavejson/my/n',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
			
	otherapprovedleavessource:{
		url: '/leave/getapprovedleavejson/allexceptmy/n',
		type: 'GET',
		cache: false,
		error: function() {
			alert('There was an error while fetching your leave!');
		},
		color: 'blue',   // a non-ajax option
		textColor: 'white' // a non-ajax option
	},
			
	initCalendar:function(){
		var date = new Date();
		var d = date.getDate();
		var m = date.getMonth();
		var y = date.getFullYear();
		$('#calendar').fullCalendar({
		     eventRender: function(event, element) {
		    	 $(element).tooltip({title: event.tip});
		     }
		});	
	},

	removeEvents:function(p_source){
		$('#calendar').fullCalendar( 'removeEventSource', p_source )
	},

	showCompanyHoliday:function(){
		$('#calendar').fullCalendar('addEventSource',this.companyholidaysource);
	},
			
	showMyLeave:function(){
		$('#calendar').fullCalendar('addEventSource',this.myapprovedleavessource);
	},
		
	showOtherLeave:function(){
		$('#calendar').fullCalendar('addEventSource',this.otherapprovedleavessource);
	}
};

function setApplyBtn(p_loader) {
	
	var selPerson = $("#pid").val();
	var selLT = $("#lt").val();
	var selDT = $("#dt").val();
	var selFDat = $("#fdat").val();
	var selTDat = $("#tdat").val();
	
	if (selPerson=="" || selLT=="" || selDT=="" || selFDat=="" || selTDat=="") {
		$("#btnApply").text("Apply for 0 day");
		$("#btnApply").attr("disabled", "disabled");
	} else {
		
		$.ajax({
			url: "/leave/getapplyday/" + selPerson + "/" + selLT + "/" + selDT + "/" + selFDat + "/" + selTDat,
			dataType: "json",
			beforeSend: function(){
				if (p_loader) { loader.on() };
			},
			success: function(data){
				if (data.msg == "overlap") {
					$("#btnApply").html("Conflict with other leave application <br /> Please select to other date");
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
				}
				if (p_loader) { loader.off() };
			},
			error: function(xhr, status, error){
				alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
				if (p_loader) { loader.off() };
			}
		});	

	}
};

// Form submit function
var handleSubmit = function() {
	$("#dt").removeAttr("disabled");
 	$("#tdat").removeAttr("disabled");
	$('#leaveonbehalfform').submit();	
};

//On delete file
var onDelete = function(p_lk) {

    $.ajax({
        url: "/leavefile/deleteByLK?p_lk=" + $("#docnum").val(),
        dataType: "json",
        success: function(data, textStatus, jqXHR){
        	$('#file').ace_file_input('reset_input');
        	$("#file-view").addClass("hidden");
        	$("#file-input-control").removeClass("hidden");
        },
        error: function(jqXHR, textStatus, errorThrown){
        	alert("There was an error while fetching data from server. Do not proceed! Please contact support@hrsifu.my.");
        }
    });
    
};