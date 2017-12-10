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
    
    // Disable applicable if all
    if($("#all").prop('checked') == true){
    	$("#app").attr('disabled', true).trigger("chosen:updated");
    };
    
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
    
    // Append help html
    $("#hlp-editor").html($("#hlp").val());
    
	// Mouse over help
	$('[data-rel=popover]').popover({container:'body'});
	
    // form validation
	$("#claimcategoryform").validate({
		onkeyup: false,
		rules: {
			"cat": {
				required: true,
				remote: {
					url: "/claimcategory/iscatnotunique",
			        type: "get",
			        cache: false,
			        data: {
			        	p_id: function() {
			        		return oid;
			        	},
			        	p_cat: function() {
			        		return $( "#cat" ).val();
			        	}
			        }
				}
			},
			"tlim": {
				required: true,
				digits: true
			}
		},
		messages: {
			"cat": {
				required: "Please enter category.",
				remote: "Category name already been used."
			},
			"tlim": {
				required: "Please enter transaction limit."
			},
		},		 
		submitHandler: function(form) {
			$("#hlp").val($("#hlp-editor").html());
			form.submit();
		 }
	});
	
});

$("#all").click(function(){
	if(this.checked){
		// Disable applicable field
		$("#app").val("").trigger('chosen:updated');
		$("#app").attr('disabled', true).trigger("chosen:updated");
	} else {
		$("#app").attr('disabled', false).trigger("chosen:updated");
	}
});