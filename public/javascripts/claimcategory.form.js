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
    
    // wysiwyg
    
    $('#hlp-editor').ace_wysiwyg({
    	toolbar: [
    		'bold', 
    		'italic', 
    		'strikethrough', 
    		'underline', 
    		null,
    		'insertunorderedlist',
    		'insertorderedlist',
    		'outdent',
    		'indent',
    		null,
    		'justifyleft',
    		'justifycenter',
    		'justifyright',
    		'justifyfull',
    		null,
    		'createLink',
    		'unlink',
    		null,
    		'foreColor',
    		null,
    		'viewSource'
    	]
    });
    
});