/**
 * Adds the default string to the given div.
 * 
 * @param idDiv
 * @returns
 */
function addDefaults(idDiv) {
	var div = document.getElementById(idDiv);
	if (div) {
		div.innerHTML += 'Rohtash Singh Lakra\n';
	}
}

/**
 * Clears the given div.
 * 
 * @param idDiv
 * @returns
 */
function clearDefaults(idDiv) {
	var div = document.getElementById(idDiv);
	if (div) {
		div.innerHTML = '';
	}
}