print = lessenv.print;
quit = lessenv.quit;
readFile = lessenv.readFile;
delete arguments;

if (lessenv.css) {
	readUrl = function(url, charset, resourceLoader) {
		var content;
		if (!/^\w+:/.test(url)) {
			url = 'file:' + url;
		}
		if (resourceLoader) {
			try {
				content = resourceLoader.readUrl(url, charset);
			} catch (e) {
				content = resourceLoader.readUrl(url.replace(/\.less$/, '.css'), charset);
			}
		} else {
			try {
				content = lessenv.readUrl.apply(this, arguments);
			} catch (e) {
				content = lessenv.readUrl.apply(this, [url.replace(/\.less$/, '.css'), charset]);
			}
		}
		return content.replace(/\.css/g, '.less');
	};
} else {
	var existingReadUrl = readUrl;
	readUrl = function(url, charset, resourceLoader) {
		if (resourceLoader)
			return resourceLoader.readUrl(url, charset);
		else
			existingReadUrl(url, charset);
	}
}

var compileString = function(css, options, variables) {
	var result;
	less.Parser.importer = function(path, paths, fn) {
		if (!/^\//.test(path)) {
			path = paths[0] + path;
		}
		if (path != null) {
			new(less.Parser)({ optimization: 1, paths: [String(path).replace(/[\w\.-]+$/, '')] }).parse(readUrl(path, lessenv.charset).replace(/\r/g, ''), function (e, root) {
				fn(e, root);
				if (e instanceof Object)
					throw e;
			});
		}
	};
	new (less.Parser) ({ optimization: 1 }).parse(css, function (e, root) {
		result = root.toCSS(options, convertVariables(variables));
		if (options && options.compress)
			result = exports.compressor.cssmin(result);
		if (e instanceof Object)
			throw e;
	});
	return result;
};

var canonicalizePath = function(path) {
	var cPath = path.replace('/./', '/');
	var collapsible = new RegExp('/([\\w-]{0,2}|[\\w\\.-]{3,})/\\.\\./');
	var invalid = new RegExp('(^|/)\\.\\.(/|$)');
	var oldPath = cPath;
	cPath = cPath.replace(collapsible, '/');
	var loopCount = 0;
	while (cPath != oldPath && loopCount++ < 100) {
		oldPath = cPath;
		cPath = cPath.replace(collapsible, '/');
	}
	if (loopCount >= 100 || cPath.search(invalid) > -1) {
		throw "Unable to normalize path " + path;
	}
	return cPath;
};


var compileFile = function(file, resourceLoader, options, variables) {
	var result, cp = 'classpath:';
	less.Parser.importer = function(path, paths, fn) {
		if (path.indexOf(cp) != -1) {
			var resource = String(resourceLoader.getResource(path.replace(new RegExp('^.*' + cp), '')));
			if (lessenv.css && (!resource || resource == "null")) {
				path = String(resourceLoader.getResource(path.replace(new RegExp('^.*' + cp), '').replace(/\.less$/, '.css')));
			} else {
				path = resource;
			}
		} else if (!/^\//.test(path)) {
			path = canonicalizePath(paths[0] + path);
		}
		if (path != null) {
			new(less.Parser)({ optimization: 1, paths: [String(path).replace(/[\w\.-]+$/, '')] }).parse(readUrl(path, lessenv.charset, resourceLoader).replace(/\r/g, ''), function (e, root) {
				fn(e, root);
				if (e instanceof Object)
					throw e;
			});
		}
	};
	new(less.Parser)({ optimization: 1, paths: [file.replace(/[\w\.-]+$/, '')] }).parse(readUrl(file, lessenv.charset, resourceLoader).replace(/\r/g, ''), function (e, root) {
		result = root.toCSS(options, convertVariables(variables));
		if (options && options.compress)
			result = exports.compressor.cssmin(result);
		if (e instanceof Object)
			throw e;
	});
	return result;
};

var convertVariables = function(variables) {
    var converted = {};
    for (var key in variables) converted[key] = convertVariable(key, variables[key]);
    return converted;
}

var convertVariable = function(key, value) {
    var result, parser = new(less.Parser)({ optimization: 1 });
    parser.parse('@' + key + ':' + value.toString() + ';', function(e, root) {
        if (e instanceof Object)
            throw e;
        result = root.rules[0].value;
    });
    return result;
}
