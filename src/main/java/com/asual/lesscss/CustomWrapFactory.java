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
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

/**
 * A custom implementation of {@link WrapFactory} that can wrap Java maps.
 * 
 * @author Jon Vincent
 */
final class CustomWrapFactory extends WrapFactory {
	
	@Override
	public Object wrap(Context cx, Scriptable scope, Object obj, @SuppressWarnings("rawtypes") Class staticType) {
		if (obj instanceof Map) return new NativeJavaMap(scope, (Map<?, ?>)obj);
		return super.wrap(cx, scope, obj, staticType);
	}
	
	@Override
	public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
		if (obj instanceof Map) return new NativeJavaMap(scope, (Map<?, ?>)obj);
		return super.wrapNewObject(cx, scope, obj);
	}
	
}