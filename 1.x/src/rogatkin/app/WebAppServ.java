/*  TJWS WebAppServ
 * Copyright (C) 2010 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  
 *  Visit http://tjws.sourceforge.net to get the latest information
 *  about Rogatkin's products.                                                        
 *  $Id: WebAppServ.java,v 1.3 2009/12/31 05:02:13 dmitriy Exp $                
 *  Created on Mar 5, 2008
 *  @author dmitriy
 */
package rogatkin.app;

import rogatkin.web.WebApp;
import Acme.Utils;

public class WebAppServ extends Main {

	/** launches embedded app with app server settings
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0)
			try {
				initAppServer(Utils.splitStr(WebApp.readDescriptor()[0]));
			} catch (NullPointerException npe) {
				//npe.printStackTrace();
			}
		else
			initAppServer(args);
		WebApp.main(args);
	}
}
