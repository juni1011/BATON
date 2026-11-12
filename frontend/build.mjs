import * as esbuild from 'esbuild';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const options = {
    absWorkingDir: root,
    entryPoints: ['frontend/support/main.jsx'],
    outfile: resolve(root, 'src/main/resources/static/js/support.js'),
    bundle: true,
    minify: true,
    jsx: 'automatic',
    target: ['es2020'],
    define: { 'process.env.NODE_ENV': '"production"' },
    legalComments: 'linked',
    logLevel: 'info'
};

if (process.argv.includes('--watch')) {
    const context = await esbuild.context(options);
    await context.watch();
} else {
    await esbuild.build(options);
}
