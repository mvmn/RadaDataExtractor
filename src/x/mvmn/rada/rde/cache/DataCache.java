package x.mvmn.rada.rde.cache;

public interface DataCache {

	void flush() throws Exception;

	void close() throws Exception;

	byte[] get(String key) throws Exception;

	void put(String key, byte[] contents) throws Exception;

	boolean exists(String key) throws Exception;
}