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
			{name:'createLink', className:'btn-pink'},
			{name:'unlink', className:'btn-pink'},
    		null,
    		'foreColor',
    		null,
    		{name:'viewSource', className:'btn-grey'}
    	]
    });
    
});