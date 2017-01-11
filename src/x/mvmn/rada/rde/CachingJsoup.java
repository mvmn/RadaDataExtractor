package x.mvmn.rada.rde;

import java.util.function.Predicate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class CachingJsoup {

	protected static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36";
	protected static final int TIMEOUT = 60000;

	protected final ZipFSCache cache;
	protected final Predicate<String> onCacheHit;
	protected final Predicate<String> onCacheMiss;

	public CachingJsoup(final ZipFSCache cache, final Predicate<String> onCacheHit, final Predicate<String> onCacheMiss) {
		this.cache = cache;
		this.onCacheHit = onCacheHit;
		this.onCacheMiss = onCacheMiss;
	}

	public Document getSafe(final String url) {
		try {
			return get(url);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Document get(final String url) throws Exception {
		return get(url, true, true);
	}

	public String httpGet(final String url, final boolean getFromCache, final boolean putToCache) throws Exception {
		String content = getFromCache ? cache.get(url) : null;
		if (content != null) {
			if (onCacheHit != null) {
				onCacheHit.test(url);
			}
		} else {
			if (onCacheMiss != null) {
				onCacheMiss.test(url);
			}
			content = Jsoup.connect(url).userAgent(USER_AGENT).timeout(TIMEOUT).execute().body();
			if (putToCache) {
				cache.put(url, content);
			}
		}
		return content;
	}

	public Document get(final String url, final boolean getFromCache, final boolean putToCache) throws Exception {
		final String content = httpGet(url, getFromCache, putToCache);
		final Document result = Jsoup.parse(content);
		result.setBaseUri(url);
		return result;
	}

	public void close() {
		try {
			cache.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void flushCache() {
		try {
			long t1 = System.currentTimeMillis();
			cache.flush();
			long t2 = System.currentTimeMillis();
			System.out.println("Cache flushed. T millis = " + (t2 - t1));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
