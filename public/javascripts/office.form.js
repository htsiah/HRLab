$(function(){

	$("#navCompany").addClass("active open");
    $("#navComProfile").addClass("active");
        
	$("#officeform").validate({
		onkeyup: false,
		rules: {
			"n": {
				required: true,
				remote: {
					url: "/office/checkoffice",
			        type: "get",
			        cache: false,
			        data: {
			        	p_id: function() {
			        		return oid;
			        	},
			        	p_officename: function() {
			        		return $( "#n" ).val();
			        	}
			        }
				}
			},
			"st": "required"
		},
		messages: {
			"n": {
				required: "Please enter office name.",
				remote: "Office name already been used."
			},
			"st": "Please select state."
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});