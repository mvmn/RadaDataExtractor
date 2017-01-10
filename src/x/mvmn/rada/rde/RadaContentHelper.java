package x.mvmn.rada.rde;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

public class RadaContentHelper {

	protected static class ExtractAttr implements Function<Element, String> {
		protected final String attrName;

		public ExtractAttr(final String attrName) {
			this.attrName = attrName;
		}

		@Override
		public String apply(Element elem) {
			return elem.attr(attrName);
		}
	}

	protected static class ExtractSplinter implements Function<String, String> {
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

	protected static class ExtractUrlParam implements Function<String, String> {

		protected final String param;

		public ExtractUrlParam(final String param) {
			this.param = param;
		}

		@Override
		public String apply(String url) {
			return url.split(param)[1].split("&")[0];
		}
	}

	protected static class FunctionStrFormat implements Function<String, String> {

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
	protected static final String RADA_BASE_URL = "http://w1.c1.rada.gov.ua";

	protected static final ExtractAttr EXTRACT_ON_CLICK = new ExtractAttr("onclick");
	protected static final ExtractAttr EXTRACT_HREF = new ExtractAttr("abs:href");
	protected static final ExtractSplinter EXTRACT_SPLINTER_APOSTROPHE_1 = new ExtractSplinter("'", 1);
	protected static final ExtractUrlParam EXTRACT_URLPARAM_nom_s = new ExtractUrlParam("nom_s=");
	protected static final Function<String, String> FN_PREPEND_BASEURL = new FunctionStrFormat(RADA_BASE_URL + "%s");
	protected static final Function<String, Integer> FN_PARSE_INT = new Function<String, Integer>() {
		@Override
		public Integer apply(final String arg) {
			return Integer.parseInt(arg.trim());
		}
	};

