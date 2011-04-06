package sk.fiit.rabbit.adaptiveproxy.plugins.services.searchgoals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Set;

import org.apache.log4j.Logger;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.plugins.processing.ResponseProcessingPlugin;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlDomReaderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlDomWriterService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ModifiableSearchResultService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.SearchResultObject;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;

public class ClickTrackingInjectorProcessingPlugin implements ResponseProcessingPlugin {

	protected Logger logger = Logger.getLogger(ClickTrackingInjectorProcessingPlugin.class);
	
	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		desiredServices.add(ModifiableSearchResultService.class);
		desiredServices.add(HtmlDomReaderService.class);
		desiredServices.add(HtmlDomWriterService.class);
		
	}

	@Override
	public boolean supportsReconfigure(PluginProperties newProps) {
		return false;
	}

	@Override
	public boolean start(PluginProperties props) {
		return true;
	}

	@Override
	public void stop() {
		
	}

	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		
		if(response.getServicesHandle().isServiceAvailable(ModifiableSearchResultService.class)){
			
			int searchResultID = injectOnclickToSearchResults(response);
			
			injectUidSenderScript(response, searchResultID);
			
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	private int injectOnclickToSearchResults(ModifiableHttpResponse response){
		
		int searchResultID = -1;
		
		ModifiableSearchResultService modifiableSearchResultService = response.getServicesHandle().getService(ModifiableSearchResultService.class);
		
		ArrayList<SearchResultObject> searchResultObjectList = modifiableSearchResultService.getSearchedData();
		int resultCount = searchResultObjectList.size();
		
		
		if (resultCount > 0) {
			Connection connection = response.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();

			String queryString = modifiableSearchResultService.getQueryString().trim();
			
			searchResultID = insertSearchToDB(connection, queryString);
			
			for (SearchResultObject searchResultObject : searchResultObjectList){
				modifiableSearchResultService.deleteResult(1);
				modifiableSearchResultService.putResult(new SearchResultObject(searchResultObject, "alert('TOTO: log what was clicked')"), resultCount);
				
				insertSearchResultToDB(connection, searchResultObject, searchResultID);
				
			}
			
			SqlUtils.close(connection);
		}
		
		return searchResultID;
	}
	
	private void injectUidSenderScript(ModifiableHttpResponse response, int searchResultID){
		if (response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)){
			HtmlInjectorService htmlInjectionService = response.getServicesHandle().getService(HtmlInjectorService.class);
			
			String script = "<script type=\"text/javascript\">\n" +
					"var __ap_search_id = " + searchResultID + ";\n" +
					"__ap_register_callback(function() {\n" +
					"	adaptiveProxyJQuery.post(\"./adaptive-proxy/search-goals.html?action=addUID\", { \"uid\": __peweproxy_uid, \"id\": __ap_search_id});\n" +
					"});\n" +
					"</script>\n"; 
			
			htmlInjectionService.inject(script, HtmlPosition.END_OF_BODY);
		}
	}

	private void insertSearchResultToDB(Connection connection, SearchResultObject searchResultObject, int searchResultID){
		try {
			PreparedStatement stmt = connection.prepareStatement("INSERT INTO `searchgoals_search_results` (`url`, `heading`, `perex`, `id_search`) VALUES (?, ?, ?, ?);");
			
			stmt.setString(1, searchResultObject.getUrl());
			stmt.setString(2, searchResultObject.getHeader());
			stmt.setString(3, searchResultObject.getPerex());
			stmt.setInt(4, searchResultID);
			
			stmt.execute();
		} catch (SQLException e) {
			logger.error("Error inserting search result for search id "+searchResultID+" to database");
		}
	}
	
	private int insertSearchToDB(Connection connection, String queryString){
		int searchResultID = -1;
		
		try {
			PreparedStatement stmt = connection.prepareStatement("INSERT INTO `searchgoals_searches` (`timestamp`, `search_query`) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, getFormatedTimestamp());
			stmt.setString(2, queryString);
			stmt.execute();
			
			ResultSet set = stmt.getGeneratedKeys();
			
			if (set.next()){
				searchResultID = set.getInt(1);
			}
			
		} catch (SQLException e) {
			logger.error("Error inserting search to database");
		}
		
		return searchResultID;
	}
	
	private String getFormatedTimestamp(){
		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		return timestamp.substring(0, timestamp.indexOf("."));
	}
	
	@Override
	public HttpResponse getNewResponse(ModifiableHttpResponse response,
			HttpMessageFactory messageFactory) {
		return null;
	}

	@Override
	public void processTransferedResponse(HttpResponse response) {
	}

}
