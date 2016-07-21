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
			"ct": "required"
		},
		messages: {
			"n": {
				required: "Please enter office name.",
				remote: "Office name already been used."
			},
			"ct": "Please select country."
		},		 
		submitHandler: function(form) {
		   form.submit();
		 }
	});
	
});