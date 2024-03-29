package sk.fiit.rabbit.adaptiveproxy.plugins.services.searchgoals;

import java.sql.Connection;
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
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ModifiableSearchResultService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.SearchResultObject;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.SqlUtils;

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
			
			int searchID = injectOnclickToSearchResults(response);
			
			injectUidSenderScript(response, searchID);
			
		}
		
		return ResponseProcessingActions.PROCEED;
	}
	
	private int injectOnclickToSearchResults(ModifiableHttpResponse response){
		
		int searchID = -1;
		
		ModifiableSearchResultService modifiableSearchResultService = response.getServicesHandle().getService(ModifiableSearchResultService.class);
		
		ArrayList<SearchResultObject> searchResultObjectList = modifiableSearchResultService.getSearchedData();
		int resultCount = searchResultObjectList.size();
		
		Connection connection = null;
		
		if (resultCount > 0 && response.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)) {
			try {
				connection = response.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
				JdbcTemplate jdbc = new JdbcTemplate(connection);

				String queryString = modifiableSearchResultService.getQueryString().trim();
			
				searchID = insertSearchToDB(jdbc, queryString);
			
				for (SearchResultObject searchResultObject : searchResultObjectList){
					int searchResultID = insertSearchResultToDB(jdbc, searchResultObject, searchID);
					
					modifiableSearchResultService.deleteResult(1);
					modifiableSearchResultService.putResult(new SearchResultObject(searchResultObject, "peweproxy.modules.searchgoals.clicked_result("+searchResultID+")"), resultCount);
				}
			} finally {
				SqlUtils.close(connection);
			}
		}
		
		return searchID;
	}
	
	private void injectUidSenderScript(ModifiableHttpResponse response, int searchID){
		
		if (response.getServicesHandle().isServiceAvailable(HtmlInjectorService.class)){
			HtmlInjectorService htmlInjectionService = response.getServicesHandle().getService(HtmlInjectorService.class);
			
			String script = "<script type=\"text/javascript\">\n" +
					"<![CDATA[\n" +
					"var __ap_search_id = " + searchID + ";\n" +
					"peweproxy.on_uid_ready(function() {\n" +
					"	peweproxy.jQuery.post(\"./adaptive-proxy/search-goals.html?action=addUID\", { \"uid\": peweproxy.uid, \"id\": __ap_search_id});\n" +
					"});\n" +
					"]]>" +
					"</script>\n"; 
			
			htmlInjectionService.inject(script, HtmlPosition.END_OF_BODY);
		}
	}

	private int insertSearchResultToDB(JdbcTemplate jdbc, SearchResultObject searchResultObject, int searchID){
		int searchResultID = jdbc.insert("INSERT INTO searchgoals_search_results (url, heading, perex, id_search) VALUES (?, ?, ?, ?)", 
				new Object[] { searchResultObject.getUrl(), searchResultObject.getHeader(), searchResultObject.getPerex(), searchID });
		
		return searchResultID;
	}
	
	private int insertSearchToDB(JdbcTemplate jdbc, String queryString){
		int searchID = jdbc.insert("INSERT INTO searchgoals_searches (timestamp, search_query) VALUES (?, ?)", 
				new Object[] { getFormatedTimestamp(), queryString });
		
		return searchID;
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
