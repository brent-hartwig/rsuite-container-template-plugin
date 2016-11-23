// This global variable is an instance of the create product handler.  We're passing in the global variable's name
// to getInstance().  That code uses it to call the instance's methods, leaving open the possibility of having 
// multiple instances at the same time.  To simplify deployment and use, we're declaring just one here.  When the
// need arises for more, something else can be done.
var createProductHandlerInstance = createProductHandler.getInstance("createProductHandlerInstance");

// This "listens" for the presence of the dummy form element in the form handler that we will use to
// provide a non-ember form UX. initialize() is not a jQuery OOTB function but is provided by a library - jquery-initialize.js
// - which essentially wraps MututionObservers.form-dialog-dialog
$(".createProductReport").initialize(function() {
    createProductHandlerInstance.overrideForm();
});
