// Groovy script to import and configure this project's schemas.

import com.reallysi.rsuite.admin.importer.*
import com.reallysi.rsuite.client.api.*

// -----------------------------------------------------------------------

def projectDir = new File(scriptFile.absolutePath).parentFile.parentFile.parentFile
def doctypesDir = new File(projectDir, "doctypes");

def namespaceDecls

println " + [INFO] Logging into RSuite...";

rsuite.login();

// --------- Container Wizard ----------

schemaType = "XMLSchema";
schemaDir = new File(doctypesDir, "containerWizardConf");
schemaName = "containerWizardConf.xsd";
publicId = null;
previewXsltFile = null;

moDefList = [ 
  new ManagedObjectDefinition([
    name:             '{http://www.rsicms.com/rsuite/ns/conf/container-wizard}:container-wizard-conf',
    displayNameXPath: './@name',
    versionable:      'true',
    browsable:        'true',
    reusable:         'true',
  ]),
  new ManagedObjectDefinition([
    name:             '{http://www.rsicms.com/rsuite/ns/conf/container-wizard}:pages',
    displayNameXPath: 'concat(\'Pages (\', count(./element()), \')\')',
    versionable:      'true',
    browsable:        'true',
    reusable:         'true',
  ]),
  new ManagedObjectDefinition([
    name:             '{http://www.rsicms.com/rsuite/ns/conf/container-wizard}:primary-container',
    displayNameXPath: 'concat(\'Contents of Primary Container (\', count(./element()), \')\')',
    versionable:      'true',
    browsable:        'true',
    reusable:         'true',
  ]),
  new ManagedObjectDefinition([
    name:             '{http://www.rsicms.com/rsuite/ns/conf/container-wizard}:acls',
    displayNameXPath: 'concat(\'ACLs (\', count(./element()), \')\')',
    versionable:      'true',
    browsable:        'true',
    reusable:         'true',
  ])
]

namespaceDecls = (String[])["c=http://www.rsicms.com/rsuite/ns/conf/container-wizard"];
            
loadSchema(schemaType, schemaDir, schemaName, publicId, schemaName, previewXsltFile, namespaceDecls, moDefList);

//-----------------------------------------------------------------------

rsuite.logout();
println " + [INFO] Done."

//-----------------------------------------------------------------------

def loadSchema(schemaType, schemaDir, schemaName, publicId, systemId, htmlPreviewXsltFile, namespaceDecls, moDefList) 
{

  println "";

  println " + [INFO] loadSchema(): Loading \"" + schemaName + "\"";

  def schemaId 

  def schemaFile = new File(schemaDir, schemaName);

  if (schemaType == "DTD") {

    def schemaSrc = new SchemaInputSource(schemaFile, systemId, publicId);

    def importer = importerFactory.generateImporter(schemaType, schemaSrc);

    schemaId = importer.importDtd()

  } else {

    def schemaSrc = new SchemaInputSource(schemaFile);

    def importer = importerFactory.generateImporter(schemaType, schemaSrc);

    schemaId = importer.execute()

  }

    

  if (moDefList != null) {

    println " + [INFO] loadSchema(): Setting managed object definitions...";

    rsuite.setManagedObjectDefinitions(schemaId, false, namespaceDecls, moDefList)

  }

  if (htmlPreviewXsltFile != null) {

    println " + [INFO] loadSchema(): Setting preview style sheet to \"" + htmlPreviewXsltFile.name + "\"...";

    rsuite.loadStylesheetForSchema(schemaId, htmlPreviewXsltFile)

  }

  return schemaId;

}

// -----------------------------------------------------------------------
