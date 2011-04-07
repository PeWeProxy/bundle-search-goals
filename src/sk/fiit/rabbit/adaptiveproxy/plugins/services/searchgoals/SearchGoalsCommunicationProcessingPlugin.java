package sk.fiit.rabbit.adaptiveproxy.plugins.services.searchgoals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

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
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;

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
		

		Map<String, String> postData = request.getServicesHandle().getService(PostDataParserService.class).getPostData();
		String content = "UNKNOWN_ACTION";
		Connection connection = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();

		if (request.getRequestHeader().getRequestURI().contains("action=addUID")) {
			content = assignUIDToSearch(connection, postData.get("id"), postData.get("uid"));
		}
		else if (request.getRequestHeader().getRequestURI().contains("action=setGoal")) {
			content = setSearchGoal(connection, postData.get("id"), postData.get("uid"), postData.get("goal"));
		}
		
		SqlUtils.close(connection);
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
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
