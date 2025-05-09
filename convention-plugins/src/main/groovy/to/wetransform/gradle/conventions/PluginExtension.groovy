package to.wetransform.gradle.conventions

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

@CompileStatic
class PluginExtension {

  private final Project project

  private final WetransformPlugin plugin

  final PluginConfig setup

  @Inject
  PluginExtension(Project project, WetransformPlugin plugin, ObjectFactory objectFactory) {
    this.project = project
    this.plugin = plugin

    // use object factory so DSL augmentation is done (e.g. create methods taking closure instead of Action as argument)
    setup = objectFactory.newInstance(PluginConfig, plugin)
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
    action.execute(setup)

    setup()
  }

  void setup() {
    plugin.setup(project)

    def spotless = new SpotlessPlugin(plugin.spotlessConfig)
    spotless.apply(project)

    def publish = new PublishPlugin(plugin.publishConfig)
    publish.apply(project)
  }
}
