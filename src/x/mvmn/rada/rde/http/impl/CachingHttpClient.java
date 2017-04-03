package x.mvmn.rada.rde.http.impl;

import java.nio.charset.StandardCharsets;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import x.mvmn.rada.rde.cache.DataCache;
import x.mvmn.rada.rde.http.HttpClient;
import x.mvmn.rada.rde.http.HttpClientResponse;

public class CachingHttpClient implements HttpClient {

	protected final CloseableHttpClient httpClient;
	protected final DataCache cache;
	protected final String userAgent;

	public CachingHttpClient(final CloseableHttpClient httpClient, final DataCache cache, final String userAgent) {
		this.httpClient = httpClient;
		this.cache = cache;
		this.userAgent = userAgent;
	}

	@Override
	public void close() throws Exception {
		cache.flush();
		httpClient.close();
	}

	public void flushCache() throws Exception {
		cache.flush();
	}

	public CachingHttpClientResponse get(String absoluteUrl) throws Exception {
		return get(absoluteUrl, true, true);
	}

	public CachingHttpClientResponse get(String absoluteUrl, boolean getFromCache, boolean storeToCache) throws Exception {
		HttpClientResponse result = null;
		boolean newlyObtained = false;
		boolean cacheHit = false;
		if (getFromCache) {
			result = doCacheGet(absoluteUrl);
		}
		if (result == null) {
			result = doHttpGet(absoluteUrl);
			newlyObtained = true;
		} else {
			cacheHit = true;
		}
		if (storeToCache && result != null && newlyObtained) {
			doCachePut(absoluteUrl, result);
		}
		return result != null ? new CachingHttpClientResponse(result.getContent(), result.getCharset(), cacheHit) : null;
	}

	protected HttpClientResponse doCacheGet(String url) throws Exception {
		byte[] content = cache.get(url);
		return content != null ? new HttpClientResponse(content, StandardCharsets.UTF_8.name()) : null;
	}

	protected void doCachePut(String url, HttpClientResponse response) throws Exception {

		byte[] utf8Content = new String(response.getContent(), response.getCharset()).getBytes(StandardCharsets.UTF_8);

		cache.put(url, utf8Content);
		cache.flush();
	}

	protected HttpClientResponse doHttpGet(String absoluteUrl) throws Exception {
		byte[] content = null;
		String charset = StandardCharsets.UTF_8.name();
		HttpGet get = new HttpGet(absoluteUrl);
		if (userAgent != null && !userAgent.isEmpty()) {
			get.setHeader("User-Agent", userAgent);
		}

		try (CloseableHttpResponse response = httpClient.execute(get)) {
			HttpEntity entity = response.getEntity();
			if (response.getStatusLine().getStatusCode() == 200) {
				if (entity != null) {
					content = EntityUtils.toByteArray(entity);
				}
			} else {
				throw new RuntimeException("Bad status code: " + response.getStatusLine().getStatusCode() + ". Response content: "
						+ (entity != null ? EntityUtils.toByteArray(entity) : " -- none --"));
			}
			for (Header header : response.getHeaders("Content-Type")) {
				String hv = header.getValue();
				if (hv.toLowerCase().contains("charset=")) {
					charset = hv.substring(hv.toLowerCase().indexOf("charset=") + "charset=".length());
					if (charset.indexOf(";") >= 0) {
						charset = charset.substring(0, charset.indexOf(";"));
					}
					break;
				}
			}
		}

		return new HttpClientResponse(content, charset);
	}
}
