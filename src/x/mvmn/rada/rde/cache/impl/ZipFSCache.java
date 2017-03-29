package x.mvmn.rada.rde.cache.impl;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import x.mvmn.rada.rde.cache.DataCache;

public class ZipFSCache implements DataCache {
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
	protected final AtomicLong unflushed = new AtomicLong(0);
	protected Consumer<String> beforeCachePut;

	public ZipFSCache(final String path, Consumer<String> beforeCachePut) throws Exception {
		this(path);
		this.beforeCachePut = beforeCachePut;
	}

	public ZipFSCache(final String path) throws Exception {
		this.path = path;
		readLock = rwLock.readLock();
		writeLock = rwLock.writeLock();
		reopen();
	}

	public String getPath() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see x.mvmn.rada.rde.Cache#flush()
	 */
	public void flush() throws Exception {
		reopen();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see x.mvmn.rada.rde.Cache#close()
	 */
	public void close() throws Exception {
		writeLock.lock();
		try {
			if (fs != null) {
				fs.close();
			}
			fs = null;
		} finally {
			writeLock.unlock();
		}
	}

	public void reopen() throws Exception {
		writeLock.lock();
		try {
			if (fs != null) {
				fs.close();
			}
			fs = FileSystems.newFileSystem(URI.create("jar:file:" + path), FS_ENV);
		} finally {
			unflushed.set(0);
			writeLock.unlock();
		}
	}

	protected Path getCachePath(final String key) {
		final String actualKey = key.replaceAll("[^0-9A-Za-z\\-_\\. ]", "__");
		return fs.getPath("/" + actualKey);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see x.mvmn.rada.rde.Cache#get(java.lang.String)
	 */
	public byte[] get(final String key) throws Exception {
		byte[] result = null;
		readLock.lock();
		try {
			final Path cacheFile = getCachePath(key);
			if (Files.exists(cacheFile)) {
				result = Files.readAllBytes(cacheFile);
			}
		} finally {
			readLock.unlock();
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see x.mvmn.rada.rde.Cache#put(java.lang.String, byte[])
	 */
	public void put(final String key, final byte[] contents) throws Exception {
		if (beforeCachePut != null) {
			beforeCachePut.accept(key);
		}
		writeLock.lock();
		unflushed.incrementAndGet();
		try {
			Files.write(getCachePath(key), contents, StandardOpenOption.CREATE);
		} finally {
			writeLock.unlock();
		}
	}

	public long unflushedCount() {
		return unflushed.get();
	}

	@Override
	public boolean exists(String key) throws Exception {
		final Path cacheFile = getCachePath(key);
		return Files.exists(cacheFile);
	}
}
