package x.mvmn.rada.rde.http.impl;

import x.mvmn.rada.rde.http.HttpClientResponse;

public class CachingHttpClientResponse extends HttpClientResponse {

	protected final boolean cacheHit;

	public CachingHttpClientResponse(byte[] content, String charset, boolean cacheHit) {
		super(content, charset);
		this.cacheHit = cacheHit;
	}

	public boolean isCacheHit() {
		return cacheHit;
	}
}
