/*
 * Copyright 2025 wetransform GmbH
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

import groovy.transform.CompileStatic

import javax.inject.Inject

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

@CompileStatic
class PluginExtension {

  private final Project project

  private final WetransformPlugin plugin

  private final ObjectFactory objectFactory

  final PluginConfig setup

  @Inject
  PluginExtension(Project project, WetransformPlugin plugin, ObjectFactory objectFactory) {
    this.project = project
    this.plugin = plugin
    this.objectFactory = objectFactory

    // use object factory so DSL augmentation is done (e.g. create methods taking closure instead of Action as argument)
    setup = objectFactory.newInstance(PluginConfig, plugin)
  }

  void repos(Action<RepoConfig> action) {
    action.execute(new RepoConfig(project))
  }

  void setup(Closure closure) {
    /*
     * Note: The object factory that creates this object is responsible for creating this method.
     * So there should not be the need to create this method manually.
     * However the behavior of the generated method seems to be different in that it does not
     * resolve against the delegate first and it tries to resolve against the extension which fails.
     *
     * Unclear why this is the behavior. This article shows a similar example and there everything
     * works as intended:
     * https://dev.to/autonomousapps/gradle-plugins-and-extensions-a-primer-for-the-bemused-51lp
     */

    def cl = closure.clone() as Closure
    cl.delegate = setup
    cl.resolveStrategy = Closure.DELEGATE_FIRST

    cl.call(setup)

    setup()
  }

  void setup(Action<PluginConfig> action) {
    config(action)

    setup()
  }

  void setup() {
    plugin.setup(project)

    def spotless = new SpotlessPlugin(objectFactory, plugin.spotlessConfig, plugin)
    spotless.apply(project)

    def publish = new PublishPlugin(objectFactory, plugin.publishConfig, plugin)
    publish.apply(project)
  }

  void config(Action<PluginConfig> action) {
    action.execute(setup)
  }
}
