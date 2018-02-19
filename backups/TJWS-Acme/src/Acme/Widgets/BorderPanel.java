// BorderPanel - a panel that adds a border around the contents
//
// Copyright (C) 1996 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package Acme.Widgets;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Panel;

/// A panel that adds a border around the contents.
// <P>
// Sample usage:
// <IMG ALIGN=RIGHT WIDTH=340 HEIGHT=60 SRC="BorderPanel.gif">
// <BLOCKQUOTE><PRE><CODE>
// BorderPanel p = new BorderPanel( BorderPanel.IN );
// </CODE></PRE></BLOCKQUOTE>
// <P>
// <A HREF="/resources/classes/Acme/Widgets/BorderPanel.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class BorderPanel extends Panel {
	
	public static final int SOLID = 0;
	public static final int RAISED = 1;
	public static final int LOWERED = 2;
	public static final int IN = 3;
	public static final int OUT = 4;
	
	private int type;
	private int thickness;
	
	private Panel innerPanel = null;
	
	/// Constructor.
	public BorderPanel(int type, int thickness) {
		this.type = type;
		this.thickness = thickness;
		build();
	}
	
	/// Constructor, default thickness for this type.
	public BorderPanel(int type) {
		this.type = type;
		switch(type) {
			case SOLID:
				thickness = 2;
				break;
			case RAISED:
				thickness = 2;
				break;
			case LOWERED:
				thickness = 2;
				break;
			case IN:
				thickness = 2;
				break;
			case OUT:
				thickness = 2;
				break;
		}
		build();
	}
	
	private void build() {
		// Make the inner panel.
		Panel ip = new Panel();
		
		// Add the inner panel using a GridBagLayout because that layout
		// manager implements inserts.
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = gbc.weighty = 1.0;
		gbc.insets = new Insets(thickness, thickness, thickness, thickness);
		gb.setConstraints(ip, gbc);
		add(ip, -1);
		
		// And set the inner panel so that the delegated methods start using it.
		innerPanel = ip;
	}
	
	/// Paint the border.
	public void paint(Graphics graphics) {
		Dimension size = size();
		// Dimension size = getSize();
		graphics.setColor(getBackground());
		switch(type) {
			case SOLID:
				graphics.setColor(getForeground());
				for(int i = 0; i < thickness; ++i)
					graphics.drawRect(i, i, size.width - i * 2 - 1, size.height - i * 2 - 1);
				break;
			
			case RAISED:
				for(int i = 0; i < thickness; ++i)
					graphics.draw3DRect(i, i, size.width - i * 2 - 1, size.height - i * 2 - 1, true);
				break;
			
			case LOWERED:
				for(int i = 0; i < thickness; ++i)
					graphics.draw3DRect(i, i, size.width - i * 2 - 1, size.height - i * 2 - 1, false);
				break;
			
			case IN:
				graphics.draw3DRect(0, 0, size.width - 1, size.height - 1, false);
				graphics.draw3DRect(thickness - 1, thickness - 1, size.width - thickness * 2 + 1, size.height - thickness * 2 + 1, true);
				break;
			
			case OUT:
				graphics.draw3DRect(0, 0, size.width - 1, size.height - 1, true);
				graphics.draw3DRect(thickness - 1, thickness - 1, size.width - thickness * 2 + 1, size.height - thickness * 2 + 1, false);
				break;
		}
	}
	
	// Delegate other Container/Panel methods to the superclass if we
	// are initializing, otherwise to the inner panel.
	
	public int countComponents() {
		if(innerPanel == null)
			return super.getComponentCount();
		else
			return innerPanel.getComponentCount();
	}
	
	public Component getComponent(int n) {
		if(innerPanel == null)
			return super.getComponent(n);
		else
			return innerPanel.getComponent(n);
	}
	
	public Insets insets(int n) {
		if(innerPanel == null)
			return super.getInsets();
		else
			return innerPanel.getInsets();
	}
	
	public Component add(Component comp) {
		if(innerPanel == null)
			return super.add(comp);
		else
			return innerPanel.add(comp);
	}
	
	public Component add(Component comp, int pos) {
		if(innerPanel == null)
			return super.add(comp, pos);
		else
			return innerPanel.add(comp, pos);
	}
	
	public Component add(String name, Component comp) {
		if(innerPanel == null)
			return super.add(name, comp);
		else
			return innerPanel.add(name, comp);
	}
	
	public void remove(Component comp) {
		if(innerPanel == null)
			super.remove(comp);
		else
			innerPanel.remove(comp);
	}
	
	public void removeAll() {
		if(innerPanel == null)
			super.removeAll();
		else
			innerPanel.removeAll();
	}
	
	public LayoutManager getLayout() {
		if(innerPanel == null)
			return super.getLayout();
		else
			return innerPanel.getLayout();
	}
	
	public void setLayout(LayoutManager mgr) {
		if(innerPanel == null)
			super.setLayout(mgr);
		else
			innerPanel.setLayout(mgr);
	}
	
}
