package com.asual.lesscss;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;


public class LessEngineResourceLoaderDefaultImpl implements LessEngineResourceLoader {

	public LessEngineResourceLoaderDefaultImpl() {
	}

	public URL getResource(String resource) throws MalformedURLException {
		try {
			URL u = getClass().getClassLoader().getResource(resource);
			if (u == null) {
				u = new URL(resource);
			}
			return u;
		} catch (Exception e) {
			return null;
		}
	}

	public String readUrl(URL url, String charset) throws IOException {
		InputStream is = url.openStream();
		InputStreamReader isr = new InputStreamReader(is, Charset.forName(charset));
		return readInputStream(isr);
	}

	public String readUrl(URL url) throws IOException {
		InputStream is = url.openStream();
		InputStreamReader isr = new InputStreamReader(is);
		return readInputStream(isr);
	}

	public String readUrl(String urlString, String charset) throws MalformedURLException, IOException {
		return readUrl(getResource(urlString), charset);
	}

	public String readUrl(String urlString) throws MalformedURLException, IOException {
		return readUrl(getResource(urlString));
	}

	protected String readInputStream(InputStreamReader isr) throws IOException {
		StringBuffer sb = new StringBuffer();
		BufferedReader br = new BufferedReader(isr);
		int cc;
		while ((cc = br.read()) != -1)
			sb.appendCodePoint(cc);
		return sb.toString();
	}

}
