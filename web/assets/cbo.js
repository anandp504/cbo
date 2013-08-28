
function setActionToFile(form, fileName, saveFormats)
{
    var act = form.action;
    var ix = act.lastIndexOf("/");
    if(ix >= 0)
      act = act.substring(0, ix);
    act = act + "/" + fileName + ".";
    var format = "TEXT";
    if(form.Format != null)
    {
        var formatIndex = form.Format.value;
        format = saveFormats[formatIndex];
    }
    act = act + format.toLowerCase();
    form.action = act;
}

function submitFormWithDeleteAction(formId, action,message) {

	var x=window.confirm(message)
	if (x){
		document.forms[formId].action = action;
    	document.forms[formId].submit();
	}
}
function submitFormWithAction(formId, action) {
    document.forms[formId].action = action;
    document.forms[formId].submit();
}

function submitForm(formId) {
    document.forms[formId].submit();
}

function submitFormOnEnter(formId, e) {
    var keycode;
    if (window.event) {
        keycode = window.event.keyCode;
    }
    else if (e) {
        keycode = e.which;
    }
    else {
        return true;
    }
    if (keycode == 13) {
        document.forms[formId].submit();
        return false;
    }
    else {
        return true;
    }
}
