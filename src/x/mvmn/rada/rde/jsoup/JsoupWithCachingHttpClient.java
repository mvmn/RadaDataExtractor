package x.mvmn.rada.rde.jsoup;

import org.jsoup.nodes.Document;

import x.mvmn.rada.rde.http.HttpClientResponse;
import x.mvmn.rada.rde.http.impl.CachingHttpClient;

public class JsoupWithCachingHttpClient extends JsoupWithHttpClient {

	protected final CachingHttpClient cachingHttpClient;

	public JsoupWithCachingHttpClient(CachingHttpClient cachingHttpClient) {
		super(cachingHttpClient);
		this.cachingHttpClient = cachingHttpClient;
	}

	public Document get(final String url, final boolean getFromCache, final boolean putToCache) throws Exception {
		HttpClientResponse response = cachingHttpClient.get(url, getFromCache, putToCache);
		return parseResponse(response, url);
	}

	public Document getSafe(final String url, final boolean getFromCache, final boolean putToCache) {
		try {
			return get(url, getFromCache, putToCache);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
