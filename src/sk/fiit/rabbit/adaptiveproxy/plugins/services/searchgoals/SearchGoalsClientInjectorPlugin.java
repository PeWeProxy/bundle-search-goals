package sk.fiit.rabbit.adaptiveproxy.plugins.services.searchgoals;

import java.util.Set;

import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.ModifiableSearchResultService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class SearchGoalsClientInjectorPlugin extends JavaScriptInjectingProcessingPlugin {	
	

	@Override
	public void desiredResponseServices(
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		super.desiredResponseServices(desiredServices, webRPHeader);
		desiredServices.add(ModifiableSearchResultService.class);
	}
	
}
