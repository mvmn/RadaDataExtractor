package x.mvmn.rada.rde.http;

public interface HttpClient extends AutoCloseable {

	public HttpClientResponse get(String url) throws Exception;
}
