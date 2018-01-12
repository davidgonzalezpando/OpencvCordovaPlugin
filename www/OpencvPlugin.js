var exec = require('cordova/exec');

exports.echo = function (arg0, success, error) {
    exec(success, error, 'OpencvPlugin', 'echo', [arg0]);
};

exports.cvtColor = function (arg0, success, error) {
    exec(success, error, 'OpencvPlugin', 'cvtColor', [arg0]);
};
