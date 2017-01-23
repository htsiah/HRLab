$(function(){

	$("#navAdmin").addClass("active open");
    $("#navKeyword").addClass("active");
        
	// Set the form horizontal layout 
	$("dl").addClass("dl-horizontal");
	
	//Bind remove keyword's value button
	$(document).on('click', '.removeValue',  function(e) {
		$(this).parents('.value').remove();
		renumber();
	})
	
	// Bind add keyword's value button
	$(document).on('click', '#addValue', function(e) {
		const template = $('.value_template');
		template.before('<div class="twipsies well value">' + template.html() + '</div>');
		renumber();
	})
	
	// remove the first Remove this value	
	$(".removeValue:first").remove();
	
});

// Renumber field number
var renumber = function() {
	$('.value').each(function(i) {
		$('input', this).each(function() {
			$(this).attr('name', $(this).attr('name').replace(/v\[.+?\]/g, 'v[' + i + ']'))
		})
	})
}