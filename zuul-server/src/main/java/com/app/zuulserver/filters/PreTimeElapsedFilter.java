package com.app.zuulserver.filters;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

@Component
public class PreTimeElapsedFilter extends ZuulFilter {

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(PreTimeElapsedFilter.class);

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() throws ZuulException {

		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();

		logger.info(String.format("%s request routed to %s ", request.getMethod(), request.getRequestURL().toString()));

		Long startTime = System.currentTimeMillis();
		request.setAttribute("startTime", startTime);
		return null;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 1;
	}

}
