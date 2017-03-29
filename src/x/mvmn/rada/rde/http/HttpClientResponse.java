package x.mvmn.rada.rde.http;

public class HttpClientResponse {
	protected final byte[] content;
	protected final String charset;

	public HttpClientResponse(byte[] content, String charset) {
		this.content = content;
		this.charset = charset;
	}

	public byte[] getContent() {
		return content;
	}

	public String getCharset() {
		return charset;
	}
}