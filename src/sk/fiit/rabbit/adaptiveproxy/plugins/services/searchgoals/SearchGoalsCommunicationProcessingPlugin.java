package sk.fiit.rabbit.adaptiveproxy.plugins.services.searchgoals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpRequest;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.RequestProcessingPlugin;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PostDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.SqlUtils;

public class SearchGoalsCommunicationProcessingPlugin  implements RequestProcessingPlugin, ResponseProcessingPlugin {
	
	protected Logger logger = Logger.getLogger(SearchGoalsCommunicationProcessingPlugin.class);

	private String bypassPattern;
	
	@Override
	public void desiredRequestServices(
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return false;
	}

	@Override
	public boolean start(PluginProperties props) {
		bypassPattern = props.getProperty("bypassPattern");
		return true;
	}

	@Override
	public void stop() {
	}

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
	}

	@Override
	public ResponseProcessingActions processResponse(
			ModifiableHttpResponse response) {
		return null;
	}

	@Override
	public HttpResponse getNewResponse(ModifiableHttpResponse response,
			HttpMessageFactory messageFactory) {
		return null;
	}

	@Override
	public void processTransferedResponse(HttpResponse response) {
	}

	@Override
	public RequestProcessingActions processRequest(ModifiableHttpRequest request) {
		if(request.getOriginalRequest().getRequestHeader().getRequestURI().contains(bypassPattern)) {
			return RequestProcessingActions.FINAL_RESPONSE;
		}
		
		return RequestProcessingActions.PROCEED;
	}

	@Override
	public HttpRequest getNewRequest(ModifiableHttpRequest request,
			HttpMessageFactory messageFactory) {
		return null;
	}

	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request,
			HttpMessageFactory messageFactory) {
		

		String content = "UNKNOWN_ACTION";
		Connection connection = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();

		if (request.getRequestHeader().getRequestURI().contains("action=getLastGoals")){
			Map<String,String> getParams = getGETParameters(request.getRequestHeader().getRequestURI());
			content = getLastGoals(connection, getParams.get("uid"), getParams.get("count"));
		} else {
			Map<String, String> postData = request.getServicesHandle().getService(PostDataParserService.class).getPostData();
			if (request.getRequestHeader().getRequestURI().contains("action=addUID")) {
				content = assignUIDToSearch(connection, postData.get("id"), postData.get("uid"));
			}
			else if (request.getRequestHeader().getRequestURI().contains("action=setGoal")) {
				content = setSearchGoal(connection, postData.get("id"), postData.get("uid"), postData.get("goal"));
			}
			else if (request.getRequestHeader().getRequestURI().contains("action=clickedResult")){
				content = addClickedResult(connection, postData.get("id"));
			}
		}
		
		SqlUtils.close(connection);
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
	}
	
	private Map<String,String> getGETParameters(String request){
		Map<String,String> map = new HashMap<String,String>();
		String attributeName;
		String attributeValue;
	    
		if (!request.contains("?")){
			request = request.split("?")[1];
		}
		
		for (String pair : request.split("&")) {
			if (pair.split("=").length == 2) {
				attributeName = pair.split("=")[0];
				attributeValue = pair.split("=")[1];
				map.put(attributeName, attributeValue);
			}
		}
		return map;
	}

	private String getLastGoals(Connection connection, String uid, String count) {
		
		int goalCount;
		
		try {
			goalCount = Integer.parseInt(count);
		} catch (NumberFormatException e) {
			return "FAIL";
		}
		
		JSONObject recentGoalsJson = new JSONObject();
		List<String> recentGoals = new ArrayList<String>();
		
		PreparedStatement stmt;
		try {
			stmt = connection.prepareStatement("SELECT DISTINCT `goal` FROM `searchgoals_searches` WHERE `uid` LIKE ? AND `goal` IS NOT NULL ORDER BY `timestamp` DESC LIMIT ?;");
			stmt.setString(1, uid);
			stmt.setInt(2, goalCount);
			stmt.execute();
			
			ResultSet rs = stmt.getResultSet();
			while (rs.next()){
				recentGoals.add(rs.getString(1));
			}
			recentGoalsJson.put("recentGoals", recentGoals);
			
		} catch (SQLException e) {
			logger.error("Error selecting last "+goalCount+" goals for user UID "+uid);
		}

		return recentGoalsJson.toJSONString();
	}

	private String addClickedResult(Connection connection, String resultID) {
		
		int id;
		
		try {
			id = Integer.parseInt(resultID);
		} catch (NumberFormatException e) {
			return "FAIL";
		}

		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		String formatedTimestamp = timestamp.substring(0, timestamp.indexOf("."));
		
		PreparedStatement stmt;
		try {
			stmt = connection.prepareStatement("INSERT INTO `searchgoals_clicked_results` (`timestamp`, `id_search_result`) VALUES (?, ?);");
			stmt.setString(1, formatedTimestamp);
			stmt.setInt(2, id);
			
			stmt.execute();
		} catch (SQLException e){
			logger.error("Error inserting click for search result id "+id);
			return "FAIL";
		}
		return "OK";
	}

	private String setSearchGoal(Connection connection, String searchID, String uid, String goal) {
		
		int id;
		
		try {
			id = Integer.parseInt(searchID);
		} catch (NumberFormatException e) {
			return "FAIL";
		}
		
		PreparedStatement stmt;
		try {
			stmt = connection.prepareStatement("UPDATE `searchgoals_searches` SET `goal` = ? WHERE `id` = ? AND `uid` = ? LIMIT 1;");
			stmt.setString(1, goal);
			stmt.setInt(2, id);
			stmt.setString(3, uid);
			
			stmt.execute();
		} catch (SQLException e){
			logger.error("Error setting goal for search id "+id);
			return "FAIL";
		}
		return "OK";
	}

	private String assignUIDToSearch(Connection connection, String searchID, String uid){
		
		int id;
		
		try {
			id = Integer.parseInt(searchID);
		} catch (NumberFormatException e) {
			return "FAIL";
		}
		
		PreparedStatement stmt;
		try {
			stmt = connection.prepareStatement("UPDATE `searchgoals_searches` SET `uid` = ? WHERE `id` = ? LIMIT 1;");
			stmt.setString(1, uid);
			stmt.setInt(2, id);
			
			stmt.execute();
		} catch (SQLException e){
			logger.error("Error updating uid for search id "+id);
			return "FAIL";
		}
		return "OK";
	}
	
	@Override
	public void processTransferedRequest(HttpRequest request) {
	}

}
