module.exports = function(context) {
  console.log('Installing VLC Framework To iOS Project');

  var util = context.requireCordovaModule("cordova-lib/src/cordova/util");
  var parser = context.requireCordovaModule('cordova-lib/src/configparser/ConfigParser');
  var xml = util.projectConfig(context.opts.projectRoot);
  var config = new parser(xml);

  var xcode = context.requireCordovaModule('xcode');
  var shell = context.requireCordovaModule('shelljs');
  var fs = context.requireCordovaModule('fs');
  var path = context.requireCordovaModule('path');

  var projectPath = context.opts.projectRoot + '/platforms/ios/';
  var projectFile = projectPath + config.name() + '.xcodeproj/project.pbxproj';

  var project = xcode.project(projectFile);
  project.parseSync();

  var plugins = project.pbxGroupByName('Plugins');
  var id = context.opts.plugin.pluginInfo.id;
  if (process.env.VLC_FRAMEWORK_LOCATION===undefined) { throw new Error('environment variable VLC_FRAMEWORK_LOCATION not found'); }
  var srcFile = process.env.VLC_FRAMEWORK_LOCATION;
  var frameworkFolder = srcFile.substring(srcFile.lastIndexOf('/')+1);
  var pluginsPath;
  if ( /"/.test( plugins.path ) ){
    pluginsPath = plugins.path.match( /"(.*?)"/ )[1];
  } else {
    pluginsPath = pluginsPath;
  }
  var targetDir = projectPath + pluginsPath + '/' + id + '/' + frameworkFolder;

  if (!fs.existsSync(srcFile)) throw new Error('cannot find "' + srcFile + '" ios <framework>');
  if (fs.existsSync(targetDir)) throw new Error('target destination "' + targetDir + '" already exists');

  shell.mkdir('-p', path.dirname(targetDir));
  shell.cp('-R', srcFile, path.dirname(targetDir)); // frameworks are directories

  var projectRelative = path.relative(projectPath, targetDir);
  project.addFramework(projectRelative, {customFramework: true});
  fs.writeFileSync(projectFile, project.writeSync());
  
  console.log('Finished Installing VLC Framework To iOS Project');
}
