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
Within the `primary-container` element, one may configure static metadata name-value pairs and zero or more child containers and XML MOs, in any order.

For each child container, one may specify the display name, type, and ACL.

For each MO, one may specify a display name, local (element) name, the template's layered metadata value (used to find the templates), whether it is required, and whether more than one is allowed.

Please review the schema for more details.

Sample:

```
<primary-container type="publication" acl-id="non-support-containers" default-acl-id="mo">
    
    <metadata-conf>
        <name-value-pair name="Hello" value="World"/>
        <name-value-pair name="Status" value="New"/>
    </metadata-conf>

    <xml-mo-conf display-name="Cover Page" local-name="cover_page" template-lmd-value="DitaCoverPage" required="1" multiple="0"/>
    <xml-mo-conf display-name="Introduction" local-name="intro" template-lmd-value="DitaIntroduction" required="1" multiple="0"/>
    <xml-mo-conf display-name="Background" local-name="background" template-lmd-value="DitaBackground" required="1" multiple="0"/>
    <xml-mo-conf display-name="Discovery" local-name="discovery" template-lmd-value="DitaDiscovery" required="1" multiple="1"/>
    <xml-mo-conf display-name="Conclusions" local-name="conclusions" template-lmd-value="DitaConclusions" required="0" multiple="0"/>
    <xml-mo-conf display-name="Supplementary" local-name="supplementary" template-lmd-value="DitaSupplementary" required="0" multiple="1"/>
    
    <container-conf type="support" display-name="Supporting Documentation" acl-id="support-container"/>
    <container-conf type="folder" display-name="Images" acl-id="non-support-containers"/>
    
</primary-container>
```

### ACLs
The `acls` element may be used to define the ACL to apply to the main container, its child containers, and its MOs.  An ACL may be defined once and used multiple times.  Use the `*acl-id` attribute on and within the `primary-container` element.

ACL configuration is limited to project-specific ACLs.  The container ID plus the value of the `project-role` attribute are used in the role name.  New roles are created automatically.

The user creating the container is automatically granted the container's "AIC_AD" role.  Unfortunately, for now, this is hard-coded.  Starting in version 1.0.4, immediately after granting this role, the "user granted role" event is fired.  For more information on this event, see the rsuite-event-lib project.

Version 0.9.7 includes a hard-coded project-agnostic role, "Viewers".  It provides the list and view content permissions to users with the "Viewers" role on any container the wizard sets the ACL on, unless the ACL ID is "feedback-container".  Should this be adopted by a second project, we should take the time to make this configurable (i.e., alternative attribute to `project-role`). 

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

## Execution Mode
The default execution mode is to create a new container, referencing containers and MOs.  There is also an execution mode to add MOs to an existing container.  The execution mode may be specified by the "executionMode" web service parameter.  Supported values are "CREATE\_CONTAINER" (default) and "ADD\_XML\_MO".

Below are example context menu items for adding MOs to an existing container.  Notes:

1. The "operationName" parameter may be used to configure the operation's display name (both modes).
2. The "insertionPosition" parameter is only applicable to the ADD\_XML\_MO mode.  "BEFORE" and "AFTER" (default) are the supported values.
3. The "nextPageIdx" parameter value should align with a page configured to the "rsuite-container-wizard-plugin:wizardPage" action.

```
<menuItem id="acme:addSectionAbove">
    <type>action</type>
    <actionName>rsuite:invokeWebservice</actionName>
    <label>Add section above</label>
    <property name="remoteApiName" value="rsuite-container-wizard-plugin-ws-invoke-wizard"/>
    <property name="serviceParams.confAlias" value="Sample Product Configuration" />
    <property name="serviceParams.operationName" value="Add Section" />
    <property name="serviceParams.executionMode" value="ADD_XML_MO" />
    <property name="serviceParams.insertionPosition" value="BEFORE" />
    <property name="serviceParams.nextPageIdx" value="2" />
    <property name="rsuite:icon" value="add" />
</menuItem>
<menuItem id="acme:addSectionAbove">
    <type>action</type>
    <actionName>rsuite:invokeWebservice</actionName>
    <label>Add section below</label>
    <property name="remoteApiName" value="rsuite-container-wizard-plugin-ws-invoke-wizard"/>
    <property name="serviceParams.confAlias" value="Sample Product Configuration" />
    <property name="serviceParams.operationName" value="Add Section" />
    <property name="serviceParams.executionMode" value="ADD_XML_MO" />
    <property name="serviceParams.insertionPosition" value="AFTER" />
    <property name="serviceParams.nextPageIdx" value="2" />
    <property name="rsuite:icon" value="add" />
</menuItem>
``` 

## Retaining User Input
Each time the wizard's main web service is consumed, it retains expected user-input.  The following documents what the web service looks for.

### Container Name
The new container name should be specified by a parameter named "containerName".

### Container Layered Metadata

#### Dynamic Values and Optional Validation
Layered metadata values may be specified by parameters whose names begin with "lmd".  That prefix is stripped off before the metadata is set.  For instance, a parameter named "lmdMyName" with a value of "My Value" instructs the wizard to set the "MyName" layered metadata to "My Value" on the new container.

At this time, this metadata is only applied to the main container created by the plugin, as opposed to any other object created by the wizard.

It's possible to validate some parameter values.  Our plan is to make the validation logic configurable.  We're not there yet.  There's one hard-coded test against "lmdJobCode".  There's also a second yet generic test.  If the parameter being tested is accompanied by a second with the same name plus "-Verify", the wizard will throw an exception if the two values do not match.  This is a way to ask the user to enter a value twice, and only proceed if the values match.  For the verification arguments, do not include the "lmd" parameter name prefix.  For example, to add a verified piece of layered metadata named "JobCode", configure a form (wizard page) to include parameters named "lmdJobCode" and "JobCode-Verify". 

#### Static Values
As discussed above, the primary container contents configuration may include metadata configuration.  This is a way for the container wizard configuration file to instruct the wizard to add static name-value pairs of layered metadata to the primary container.  The same configuration (metadata-conf) is supported within non-primary container configuration nodes too (container-conf).

### Managed Objects
The wizard also looks for parameters named "templateId".  For each one it finds, the wizard keeps track of the page it was specified on, the template ID, and the value of the "title" parameter (optional).  Later this information is used to create new managed objects.  A future version of this wizard may also be able to use this information to support "back" and confirmation features.

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
JUnit tests were selected for this project, so as not to impose TestNG on others.  Yet, as shown below, these tests may be executed by TestNG.

Unit tests are to be within the `test.com.rsicms.rsuite.containerWizard` package.

Version 0.9.3 includes 33 tests, that begin to provide coverage for the wizard's code base.  These are being successfully executed by a client project, from within that project's test harness.  Below is a sample TestNG configuration file, incorporating the unit tests of community projects it uses (Only one at the time.).  Your project may do the equivalent.  This project produces two JAR files, one for the runtime wizard, and one for its tests.

```
<suite name="Java Community Unit Tests" verbose="1">
  
  <test name="Java Community Unit Tests" junit="true">
    <packages>
      <package name="test.com.rsicms.rsuite.containerWizard.*" />
    </packages>
  </test>

</suite>
```

Per [TestNG documentation](http://testng.org/doc/migrating.html), there are two additional requirements:

1. The JUnit JAR needs to be added to TestNG's classpath
2. testng/@mode needs to be set to "mixed"

