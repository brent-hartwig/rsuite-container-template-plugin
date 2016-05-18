# rsuite-container-wizard-plugin
Configure a wizard-like GUI to gather user input which is used to create an RSuite container and contents thereof.

As of 5 May 16, this wizard isn't yet completely operational.  We expect to have a working version by 20 May 16.  

## Wizard Configuration
The wizard is driven by a configuration file.  The configuration file must be accessible by an RSuite alias, and be a {http://www.rsicms.com/rsuite/ns/conf/container-wizard}:container-wizard-conf element.

The configuration file breaks into the following three sections.

A GUI has not been provided to edit the wizard configuration file.

### Page Configuration
The `pages` element may be used to string together forms.  These may be RSuite utility forms, identified by form ID, or forms provided by RSuite CMS UI actions.  Forms may be configured to have "sub" pages; such forms will be requested multiple times, enabling the system to use the same form to collect different information from the user, before moving on to the next page.

This project includes one CMS UI action capable of processing the MO portion of the container contents configuration.  Thanks to @Fordi for his work on this action and form.  It may be incorporated into the wizard by including `<page action-id="rsuite-container-wizard-plugin:wizardPage" sub-pages="1"/>` in the `<pages>` element.

Sample:

```
<pages>
    
    <page form-id="acme-form-create-product-page-1" sub-pages="0"/>
    <page action-id="rsuite-container-wizard-plugin:wizardPage" sub-pages="1"/>
    <page form-id="acme-form-create-product-page-3" sub-pages="0"/>
    
</pages>
```

At present, forms always submit to this project's invoke container wizard web service; this may need to change in the future.  We're also looking to fully implement back functionality as well as an end-of-wizard confirmation prompt.

### Container Contents Configuration
Within the `primary-container` element, one may configure zero or more child containers and XML MOs, in any order.

For each child container, one may specify the name, type, and ACL.

For each MO, one may specify a display name, the template's layered metadata value (used to find the templates), whether it is required, and whether more than one is allowed.

Sample:

```
<primary-container type="publication" acl-id="non-support-containers" default-acl-id="mo">
    
    <xml-mo-conf name="Cover Page" template-lmd-value="DitaCoverPage" required="1" multiple="0"/>
    <xml-mo-conf name="Introduction" template-lmd-value="DitaIntroduction" required="1" multiple="0"/>
    <xml-mo-conf name="Background" template-lmd-value="DitaBackground" required="1" multiple="0"/>
    <xml-mo-conf name="Discovery" template-lmd-value="DitaDiscovery" required="1" multiple="1"/>
    <xml-mo-conf name="Conclusions" template-lmd-value="DitaConslusions" required="0" multiple="0"/>
    <xml-mo-conf name="Supplementary" template-lmd-value="DitaSupplementary" required="0" multiple="1"/>
    
    <container-conf type="support" name="Supporting Documentation" acl-id="support-container"/>
    <container-conf type="folder" name="Images" acl-id="non-support-containers"/>
    
</primary-container>
```

### ACLs
The `acls` element may be used to define the ACL to apply to the main container, its child containers, and its MOs.  An ACL may be defined once and used multiple times.  Use the `*acl-id` attribute on and within the `primary-container` element.

Sample:

```
<acls>
    <acl id="non-support-containers">
        <ace project-role="SMEs" content-permissions="list, view, edit"/>
        <ace project-role="Reviewers" content-permissions="list, view"/>
        <ace project-role="Managers" content-permissions="list, view, edit, delete"/>
    </acl>
    <acl id="support-container">
        <ace project-role="SMEs" content-permissions="list, view, edit"/>
        <ace project-role="Reviewers" content-permissions="list, view, edit"/>
        <ace project-role="Managers" content-permissions="list, view, edit, delete"/>
    </acl>
    <acl id="mo">
        <ace project-role="SMEs" content-permissions="list, view, edit, delete"/>
        <ace project-role="Reviewers" content-permissions="list, view, edit"/>
        <ace project-role="Managers" content-permissions="list, view, edit, delete"/>
    </acl>
</acls>
```

## JAXB
This project's build script includes the `jaxb` task to generate JAXB classes from the wizard configuration's XML Schema.  This task should be executed after modifying `containerWizardConf.xsd`. 

## Deployment
Two additional deployment steps are required when deploying this project's plugin:

1. Configure RSuite to allow the "XmlTemplateType" LMD on all XML MOs that could be a template.  Allow any string value.
2. Load the wizard's XML Schema into RSuite.  The following is an Ant target that illustrates how this may be scripted.  It presumes this project's plugin has already been downloaded, and the build script is already able to execute Groovy scripts.

```
<property name="container.wizard.tmp.home" value="${target.home}/container-wizard-tmp"/>

<target name="deploy-container-wizard-plugin-dependencies"
	    description="Run the container wizard plugin's Groovy script(s).">
	<!-- Extract contents of container wizard plugin to empty temp dir. -->
    <delete dir="${container.wizard.tmp.home}" failonerror="yes"/>
	<unzip src="${lib.home}/rsuite-container-wizard-plugin.jar" dest="${container.wizard.tmp.home}"/>

	<!-- Run the following script(s) located within. -->
    <run-groovy-script dir="${container.wizard.tmp.home}/scripts/groovy" script="InitSchemas.groovy" />
</target>
```

## Unit Tests
Unit tests are to be within the `test.com.rsicms.rsuite.containerWizard` package.  Version 0.9.2 includes a Hello World sample.  The sample was introduced to prove a client project is able to execute this community plugin's tests from within its test harness.  Below is a sample TestNG configuration file, incorporating the unit tests of community projects it uses (Only one at the time.).

```
<suite name="Java Community Unit Tests" verbose="1">
  
  <test name="Java Community Unit Tests" junit="true">
    <packages>
      <package name="test.com.rsicms.rsuite.containerWizard.*" />
    </packages>
  </test>

</suite>
```

