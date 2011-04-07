__ap_search_goals = function($){

	var vertical_position = function(){
		$("#__ap_search_goals").css('top',$(window).height()/2 - $("#__ap_search_goals").height()/2 - 30)
	}

	$(document).ready(function(){
		$("#__ap_search_goals_input_container input").focus(function(){
			if ($(this).val() == $(this)[0].title){
				$(this).removeClass("__ap_search_goals_defaulttext_gray");
				$(this).val("");
			}
		});

		$("#__ap_search_goals_input_container input").blur(function(){
			if ($(this).val() == ""){
				$(this).addClass("__ap_search_goals_defaulttext_gray");
				$(this).val($(this)[0].title);
			}
		});
		$("#__ap_search_goals_input_container input").val("");
		$("#__ap_search_goals_input_container input").blur();
		$("#__ap_search_goals_switch").toggle(function(){
			$("#__ap_search_goals").animate({right: "-322px"}, "fast", function(){
				$("#__ap_search_goals_switch").removeClass('opened').addClass('closed');
				$(this).addClass('closed');
			})
		}, function(){
			$("#__ap_search_goals").removeClass('closed').animate({right: 0}, "fast", function(){
				$("#__ap_search_goals_switch").removeClass('closed').addClass('opened');
			})
		});
		vertical_position.call();
		$(window).resize(vertical_position);
		$("#__ap_search_goals").show();
	});
}(adaptiveProxyJQuery);

function __ap_submit_goal(){
	var t = function($){
		var input = $("#__ap_search_goals_input_container input");
		if (input.attr("title") != input.val() && input.val() != ""){
			__ap_register_callback(function(){
				$.post("./adaptive-proxy/search-goals.html?action=setGoal", {"uid" : __peweproxy_uid, "id": __ap_search_id, "goal": input.val()}, function(response){
					alert(response);
				});
			});
		}
	}(adaptiveProxyJQuery);
}

function __ap_clicked_result(id){
	var t = function($, id){
		$.post( "./adaptive-proxy/search-goals.html?action=clickedResult" , {"id" : id});
	}(adaptiveProxyJQuery, id)
}