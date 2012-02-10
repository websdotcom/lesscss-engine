package com.asual.lesscss;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Andrew Ducore
 */
public interface LessEngineResourceLoader {

 	public abstract URL getResource(String resource) throws MalformedURLException;

	public abstract String readUrl(URL url, String charset) throws IOException;

	public abstract String readUrl(URL url) throws IOException;

	public abstract String readUrl(String urlString, String charset) throws MalformedURLException, IOException;

	public abstract String readUrl(String urlString) throws MalformedURLException, IOException;

}