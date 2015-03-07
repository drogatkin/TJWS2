var files = [], p = true;
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
	if (socket)
		return;
	socket = new WebSocket(endPoint);
	socket.onmessage = function(event) {
		self.postMessage('Websocket : ' + event.data);
	};

	socket.onclose = function(event) {
	};

	socket.onopen = function() {
		ready = true;
	};
}

function process() {
	for (var j = 0; j < files.length; j++) {
		var blob = files[j];
		if (!ready)
			continue;
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
		p = (j == files.length - 1) ? true : false;
		self.postMessage(blob.name + " Uploaded Succesfully");
	}
}

self.onmessage = function(e) {
	openSocket();
	for (var j = 0; j < e.data.files.length; j++)
		files.push(e.data.files[j]);

	if (p) {
		process();
	}
}