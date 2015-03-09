var files = [];
var endPoint = "ws" + (self.location.protocol == "https:" ? "s" : "") + "://"
		+ self.location.hostname
		+ (self.location.port ? ":" + self.location.port : "")
		+ "/echoserver/upload/*";
var socket;
var ready;
function upload(blobOrFile) {
	if (ready)
		socket.send(blobOrFile);
}

function openSocket() {
	socket = new WebSocket(endPoint);
	
	socket.onmessage = function(event) {
		self.postMessage(JSON.parse(event.data));
	};

	socket.onclose = function(event) {
		ready = false;
	};

	socket.onopen = function() {
		ready = true;
		process();
	};
}

function process() {
	while (files.length > 0) {
		var blob = files.shift();
		socket.send(JSON.stringify({
			"cmd" : 1,
			"data" : blob.name
		}));
		const
		BYTES_PER_CHUNK = 1024 * 1024 * 1;
		// 1MB chunk sizes.
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

			upload(chunk);

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
			
	if (ready) {
		process();
	} else
		openSocket();
}