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
    
    // Loan Category 
    CATEGORY.init();
    
	$( "#ed_cat" ).change( "click", function() {
		CATEGORY.change();
	});
    
    // form validation
	$("#claimform").validate({
		onkeyup: false,
		rules: {
			"ed.cat": "required",
			"ed.amt.amt": {
				required: true,
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
				required: "Please enter claim amount."
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

let CATEGORY = (function(){
	
	// Private	
	let wfcategoryJSON = {},
		selectedCategory = "";
	
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
					break;
				}
			}
		}
		
	}
	
})()