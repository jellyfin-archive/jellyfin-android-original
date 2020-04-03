#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

module.exports = ctx => {
    if (!ctx.opts.platforms.includes('android')) return;

    const platformRoot = path.join(ctx.opts.projectRoot, 'platforms/android');
    const sourceFile = path.join(ctx.opts.projectRoot, 'build-extras.gradle');
    const destinationFile = path.join(platformRoot, 'build-extras.gradle');

    if (fs.existsSync(platformRoot) && fs.existsSync(sourceFile)) {
        console.log(`Copying gradle file from ${sourceFile} to ${platformRoot}`);
        fs.createReadStream(sourceFile).pipe(fs.createWriteStream(destinationFile));
    } else {
        console.warn('No build-extras.gradle file found');
    }
}
