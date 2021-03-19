# TypeScript

TypeScript plugin exposes following config options on `typeScript` extension

| option | type | description |
| ------ | ---- | ----------- |
| sourceSets | `SourceSetContainer` | TypeScript source sets for the project. Let's you configure existing `main` and `test` sourceSets as well as create new ones |
| sourceCompatibility | `Property<String>` | Version of typeScript |
| compilerOptions | `MapProperty<String, Object>` | Compiler options to use for the compiler |
