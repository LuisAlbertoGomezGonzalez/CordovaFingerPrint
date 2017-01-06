var exec = require('cordova/exec');

function FingerPrintPlugin() { 
 console.log("FingerPrintPlugin.js: is created");
}

FingerPrintPlugin.prototype.write = function(onSuccess, onError){
 console.log("FingerPrintPlugin.js: write");

 exec(function(result){
     onSuccess();
   },
  function(result){
	 onError(result);
   },"FingerPrintPlugin","write",[]);
};

FingerPrintPlugin.prototype.authenticate = function(onSuccess,onError){
 console.log("FingerPrintPlugin.js: authenticate");

 exec(function(result){
     onSuccess();
   },
  function(result){
	 onError(result);
   },"FingerPrintPlugin","authenticate",[]);
};

FingerPrintPlugin.prototype.authenticateCustomed = function(onSuccess,onError,title,messageHeader,image,messageHelp,messageBtnCancel,numRetry){
 console.log("FingerPrintPlugin.js: authenticateCustomed");

 exec(function(result){
     onSuccess();
   },
  function(result){
	 onError(result);
   },"FingerPrintPlugin","authenticatecustom",[title,messageHeader,image,messageHelp,messageBtnCancel,numRetry]);
};

 var fingerPrintPlugin = new FingerPrintPlugin();
 module.exports = fingerPrintPlugin;