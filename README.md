# Gradle-TypeScript

Gradle plugin that provides compatibility with NPM repositories and methods for building typescript projects.

## Usage

In your top level build.gradle add

```groovy
buildscript {
    repositories {
        gradlePluginPortal()
    }

    dependencies {
        classpath 'com.gradlets.gradle.typescript:gradle-typescript:<version>'
        classpath 'com.gradlets.gradle.typescript:gradle-baseline-typescript:<version>'
    }
}

apply plugin: 'com.gradlets.baseline-typescript'

allprojects {
    apply plugin: 'com.grdlets.typescript'

    repositories {
        npm {
            url 'https://registry.npmjs.org/'
        }
    }
}
```

The typescript plugin should come with sensible defaults. If you want to tweak settings
you use `typeScript` extension.

```groovy
typeScript {
    sourceCompatibility = "4.2.3"
    compilerOptions.putAll([
        'module': 'es6',
        'target': 'es6',
        'moduleResolution': 'node',
        'strict': true,
    ])
}
```

## [Baseline](docs/baseline.md)

`com.gradlets.baseline-typescript` provides reasonable defaults for all plugins in this repo.
You can always configure them yourself if you need more flexibility.

# [Typescript](docs/typescript.md)

Once `com.gradlets.typescript` is applied you can place files in `src/main/typescript` (and `src/test/typescript`)
and they will get compiled to javascript via `compileTypescript` (and `compileTestTypescript` respectively)

```
project
├── build.gradle
└── src
    ├── main
    │   └── typescript
    │       └── index.ts
    └── test
        └── typescript
            └── index.ts
```

## Dependencies

To add dependencies use the default gradle dependencies block. By default, every source set that's created has `deps`
and `types` configurations associated with it. `deps` defines external dependencies

_Note: since npm packages don't have a group we always use `npm`_

```groovy
depencencies {
    deps 'npm:conjure-client:2.4.1'
    types 'npm:types/node:15.4.0'
}
```

### Gradle Consistent-Versions

Gradle-TypeScript provides integration with `com.palantir.consistent-versions`
via `com.gradlets.baseline-typescript-versions` plugin. That plugin is applied automatically if you opt in into
`com.gradlets.baseline-typescript` defaults plugin.


### Dependency Resolution

Gradle-TypeScript does not support full npm dependency semantics. Resolution follows maven/ivy semantics
where there can be only one version of a package in a configuration. As a result some projects
might need adjustments to align versions of transitive dependencies even though they would build just fine
using npm. We believe that's a reasonable tradeoff as in practice developers perform dependency hoisting and
deduplication using other tools (i.e. webpack) to achieve the same result.

### Post-install scripts

Every dependency is interpreted as a directory of files and none of the npm install scripts are run as a result of
dependency resolution. While there are projects that rely on that behaviour it's been long viewed as a security
vulnerability, and a drag on local development experience. If you have a project that requires post install scripts
to perform compilation it's recommended to contribute prebuilt binaries to upstream so the scripts aren't needed.

## Testing

For testing gradle-typescript relies on `jest`. All you need to do is add `jest` dependency to `testDeps` configuration.

```groovy
dependencies {
    testDeps 'npm:jest:26.6.0'
}
```

Your tests will be executed when running `./gradlew jestTest`. `jestTest` task is configured to be required
by lifecycle `check` task.

# [Webpack](docs/webpack.md)

Gradle-Typescript provides webpack integration to produce assets for frontend builds. You can apply webpack plugin by
adding following snippet to your `build.gradle`

```groovy
apply plugin: 'com.gradlets.webpack'

webpack {
    configFile 'webpack.config.js'
}
```

This will add `bundleWebpack` task that will use `webpack` configuration to figure out webpack binary
as well as any plugins and run webpack build with the desired config. By default, output is placed in `$buildDir/webpack`
directory. If you need to change it make changes to your config file and override `outputDir` extension property
to make sure gradle caches correctly

`com.gradlets.baseline-typescript` ships with simple webpack config that will produce single bundle (with source map)
by combining all the produced javascript files using index.ts as an entrypoint.

# [Eslint](docs/eslint.md)

To use `ESLint` integration you need to add folliowing snippet to your `build.gradle`

```groovy
apply plugin: 'com.gradlets.eslint'
```

the eslint plugin is configured via an extension


```groovy
eslint {
    inheritedConfig.add(['plugin:prettier/recommended'])
    rules.put([
            "prettier/prettier": ["error", ["printWidth": "120"]]
    ])
}
```

eslint executable will be search for in `eslint` configuration

`com.gradlets.baseline-typescript` provides reasonable default eslint configuration

# Dependency Recommendations

Gradle-typescript integrates with [sls-packaging](https://github.com/palantir/sls-packaging/) product dependencies.
You can configure your projects product dependencies using [`recommendedProductDependencies`](https://github.com/palantir/sls-packaging/#recommended-product-dependencies-plugin) extension.
```groovy
recommendedProductDependencies {
    productDependency {
        productGroup = 'com.foo.bar.group'
        productName = 'product'
        minimumVersion = rootProject.version
        maximumVersion = "${rootProject.version.tokenize('.')[0].toInteger()}.x.x"
        recommendedVersion = rootProject.version
    }
}
```

The recommended product dependencies embedded in package.json of your dependencies will be propagated into your
product's dependencies as well.

# Conjure Typescript

Gradle-TypeScript offers an alternative to default conjure-typescript integration. Instead of generating a project
that produces only a distribution that can be consumed from npm projects there's a full fledged gradle support.

To use the conjure integration you only need to add the plugin to your project

```groovy
apply plugin: 'com.gradlets.conjure-typescript-local'
```

Then you can add conjure ir files to the project

```groovy
dependencies {
    conjure 'com.foo:bar@conjure.json'
}
```

Each of the dependencies on conjure configuration will be generated to a subproject to the project you added
`com.gradlets.conjure-typescript-local` to (`remote-api` in example below).

```
remote-api
├── build.gradle
└── bar-api
    └── src
        └── main
            └── typescript
                └── index.ts
```

note: since gradle requires all projects to be defined upfront you will also need to add

```groovy
include 'remote-api:bar-api'
```

to your settings.gradle.