/*
 * Copyright 2026 wetransform GmbH
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
package to.wetransform.gradle.conventions

import java.util.function.Function
import java.util.function.Supplier

import spock.lang.Specification

class GrEclipseReposSpec extends Specification {

  def "resolves default version and Groovy-Eclipse repository from spotless"() {
    when:
    def version = GrEclipseRepos.defaultEclipseVersion()
    def url = GrEclipseRepos.greclipseRepoUrl(version)

    then:
    version ==~ /\d+\.\d+/
    url.startsWith('https://groovy.jfrog.io/')
    url.contains("e${version}")
  }

  def "lookup uses the override for the repository"() {
    when:
    def info = GrEclipseRepos.lookup('http://localhost/greclipse/',
      { '4.99' } as Supplier<String>,
      { String v -> throw new IllegalStateException('must not be called') } as Function<String, String>)

    then:
    info.failure == null
    info.eclipseVersion == '4.99'
    info.greclipseRepoUrl == 'http://localhost/greclipse/'
  }

  def "lookup reports a failure of the version lookup"() {
    when:
    def info = GrEclipseRepos.lookup(null,
      { throw new NoSuchMethodError('defaultVersion') } as Supplier<String>,
      { String v -> 'unused' } as Function<String, String>)

    then:
    info.failure == 'could not determine Groovy-Eclipse P2 repositories (java.lang.NoSuchMethodError: defaultVersion)'
  }

  def "lookup reports a failure of the repository lookup"() {
    when:
    def info = GrEclipseRepos.lookup(null,
      { '4.41' } as Supplier<String>,
      { String v -> throw new IllegalStateException('model changed') } as Function<String, String>)

    then:
    info.failure == 'could not determine Groovy-Eclipse P2 repositories (java.lang.IllegalStateException: model changed)'
  }

  def "lookup reports a failure without message"() {
    when:
    def info = GrEclipseRepos.lookup(null,
      { throw new NullPointerException() } as Supplier<String>,
      { String v -> 'unused' } as Function<String, String>)

    then:
    info.failure == 'could not determine Groovy-Eclipse P2 repositories (java.lang.NullPointerException)'
    SpotlessPlugin.lookupFailureWarning(info.failure) ==
      'Spotless: could not determine Groovy-Eclipse P2 repositories (java.lang.NullPointerException), using download.eclipse.org without health check'
  }
}
