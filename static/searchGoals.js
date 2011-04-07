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
		}).blur(function(){
			if ($(this).val() == ""){
				$(this).addClass("__ap_search_goals_defaulttext_gray");
				$(this).val($(this)[0].title);
			}
		}).val("").blur().keydown(function(key){
			if (key.keyCode == 13){
				__ap_submit_goal();
			}
		});
		$("#__ap_search_goals_switch").toggle(function(){
			$("#__ap_search_goals").animate({right: "-322px"}, "fast", function(){
				$("#__ap_search_goals_switch").removeClass('opened').addClass('closed');
				$(this).addClass('closed');
			})
			$(this).blur();
			return false;
		}, function(){
			$("#__ap_search_goals").removeClass('closed').animate({right: 0}, "fast", function(){
				$("#__ap_search_goals_switch").removeClass('closed').addClass('opened');
			});
			$(this).blur();
			return false;
		});

		__ap_register_callback(function(){
			var f = function($){
				$.getJSON("./adaptive-proxy/search-goals.html", {action : "getLastGoals", uid : __peweproxy_uid, count : 4}, function(data){
					var recentGoal;
					for (index in data.recentGoals){
						recentGoal = $.trim(data.recentGoals[index]);
						if (recentGoal == null || recentGoal == "null"){
							continue;
						}
						$("#__ap_search_goals_recent_goals").append('<li><a href="#" onclick="__ap_search_goal_insert(adaptiveProxyJQuery(this).html()); return false">'+recentGoal+'</a></li>');
					}
				})
			}(adaptiveProxyJQuery);
		});

		vertical_position.call();
		$(window).resize(vertical_position);
		$("#__ap_search_goals").show();
	});
}(adaptiveProxyJQuery);

function __ap_search_goal_insert(goal){
	adaptiveProxyJQuery("#__ap_search_goals_input_container input").removeClass("__ap_search_goals_defaulttext_gray").val(goal);
}

function __ap_submit_goal(){
	var t = function($){
		var input = $("#__ap_search_goals_input_container input");
		var search_goal = input.val();
		if (input.attr("title") != search_goal && search_goal != ""){
			__ap_register_callback(function(){
				$.post("./adaptive-proxy/search-goals.html?action=setGoal", {"uid" : __peweproxy_uid, "id": __ap_search_id, "goal": search_goal}, function(response){
					if ($.trim(response) == "OK"){
						$("#__ap_search_goals_thank_you span.__ap_search_goals_s1").html(search_goal);
						$("#__ap_search_goals_input_container, #__ap_search_goals_info, #__ap_search_goals_recent_goals").fadeOut("fast", function(){
							$("#__ap_search_goals_thank_you").fadeIn("fast");
						});
					}
				});
			});
		}
	}(adaptiveProxyJQuery);
}

function __ap_clicked_result(id){
	var t = function($, id){
		$.ajax({
			type: "POST",
			async: false,
			url: "./adaptive-proxy/search-goals.html?action=clickedResult",
			data: {"id" : id}
		});
		//$.post( "./adaptive-proxy/search-goals.html?action=clickedResult" , {"id" : id});
	}(adaptiveProxyJQuery, id)
}