/*
 * Instead of using Ember to create custom form controls, we'll do it using pretty vanilla JS (making use of jQuery).
 * There is a "dummy" form handler that gets called by the create product wizard. At the bottom of this file is a piece
 * of code that "listens" for a form input item of a certain class to appear (it does this by using the jquery-initialize
 * js library, which is essentially a wrapper for MutationObject). When the "listener" is triggered, it kicks off
 * a process which queries create-product's web services and displays a form to the user. The user clicks to continue
 * and the process repeats until done as defined by the create-product web service.
 */
var createProductHandler = (function () {
    "use strict";

    var instance;
    var confAlias;
    var sectionTypeIdx;
    var nextPageIdx;
    var nextSubPageIdx;
    var newNextSubPageIdx;
    var containerWizard;
    var retrievedSectionOptions;
    var instantiatedHandlerName;

    // Helper function; useful for debugging, will take XML node and convert to string
    function serializeXml(xml) {
        return new XMLSerializer().serializeToString(xml);
    }

    // Helper function; useful for debugging will write out to console a JS object; DO NOT CALL on event objects
    // you'll end up with an infinite loop
    function iterate(obj) {
        // watch for objects we've already iterated so we won't end in endless cycle
        // for cases like var foo = {}; foo.bar = foo; iterate(foo);
        var walked = [];
        var stack = [{obj: obj, stack: ''}];
        while (stack.length > 0) {
            var item = stack.pop();
            var obj = item.obj;
            for (var property in obj) {
                if (obj.hasOwnProperty(property)) {
                    if (typeof obj[property] == "object") {
                        // check if we haven't iterated through the reference yet
                        var alreadyFound = false;
                        for (var i = 0; i < walked.length; i++) {
                            if (walked[i] === obj[property]) {
                                alreadyFound = true;
                                break;
                            }
                        }
                        // new object reference
                        if (!alreadyFound) {
                            walked.push(obj[property]);
                            stack.push({obj: obj[property], stack: item.stack + '.' + property});
                        }
                    }
                    else {
                        console.log(item.stack + '.' + property + "=" + obj[property]);
                    }
                }
            }
        }
    }

    // Wrapper function that returns the number of items in an object
    function getNumObjectItems(obj) {
        return Object.keys(obj).length;
    }


    // Next two functions essentially get the computed CSS value; not used currently
    function css2json(css) {
        var s = {};
        if (!css) return s;
        if (css instanceof CSSStyleDeclaration) {
            for (var i in css) {
                if ((css[i]).toLowerCase) {
                    s[(css[i]).toLowerCase()] = (css[css[i]]);
                }
            }
        } else if (typeof css == "string") {
            css = css.split("; ");
            for (var i in css) {
                var l = css[i].split(": ");
                s[l[0].toLowerCase()] = (l[1]);
            }
        }
        return s;
    }

    function css(a) {
        var sheets = document.styleSheets, o = {};
        for (var i in sheets) {
            var rules = sheets[i].rules || sheets[i].cssRules;
            for (var r in rules) {
                if (a.is(rules[r].selectorText)) {
                    o = $.extend(o, css2json(rules[r].style), css2json(a.attr('style')));
                }
            }
        }
        return o;
    }

    // Get the current user's session key from local storage
    function getSkey() {
        var skey = localStorage.getItem('rsuite-session');
        skey = skey.replace('"', '');
        skey = skey.replace('"', '');
        return skey;
    }

    // Call the web service that returns information on a given template type (e.g., AuditReportCover)
    function doTemplateInfoCall(xmlTemplateType) {
        var skey = getSkey();
        var response;
        var apiUri = "api/@pluginId@-ws-get-template-info";
        var url = window.location.protocol + '//' + window.location.host + "/rsuite/rest/v1/" + apiUri + "?skey=" + skey + "&xmlTemplateType=" + xmlTemplateType;
        var success = function (apiResponse) {
            // not doing anything here
        };
        var complete = function (apiResponse) {
            if (apiResponse.hasOwnProperty("responseText")) {
                response = apiResponse.responseText;
            }
        };
        $.ajax({
            url: url,
            type: "GET",
            datatype: "text",
            async: false,
            cache: false,
            success: success,
            error: function (e) {  console.log("Error! ["); iterate(e); },
            complete: complete
        });
        // console.log("For URL [" + url + "] RestResponse [" + response + "]");
        return response;

        /*
         Expect a response along these lines:
         <?xml version="1.0" encoding="UTF-8"?>
         <RestResult>
         <response type="list">
         <map>
         <name type="string">Cover Page: Basic - Sample...</name>
         <moid type="string">16737</moid>
         </map>
         <map>
         <name type="string">Cover Page: Deluxe - Sampl...</name>
         <moid type="string">16750</moid>
         </map>
         </response>
         <actions type="list"/>
         </RestResult>

         */
    }

    // Call the web service that returns information on the next section to deal with (based off of nextSubPageIdx)
    function doSectionInfoCall() {
        var skey = getSkey();
        var response;
        var apiUri = "api/@pluginId@-ws-get-section-type-info";
        var url = window.location.protocol + '//' + window.location.host + "/rsuite/rest/v1/" + apiUri + "?skey=" + skey + "&confAlias=" + confAlias + "&nextSubPageIdx=" + nextSubPageIdx;
        var success = function (apiResponse) {
            // not doing anything here
        };
        var complete = function (apiResponse) {
            if (apiResponse.hasOwnProperty("responseText")) {
                response = apiResponse.responseText;
            }
        };
        $.ajax({
            url: url,
            type: "GET",
            datatype: "text",
            async: false,
            cache: false,
            success: success,
            error: function (e) {  console.log("Error! ["); iterate(e); },
            complete: complete
        });
        //console.log("For URL [" + url + "] RestResponse [" + response + "]");
        return response;

        /* Expect response to be along these lines:
         <RestResult>
         <response type="list">
         <map>
         <isRequired type="string">true</isRequired>
         <string name="xmlTemplateType">AuditReportCover</string>
         <name type="string">Cover</name>
         <mayRepeat type="string">false</mayRepeat>
         </map>
         </response>
         <actions type="list"/>
         </RestResult>
         */
    }

    // Get the section options for a given next sub page index
    function getSectionOptions() {
        var sectionResponse = doSectionInfoCall();
        var sectionOptions = {};
        // build the object from the sectionResponse; will mirror the result from the web service
        // For some reason this is producing an object of 0.0 instead of just 0 (i.e., we should have 0.name not 0.0.name for the first section)
        $(sectionResponse).find("map").each(function () {
            var sectionId = getNumObjectItems(sectionOptions);
            sectionOptions[sectionId] = {};
            sectionOptions[sectionId].isRequired = $(this).find("isRequired").text();
            sectionOptions[sectionId].xmlTemplateType = $(this).find("string[name=xmlTemplateType]").text();
            sectionOptions[sectionId].name = $(this).find("name").text();
            sectionOptions[sectionId].mayRepeat = $(this).find("mayRepeat").text();
        });
        return sectionOptions;
    }

    // Get the template options for a given section
    function getXmlTemplateOptions(sectionOption) {
        var templateWebServiceResponse = doTemplateInfoCall(sectionOption);
        var templateOptions = {};
        $(templateWebServiceResponse).find("map").each(function () {
            var templateId = $(this).find("moId").text();
            templateOptions[templateId] = $(this).find("name").text();
        });
        return templateOptions;
    }

    // Sets the value of a hidden element, not sure if the fact that we do this means there is a bigger issue
    // but everything is working
    function setNextSubPageIdxHiddenElem(nextSubPageIdxValueToSet) {
        $("input[name=nextSubPageIdx]").val(nextSubPageIdxValueToSet);
    }

    // Remove a template input row from the form (i.e., user clicked trash can)
    function removeTemplateInputRow(templateOptionDivId) {
        $("#" + templateOptionDivId).remove();
    }

    /* Adds the inputs - select and text in put - for creating a new section, may be multiple if mayRepeat is true
     */
    function addTemplateInput(sectionOption, templateOptions, mayRepeat) {
        var objectKey;
        var templateSelectElem;
        var optionElem;
        var templateTitleInputElem;
        var templateNumber = 0; // how many of these is the user trying to create
        var spanForDelete = null;
        var divForInput = $(document.createElement("div"));

        if (mayRepeat) {
            // multiple fields allowed, so count the number of existing ones; web service seems to expect the provided
            // values to be zero indexed, so we do not add one
            templateNumber = $(".inputTemplateType").length;
            if (templateNumber > 0) {
                spanForDelete = $(document.createElement("span"));
                spanForDelete.html("<img src='/rsuite-cms/plugin/gao-newblue/cmsui/images/16/delete.png' title='Delete' alt='Delete' />");
                spanForDelete.attr("onclick", instantiatedHandlerName + ".removeTemplateInputRow('template" + templateNumber + "')");
                spanForDelete.css("cursor", "pointer");
            }
        }

        divForInput.attr("id", "template" + templateNumber);

        if (templateOptions !== undefined && templateOptions !== null) {
            // templateOptions looks good, so we are likely doing this from the section's initialization

            // Create the select element
            templateSelectElem = $(document.createElement("select"));
            // Create the input element for the new section's title
            templateTitleInputElem = $(document.createElement("input"));
            templateTitleInputElem.attr("class", "inputTemplateType");
            templateTitleInputElem.attr("name", "title[" + templateNumber + "]");

            // Not sure why we have to do this, for some reason the templateOptions object has a default 0 key with all added
            // ones as parameter; this same technique works in the NBE code base without issue; some weird Ember object prototype
            // extension? Would be supremely weird
            templateOptions = templateOptions["0"];

            // Further setup the select dropdown
            templateSelectElem.attr("class", "selectTemplateType");
            templateSelectElem.attr("name", "templateId[" + templateNumber + "]");

            // Create the options for the select element
            for (objectKey in templateOptions) {
                if (templateOptions.hasOwnProperty(objectKey)) {
                    optionElem = $(document.createElement("option"));
                    optionElem.val(objectKey);
                    optionElem.html(templateOptions[objectKey]);
                    templateSelectElem.append(optionElem);
                }
            }
        } else {
            // templateOptions null or undefined; meaning user had to have clicked the icon to add another of the same section type
            templateSelectElem = $(".selectTemplateType").first().clone();
            templateSelectElem.attr("name", "templateId[" + templateNumber + "]");
            templateTitleInputElem = $(".inputTemplateType").first().clone();
            templateTitleInputElem.attr("name", "title[" + templateNumber + "]");
        }

        // Stitch the HTML together
        divForInput.append("<div style='margin-top:20px;'><strong>Template</strong> " + templateSelectElem.prop("outerHTML") + "</div>");
        divForInput.append("<div><strong>Title</strong> " + templateTitleInputElem.prop("outerHTML"));

        if (spanForDelete !== null) {
            divForInput.append(spanForDelete.prop("outerHTML"));
        }

        divForInput.append("</div>");

        // Insert the html into the form
        $("#createProductTemplateHolder").append(divForInput.prop("outerHTML"));
        
        // Scroll to the bottom of the div containing these fields
        $("div.fieldset").scrollTop($("div.fieldset").innerHeight());
    }

    // After choosing a section type (either automatically via next or choosing a different option in a dropdown,
    // populate the template choices a user can choose
    function populateTemplateChoices() {
        var sectionOption = $("#selectSectionType").val(); // xmlTemplateType value

        // wipe out any existing template entries (i.e., user has changed section options)
        $("#createProductTemplateHolder").html("");

        // Iterate through section types so we can figure out if we need to increment the next subpage index
        // Not sure if the fact that we need to do this here means something has been missed when working with
        // the web service (or having PageNavigation.java returning us a form request rather than a
        // custom action request.
        $("#selectSectionType").find("option").each(function (idx, elem) {
            if ($(elem).val() === sectionOption) {
                // Always increment from the original next subpage index value that started this form
                newNextSubPageIdx = parseInt(nextSubPageIdx, 10) + idx;
            }
        });

        // Get template options from the web service
        var templateOptions = $(getXmlTemplateOptions(sectionOption));

        var objectKey;
        var curOption;
        var foundXmlTemplateType = false;
        var mayRepeat = false;
        var spanForRepeat = null;

        for (objectKey in retrievedSectionOptions) {
            if (retrievedSectionOptions.hasOwnProperty(objectKey)) {
                curOption = retrievedSectionOptions[objectKey];
                if (curOption.hasOwnProperty("xmlTemplateType") && curOption.xmlTemplateType === sectionOption) {
                    foundXmlTemplateType = true;
                    if (curOption.hasOwnProperty("mayRepeat") && curOption.mayRepeat === "true") {
                        mayRepeat = true;
                    }
                    break;
                }
            }
        }

        if (!foundXmlTemplateType) {
            // no matching xmlTemplateType found, I guess we will only allow one
            mayRepeat = false;
        }

        addTemplateInput(sectionOption, templateOptions, mayRepeat);

        if (mayRepeat) {
            spanForRepeat = $(document.createElement("span"));
            spanForRepeat.html("Add another <img src='/rsuite-cms/plugin/gao-newblue/cmsui/images/16/add.png' title='Add' alt='Add' />");
            spanForRepeat.attr("onclick", instantiatedHandlerName + ".addTemplateInput('" + sectionOption + "', null, true)");
            spanForRepeat.css("cursor", "pointer");
            $("#createProductTemplateRepeatHolder").html(spanForRepeat.prop("outerHTML"));
        }

        // increment nextSubPageIdx by one
        newNextSubPageIdx = parseInt(newNextSubPageIdx, 10) + 1;
        setNextSubPageIdxHiddenElem(newNextSubPageIdx);
    }

    // The entry point
    function overrideForm() {

        var formName = ".@pluginId@-create-product-template-form";
        
        var sectionSelectElem = $(document.createElement("select"));

        var objectKey;
        var optionElem;

        var sectionSelectHTML = "<div><strong>Section type</strong> ";

        var defaultSectionOption = "";
        var firstSectionOptionSeen = false;

        // Retrieve values from hidden form inputs
        confAlias = $("input[name=confAlias]").val();
        sectionTypeIdx = $("input[name=sectionTypeIdx]").val();
        nextPageIdx = $("input[name=nextPageIdx]").val();
        nextSubPageIdx = $("input[name=nextSubPageIdx]").val();
        containerWizard = $("input[name=containerWizard]").val();

        // Change the text of the button from "Submit" to "Next"
        var buttonLabel = $(formName + " button label");
        if (buttonLabel.text().toLowerCase().indexOf("submit") >= 0) {
            buttonLabel.text("Next");
        }

		// Change title of form if we are in execution mode AddXmlMo
		if ($("input[name='executionMode']").val() === "AddXmlMo") {
			$(".ui-dialog-title").html("Add Section");
		}
        // Not sure how to add a cancel button. Oh well. Users can use the x in the top right.

        // perform a sectionInfo call
        retrievedSectionOptions = $(getSectionOptions(confAlias, nextSubPageIdx));

        // Not sure why we have to do this, for some reason the sectionOptions object has a default 0 key with all added
        // ones as parameter; this same technique works in the NBE code base without issue; some weird Ember object prototype
        // extension? Would be supremely weird
        retrievedSectionOptions = retrievedSectionOptions["0"];

        sectionSelectElem.attr("id", "selectSectionType");

        for (objectKey in retrievedSectionOptions) {
            if (retrievedSectionOptions.hasOwnProperty(objectKey)) {
                optionElem = $(document.createElement("option"));
                optionElem.val(retrievedSectionOptions[objectKey].xmlTemplateType);
                optionElem.html(retrievedSectionOptions[objectKey].name);
                sectionSelectElem.append(optionElem);
                if (!firstSectionOptionSeen) {
                    defaultSectionOption = retrievedSectionOptions[objectKey].xmlTemplateType;
                }
                firstSectionOptionSeen = true;
            }
        }

        sectionSelectElem.attr("onchange", instantiatedHandlerName + ".populateTemplateChoices();");

        var spanForRefreshTemplateOptions = $(document.createElement("span"));
        spanForRefreshTemplateOptions.attr("onclick", instantiatedHandlerName + ".populateTemplateChoices();");
        spanForRefreshTemplateOptions.html(" <img src='/rsuite-cms/plugin/rsuite-cms-theme/images/rsuite/action/16/search-sidebar-execute.png' title='Refresh Template Choices' alt='Refresh Template Choices' />");
        spanForRefreshTemplateOptions.css("cursor", "pointer");
        sectionSelectHTML += sectionSelectElem.prop("outerHTML") + spanForRefreshTemplateOptions.prop("outerHTML") + "</div>";

        // hide the OOTB submit button; we will unhide this later
        //emberSubmitBtn.css("display", "none");
        // Add our own button
        // emberSubmitBtn.after("<button id='createProductButton' onclick='submitSectionPage();'>Next</button>");

        // Set the css values so our new button will look the same
        //$("#createProductButton").css(emberSubmitBtnCss);

        //$("#createProductButton").click(function(event) {
        //    // The CMS UI listens for <button>s to be clicked, so prevent it from doing so
        //    event.preventDefault();
        //    event.stopImmediatePropagation();
        //});

        $(".createProductReport").prop("outerHTML", sectionSelectHTML + "<div id='createProductTemplateHolder' style='overflow-y: auto;'></div><div id='createProductTemplateRepeatHolder'></div>");

        populateTemplateChoices();
        
        // After we have our initial height, lock it in as the max such that the scroll bar will appear should the user 
        // user add another set of controls for the same template type.  Else, the bottom of the form can be pushed off
        // the screen.  This 'fix' breaks the user's ability to manually resize the form, whereby the form will use 
        // additional space provided by the user.
        var height = $(formName).height();
        if (height > 0) {
            console.log("Setting form's maximum height to " + height);
            $(formName).css('max-height', height);
        } else {
            console.log("Told form height is 0; electing not to set a max height.");
        }
    }

    function init(instantiatedHandlerNameParam) {
        instantiatedHandlerName = instantiatedHandlerNameParam;

        return {
            // public methods and properties
            overrideForm: overrideForm,
            populateTemplateChoices: populateTemplateChoices,
            addTemplateInput: addTemplateInput,
            removeTemplateInputRow: removeTemplateInputRow
        };
    }


    return {
        getInstance: function (instantiatedHandlerNameParam) {
            if (!instance) {
                instance = init(instantiatedHandlerNameParam);
            }

            return instance;
        }

    };

}());
