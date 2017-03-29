package x.mvmn.rada.rde.extract;

import java.io.File;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import x.mvmn.rada.rde.cache.DataCache;
import x.mvmn.rada.rde.cache.impl.TrueZipCache;
import x.mvmn.rada.rde.http.impl.CachingHttpClient;
import x.mvmn.rada.rde.jsoup.JsoupWithCachingHttpClient;
import x.mvmn.rada.rde.model.RadaSessionDayInfo;

public class RadaDataExtractor {

	public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36";

	protected final DataCache cache;
	protected final CachingHttpClient httpClient;
	protected final JsoupWithCachingHttpClient jSoup;

	public RadaDataExtractor(CloseableHttpClient commonsHttpClient, DataCache cache) {
		this.cache = cache;
		this.httpClient = new CachingHttpClient(commonsHttpClient, cache, USER_AGENT);
		this.jSoup = new JsoupWithCachingHttpClient(this.httpClient);
	}

	public Map<Integer, String> listRadaSessionUrls(int radaAssembly, boolean refresh) {
		String sessionListPageUrl = RadaContentHelper.getUrl_sessionListPage(radaAssembly);
		Map<Integer, String> result = RadaContentHelper.getUrls_sessionDetailsPages(jSoup.getSafe(sessionListPageUrl, !refresh, true));
		return result;
	}

	public Stream<String> listSessionDays(String sessionPageUrl, boolean refresh) {
		return RadaContentHelper.getUrls_sessionDayUrls(jSoup.getSafe(sessionPageUrl, !refresh, true));
	}

	public Stream<String> listSessionDays(int radaAssembly, int sessionNumber, boolean refresh) {
		Stream<String> result = Stream.empty();

		String sessionPageUrl = listRadaSessionUrls(radaAssembly, refresh).get(sessionNumber);
		if (sessionPageUrl != null) {
			result = listSessionDays(sessionPageUrl, refresh);
		}

		return result;
	}

	public static void main(String args[]) throws Exception {
		boolean refresh = true;

		TrueZipCache cache = new TrueZipCache(new File(new File(System.getProperty("user.home", "/")), "radadata_cache.zip").getAbsolutePath(),
				new Consumer<String>() {
					@Override
					public void accept(String t) {
						System.err.println("Saving to cache: " + t);
					}
				});

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(1000);
		cm.setDefaultMaxPerRoute(1000);

		CloseableHttpClient commonsHttpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(
				RequestConfig.custom().setConnectTimeout(60 * 1000).setConnectionRequestTimeout(60 * 1000).setSocketTimeout(60 * 1000).build()).build();

		RadaDataExtractor rada = new RadaDataExtractor(commonsHttpClient, cache);
		Map<Integer, String> sessionLinks = rada.listRadaSessionUrls(8, refresh);
		System.out.println(sessionLinks);
		Map<RadaSessionDayInfo, String> sessionDays = rada.listSessionDays(sessionLinks.get(1), refresh)
				.collect(Collectors.toMap(RadaSessionDayInfo.FROM_SESSION_DAY_URL, Function.identity()));
		SortedMap<RadaSessionDayInfo, String> sessionDaysSorted = new TreeMap<>(sessionDays);
		for (Map.Entry<RadaSessionDayInfo, String> urlEntry : sessionDaysSorted.entrySet()) {
			System.out.println(urlEntry.getKey() + " == " + urlEntry.getValue());
		}
		cache.close();
	}
}
