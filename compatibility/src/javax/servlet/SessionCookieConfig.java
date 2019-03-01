package javax.servlet;

/**
 * Please, don't remove this file. This file is copied from the 'servlet.jar'
 * which is missing in
 * 'servlet-2-3.jar' file. Just added this to avoid unwanted exception.
 *
 * @Author: Rohtash Singh Lakra
 *          Created On: 02/13/2018 12:27.
 */
public interface SessionCookieConfig {
	public abstract void setName(String paramString);
	
	public abstract String getName();
	
	public abstract void setDomain(String paramString);
	
	public abstract String getDomain();
	
	public abstract void setPath(String paramString);
	
	public abstract String getPath();
	
	public abstract void setComment(String paramString);
	
	public abstract String getComment();
	
	public abstract void setHttpOnly(boolean paramBoolean);
	
	public abstract boolean isHttpOnly();
	
	public abstract void setSecure(boolean paramBoolean);
	
	public abstract boolean isSecure();
	
	public abstract void setMaxAge(int paramInt);
	
	public abstract int getMaxAge();
}
