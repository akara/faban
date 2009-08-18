function checkFields() {
    var targetname = document.targetform.targetname.value.length;
    var targetowner = document.targetform.targetowner.value.length;
    var targetmetric = document.targetform.targetmetric.value.length;
    var targetmetricunit = document.targetform.targetmetricunit.value.length;
    var targettags = document.targetform.targettags.value.length;
    var targetcolorred = document.targetform.targetcolorred.value.length;
    var targetcolororange = document.targetform.targetcolororange.value.length;
    var targetcoloryellow = document.targetform.targetcoloryellow.value.length;
    var metric = document.targetform.targetmetric.value  ;
    var red = document.targetform.targetcolorred.value;
    var orange = document.targetform.targetcolororange.value;
    var yellow = document.targetform.targetcoloryellow.value;

    var returnval;

    if (targetname > 0 && targetowner > 0 && targetmetric > 0 && targettags > 0 
        && targetmetricunit > 0 && targetcolorred > 0 && targetcolororange > 0
        && targetcoloryellow > 0 && isNumeric(metric) && parseFloat(metric) > 0
        && isNumeric(red) && parseFloat(red) > 0
        && isNumeric(orange) && parseFloat(orange) > 0
        && isNumeric(yellow) && parseFloat(yellow) > 0
        ){
        returnval = true;
    }
    else if (targetname == 0) {
        alert("Please enter target name");returnval=false;
    }
    else if (targetowner == 0) {
        alert("please enter target owner"); returnval=false;
    }
    else if (targetmetric == 0 ||  (!isNumeric(metric)  || parseFloat(metric) <= 0) ) {
        alert("Please enter a positive number for Target Metric."); returnval=false;
    }
    else if (targettags == 0) {
        alert("please enter target tags"); returnval=false;
    }
    else if (targetcolorred == 0) {
        alert("please enter percentage value for red color"); returnval=false;
    }
    else if (targetcolororange == 0) {
        alert("please enter percentage value for orange color"); returnval=false;
    }
    else if (targetcoloryellow == 0) {
        alert("please enter percentage value for yellow color"); returnval=false;
    }
    return returnval;
}

function isNumeric(sText) {
    var ValidChars = "0123456789.";
    var IsNumber=true;
    var Char;
    for (i = 0; i < sText.length && IsNumber == true; i++) {
        Char = sText.charAt(i);
        if (ValidChars.indexOf(Char) == -1) {
            IsNumber = false;
        }
    }
    return IsNumber;
}
