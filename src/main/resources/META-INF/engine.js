var lessParser = require('./Parser');
var compileString = function(css, options, variables) {
    var result;
    new (lessParser) ({ optimization: 1 }).parse(css, function (e, root) {
            result = root.toCSS(options, convertVariables(variables));
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

var compileFile = function(file, classLoader, options, variables) {
    var result, charset = 'UTF-8', cp = 'classpath:', dirname = file.replace(/\\/g, '/').replace(/[^\/]+$/, '');
    lessParser.importer = function(path, paths, fn) {
        if (path.indexOf(cp) == 0) {
            path = classLoader.getResource(path.replace(cp, ''));
        } else if (path.substr(0, 1) != '/') {
            path = canonicalizePath(dirname + path);
        }
        new(lessParser)({ optimization: 1 }).parse(readUrl(path, charset).replace(/\r/g, ''), function (e, root) {
            fn(root);
            if (e instanceof Object)
                throw e;
        });
    };
    new(lessParser)({ optimization: 1 }).parse(readUrl(file, charset).replace(/\r/g, ''), function (e, root) {
        result = root.toCSS(options, convertVariables(variables));
        if (e instanceof Object)
            throw e;
    });
    return result;
};

var convertVariables = function(variables) {
    var converted = {};
    for (var key in variables) converted[key] = convertVariable(key, variables[key]);
    return converted;
};

var convertVariable = function(key, value) {
    var result, parser = new(lessParser)({ optimization: 1 });
    parser.parse('@' + key + ':' + value.toString() + ';', function(e, root) {
        if (e instanceof Object)
            throw e;
        result = root.rules[0].value;
    });
    return result;
};


var lessTree = require('./tree');
var treeImport = lessTree.Import;

lessTree.Import = function (path, imports) {
    var that = this;

    this._path = path;

    // The '.less' extension is optional
    if (path instanceof lessTree.Quoted) {
        this.path = /\.(le?|c)ss$/.test(path.value) ? path.value : path.value + '.less';
    } else {
        this.path = path.value.value || path.value;
    }

    // Pre-compile all files
    imports.push(this.path, function (root) {
        if (! root) {
            throw new(Error)("Error parsing " + that.path);
        }
        that.root = root;
    });
};
for (var p in treeImport) {
    lessTree.Import[p] = treeImport[p];
};