  // Taken from zini/doc/scripts/
  createClicked = false;

  function checkPasswordsSame (widget1, widget2) {
   //$enterTrace('checkPasswordsSame', arguments);
    if (widget1.value != widget2.value)
      {
        window.alert("Password does not match the verification field.");
        widget1.focus();
        widget1.select();
        //$exitTrace('checkPasswordsSame', arguments, false);
        return false;
      }
   //$exitTrace('checkPasswordsSame', arguments, true);
  }


  function pushSubmitTimeAction (string) {
   //$enterTrace('pushSubmitTimeAction', arguments);
    submitTimeActions.length = submitTimeActions.length+1;
    submitTimeActions[submitTimeActions.length-1] = string;
   //$exitTrace('pushSubmitTimeAction', arguments, null);
  }


  function verifyName(widget, title, name_must_be_supplied_p) {
    //$enterTrace('verifyName', arguments);
    // window.alert("Name to verify=[" + widget.value + "]");
    var res = null				;
    if(widget.value=='' && (createClicked != false ||
			    name_must_be_supplied_p))
     {window.alert('You have not supplied a name for your ' + title);
      res = false; 
     }
    else { res = true; }
    //$exitTrace('verifyName', arguments, res);
   return res;
  }


  function checkNumberP(widget, decimalAllowedP, commaAllowedP,
			atLeastP, atLeast, 
                        atMostP, atMost, widthP, width) {
    //$enterTrace('checkNumberP', arguments);
    var str = widget.value;
    for (i=0; i<str.length; i++) {
      var ch = str.charAt(i);
      if((ch >= '0' && ch <= '9')
         || (ch == '.' && decimalAllowedP)
         || (ch == ',' && commaAllowedP)
         || (ch == '-' && i == 0))
      {}
      else
       { 
         var val = widget.value;
         widget.value = widget.value.slice(0,i);
         if (decimalAllowedP)
           { window.alert(val + ' should be a number.'); }
         else { window.alert(val + ' should be an integer'); }
         widget.focus();
         widget.select();
	 //$exitTrace('checkNumberP', arguments, false);
         return false; 
       }
    }
    var pos;
    while (true)
    {
       pos = widget.value.indexOf(",");
       if(pos < 0) break;
       widget.value = widget.value.substring(0, pos) + 
                      widget.value.substring(pos + 1);
    }
    if (atLeastP)
     { var val = eval(widget.value);
       if (val < atLeast)
         { window.alert(val + ' should be at least ' + atLeast);
           widget.focus();
           widget.select();
	 //$exitTrace('checkNumberP', arguments, false);
           return false; 
         }
     }
    if (atMostP)
     { var val = eval(widget.value);
       if (val > atMost)
         { window.alert(val + ' should be at most ' + atMost);
           widget.focus();
           widget.select();
           //$exitTrace('checkNumberP', arguments, false);
           return false; 
         }
     }

    //window.alert('str=' + str + ' length=' + str.length + ' width=' + width);
    if (widthP)
     { if (str.length < width)
         { window.alert('Value in field is too short.  Expected '
                        + width + ' characters ');
           widget.focus();
           widget.select();
	   //$exitTrace('checkNumberP', arguments, false);
           return false; 
         }
        else if (str.length > width)
         { window.alert('Value in field is too long.  Expected '
                        + width + ' characters ');
           widget.focus();
           widget.select();
	   //$exitTrace('checkNumberP', arguments, false);
           return false; 
         }
     }
    //$exitTrace('checkNumberP', arguments, true);
    return true;
  }
 

