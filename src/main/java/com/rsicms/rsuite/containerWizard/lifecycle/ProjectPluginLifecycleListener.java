package com.rsicms.rsuite.containerWizard.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.Plugin;
import com.reallysi.rsuite.api.extensions.PluginLifecycleListener;

/**
 * RSuite extension point when RSuite loads or unloads the plugin.
 */
public class ProjectPluginLifecycleListener implements PluginLifecycleListener {

  private static Log log = LogFactory.getLog(ProjectPluginLifecycleListener.class);

  // public final static String NAMESPACE_CONTAINER_WIZARD =
  // "http://www.rsicms.com/rsuite/ns/conf/container-wizard";

  @Override
  public void start(ExecutionContext context, Plugin plugin) {
    log.info("RSuite is loading the " + plugin.getId());
    // loadSchemas(context, plugin);
  }

  @Override
  public void stop(ExecutionContext context, Plugin plugin) {
    log.info("RSuite is unloading the " + plugin.getId());
  }

  /**
   * Load or update the schemas and MO defs this plugin is responsible for.
   * 
   * @param context
   * @param plugin
   */
  protected void loadSchemas(ExecutionContext context, Plugin plugin) {

    try {

      // SchemaService schemaService = context.getSchemaService();

      // FIXME: Right now, we have to load via Groovy as this way doesn't rewrite the reference from
      // containerWizardConf.xsd to rsuite.xsd.
      //
      // // Load schemas
      // log.info("Loading schemas...");
      // SchemaInfo rsuiteSchemaInfo = schemaService.loadXMLSchema("rsuiteAtts", true,
      // "http://www.rsuitecms.com/rsuite/ns/metadata", "rsuite.xsd", IOUtils
      // .toByteArray(plugin.getResourceAsStream("/doctypes/containerWizardConf/rsuite.xsd")));
      // SchemaInfo wizardSchemaInfo = schemaService.loadXMLSchema("containerWizard", true,
      // NAMESPACE_CONTAINER_WIZARD, "containerWizardConf.xsd", IOUtils.toByteArray(
      // plugin.getResourceAsStream("/doctypes/containerWizardConf/containerWizardConf.xsd")));
      // log.info("Schemas loaded.");

      // FIXME: RSuite 4.1.15 casts to its ManagedObjectDefinitionImpl
      // // Configure MOs
      // String wizardSchemaId = wizardSchemaInfo.getSchemaId();
      // log.info("Configure MOs for container wizard schema (ID: " + wizardSchemaId + ")...");
      // List<NamespaceDecl> namespaces = new ArrayList<NamespaceDecl>();
      // namespaces.add(new NamespaceDecl("c", NAMESPACE_CONTAINER_WIZARD));
      //
      // // Required before use.
      // ManagedObjectDefinitionImpl.setXmlApiManager(context.getXmlApiManager());
      //
      // ManagedObjectDefinition[] moDefs =
      // {new ManagedObjectDefinitionImpl(NAMESPACE_CONTAINER_WIZARD, "container-wizard-conf",
      // "string", "./@name", true, true, true, wizardSchemaId, null, null)};
      //
      // schemaService.setManagedObjectDefinitions(wizardSchemaId, namespaces, moDefs);

    } catch (Exception e) {
      log.warn("Unable to load or update the schemas associated with this plugin", e);
    }

  }

}
