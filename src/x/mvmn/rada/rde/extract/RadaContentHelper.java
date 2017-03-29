package x.mvmn.rada.rde.extract;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import x.mvmn.rada.rde.cache.impl.ZipFSCache;
import x.mvmn.rada.rde.http.impl.CachingHttpClient;
import x.mvmn.rada.rde.jsoup.JsoupWithHttpClient;

public class RadaContentHelper {

	public static class ExtractAttr implements Function<Element, String> {
		protected final String attrName;

		public ExtractAttr(final String attrName) {
			this.attrName = attrName;
		}

		@Override
		public String apply(Element elem) {
			return elem.attr(attrName);
		}
	}

	public static class ExtractSplinter implements Function<String, String> {
		protected final String pattern;
		protected final int splinterIndex;

		public ExtractSplinter(final String pattern, final int splinterIndex) {
			this.pattern = pattern;
			this.splinterIndex = splinterIndex;
		}

		@Override
		public String apply(String str) {
			return str.split(pattern)[splinterIndex];
		}
	}

	public static class ExtractUrlParam implements Function<String, String> {

		protected final String param;

		public ExtractUrlParam(final String param) {
			this.param = param;
		}

		@Override
		public String apply(String url) {
			return url.split("[\\?&]" + param + "=")[1].split("[&]")[0];
		}
	}

	public static class FunctionStrFormat implements Function<String, String> {

		protected final String format;

		public FunctionStrFormat(final String format) {
			this.format = format;
		}

		@Override
		public String apply(String arg) {
			return String.format(format, arg);
		}
	}

	// /////
	public static final String RADA_BASE_URL = "http://w1.c1.rada.gov.ua";

	public static final ExtractAttr EXTRACT_ON_CLICK = new ExtractAttr("onclick");
	public static final ExtractAttr EXTRACT_HREF = new ExtractAttr("abs:href");
	public static final ExtractAttr EXTRACT_SRC = new ExtractAttr("src");
	public static final ExtractSplinter EXTRACT_SPLINTER_APOSTROPHE_1 = new ExtractSplinter("'", 1);
	public static final ExtractUrlParam EXTRACT_URLPARAM_nom_s = new ExtractUrlParam("nom_s");
	public static final Function<String, String> FN_PREPEND_BASEURL = new FunctionStrFormat(RADA_BASE_URL + "%s");
	public static final Function<String, Integer> FN_PARSE_INT = new Function<String, Integer>() {
		@Override
		public Integer apply(final String arg) {
			return Integer.parseInt(arg.trim());
		}
	};

	public static String getUrl_sessionListPage(int assembly) {
		String result = null;
		switch (assembly) {
			case 8:
				result = RADA_BASE_URL + "/pls/radan_gs09/ns_h1";
			break;
			case 7:
			case 6:
			case 5:
			case 4:
			case 3:
				result = RADA_BASE_URL + "/pls/radan_gs09/ns_arh_h1?nom_skl=" + assembly;
			break;
		}
		return result;
	}

	public static String getUrl_deputeesListPage(int assembly) {
		String result = null;
		switch (assembly) {
			case 8:
			case 7:
			case 6:
			case 5:
			case 4:
				result = RADA_BASE_URL + "/pls/site2/fetch_mps?skl_id=" + (assembly + 1);
			break;
			case 3:
			case 2:
				result = RADA_BASE_URL + "/pls/radan_gs09/d_index_arh?skl=" + assembly;
			break;
			case 1:
				result = "http://static.rada.gov.ua/zakon/new/NEWSAIT/DEPUTAT1/spisok1.htm";
			break;
		}
		return result;
	}

	public static Map<String, String> getUrls_votes(Document sessionDayPage) {
		return StreamSupport.stream(sessionDayPage.select("a[href*=/ns_golos?], a[href*=/ns_arh_golos?]").spliterator(), false)
				.collect(Collectors.toMap(EXTRACT_HREF, Element::text));

	}

	public static Stream<String> getUrls_writtenRegs(Document sessionDayPage) {
		return StreamSupport.stream(sessionDayPage.select("a[href*=/ns_reg_write?], a[href*=/ns_arh_reg_write?]").spliterator(), false).map(EXTRACT_HREF);
	}

	public static Stream<String> getUrls_eRegs(Document sessionDayPage) {
		return StreamSupport.stream(sessionDayPage.select("a[href*=/ns_reg?], a[href*=/ns_arh_reg?]").spliterator(), false).map(EXTRACT_HREF);
	}

	public static Stream<String> getUrls_sessionDayUrls(Document sessionDetailsPage) {
		return StreamSupport.stream(sessionDetailsPage.select("table li a").spliterator(), false).map(EXTRACT_HREF);
	}

