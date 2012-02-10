/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asual.lesscss;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.tools.shell.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Rostislav Hristov
 * @author Uriah Carpenter
 */
public class LessEngine {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final FunctionContextFactory contextFactory = new FunctionContextFactory();

	private Scriptable scope;
	private Function cs;
	private Function cf;
	
	public LessEngine() {
		Context cx = Context.enter();
		try {
			logger.debug("Initializing LESS Engine.");
			URL less = getClass().getClassLoader().getResource("META-INF/less.js");
			URL engine = getClass().getClassLoader().getResource("META-INF/engine.js");
			logger.info("Using implementation version: {}", cx.getImplementationVersion());
			cx.setOptimizationLevel(9);
			Global global = new Global();
			global.init(cx);		  
			scope = cx.initStandardObjects(global);
			cx.evaluateReader(scope, new InputStreamReader(less.openConnection().getInputStream()), less.getFile(), 1, null);
			cx.evaluateReader(scope, new InputStreamReader(engine.openConnection().getInputStream()), engine.getFile(), 1, null);
			cs = (Function) scope.get("compileString", scope);
			cf = (Function) scope.get("compileFile", scope);
		} catch (Exception e) {
			logger.error("LESS Engine intialization failed.", e);
		} finally {
			Context.exit();
		}
	}
	
	public String compile(String input) throws LessException {
		return compile(input, null, null);
	}

	public String compile(String input, Map<String, ?> options, Map<String, ?> variables) throws LessException {
		try {
			long time = System.currentTimeMillis();
			String result = call(cs, new Object[] {input, options, variables});
			logger.debug("The compilation took {} ms.", System.currentTimeMillis() - time);
			return result;
		} catch (Exception e) {
			throw parseLessException(e);
		}
	}
	
	public String compile(URL input) throws LessException {
		return compile(input, null, null);
	}
	
	public String compile(URL input, Map<String, ?> options, Map<String, ?> variables) throws LessException {
		return compile(input, options, variables, new LessEngineResourceLoaderDefaultImpl());
	}

	public String compile(URL input, Map<String, ?> options, Map<String, ?> variables, LessEngineResourceLoader resourceLoader) throws LessException {
		try {
			long time = System.currentTimeMillis();
			logger.debug("Compiling URL: {}:{}", input.getProtocol(), input.getFile());
			String location = input.getProtocol() + ":";
			if (!"".equals(input.getHost()))
				location += "//" + input.getHost();
			location += input.getFile();
			String result = call(cf, new Object[] {location, resourceLoader, options, variables});
			logger.debug("The compilation of '{}' took {} ms.", input, System.currentTimeMillis() - time);
			return result;
		} catch (Exception e) {
			throw parseLessException(e);
		}
	}
	
	public String compile(File input) throws LessException {
		return compile(input, null, null);
	}
	
	public String compile(File input, Map<String, ?> options, Map<String, ?> variables) throws LessException {
		return compile(input, options, variables, new LessEngineResourceLoaderDefaultImpl());
	}

	public String compile(File input, Map<String, ?> options, Map<String, ?> variables, LessEngineResourceLoader resourceLoader) throws LessException {
		try {
			long time = System.currentTimeMillis();
			logger.debug("Compiling File: file:{}", input.getAbsolutePath());
			String result = call(cf, new Object[] {"file:" + input.getAbsolutePath(), resourceLoader, options, variables});
			logger.debug("The compilation of '{}' took {} ms.", input, System.currentTimeMillis() - time);
			return result;
		} catch (Exception e) {
			throw parseLessException(e);
		}
	}
	
	public void compile(File input, File output) throws LessException {
		try {
			String content = compile(input);
			if (!output.exists()) {
				output.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			bw.write(content);
			bw.close();
		} catch (Exception e) {
			throw parseLessException(e);
		}
	}

	private synchronized String call(Function fn, Object[] args) {
		return contextFactory.call(fn, scope, args);
	}
	
	private LessException parseLessException(Exception root) throws LessException {
		
		logger.debug("Parsing LESS Exception", root);
		
		if (root instanceof JavaScriptException) {
			
			Scriptable value = (Scriptable) ((JavaScriptException) root).getValue();
			
			boolean hasName = ScriptableObject.hasProperty(value, "name");
			boolean hasType = ScriptableObject.hasProperty(value, "type");
			
			if (hasName || hasType) {
				String errorType = "Error";
				
				if (hasName) {
					String type = (String) ScriptableObject.getProperty(value, "name");
					if ("ParseError".equals(type)) {
						errorType = "Parse Error";
					} else {
						errorType = type + " Error";
					}
				} else if (hasType) {
					Object prop = ScriptableObject.getProperty(value, "type");
					if (prop instanceof String) {
						errorType = (String) prop + " Error"; 
					}
				}
				
				String message = (String) ScriptableObject.getProperty(value, "message");
				
				String filename = "";
				if (ScriptableObject.hasProperty(value, "filename")) {
					filename = (String) ScriptableObject.getProperty(value, "filename"); 
				}
				
				int line = -1;
				if (ScriptableObject.hasProperty(value, "line")) {
					Double lineValue = (Double) ScriptableObject.getProperty(value, "line");
					if (lineValue != null) line = lineValue.intValue();
				}
				
				int column = -1;
				if (ScriptableObject.hasProperty(value, "column")) {
					column = ((Double) ScriptableObject.getProperty(value, "column")).intValue();
				}
				
				
				List<String> extractList = new ArrayList<String>();
				if (ScriptableObject.hasProperty(value, "extract")) {
					NativeArray extract = (NativeArray) ScriptableObject.getProperty(value, "extract");
					for (int i = 0; i < extract.getLength(); i++) {
						if (extract.get(i, extract) instanceof String) {
							extractList.add(((String) extract.get(i, extract)).replace("\t", " "));
						}
					}
				}
				
				throw new LessException(message, errorType, filename, line, column, extractList);
			}
		}
		
		throw new LessException(root);
	}
	
	private static final class FunctionContextFactory extends ContextFactory {
		
		private final WrapFactory wrapFactory = new CustomWrapFactory();
		
		FunctionContextFactory() {
			wrapFactory.setJavaPrimitiveWrap(false);
		}
		
		String call(final Function fn, final Scriptable scope, final Object[] args) {
			return (String) call(new ContextAction() {
				
				public Object run(Context cx) {
					for (int i = 0; i < args.length; ++i) {
						args[i] = wrapFactory.wrap(cx, scope, args[i], null);
					}
					
					return fn.call(cx, scope, scope, args);
				}
				
			});
		}
		
		@Override
		public Context makeContext() {
			Context cx = super.makeContext();
			cx.setWrapFactory(wrapFactory);
			return cx;
		}
		
	}
	
}