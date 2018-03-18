function addDefaults(idDiv) {
	var div = document.getElementById(idDiv);
	if (div) {
		div.innerHTML += 'Rohtash Singh Lakra';
	}
}

function clearDefaults(idDiv) {
	var div = document.getElementById(idDiv);
	if (div) {
		div.innerHTML += '';
	}
}