	public String getUrl_sessionListPage(int assembly) {
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

	public String getUrl_deputeesListPage(int assembly) {
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

	public FluentIterable<String> getUrls_votes(Document sessionDayPage) {
		return FluentIterable.from(sessionDayPage.select("a[href*=/ns_golos?], a[href*=/ns_arh_golos?]")).transform(EXTRACT_HREF);
		// .transform(FN_PREPEND_BASEURL);
	}

	public FluentIterable<String> getUrls_writtenRegs(Document sessionDayPage) {
		return FluentIterable.from(sessionDayPage.select("a[href*=/ns_reg_write?], a[href*=/ns_arh_reg_write?]")).transform(EXTRACT_HREF);
	}

	public FluentIterable<String> getUrls_eRegs(Document sessionDayPage) {
		return FluentIterable.from(sessionDayPage.select("a[href*=/ns_reg?], a[href*=/ns_arh_reg?]")).transform(EXTRACT_HREF);
	}

	public FluentIterable<String> getUrls_sessionDayUrls(Document sessionDetailsPage) {
		return FluentIterable.from(sessionDetailsPage.select("table li a")).transform(EXTRACT_HREF);
	}

	public Map<String, Integer> getUrls_sessionDetailsPages(Document sessionListPage) {
		return FluentIterable.from(sessionListPage.select("ul.m_ses li")).transform(EXTRACT_ON_CLICK).transform(EXTRACT_SPLINTER_APOSTROPHE_1)
				.transform(FN_PREPEND_BASEURL).toMap(Functions.compose(FN_PARSE_INT, EXTRACT_URLPARAM_nom_s));
	}

	public String getUrl_lawprojectsList(int assemblyNum) {
		return RADA_BASE_URL + "/pls/zweb2/webproc2_5_1_J?ses=" + (10001 + assemblyNum) + "&num_s=2&num=&date1=&date2=&name_zp=&out_type=&id=&page=1&zp_cnt=-1";
	}

	public FluentIterable<String> getUrls_lawprojects(Document lawprojectsListPage) {
		return FluentIterable.from(lawprojectsListPage.select(".information_block table a")).transform(EXTRACT_HREF);
	}

	static CachingJsoup jSoup;
	static volatile String msg;

	public static void main(String args[]) throws Exception {
		jSoup = new CachingJsoup(new ZipFSCache(new File(new File(System.getProperty("user.home", "/")), "radadata_cache.zip").getAbsolutePath()),
				new Predicate<String>() {
					@Override
					public boolean apply(String url) {
						// System.out.println("Cache hit: " + url);
						return false;
					}
				}, new Predicate<String>() {
					private int flushTime = 1000;

					@Override
					public boolean apply(String url) {
						if (msg != null) {
							System.out.println(msg);
							msg = null;
						}
						System.err.println("Cache miss: " + url);
						if (--flushTime < 1) {
							jSoup.flushCache();
							flushTime = 100;
						}
						RadaContentHelper.sleep();
						return false;
					}
				});

		final RadaContentHelper unit = new RadaContentHelper();
		for (int i = 7; i > 3; i--) {
			FluentIterable.from(jSoup.get(unit.getUrl_deputeesListPage(i)).select(".search-filter-results li")).transform(new Function<Element, Element>() {
				// ExtractAttr extractSrc = new ExtractAttr("src");

				@Override
				public Element apply(Element elem) {
					try {
						FluentIterable.from(jSoup.get(EXTRACT_HREF.apply(elem.select(".title a").first())).select(".information_block_ins .topTitle a"))
								.transform(EXTRACT_HREF).transform(new Function<String, String>() {
									@Override
									public String apply(String url) {
										try {
											jSoup.get(url);
										} catch (Exception e) {
											e.printStackTrace();
										}
										return "";
									}
								}).toList();
						// jSoup.get(extractSrc.apply(elem.select(".thumbnail img").first()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					return elem;
				}
			}).toList();
			jSoup.flushCache();
		}
		// for (int i = 7; i > 6; i--) {
		// unit.getUrls_lawprojects(jSoup.get(unit.getUrl_lawprojectsList(i))).transform(new Function<String, String>() {
		// @Override
		// public String apply(String url) {
		// try {
		// jSoup.get(url);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// return "";
		// }
		// }).toSet();
		// jSoup.flushCache();
		// }
		// for (int i = 7; i >= 3;) {
		// try {
		// System.out.println("Скликання " + i);
		// fetchVotes(i, jSoup, unit);
		// i--;
		// } catch (Exception e) {
		// jSoup.flushCache();
		// Thread.sleep(3 * 60 * 1000);
		// } finally {
		// jSoup.flushCache();
		// }
		// }
	}

	protected static void fetchVotes(int i, final CachingJsoup jSoup, final RadaContentHelper unit) throws Exception {
		final int skl = i;
		FluentIterable.from(unit.getUrls_sessionDetailsPages(jSoup.get(unit.getUrl_sessionListPage(i))).entrySet())
				.transform(new Function<Map.Entry<String, Integer>, List<String>>() {
					@Override
					public List<String> apply(Entry<String, Integer> entry) {
						final String sessionInfo = String.format("Скликання %s, сесія %s:", skl, EXTRACT_URLPARAM_nom_s.apply(entry.getKey()));
						List<String> result = unit.getUrls_sessionDayUrls(jSoup.getSafe(entry.getKey())).transform(new Function<String, String>() {
							@Override
							public String apply(String sessionDayUrl) {
								FluentIterable<String> votesLinks = unit.getUrls_votes(jSoup.getSafe(sessionDayUrl));
								final String sesDayInfo = sessionInfo + " - " + sessionDayUrl.split("\\?")[1];
								int vls = votesLinks.size();
								for (int i = 0; i < vls; i++) {
									String url = votesLinks.get(i);
									try {
										msg = sesDayInfo + ": " + i + "/" + vls;
										jSoup.httpGet(url, true, true);
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
								return "";
							}
						}).toList();
						return result;
					}
				}).toList();
	}

	public static void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
