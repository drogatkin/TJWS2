/**
 * Adds the default contents into the div.
 */
function addDefaults(idDiv) {
	var div = document.getElementById(idDiv);
	if (div) {
		div.innerHTML += 'Rohtash Singh Lakra';
	}
}

/**
 * Clears the div contents.
 */
function clearDefaults(idDiv) {
	var div = document.getElementById(idDiv);
	if (div) {
		div.innerHTML = '';
	}
}