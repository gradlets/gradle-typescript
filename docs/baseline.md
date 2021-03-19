# Baseline

`com.gradlets.baseline-typescript` provides reasonable defaults for other plugins included.

## TypeScript

following tsconfig settings are applied

| option | value |
| ------ | ----- |
| lib | ES2015, DOM |
| sourceMap | true |
| declarationMap | true |
| forceConsistentCasingInFileNames | true |
| moduleResolution | node |
| noImplicitAny | true |
| noImplicitReturns | true |
| noImplicitThis | true |
| noUnusedLocals | true |
| preserveConstEnums | true |
| strict | true |
| strictNullChecks | true |
| suppressImplicitAnyIndexErrors | true |
| module | es6 |
| target | es6 |

## Eslint

`eslint-config-prettier`, `eslint-plugin-prettier`, `prettier` are added to the eslint config
with following options configured to error on violation.

| option | value |
| ------ | ----- |
| printWidth | 120 |
| tabWidth | 4 |
| trailingComma | all |
| arrowParens | avoid |

## Webpack

Applies simple webpack config that will produce single bundle (with source map) by combining all the produced
javascript files using index.ts as an entrypoint.

## Dependency Recommendations

Gradle-typescript's integration with [sls-packaging](https://github.com/palantir/sls-packaging/) product dependencies is provided by the baseline plugin
