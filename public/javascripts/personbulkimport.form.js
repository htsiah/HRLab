$(function(){

	$("#navCompany").addClass("active open");
	$("#navStaff").addClass("active");
	
    // Setup input file field
    $('#file').ace_file_input({
    	maxSize: 1000000, //1 MB
        allowExt:  ['csv']
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
        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Invalid file extension. Allowed file extensions is CSV.</label>");
        	$("#file-error").removeClass("hidden");
    	} else if (info.error_count['size'] > 0) {
        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Over 1 MB file size limit.</label>");
        	$("#file-error").removeClass("hidden");
    	};
    	
    	//if you do this
    	event.preventDefault();
    	//it will reset (empty) file input, i.e. no files selected
     }); 
    
	// Binder on supporting document field
	$(document).on('change', '#file', function(e) {
		
		let formfile = new FormData(), // Create a new FormData object for file upload.
			file = e.target.files[0]; // Get upload file
		
		// Add the file to the request.
		formfile.append("file", file, file.name);
		
		// Upload using AJAX
	    $.ajax({
	        url: "/personbulkimport/insert",
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
		        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Over 1 MB file size limit.</label>");
		        	$("#file-loader").addClass("hidden");
		        	$("#file-input-control").removeClass("hidden");
		        	$("#file-error").removeClass("hidden");
		        } else if (data.status == "error import employee") {
		        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>There was an error while importing employee from server. Do not proceed! Please contact support@hrsifu.com.</label>");
		        	$("#file-loader").addClass("hidden");
		        	$("#file-input-control").removeClass("hidden");
		        	$("#file-error").removeClass("hidden");
		        } else if (data.status == "exceed 50 employee") {
		        	$("#file-error").html("<label id='p_file-error' class='error red' for='p_file'>Over 50 employee import limit. Please split into multiple files with header rows and import separately.</label>");
		        	$("#file-loader").addClass("hidden");
		        	$("#file-input-control").removeClass("hidden");
		        	$("#file-error").removeClass("hidden");
		        } else {
					$("#file-loader").addClass("hidden");
					$("#file-view").html("<label id='p_file-success' class='succss green' for='p_file'>Employee Bulk Import Complete</label>");
					$("#file-view").removeClass("hidden");
					$("#import-reason").html("<h4 class='header smaller lighter blue'>Step 4: Import Result</h4><p>Your employee import is complete.</p>" + data.result);
					$("#import-reason").removeClass("hidden");
		        }; 
	        },
	        error: function(jqXHR, textStatus, errorThrown){
	        	$("#file-loader").addClass("hidden");
	        	$("#file-view").removeClass("file-input-control");
	        	alert("There was an error while importing employee from server. Do not proceed! Please contact support@hrsifu.com.");
	        }
	    });	
		
	});
	
});