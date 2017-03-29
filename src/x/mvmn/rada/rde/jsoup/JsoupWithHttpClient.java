package x.mvmn.rada.rde.jsoup;

import java.io.ByteArrayInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import x.mvmn.rada.rde.http.HttpClient;
import x.mvmn.rada.rde.http.HttpClientResponse;

public class JsoupWithHttpClient implements AutoCloseable {

	protected final HttpClient httpClient;

	public JsoupWithHttpClient(final HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public Document getSafe(final String url) {
		try {
			return get(url);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Document get(final String url) throws Exception {
		HttpClientResponse response = httpClient.get(url);
		return parseResponse(response, url);
	}

	protected Document parseResponse(final HttpClientResponse response, final String url) throws Exception {
		final Document result = Jsoup.parse(new ByteArrayInputStream(response.getContent()), response.getCharset(), url);
		result.setBaseUri(url);
		return result;
	}

	@Override
	public void close() {
		try {
			httpClient.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
