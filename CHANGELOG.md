## [2.2.0](https://github.com/wetransform/gradle-conventions/compare/v2.1.0...v2.2.0) (2025-09-24)

### Features

* auto-detect Java version from mise.toml or .java-version ([7a05a39](https://github.com/wetransform/gradle-conventions/commit/7a05a39bb7a0f72a3a61ca766f8eea975d3107a6))
* **spotless:** add Windows specific script for applying formatting ([ea16d77](https://github.com/wetransform/gradle-conventions/commit/ea16d778003102207914f7c3137284dc4c13ba9b))
* **spotless:** script also supports build.gradle.kts files ([e504ffb](https://github.com/wetransform/gradle-conventions/commit/e504ffb63735bbf6253006a5c4c1767bad0dd443))
* **spotless:** support running script on macOS ([2410e30](https://github.com/wetransform/gradle-conventions/commit/2410e3022642d8d68e50bb61582c7c9a7a713277))
* support defining custom Maven repos ([0a698bf](https://github.com/wetransform/gradle-conventions/commit/0a698bf6545eb161a881ce96aa3726890a3c1acb))

### Bug Fixes

* **deps:** update dependency com.diffplug.spotless:spotless-plugin-gradle to v7.0.4 ([c199fe7](https://github.com/wetransform/gradle-conventions/commit/c199fe7d435d2102cffc4ddb56e805b68fbe663f))
* **deps:** update dependency com.diffplug.spotless:spotless-plugin-gradle to v7.1.0 ([dde4d59](https://github.com/wetransform/gradle-conventions/commit/dde4d597148bc66264060cd2baf4b38addcce9bf))
* **deps:** update dependency com.diffplug.spotless:spotless-plugin-gradle to v7.2.0 ([6beddb5](https://github.com/wetransform/gradle-conventions/commit/6beddb5ebde1007fa4ba0e4157e365824fd3c599))
* **deps:** update dependency com.diffplug.spotless:spotless-plugin-gradle to v7.2.1 ([2b72e29](https://github.com/wetransform/gradle-conventions/commit/2b72e293148f2dcc7996061aa2bb91fbd0b3669e))
* **deps:** update dependency org.ajoberstar.grgit:grgit-gradle to v5.3.2 ([de638f0](https://github.com/wetransform/gradle-conventions/commit/de638f02b43a5f50a94bd3bd5cbf14151c3b7b96))
* **deps:** update dependency org.ajoberstar.grgit:grgit-gradle to v5.3.3 ([7a82865](https://github.com/wetransform/gradle-conventions/commit/7a82865266779b5df3f8e006517ea9926d48d5fd))
* **deps:** update dependency to.wetransform:gradle-semantic-release-version to v2.1.3 ([133d3ea](https://github.com/wetransform/gradle-conventions/commit/133d3eaacff0eedbf8b4bb84048c2f5b81baf202))

## [2.1.0](https://github.com/wetransform/gradle-conventions/compare/v2.0.0...v2.1.0) (2025-05-22)

### Features

* support publishing java-platform projects ([29d49a8](https://github.com/wetransform/gradle-conventions/commit/29d49a8afafa63cef9ef03cbd3e655b23923419a))

### Bug Fixes

* **deps:** update dependency org.ec4j.core:ec4j-core to v1.1.1 ([cc0e10f](https://github.com/wetransform/gradle-conventions/commit/cc0e10f3b7cec73c0175a80dcba454bca063143a))

## [2.0.0](https://github.com/wetransform/gradle-conventions/compare/v1.7.0...v2.0.0) (2025-05-15)

### ⚠ BREAKING CHANGES

* released under Apache license

### Miscellaneous Chores

* released under Apache license ([7906802](https://github.com/wetransform/gradle-conventions/commit/79068025d5def7d31fac7eba2a852abb2eeab4c9))

## [1.7.0](https://github.com/wetransform/gradle-conventions/compare/v1.6.0...v1.7.0) (2025-05-15)

### Features

* add Apache pre-defined license header ([968d2a1](https://github.com/wetransform/gradle-conventions/commit/968d2a14d71542bdf729da55189ac08640944aad))

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
