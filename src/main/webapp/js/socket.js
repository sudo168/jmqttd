'use strict'

var Socket = window.Socket || (function($,global){

	$ = !$ || (function(){
		var _sender = (function(){
			function getXHR(crossDomain){
				var _xhr = null;
				if(window.XMLHttpRequest){ //非IE
					_xhr=new XMLHttpRequest();
				}else if(window.ActiveXObject){//IE
				   try{
					   if(crossDomain && window.XDomainRequest){
						   _xhr = new XDomainRequest();
					   }else{
						   _xhr = new ActiveXObject("Msxml2.HTTP");
					   }
				   }catch(e){
				      try{
				    	  _xhr = new ActiveXObject("microsoft.HTTP");
				      }catch(e){
				    	  alert("亲，落伍了哦！您的浏览器版本太老了！请速升级...");
				      }
				   }
				}
				return _xhr;
			};
			
			return {
				send : function (options) {
					var xmlReq = getXHR(options.crossDomain);
					xmlReq.onload=function(){
					   if(this.status==200){
						   var data = this.response;
				    	   if (options.dataType.toLowerCase() == "json" && data) {
				    		   data = JSON.parse(this.response);
				    	   }
				    	   options.success(data,this.statusText,this);
				       }else{
				    	   options.error(this,this.statusText,new Error(this.response));
				    	   if(options.statusCode){
				    		   options.statusCode[this.status] && options.statusCode[this.status]();
				    	   }
				       }
					}
					if(options.async){
						xmlReq.responseType = options.dataType;
						xmlReq.timeout = options.timeout * 1000;
					}
					xmlReq.onerror = function (e) {
						options.error(this,this.statusText,e);
					};
					xmlReq.ontimeout = function (e) {
						options.error(this,this.statusText,e);
					};
					//xmlReq.setRequestHeader(key,value);
					xmlReq.upload.onprogress = function(e) {};
					if (options.xhrFields) {
						for(var i in options.xhrFields){
							if(options.xhrFields.hasOwnProperty(i)){
								xmlReq[i] = options.xhrFields[i];
							}
						}
					}
					//打开与服务器连接的通道
					xmlReq.open(options.type,options.url,options.async);
					if(options.type.toLowerCase() == "post")xmlReq.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
					//开始请求并发送参数 
					xmlReq.send(options.data ? options.data : null);
				}
			}
		})();
		return {
			ajax : function (options) {
				options.type = options.type || "GET";
				options.async = options.async === true ? true : false;
				options.dataType = options.dataType || "text";
				options.success = options.success || function(){};
				options.error = options.error || function(){};
				options.xhrFields = {};
				options.timeout = options.timeout || 15;
				options.statusCode = options.statusCode || {};
				options.crossDomain = options.crossDomain === true ? true : false;
				_sender.send(options);
			}
		}
	})();
	
	if(!$)throw new Error("missing jQuery, socket.js require jQuery 1.x !");
	
	var version = "1.0.0.0";
	/*消息类型*/
	var MSG_TYPE = {
		OPEN:0,MSG:1,PUB:2,SUB:3,UNSUB:4,PING:5,PONG:6,CLOSE:7
	};
	/*消息发送源*/
	var MSG_SRC = {
		TCP:0,MQTT:1,WEBSOCKET:2,POLLING:3,FLASH:4
	};
	
	var Utils = {
		/**
		 * console.log(isEmpty("123"));//false
		 * console.log(isEmpty(""));//true
		 * console.log(isEmpty(0));//true
		 * console.log(isEmpty(123));//false
		 * console.log(isEmpty(12.3));//false
		 * console.log(isEmpty(-12.3));//false
		 * console.log(isEmpty({}));//true
		 * console.log(isEmpty([]));//true
		 */
		isEmpty : function (obj) { 
			if (typeof obj === "object"){
				if(obj instanceof Array)return obj.length == 0;
			    for (var t in obj)
			    	return !1;
			    return !0;
			}
		    return !obj; 
		},
	};
	
	var Message = function(type,clientId,content){
		this.type = type;
		this.src = null;
		this.clientId = clientId;
		this.content = content;
		this.dest = "test";// 目的地destination
		this.retained = 0;// 是否保存
		this.duplicate = 0;// DUP flag（打开标志）保证消息可靠传输，默认为0，只占用一个字节，表示第一次发送。1 表示已经发送过
		this.qos = 1; // 0:<=1,1:>=1,2=1
		this.toString = function(){
			var msg = {};
			msg.type = this.type;
			msg.clientId = this.clientId;
			msg.src = this.src;
			msg.content = this.content;
			return JSON.stringify(msg);
		}
	}
	
	var Event = function(type,data){
		this.type = type;
		this.data = data;
	};
	
	var Client = function (socket,host,port,cfg){
		var _socket = socket;
		if (!(_socket instanceof global.Socket))throw new Error("invalid argument : socket !");
		var _client = this;
		for (var attr in _socket) {
			this[attr] = (function (evt){
				return function () {
					if(evt.indexOf("on") == 0){
						_socket[evt]["call"](_socket,arguments[0]);
					}else{
						//connect reconnect send close pub sub unsub
						switch (evt) {
						case "send":
							if (typeof arguments[0] !== "object") throw new Error("sending message must be a standard Json Object!");
							_client.context.send(new Message(MSG_TYPE.MSG, _client.cid, arguments[0]));
							break;
						case "pub":
							break;
						case "sub":
							break;
						case "unsub":
							break;
						case "connect":
							break;
						case "reconnect":
							break;
						case "close":
							break;
						default:
							break;
						}
					}
				}
			})(attr);
		}
		_client.host = host;
		_client.port = port;
		_client.cfg = cfg || {};
		_client.context = null;
		_client.transports = ["websocket","polling","mqtt","flash"];
		_client.support = {};
		_client.publist = [];// 发布的主题
		_client.sublist = [];// 订阅的主题
		_client.support = {};
		_client.path = null;
		_client.cid = null;
		_client.init = function(){
			if(!_socket.open){
				$.ajax({
				   type: "POST",
				   url: "http://"+_client.host+":"+_client.port+"/init",
				   async : false,
				   dataType : "json",
				   success: function(config){
				      if(config){
				    	  for(var attr in config){
				    		  if(config.hasOwnProperty(attr)){
				    			  if (attr == "support") {
				    				  var p = config[attr];
				    				  for(var i in p){
				    					  _client.support[p[i]] = 1;
				    				  }
				    				  continue;
				    			  }
				    			  _client[attr] = config[attr];
				    		  }
				    	  }
				      }else{
				    	  console.log("=> get socket config failed!["+msg+"]");
				      }
				   },
				   error: function(err){
					  console.log("=> get socket config error!");
					  console.log(err);
				   }
				});
			}
			
			if(Utils.isEmpty(this.support)){
				console.log("=> socket init failed!");
				return;
			}
			console.log("socket 配置初始化成功！");
			_client.context = new Context(this).start();
		};
		var Context = function(_cli){
			this.connection = null;
			this.transport = null;
			this.init = function(){
				console.log("服务器支持:");
				console.log(_cli.support);
				//WebSocket readyState: CONNECTING(0)OPEN(1)CLOSING(2)CLOSED(3)
				var webSocket = window.WebSocket || window.MozWebSocket;
				var flashSocket = null;
				if (webSocket){// 浏览器支持 WebSocket
					console.log("浏览器支持 WebSocket");
					if ("MQTT" in _cli.support) {
						this.transport = _cli.transports[2];
						console.log("浏览器使用mqtt连接...");
						this.connection = new Messaging.Client(_cli.mHost, _cli.mPort, _cli.cid);
						this.connection.onConnectionLost = _cli.onClose;
						this.connection.onMessageArrived = _cli.onMessage;
						this.connection.connect({onSuccess:_cli.onOpen,context:_cli.path});
					} else if ("WEBSOCKET" in _cli.support){
						this.transport = _cli.transports[0];
						console.log("浏览器使用WebSocket连接...");
						this.connection = new webSocket("ws://"+_cli.host+":"+_cli.port+_cli.path);
						this.connection.onopen = _cli.onOpen;
						this.connection.onmessage = _cli.onMessage;
						this.connection.onclose = _cli.onClose;
					} else if ("POLLING" in _cli.support){
						this.transport = _cli.transports[1];
						console.log("浏览器使用polling连接...");
						this.connection = new Poller("http://"+_cli.host+":"+_cli.port+_cli.path+"/polling");
						this.connection.onopen = _cli.onOpen;
						this.connection.onmessage = _cli.onMessage;
						this.connection.onclose = _cli.onClose;
					} else {
						console.log("服务器参数配置有误,请联系管理员!");
					}
				} else if (flashSocket){// 浏览器不支持原生 WebSocket, 使用flashWebSocket
					console.log("浏览器不支持原生 WebSocket, 使用flashWebSocket");
					this.transport = _cli.transports[3];
					if ("MQTT" in _cli.support) {
						console.log("浏览器使用mqtt连接...");
					} else if ("WEBSOCKET" in _cli.support){
						console.log("浏览器使用WebSocket连接...");
					} else if ("POLLING" in _cli.support){
						this.transport = _cli.transports[1];
						console.log("浏览器使用polling连接...");
					} else {
						console.log("服务器参数配置有误,请联系管理员!");
					}
				} else if ("POLLING" in _cli.support){// 浏览器不支持 WebSocket,也不支持flashWebSocket，使用长轮询
					this.transport = _cli.transports[1];
					console.log("浏览器不支持WebSocket，使用polling连接...");
				} else {
					console.log("服务器参数配置有误,请联系管理员!");
				}
				return this;
			};
			this.start = function(){
				console.log("context start");
				return this;
			}
			this.send = function(message){
				if (!(message instanceof Message))
	                throw new Error("Invalid argument:" + typeof message);
	            if (typeof message.dest === "undefined")
	            	throw new Error("Invalid parameter Message.dest:" + message.dest);
	            message.src = this.transport;
	            // TODO 消息发送统一处理
	            if(this.transport = _cli.transports[2]){
	            	var _msg = message;
	            	message = new Messaging.Message(message.toString());
	            	message.destinationName = _msg.dest;
	            	message.payload = _msg.payload;
	            	message.qos = _msg.qos;
	            }
	            this.connection.send(message);
	            return this;
			};
			this.init();
		};
		
		var Poller = function(url){
			
			this.send = function(){
				$.ajax({
					   type: "POST",
					   url: url,
					   async : true,
					   dataType : "json",
					   success: function(msg){
					   },
					   error: function(err){
					   }
				});
			};
			
			var Ping = function(client, window, timeoutSeconds, action, args) {
				this._window = window;
				if (!timeoutSeconds)
					timeoutSeconds = 30;
				
				var doTimeout = function (action, client, args) {
			        return function () {
			            return action.apply(client, args);
			        };
			    };
		        this.timeout = setTimeout(doTimeout(action, client, args), timeoutSeconds * 1000);
		        
				this.cancel = function() {
					this._window.clearTimeout(this.timeout);
				}
			}; 
		};
		_client.init();
	};
	/** new Socket(host,port,cfg)*/
	return (function(){
		this.connect = function (host,port){
			if (!this.open) {
				this.Client = new Client(this,host,port,arguments[2]);
			}
		};
		this.reconnect = function (){
			this.close();
			this.connect();
		};
		this.close = function (){
			this.open && this.Client.close();
		};
		
		this.send = function (msg,callback){
			this.Client.send(msg,callback);
		};
		
		this.pub = function (topic,callback){};
		this.sub = function (topic,callback){};
		this.unsub = function (topic,callback){};

		this.onOpen = function (evt){
			this.open = true;
			console.log("onOpen");
			console.log(evt);
			this.send({company:13322,name:"华海乐盈"});
		};

		this.onMessage = function (evt){
			console.log("onMessage");
			console.log(evt);
		};
		this.onClose = function (evt){
			this.open = false;
			console.log("onClose");
			console.log(evt);
		};
		this.onError = function (evt){
			console.log("onError");
			console.log(evt);
		};
		this.onReconnect = function (evt){
			this.open = true;
			console.log("onReconnect");
			console.log(evt);
		};
		(typeof arguments[0] === "string" && typeof arguments[1] === "number") && this.connect(arguments[0],arguments[1],arguments[2]);
		global.Socket = this;
	});
})(jQuery,window);