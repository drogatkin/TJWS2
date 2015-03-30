var files = [];
var endPoint = "ws" + (self.location.protocol == "https:" ? "s" : "") + "://"
		+ self.location.hostname
		+ (self.location.port ? ":" + self.location.port : "")
		+ "/echoserver/upload/upload";
var socket;

function openSocket() {
	socket = new WebSocket(endPoint);
	
	socket.onmessage = function(event) {
		self.postMessage(JSON.parse(event.data));
	};

	socket.onopen = function() {
		process();
	};
}

function ready() {
	return socket !== undefined
			&& socket.readyState !== WebSocket.CLOSED
}

function process() {
	while (files.length > 0) {
		var blob = files.shift();
		socket.send(JSON.stringify({
			"cmd" : 1,
			"data" : blob.name
		}));
		const
		BYTES_PER_CHUNK = 1024 * 1024 * 2;
		// 2MB chunk sizes.
		const
		SIZE = blob.size;

		var start = 0;
		var end = BYTES_PER_CHUNK;

		while (start < SIZE) {

			if ('mozSlice' in blob) {
				var chunk = blob.mozSlice(start, end);
			} else if ('slice' in blob) {
					var chunk = blob.slice(start, end);
			} else {
				var chunk = blob.webkitSlice(start, end);
			}

			socket.send(chunk);

			start = end;
			end = start + BYTES_PER_CHUNK;
		}
		socket.send(JSON.stringify({
			"cmd" : 2,
			"data" : blob.name
		}));
		//self.postMessage(blob.name + " Uploaded Succesfully");
	}
}

self.onmessage = function(e) {
	for (var j = 0; j < e.data.files.length; j++)
		files.push(e.data.files[j]);

	//self.postMessage("Job size: "+files.length);
			
	if (ready()) {
		process();
	} else
		openSocket();
}
