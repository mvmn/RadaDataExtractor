package x.mvmn.rada.rde.extract;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
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

	protected <T> List<T> toList(Stream<T> stream) {
		return stream.collect(Collectors.toList());
	}

	public List<Integer> listSessionNumbers(int assembly, boolean refresh) {
		return toList(RadaContentHelper.getSessionNumbers(jSoup.getSafe(RadaContentHelper.getUrl_sessionListPage(assembly), !refresh, true)));
	}

	public List<RadaSessionDayInfo> listSessionDays(int assembly, int sessionNumber, boolean refresh) {
		return toList(
				RadaContentHelper.getSessionDayInfos(jSoup.getSafe(RadaContentHelper.getUrl_sessionDetailsPage(assembly, sessionNumber), !refresh, true)));
	}

	public Map<Integer, String> getVotes(int assembly, int sessionNumber, RadaSessionDayInfo sessionDayInfo, boolean refresh) {
		return RadaContentHelper
				.getVoteIdsAndTitles(jSoup.getSafe(RadaContentHelper.getUrl_sessionDay(sessionDayInfo, sessionNumber, assembly), !refresh, true));
	}

	public VotingInfo parseVote(int assembly, int voteId, boolean refresh) {
		VotingInfo result = new VotingInfo();
		String votePageUrl = RadaContentHelper.getVoteUrl(assembly, voteId);
		Document votePage = jSoup.getSafe(votePageUrl, !refresh, true);

		for (Node node : votePage.select(".head_gol").first().childNodes()) {
			if (node.outerHtml().matches("\\s*\\d+\\.\\d+\\.\\d+\\s+\\d+:\\d+\\s*")) {
				try {
					result.setVoteDateTime(new SimpleDateFormat("dd.MM.yyyy HH:mm").parse((node.outerHtml().trim().replaceAll("\\s+", " "))));
				} catch (ParseException e) {
					throw new RuntimeException("Failed to parse vote date " + node.outerHtml(), e);
				}
			}
		}

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
		int assembly = 8;

		final AtomicInteger fetchLimit = new AtomicInteger(3000);
		TrueZipCache cache = new TrueZipCache(new File(new File(System.getProperty("user.home", "/")), "radadata_cache.zip").getAbsolutePath(),
				new Consumer<String>() {
					@Override
					public void accept(String t) {
						System.err.println("Saving to cache: " + t);
						fetchLimit.decrementAndGet();
					}
				});

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(1000);
		cm.setDefaultMaxPerRoute(1000);

		CloseableHttpClient commonsHttpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(
				RequestConfig.custom().setConnectTimeout(60 * 1000).setConnectionRequestTimeout(60 * 1000).setSocketTimeout(60 * 1000).build()).build();

		RadaDataExtractor rada = new RadaDataExtractor(commonsHttpClient, cache);
		work: {
			for (Integer sessionNumber : rada.listSessionNumbers(assembly, refresh)) {
				List<RadaSessionDayInfo> sessionDays = rada.listSessionDays(assembly, sessionNumber, refresh);
				Collections.sort(sessionDays);
				int c = 1;
				for (RadaSessionDayInfo sessionDay : sessionDays) {
					System.out.println("Fetching session " + sessionNumber + " day " + (c++) + "/" + sessionDays.size() + ": " + sessionDay);
					Map<Integer, String> votes = rada.getVotes(assembly, sessionNumber, sessionDay, refresh);
					int i = 1;
					for (Map.Entry<Integer, String> voteInfo : votes.entrySet()) {
						System.out.println("Fetching vote " + (i++) + " of " + votes.size() + ": " + voteInfo.getValue());
						// System.out.println(rada.parseVote(assembly, voteInfo.getKey(), refresh));
						String votePageUrl = RadaContentHelper.getVoteUrl(assembly, voteInfo.getKey());
						rada.httpClient.get(votePageUrl);
						if (fetchLimit.get() < 1) {
							break work;
						}
					}
				}
			}
		}
		cache.close();
	}
}
