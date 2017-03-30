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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import x.mvmn.rada.rde.cache.DataCache;
import x.mvmn.rada.rde.cache.impl.TrueZipCache;
import x.mvmn.rada.rde.http.impl.CachingHttpClient;
import x.mvmn.rada.rde.jsoup.JsoupWithCachingHttpClient;
import x.mvmn.rada.rde.model.RadaSessionDayInfo;
import x.mvmn.rada.rde.model.VoteItem;
import x.mvmn.rada.rde.model.VotingInfo;

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

	public Map<String, String> listVotes(String sessionDayPageUrl, boolean refresh) {
		return RadaContentHelper.getUrls_votes(jSoup.getSafe(sessionDayPageUrl, !refresh, true));
	}

	public VotingInfo parseVote(String votePageUrl, boolean refresh) {
		VotingInfo result = new VotingInfo();
		Document votePage = jSoup.getSafe(votePageUrl, !refresh, true);

		for (Element it : votePage.select("form *[name=fr][onclick^=sel_frack(]")) {
			int factionId = Integer.parseInt(it.attr("value").split("idf")[1].trim());
			String factionName = it.nextSibling().toString();
			result.getFactions().put(factionId, factionName);
		}

		for (Element it : votePage.select("#zal_dep a[onmouseover^=pre_dep_gol(], #zal_dep a[onmouseover^=pre_dep_gol_arh(]")) {
			String[] titleParts = it.attr("title").split("\n");
			String voteCast = titleParts[0];
			String mpName = titleParts[1];
			int mpId = Integer.parseInt(it.attr("onmouseover").split("\\(")[1].split(",")[0].trim());

			String parentClass = it.parent().attr("class");
			Elements elems = votePage.select("#11 .karta_zal:not([id=zal_frack]) *[class=" + parentClass + "]");
			if (elems.size() != 1) {
				throw new RuntimeException("Parsing failure for " + votePageUrl + ": failed to determine faction for mp " + mpId + ". " + elems);
			}
			Element elem = elems.first();
			int factionId = Integer.parseInt(elem.parent().attr("id").split("idf")[1].trim());
			result.getVotes().add(new VoteItem(factionId, mpId, mpName, voteCast));
		}

		return result;
	}

	public static void main(String args[]) throws Exception {
		boolean refresh = false;

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
		for (Map.Entry<Integer, String> sessionLinksEntry : rada.listRadaSessionUrls(8, refresh).entrySet()) {
			Map<RadaSessionDayInfo, String> sessionDays = rada.listSessionDays(sessionLinksEntry.getValue(), refresh)
					.collect(Collectors.toMap(RadaSessionDayInfo.FROM_SESSION_DAY_URL, Function.identity()));
			SortedMap<RadaSessionDayInfo, String> sessionDaysSorted = new TreeMap<>(sessionDays);

			for (Map.Entry<RadaSessionDayInfo, String> sessionDay : sessionDaysSorted.entrySet()) {
				System.out.println("Fetching session " + sessionLinksEntry.getKey() + " day: " + sessionDay.getKey());
				Map<String, String> votes = rada.listVotes(sessionDay.getValue(), true);
				int i = 1;
				for (Map.Entry<String, String> voteLink : votes.entrySet()) {
					System.out.println("Fetching vote " + (i++) + " of " + votes.size() + ": " + voteLink.getValue());
					// System.out.println(rada.parseVote(voteLink.getKey(), refresh));
				}
			}
		}

		cache.close();
	}
}
