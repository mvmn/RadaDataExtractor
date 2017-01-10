package x.mvmn.rada.rde;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ZipFSCache {
	static {
		Map<String, String> env = new HashMap<String, String>();
		env.put("create", "true");
		FS_ENV = Collections.unmodifiableMap(env);
	}

	protected static Map<String, String> FS_ENV;
	protected final String path;
	protected FileSystem fs;

	protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	protected final Lock readLock;
	protected final Lock writeLock;

	public ZipFSCache(final String path) throws Exception {
		this.path = path;
		readLock = rwLock.readLock();
		writeLock = rwLock.writeLock();
		reopen();
	}

	public String getPath() {
		return path;
	}

	public void flush() throws Exception {
		reopen();
	}

	public void reopen() throws Exception {
		writeLock.lock();
		try {
			if (fs != null) {
				fs.close();
			}
			fs = FileSystems.newFileSystem(URI.create("jar:file:" + path), FS_ENV);
		} finally {
			writeLock.unlock();
		}

	}

	protected Path getCachePath(final String key) {
		final String actualKey = key.replaceAll("[^0-9A-Za-z\\-_\\. ]", "__");
		return fs.getPath("/" + actualKey);
	}

	public String get(final String key) throws Exception {
		String result = null;
		readLock.lock();
		try {
			final Path cacheFile = getCachePath(key);
			if (Files.exists(cacheFile)) {
				result = new String(Files.readAllBytes(cacheFile), "UTF-8");
			}
		} finally {
			readLock.unlock();
		}
		return result;
	}

	public void put(final String key, final String contents) throws Exception {
		writeLock.lock();
		try {
			Files.write(getCachePath(key), contents.getBytes("UTF-8"), StandardOpenOption.CREATE);
		} finally {
			writeLock.unlock();
		}
	}
}
