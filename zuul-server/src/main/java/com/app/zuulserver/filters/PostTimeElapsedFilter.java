package com.app.zuulserver.filters;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

@Component
public class PostTimeElapsedFilter extends ZuulFilter {

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(PostTimeElapsedFilter.class);

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() throws ZuulException {

		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();

		logger.info("Enter to POST");

		Long startTime = (Long) request.getAttribute("startTime");
		Long endTime = System.currentTimeMillis();
		
		Long timeElapsed = endTime - startTime;
		
		logger.info(String.format("Time elapsed: %s seconds.", timeElapsed.doubleValue()/1000.00));
		logger.info(String.format("Time elapsed: %s ms.", timeElapsed.doubleValue()/1000.00));
		request.setAttribute("startTime", startTime);
		return null;
	}

	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public int filterOrder() {
		return 1;
	}

}
