$(function(){

	$("#navAdmin").addClass("active open");
    $("#navClaimSetting").addClass("active");
        	
    // Choosen field
    $(".chosen-select").chosen({
    	placeholder_text_multiple: "Please select",
    	width: "100%",
    	display_selected_options: false
    });
    
    $('ul[class="chosen-choices"]').addClass( "form-control" );
    
    $('.chosen-select-single').chosen({
    	width: "100%",
    	allow_single_deselect:true
    });
    
    // Number field
	$('.spinner-input').ace_spinner({
		value:0,
		min:0,
		max:99999,
		step:10,
		on_sides: true, 
		icon_up:'ace-icon fa fa-plus smaller-75', 
		icon_down:'ace-icon fa fa-minus smaller-75', 
		btn_up_class:'btn-success' , 
		btn_down_class:'btn-danger'
	});
    
    // Disable applicable if default
    if($("#d").prop('checked') == true){
    	$("#app").attr('disabled', true).trigger("chosen:updated");
    	$("#d").attr("disabled", "disabled")
    };
    
    // JQuery validation ignore hidden element, and since chosen pplugin add visibility: hidden, add this to remove the ignore
    $.validator.setDefaults({ ignore: ":hidden:not(.chosen-select-single)" });
    
    // form validation
	$("#claimworkflowform").validate({
		onkeyup: false,
		rules: {
			"n": {
				required: true,
				remote: {
					url: "/claimworkflow/checkclaimworkname",
			        type: "get",
			        cache: false,
			        data: {
			        	p_id: function() {
			        		return oid;
			        	},
			        	p_name: function() {
			        		return $( "#n" ).val();
			        	}
			        }
				}
			},
			"s.s1": "required",
			"at.at1": { validateAssignTo: ["1"] },
			"at.at2": { validateAssignTo: ["2"] },
			"at.at3": { validateAssignTo: ["3"] },
			"at.at4": { validateAssignTo: ["4"] },
			"at.at5": { validateAssignTo: ["5"] },
			"at.at6": { validateAssignTo: ["6"] },
			"at.at7": { validateAssignTo: ["7"] },
			"at.at8": { validateAssignTo: ["8"] },
			"at.at9": { validateAssignTo: ["9"] },
			"at.at10": { validateAssignTo: ["10"] },
			"cg.cg1": { digits: true },
			"cg.cg2": { digits: true },
			"cg.cg3": { digits: true },
			"cg.cg4": { digits: true },
			"cg.cg5": { digits: true },
			"cg.cg6": { digits: true },
			"cg.cg7": { digits: true },
			"cg.cg8": { digits: true },
			"cg.cg9": { digits: true },
			"cg.cg10": { digits: true }
		},
		messages: {
			"n": {
				required: "Please enter workflow name.",
				remote: "Workflow name name already been used."
			},
		},	
		errorPlacement: function(error, element) {
			if(element.attr("name") == "at.at1") {
				error.appendTo( $("#at_at1_chosen") );
			} else if(element.attr("name") == "at.at2") {
				error.appendTo( $("#at_at2_chosen") );
			} else if(element.attr("name") == "at.at3") {
				error.appendTo( $("#at_at3_chosen") );
			} else if(element.attr("name") == "at.at4") {
				error.appendTo( $("#at_at4_chosen") );
			} else if(element.attr("name") == "at.at5") {
				error.appendTo( $("#at_at5_chosen") );
			} else if(element.attr("name") == "at.at6") {
				error.appendTo( $("#at_at6_chosen") );
			} else if(element.attr("name") == "at.at7") {
				error.appendTo( $("#at_at7_chosen") );
			} else if(element.attr("name") == "at.at8") {
				error.appendTo( $("#at_at8_chosen") );
			} else if(element.attr("name") == "at.at9") {
				error.appendTo( $("#at_at9_chosen") );
			} else if(element.attr("name") == "at.at10") {
				error.appendTo( $("#at_at10_chosen") );
			} else {
				error.insertAfter(element);
			}
		},
		submitHandler: function(form) {
			
			// Validate status between steps
			let sprev, scurr;
			let flagBetweenSteps = true;
			
			for( let x=2; x<=10; x++) {
				sprev = $("#s_s" + (x-1)).val();
				scurr = $("#s_s" + x).val();
				
				if(scurr != "" && sprev === "") {
					flagBetweenSteps = false;
					break;
				}
			};
						
			if(flagBetweenSteps){
				$("#d").removeAttr("disabled");
				form.submit();
			} else {
				alert("You cannot leave a blank status in between workflow steps.");
			}
		 }
	});

	$.validator.addMethod(
		"validateAssignTo",
		function(value,element,params){
			let status = $("#s_s" + params[0]).val();
			if(status=="") {
				return true;
			} else {
				if (value=="") {
					return false;
				} else {
					return true;
				};
			} ; 
		},
		"This field is required."
	);
    
});

$("#d").click(function(){
	if(this.checked){
		// Disable applicable field
		$("#app").val("").trigger('chosen:updated');
		$("#app").attr('disabled', true).trigger("chosen:updated");
	} else {
		$("#app").attr('disabled', false).trigger("chosen:updated");
	}
});