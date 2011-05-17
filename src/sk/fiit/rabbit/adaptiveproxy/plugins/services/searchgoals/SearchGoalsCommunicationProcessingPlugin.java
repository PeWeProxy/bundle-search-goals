package sk.fiit.rabbit.adaptiveproxy.plugins.services.searchgoals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.svenson.JSON;

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
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.RequestDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate.ResultProcessor;

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
		
		if(request.getServicesHandle().isServiceAvailable(RequestDataParserService.class)) {
			Map<String, String> postData = request.getServicesHandle().getService(RequestDataParserService.class).getDataFromPOST();
		
			Connection connection = null;
			
			if(request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)) {
				try {
					connection = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
					JdbcTemplate jdbc = new JdbcTemplate(connection);
		
					if (request.getRequestHeader().getRequestURI().contains("action=getLastGoals")) {
						Map<String,String> getParams = getGETParameters(request.getRequestHeader().getRequestURI());
						content = getLastGoals(jdbc, getParams.get("uid"), getParams.get("count"));
					} else if (request.getRequestHeader().getRequestURI().contains("action=addUID")) {
						content = assignUIDToSearch(jdbc, postData.get("id"), postData.get("uid"));
					} else if (request.getRequestHeader().getRequestURI().contains("action=setGoal")) {
						content = setSearchGoal(jdbc, postData.get("id"), postData.get("uid"), postData.get("goal"));
					} else if (request.getRequestHeader().getRequestURI().contains("action=clickedResult")){
						content = addClickedResult(jdbc, postData.get("id"));
					}
				} finally {
					SqlUtils.close(connection);
				}
			}
		}
		
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

	private String getLastGoals(JdbcTemplate jdbc, String uid, String count) {
		
		int goalCount;
		
		try {
			goalCount = Integer.parseInt(count);
		} catch (NumberFormatException e) {
			return "FAIL";
		}
		
		
		List<String> recentGoals = jdbc.findAll(
				"SELECT DISTINCT goal " +
				"FROM searchgoals_searches " +
				"WHERE uid LIKE ? AND goal IS NOT NULL " +
				"ORDER BY `timestamp` DESC " +
				"LIMIT ?", 
				new Object[] {uid, goalCount }, 
				new ResultProcessor<String>() {
					@Override
					public String processRow(ResultSet rs) throws SQLException {
						return rs.getString("goal");
					}
				}
			);

		Map recentGoalsJson = new HashMap();
		recentGoalsJson.put("recentGoals", recentGoals);
		return JSON.defaultJSON().forValue(recentGoalsJson);
	}

	private String addClickedResult(JdbcTemplate jdbc, String resultID) {
		
		Integer id;
		
		try {
			id = Integer.parseInt(resultID);
		} catch (NumberFormatException e) {
			return "FAIL";
		}

		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		String formatedTimestamp = timestamp.substring(0, timestamp.indexOf("."));
		jdbc.insert("INSERT INTO `searchgoals_clicked_results` (`timestamp`, `id_search_result`) VALUES (?, ?)", 
				new Object[] { formatedTimestamp, id });
		return "OK";
	}

	private String setSearchGoal(JdbcTemplate jdbc, String searchID, String uid, String goal) {
		
		int id;
		
		try {
			id = Integer.parseInt(searchID);
		} catch (NumberFormatException e) {
			return "FAIL";
		}
		jdbc.update("UPDATE searchgoals_searches SET goal = ? WHERE id = ? AND uid = ? LIMIT 1",
				new Object[] { goal, id, uid } );
		return "OK";
	}

	private String assignUIDToSearch(JdbcTemplate jdbc, String searchID, String uid){
		
		int id;
		
		try {
			id = Integer.parseInt(searchID);
		} catch (NumberFormatException e) {
			return "FAIL";
		}
		
		jdbc.update("UPDATE searchgoals_searches SET uid = ? WHERE id = ? LIMIT 1",
				new Object[] { uid, id } );

		return "OK";
	}
	
	@Override
	public void processTransferedRequest(HttpRequest request) {
	}

}