	public static Map<Integer, String> getUrls_sessionDetailsPages(Document sessionListPage) {
		return StreamSupport.stream(sessionListPage.select("ul.m_ses li").spliterator(), false).map(EXTRACT_ON_CLICK).map(EXTRACT_SPLINTER_APOSTROPHE_1)
				.map(FN_PREPEND_BASEURL).collect(Collectors.toMap(EXTRACT_URLPARAM_nom_s.andThen(FN_PARSE_INT), Function.identity()));
	}

	public static String getUrl_lawprojectsList(int assemblyNum) {
		return RADA_BASE_URL + "/pls/zweb2/webproc2_5_1_J?ses=" + (10001 + assemblyNum) + "&num_s=2&num=&date1=&date2=&name_zp=&out_type=&id=&page=1&zp_cnt=-1";
	}

	public static Stream<String> getUrls_lawprojects(Document lawprojectsListPage) {
		return StreamSupport.stream(lawprojectsListPage.select(".information_block table a").spliterator(), false).map(EXTRACT_HREF);
	}

	public static void main(String args[]) throws Exception {
		ZipFSCache cache = new ZipFSCache(new File(new File(System.getProperty("user.home", "/")), "radadata_cache.zip").getAbsolutePath());

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(1000);
		cm.setDefaultMaxPerRoute(1000);

		CachingHttpClient httpClient = new CachingHttpClient(
				HttpClients.custom().setConnectionManager(cm)
						.setDefaultRequestConfig(
								RequestConfig.custom().setConnectTimeout(60 * 1000).setConnectionRequestTimeout(60 * 1000).setSocketTimeout(60 * 1000).build())
						.build(),
				cache, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");
		JsoupWithHttpClient jSoup = new JsoupWithHttpClient(httpClient);

		for (int i = 8; i > 3; i--) {
			StreamSupport.stream(jSoup.get(RadaContentHelper.getUrl_deputeesListPage(i)).select(".search-filter-results li").spliterator(), false)
					.forEach(new Consumer<Element>() {
						@Override
						public void accept(Element elem) {
							try {
								StreamSupport.stream(jSoup.get(EXTRACT_HREF.apply(elem.select(".title a").first())).select(".information_block_ins .topTitle a")
										.spliterator(), false).map(EXTRACT_HREF).map(new Function<String, String>() {
											@Override
											public String apply(String url) {
												try {
													jSoup.get(url);
												} catch (Exception e) {
													e.printStackTrace();
												}
												return "";
											}
										}).collect(Collectors.toList());
								// jSoup.get(EXTRACT_SRC.apply(elem.select(".thumbnail img").first()));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
		}
		for (int i = 7; i > 6; i--) {
			RadaContentHelper.getUrls_lawprojects(jSoup.get(RadaContentHelper.getUrl_lawprojectsList(i))).forEach(new Consumer<String>() {
				@Override
				public void accept(String url) {
					try {
						jSoup.get(url);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		for (int i = 7; i >= 3;) {
			try {
				System.out.println("Скликання " + i);
				fetchVotes(i, jSoup);
				i--;
			} catch (Exception e) {
				Thread.sleep(3 * 60 * 1000);
			}
		}
	}

	protected static void fetchVotes(int i, final JsoupWithHttpClient jSoup) throws Exception {
		final int skl = i;
		StreamSupport
				.stream(RadaContentHelper.getUrls_sessionDetailsPages(jSoup.get(RadaContentHelper.getUrl_sessionListPage(i))).entrySet().spliterator(), false)
				.forEach(new Consumer<Map.Entry<Integer, String>>() {
					@Override
					public void accept(Entry<Integer, String> entry) {
						final String sessionInfo = String.format("Скликання %s, сесія %s:", skl, EXTRACT_URLPARAM_nom_s.apply(entry.getValue()));

						Consumer<String> fn = new Consumer<String>() {
							@Override
							public void accept(String sessionDayUrl) {
								Stream<String> votesLinks = RadaContentHelper.getUrls_votes(jSoup.getSafe(sessionDayUrl)).keySet().stream();
								final String sesDayInfo = sessionInfo + " - " + sessionDayUrl.split("\\?")[1];
								final long vls = votesLinks.count();
								final AtomicInteger i = new AtomicInteger(0);
								votesLinks.forEach(new Consumer<String>() {
									@Override
									public void accept(String url) {
										try {
											System.out.println(sesDayInfo + ": " + i.incrementAndGet() + "/" + vls);
											jSoup.get(url);
										} catch (HttpStatusException e) {
											if (e.getStatusCode() != 404) {
												throw new RuntimeException(e);
											} else {
												System.err.println("!!! 404 !!! " + url);
											}
										} catch (Exception e) {
											throw new RuntimeException(e);
										}
									}
								});
							}
						};

						RadaContentHelper.getUrls_sessionDayUrls(jSoup.getSafe(entry.getValue())).forEach(fn);
					}
				});
	}

	public static void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
