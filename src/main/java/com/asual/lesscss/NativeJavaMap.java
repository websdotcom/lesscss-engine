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

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

/**
 * A Rhino wrapper to treat a Java {@link Map} as a JavaScript object.
 * 
 * @author Jon Vincent
 */
@SuppressWarnings("serial")
class NativeJavaMap extends NativeJavaObject {

	private final Map<?, ?> map;

	/** Wraps a {@link Map} instance as a JavaScript object.
	 * 
	 * @param scope the variable scope
	 * @param map the {@link Map} instance
	 */
	public NativeJavaMap(Scriptable scope, Map<?, ?> map) {
		super(scope, map, ScriptRuntime.ObjectClass);
		this.map = map;
	}
	
	@Override
	public String getClassName() {
		return "JavaMap";
	}
	
	@Override
	public boolean has(String name, Scriptable start) {
		return (map.containsKey(name) || super.has(name, start));
	}
	
	@Override
	public Object get(String name, Scriptable start) {
		if (map.containsKey(name)) {
			Object value = map.get(name);
			Class<?> staticType = (value != null ? value.getClass() : ScriptRuntime.ObjectClass);
			Context cx = Context.getCurrentContext();
			return cx.getWrapFactory().wrap(cx, this, value, staticType);
		}
		
		return super.get(name, start);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void put(String name, Scriptable start, Object value) {
		// This is obviously not type safe; to get type safety use
		// Collections.checkedMap()
		((Map<String, Object>)map).put(name, value);
	}
	
	@Override
	public void delete(String name) {
		map.remove(name);
	}
	
	@Override
	public Object getDefaultValue(@SuppressWarnings("rawtypes") Class hint) {
		if (hint == null || hint == ScriptRuntime.StringClass) return map.toString();
		if (hint == ScriptRuntime.BooleanClass) return Boolean.TRUE;
		if (hint == ScriptRuntime.NumberClass) return ScriptRuntime.NaNobj;
		return this;
	}
	
	@Override
	public Object[] getIds() {
		return map.keySet().toArray();
	}
	
}