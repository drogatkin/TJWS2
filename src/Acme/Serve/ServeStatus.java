/**
 * 
 */
package Acme.Serve;

/**
 * @author Rohtash Singh Lakra
 * @date 03/16/2018 12:00:30 PM
 */
public enum ServeStatus {
	NOT_FINISHED_CORRECTLY(-3),
	STILL_RUNNING(-2),
	TERMINATED_WITH_ERROR(-1),
	RUNNING(0),
	IO_ERROR(1),
	ERROR(2),
	BIND_ERROR(3),;
	
	private int status;
	
	private ServeStatus(int status) {
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
	
	public String toString() {
		return String.valueOf(getStatus());
	}
}
