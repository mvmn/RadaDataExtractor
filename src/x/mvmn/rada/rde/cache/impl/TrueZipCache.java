package x.mvmn.rada.rde.cache.impl;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.FsOutputOption;
import x.mvmn.rada.rde.cache.DataCache;

public class TrueZipCache implements DataCache {

	protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	protected final Lock readLock;
	protected final Lock writeLock;
	protected final String path;

	protected Consumer<String> beforeCachePut;

	public TrueZipCache(final String path, Consumer<String> beforeCachePut) throws Exception {
		this(path);
		this.beforeCachePut = beforeCachePut;
	}

	public TrueZipCache(String path) {
		this.path = path;

		readLock = rwLock.readLock();
		writeLock = rwLock.writeLock();

		TConfig config = TConfig.get();
		config.setOutputPreferences(config.getOutputPreferences().set(FsOutputOption.GROW));

		// Check
		TFile tfile = new TFile(path);
		if (!tfile.isArchive()) {
			throw new RuntimeException("Not an archive: " + path);
		}
	}

	@Override
	public void flush() throws Exception {
		TVFS.umount();
	}

	@Override
	public void close() throws Exception {
		TVFS.umount();
	}

	@Override
	public byte[] get(String key) throws Exception {
		readLock.lock();
		try (TFileInputStream reader = new TFileInputStream(getCachePath(key))) {
			return IOUtils.toByteArray(reader);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void put(String key, byte[] contents) throws Exception {
		if (beforeCachePut != null) {
			beforeCachePut.accept(key);
		}
		writeLock.lock();
		try (TFileOutputStream out = new TFileOutputStream(getCachePath(key), false)) {
			IOUtils.write(contents, out);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean exists(String key) throws Exception {
		return getCachePath(key).exists();
	}

	protected TFile getCachePath(final String key) {
		final String actualKey = key.replaceAll("[^0-9A-Za-z\\-_\\. ]", "__");
		return new TFile(new TFile(path), actualKey);
	}

	public void compact() throws Exception {
		writeLock.lock();
		try {
			new TFile(path).compact();
		} finally {
			writeLock.unlock();
		}
	}
}
