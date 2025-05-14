## [1.6.0](https://github.com/wetransform/gradle-conventions/compare/v1.5.1...v1.6.0) (2025-05-14)

### Features

* support dependency locking for use with trivy ([dc556a3](https://github.com/wetransform/gradle-conventions/commit/dc556a327603a687e65f598354cf14ee414377e6))
* support Scala publishing and formatting ([6127610](https://github.com/wetransform/gradle-conventions/commit/61276105a21bb31d09336db1542c7d58637c41ed))

## [1.5.1](https://github.com/wetransform/gradle-conventions/compare/v1.5.0...v1.5.1) (2025-05-14)

### Bug Fixes

* **spotless:** do not force newline before else in Java ([8d8b56e](https://github.com/wetransform/gradle-conventions/commit/8d8b56e8bb09b9f052b42407cb2fd95760d91ce9))

## [1.5.0](https://github.com/wetransform/gradle-conventions/compare/v1.4.0...v1.5.0) (2025-05-13)

### Features

* allow configuring Docker exposed ports ([5d2ea4d](https://github.com/wetransform/gradle-conventions/commit/5d2ea4d8856e4bf741bcc31c9128953a66fbf978))
* support splitting configuration of conventions ([42d0fd5](https://github.com/wetransform/gradle-conventions/commit/42d0fd54694761cfea452b71e1b6d83f6f0e9243))

### Bug Fixes

* fix source Jar creation ([6b16ac8](https://github.com/wetransform/gradle-conventions/commit/6b16ac8cd99d5d75202a3de04a1422964485474e))
* fix wrong spelling when setting Kotlin toolchain ([3bbf461](https://github.com/wetransform/gradle-conventions/commit/3bbf46124e13cc31eab0fc25d448b5cf3f631dd3))
* ktlint not loading editorconfig file ([f9def06](https://github.com/wetransform/gradle-conventions/commit/f9def061b9cb3639a0f7d5671e682aead24dca93))

## [1.4.0](https://github.com/wetransform/gradle-conventions/compare/v1.3.0...v1.4.0) (2025-05-13)

### Features

* better format chained method calls in Java ([9973518](https://github.com/wetransform/gradle-conventions/commit/9973518525112c9c2a3f81077044ce33b0ad3d9b))

## [1.3.0](https://github.com/wetransform/gradle-conventions/compare/v1.2.0...v1.3.0) (2025-05-12)

### Features

* support generating spotless script and file watcher config ([7c0624d](https://github.com/wetransform/gradle-conventions/commit/7c0624df7691bf3dffab39cb3deacacabd90527b))

## [1.2.0](https://github.com/wetransform/gradle-conventions/compare/v1.1.0...v1.2.0) (2025-05-12)

### Features

* better detection if a project uses Java, Groovy or Kotlin ([cb06836](https://github.com/wetransform/gradle-conventions/commit/cb06836c9c92fdc96e6f3953acf0822241543857))
* improvements to Eclipse format settings ([e03e794](https://github.com/wetransform/gradle-conventions/commit/e03e794c056b5c1a6087333a85ffc7456b4a0b5a))

### Bug Fixes

* change setting name for private repo for better compatibility ([f5fe087](https://github.com/wetransform/gradle-conventions/commit/f5fe087896870bd422aab587b95fc8a6777a3d51))
* override default group name ([2999ea7](https://github.com/wetransform/gradle-conventions/commit/2999ea7dc75a34cb77f5bdb23c669f1d136dc4cb))

## [1.1.0](https://github.com/wetransform/gradle-conventions/compare/v1.0.1...v1.1.0) (2025-05-12)

### Features

* add custom configuration for eclipse formatter ([c1cc8a9](https://github.com/wetransform/gradle-conventions/commit/c1cc8a9bf274a238d833560543c822e37651509c))
* don't cache changing modules ([19a77a1](https://github.com/wetransform/gradle-conventions/commit/19a77a144d62e7848d7dae0e590ebfc9fba88616))
* support skipping defining the MavenPublication ([9ca225b](https://github.com/wetransform/gradle-conventions/commit/9ca225b09a22d20ecbf9b0b0672559ac91ef6dfc))

### Bug Fixes

* correctly create SCM connection string ([8af5cbe](https://github.com/wetransform/gradle-conventions/commit/8af5cbea34b501c04ff796cf6a20d61ca593e210))

## [1.0.1](https://github.com/wetransform/gradle-conventions/compare/v1.0.0...v1.0.1) (2025-05-09)

### Bug Fixes

* switch to eclipse for Java formatting ([f3df854](https://github.com/wetransform/gradle-conventions/commit/f3df854fe9a447408e2061b0a49e7a16164adfc2))

## 1.0.0 (2025-05-09)

### Features

* allow configuration via common plugin and support publishing ([af0c26a](https://github.com/wetransform/gradle-conventions/commit/af0c26aeb76fe3c3143bd4e727eed5d0a5da1202))
* convenience methods to add wetransform repositories ([17e7d07](https://github.com/wetransform/gradle-conventions/commit/17e7d07c75410a9961ea1702979aba0a94b0e27e))
* support configuring the Java version ([5137ec4](https://github.com/wetransform/gradle-conventions/commit/5137ec4eda809e71e157bfc1756575ab9a7c6f04))
