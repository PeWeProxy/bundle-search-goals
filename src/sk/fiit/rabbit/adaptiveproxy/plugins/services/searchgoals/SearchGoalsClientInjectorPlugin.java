package sk.fiit.rabbit.adaptiveproxy.plugins.services.searchgoals;

import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ModifiableSearchResultService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class SearchGoalsClientInjectorPlugin extends JavaScriptInjectingProcessingPlugin {	
	
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		if (response.getServicesHandle().isServiceAvailable(ModifiableSearchResultService.class)){
			super.processResponse(response);
		}
		return ResponseProcessingActions.PROCEED;
	}
	
}
