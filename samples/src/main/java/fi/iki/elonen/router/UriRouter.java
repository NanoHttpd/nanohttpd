package fi.iki.elonen.router;
/*
 * #%L
 * NanoHttpd-Samples
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vnnv on 7/17/15.
 */
public class UriRouter {

	private List<UriResource> mappings;
	private UriResource error404Url;
	private Class<?> notImplemented;

	public UriRouter() {
		mappings = new ArrayList<UriResource>();
	}

	/**
	 * Search in the mappings if the given url matches some of the rules
	 * If there are more than one marches returns the rule with less parameters
	 * e.g.
	 * mapping 1 = /user/:id
	 * mapping 2 = /user/help
	 *
	 * if the incoming uri is www.example.com/user/help - mapping 2 is returned
	 * if the incoming uri is www.example.com/user/3232 - mapping 1 is returned
	 *
	 * @param url
	 * @return
	 */
	public UriResource matchUrl(String url) {

		String work = StringUtils.normalizeUri(url);

		String[] parts = work.split("/");
		List<UriResource> resultList = new ArrayList<UriResource>();

		for (UriResource u : mappings) {

			// Check if count of parts is the same
			if (parts.length != u.getUriParts().size()) {
				continue; // different
			}

			List<UriPart> uriParts = u.getUriParts();

			boolean match = true;
			for (int i = 0; i < parts.length; i++) {
				String currentPart = parts[i];
				UriPart uriPart = uriParts.get(i);
				boolean goOn = false;

				if (currentPart.equals(uriPart.getName())) {
					// exact match
					goOn = true;
				}else {
					// not match
					if (uriPart.isParam()){
						goOn = true;
					}else{
						match = false;
						goOn = false;
					}
				}
				if (!goOn) {
					match = false;
					break;
				}
			} // for - iterate incoming url parts
			if (match) {
				resultList.add(u); // current match
			}
		} // end iterate over all rules
		if (resultList.size() > 0) {
			// some results
			UriResource result = null;
			if (resultList.size() > 1) {
				//return the rule with less parameters
				int params = 1024;
				for (UriResource u : resultList) {
					if (!u.hasParameters()) {
						result = u;
						break;
					}else{
						if (u.getUriParamsCount() <= params) {
							result = u;
						}
					}
				}
				return result;
			}else{
				return resultList.get(0);
			}
		}
		return error404Url;
	}

	public void addRoute(String url, Class<?> handler) {
		if (url != null) {
			if (handler != null) {
				mappings.add(new UriResource(url, handler));
			}else{
				mappings.add(new UriResource(url, notImplemented));
			}
		}

	}

	public void removeRoute(String url) {
		if (mappings.contains(url)) {
			mappings.remove(url);
		}
	}

	public void setNotFoundHandler(Class<?> handler) {
		error404Url = new UriResource(null, handler);
	}

	public void setNotImplemented(Class<?> handler) {
		notImplemented = handler;
	}

	/**
	 * Extract parameters and their values for the given route
	 * @param route
	 * @param uri
	 * @return
	 */
	public Map<String, String> getUrlParams(UriResource route, String uri) {
		Map<String, String> result = new HashMap<String, String>();
		if (route.getUri() == null) {
			return result;
		}

		String workUri = StringUtils.normalizeUri(uri);
		String[] parts = workUri.split("/");


		for (int i = 0; i < parts.length; i++ ) {
			UriPart currentPart = route.getUriParts().get(i);
			if (currentPart.isParam()) {
				result.put(currentPart.getName(), parts[i]);
			}
		}
		return result;
	}
}
