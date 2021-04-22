/*
 * (c) Copyright 2021 Felipe Orozco, Robert Kruszewski. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const webpack = require("webpack");

module.exports = {
    mode: "production",
    devtool: false,
    entry: __ENTRY_POINT__,
    plugins: [
        new webpack.SourceMapDevToolPlugin({ filename: 'bundle.js.map' })
    ],
    resolve: {
        modules: process.env.NODE_PATH.split(":"),
        extensions: [ ".js", ".ts", ".json" ],
    },
    output: {
        path: __OUTPUT_DIR__,
        filename: "bundle.js",
        library: "[name]"
    },
    module: {
        rules: [
            {
                test: /\.(ts|js)($|\?)/i,
                enforce: 'pre',
                loader: require.resolve('source-map-loader'),
            }
        ]
    },
    optimization: {
        minimize: false,
        usedExports: false
    },
    devServer: {
        dev: {
            publicPath: "/build"
        },
        static: {
            directory: "<projectDir>/build/static",
            serveIndex: true,
            watch: true
        },
        port: 8089
    },
    target: "web",
};
