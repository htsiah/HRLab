$(function(){
	
	$("#navAdmin").addClass("active open");
    $("#navOrgChartSetting").addClass("active");
    	
	// Mouse over help
	$('[data-rel=popover]').popover({container:'body'});
	
    // Disable top level manager if top level is "Automatic - employee who report to himself will be at the top-level".
	if ($("#top-level-field").val() == "Automatic - employee who report to himself will be at the top-level") {
		$("#top-level-manager-value").text("N/A");
		$('#top-level-manager-field').prop('disabled', true).trigger("chosen:updated");
	};
	
	// Top Level
	$("#top-level-pencil").click(function(){
		$("#top-level-view").hide();
		$("#top-level-manager-view").hide();
		$("#top-level-edit").show();
		$("#top-level-manager-edit").show();
	});

	$("#top-level-times").click(function(){
		$("#top-level-edit").hide();
		$("#top-level-manager-edit").hide();
		resetFields();
		$("#top-level-view").show();
		$("#top-level-manager-view").show();
	});
	
	$("#top-level-check").click(function(){
		if ($("#top-level-field").val()=="Manual - define top-level manually" && !($("#top-level-manager-field").val())) {
			if ($('#top_level_manager_field-error').length == 0) {
				$("#top_level_manager_field_chosen").after( "<label id='top_level_manager_field-error' class='error' for='p_fn'>Please select top level manager(s).</label>" );
			};
		} else {
			let GetURL;
			if ($("#top-level-field").val() == "Automatic - employee who report to himself will be at the top-level") {
				GetURL = "/orgchartsetting/updateorgchartsetting/" + $("#top-level-field").val()
			} else {
				let TopLevelManagerParameter = "";
			    $("#top-level-manager-field option:selected").each(function () {
			    	TopLevelManagerParameter += '&p_toplevelmanager=' + $(this).attr('value');   
			    });
			    GetURL = "/orgchartsetting/updateorgchartsetting/" + $("#top-level-field").val() + "?" + TopLevelManagerParameter
			};
			
			$.ajax({
				url: GetURL,
				type: 'GET',
				dataType: "json",
				success: function(result){
					$("#top-level-edit").hide();
					$("#top-level-manager-edit").hide();
					updateFields();
					$("#top-level-view").show();
					$("#top-level-manager-view").show();
	            },
	            error: function(xhr, textStatus, errorThrown) {
	            	alert("Failed update org chart setting!!!");
				}
			});
		};
		
	});
	
	// Vertical Depth
	$("#vertical-depth-pencil").click(function(){
		$("#vertical-depth-view").hide();
		$("#vertical-depth-edit").show();
	});

	$("#vertical-depth-times").click(function(){
		$("#vertical-depth-edit").hide();
		$("#vertical-depth-field").val(verticaldepthvalue);
		$("#vertical-depth-view").show();
	});
	
	$("#vertical-depth-check").click(function(){
		verticaldepthvalue = $("#vertical-depth-field").val();
		$.ajax({
			url: "/orgchartsetting/updateverticaldepth/" + verticaldepthvalue,
			type: 'GET',
			dataType: "json",
			success: function(result){
				$("#vertical-depth-edit").hide();
				$('#vertical-depth-value').text(verticaldepthvalue);
				$("#vertical-depth-view").show();
            },
            error: function(xhr, textStatus, errorThrown) {
            	alert("Failed update org chart setting!!!");
			}
		});
	});
	
    // Bind Top Level field 
    $(document).on('change', '#top-level-field', function(e) {
    	if (this.options[this.selectedIndex].value == "Automatic - employee who report to himself will be at the top-level") { 
    		$("#top_level_manager_field-error").remove();
    		$('#top-level-manager-field').val("").trigger("chosen:updated");
    		$('#top-level-manager-field').prop('disabled', true).trigger("chosen:updated");
    	} else {
    		$('#top-level-manager-field').prop('disabled', false).trigger("chosen:updated");
    	};
    });
    
    // Choosen field
    $(".chosen-select").chosen({
    	placeholder_text_multiple: "Please select",
    	width: "100%",
    	display_selected_options: false
    });
    
    $('ul[class="chosen-choices"]').addClass( "form-control" );
    
});

let toplevelvalue = $("#top-level-field").val();
let toplevelmanagervalue = $("#top-level-manager-field").val() || [];
let verticaldepthvalue = $("#vertical-depth-field").val();

function resetFields() {
	$("#top_level_manager_field-error").remove();
	if (toplevelvalue == "Manual - define top-level manually") {
		$('#top-level-manager-field').prop('disabled', false).trigger("chosen:updated");
	};
	$("#top-level-field").val(toplevelvalue);
	$("#top-level-manager-field").val(toplevelmanagervalue).trigger("chosen:updated");
}

function updateFields(){
	$("#top_level_manager_field-error").remove();
	toplevelvalue = $("#top-level-field").val();
	toplevelmanagervalue = $("#top-level-manager-field").val();
	let toplevelmanagertext = $("#top-level-manager-field option:selected").map( function() {	return $(this).text(); }).get() || [];
	$('#top-level-value').text(toplevelvalue);
	if ($.isEmptyObject(toplevelmanagertext)) {
		$('#top-level-manager-value').text("N/A");
	} else {
		$('#top-level-manager-value').text(toplevelmanagertext.join(", "));
	};
}