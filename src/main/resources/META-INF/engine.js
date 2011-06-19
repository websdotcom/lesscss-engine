var compileString = function(css, options, variables) {
    var result;
    new (window.less.Parser) ({ optimization: 1 }).parse(css, function (e, root) {
            result = root.toCSS(options, convertVariables(variables));
            if (e instanceof Object)
                throw e;
    });
    return result;    
};

var compileFile = function(file, classLoader, options, variables) {
    var result, charset = 'UTF-8', cp = 'classpath:', dirname = file.replace(/\\/g, '/').replace(/[^\/]+$/, '');
    window.less.Parser.importer = function(path, paths, fn) {
        if (path.indexOf(cp) == 0) {
            path = classLoader.getResource(path.replace(cp, ''));
        } else if (path.substr(0, 1) != '/') {
            path = dirname + path;
        }
        new(window.less.Parser)({ optimization: 1 }).parse(readUrl(path, charset).replace(/\r/g, ''), function (e, root) {
            fn(root);
            if (e instanceof Object)
                throw e;
        });
    };
    new(window.less.Parser)({ optimization: 1 }).parse(readUrl(file, charset).replace(/\r/g, ''), function (e, root) {
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
}

var convertVariable = function(key, value) {
    var result, parser = new(window.less.Parser)({ optimization: 1 });
    parser.parse('@' + key + ':' + value.toString() + ';', function(e, root) {
        if (e instanceof Object)
            throw e;
        result = root.rules[0].value;
    });
    return result;
}

var treeImport = window.less.tree.Import;

window.less.tree.Import = function (path, imports) {
    var that = this;

    this._path = path;

    // The '.less' extension is optional
    if (path instanceof window.less.tree.Quoted) {
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
    window.less.tree.Import[p] = treeImport[p]
};