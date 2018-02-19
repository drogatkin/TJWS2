// JPMTest - test program for the ACME Java pixmap utilities
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

package Acme.JPM;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Event;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.io.IOException;

import Acme.JPM.Encoders.GifEncoder;
import Acme.JPM.Encoders.ImageEncoder;
import Acme.JPM.Encoders.PpmEncoder;
import Acme.JPM.Filters.EdgeDetect;
import Acme.JPM.Filters.Enlarge;
import Acme.JPM.Filters.Flip;
import Acme.JPM.Filters.Gamma;
import Acme.JPM.Filters.Invert;
import Acme.JPM.Filters.Margin;
import Acme.JPM.Filters.Oil;
import Acme.JPM.Filters.Rotate;
import Acme.JPM.Filters.ScaleCopy;
import Acme.JPM.Filters.Shear;
import Acme.JPM.Filters.Shrink;
import Acme.JPM.Filters.Smooth;
import Acme.JPM.Filters.Tile;
import Acme.Widgets.ImageLabel;

/// Test program for the ACME Java pixmap utilities.
// <P>
// <A HREF="/resources/classes/Acme/JPM/JPMTest.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class JPMTest extends Frame {
	
	private static JPMTest jpmTest;
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.err.println("usage: JPMTest [filename]");
			System.exit(1);
		}
		jpmTest = new JPMTest(args[0]);
	}
	
	private ImageLabel origImageLabel, filt1ImageLabel, filt2ImageLabel;
	private Image origImage, filt1Image, filt2Image;
	private Choice filt1Choice, filt2Choice, outputChoice;
	
	/// Constructor.
	public JPMTest(String fileName) {
		
		setTitle("JPMTest");
		
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets(5, 5, 5, 5);
		
		Panel panel = new Panel();
		GridBagLayout gb2 = new GridBagLayout();
		panel.setLayout(gb2);
		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.insets = new Insets(5, 5, 5, 5);
		
		Label label = new Label(fileName);
		gbc2.gridwidth = 1;
		gb2.setConstraints(label, gbc2);
		panel.add(label);
		
		filt1Choice = new Choice();
		addChoiceItems(filt1Choice);
		gbc2.gridwidth = 1;
		gb2.setConstraints(filt1Choice, gbc2);
		panel.add(filt1Choice);
		
		filt2Choice = new Choice();
		addChoiceItems(filt2Choice);
		gbc2.gridwidth = GridBagConstraints.REMAINDER;
		gb2.setConstraints(filt2Choice, gbc2);
		panel.add(filt2Choice);
		
		origImageLabel = new ImageLabel(Acme.GuiUtils.brokenIcon(this));
		gbc2.gridwidth = 1;
		gb2.setConstraints(origImageLabel, gbc2);
		panel.add(origImageLabel);
		
		filt1ImageLabel = new ImageLabel(Acme.GuiUtils.brokenIcon(this));
		gbc2.gridwidth = 1;
		gb2.setConstraints(filt1ImageLabel, gbc2);
		panel.add(filt1ImageLabel);
		
		filt2ImageLabel = new ImageLabel(Acme.GuiUtils.brokenIcon(this));
		gbc2.gridwidth = GridBagConstraints.REMAINDER;
		gb2.setConstraints(filt2ImageLabel, gbc2);
		panel.add(filt2ImageLabel);
		
		gb.setConstraints(panel, gbc);
		add(panel);
		
		panel = new Panel();
		gb2 = new GridBagLayout();
		panel.setLayout(gb2);
		
		outputChoice = new Choice();
		outputChoice.addItem("GIF");
		outputChoice.addItem("PPM");
		gbc2.gridwidth = 1;
		gb2.setConstraints(outputChoice, gbc2);
		panel.add(outputChoice);
		
		Button button = new Button("Write");
		gbc2.gridwidth = 1;
		gb2.setConstraints(button, gbc2);
		panel.add(button);
		
		button = new Button("Quit");
		gbc2.gridwidth = GridBagConstraints.REMAINDER;
		gb2.setConstraints(button, gbc2);
		panel.add(button);
		
		gb.setConstraints(panel, gbc);
		add(panel);
		
		pack();
		validate();
		show();
		
		origImage = getToolkit().getImage(fileName);
		origImageLabel.setImage(origImage);
		filt1Choice.select(NONE);
		filt2Choice.select(NONE);
		filter1();
		filter2();
	}
	
	private void addChoiceItems(Choice choice) {
		// These items must match the order of the enumeration.
		choice.addItem("NONE");
		choice.addItem("Shrink 2");
		choice.addItem("Enlarge 2");
		choice.addItem("ScaleCopy 0.51");
		choice.addItem("ScaleCopy 1.99");
		choice.addItem("Oil");
		choice.addItem("Smooth 2");
		choice.addItem("EdgeDetect");
		choice.addItem("Gamma 2");
		choice.addItem("Tile 400");
		choice.addItem("Flip -lr");
		choice.addItem("Flip -tb");
		choice.addItem("Flip -cw");
		choice.addItem("Invert");
		choice.addItem("Margin");
		choice.addItem("Shear 30");
		choice.addItem("Rotate 30");
	}
	
	/// Event handler.
	public boolean handleEvent(Event evt) {
		switch(evt.id) {
			case Event.ACTION_EVENT:
				if(evt.arg.equals("Quit"))
					System.exit(0);
				else if(evt.arg.equals("Write"))
					write();
				else if(evt.target == filt1Choice) {
					filter1();
					filter2();
				} else if(evt.target == filt2Choice)
					filter2();
				break;
		}
		return super.handleEvent(evt);
	}
	
	// This must match the order of the choice items.
	private static final int GIF = 0;
	private static final int PPM = 1;
	
	private void write() {
		try {
			ImageEncoder encoder = null;
			switch(outputChoice.getSelectedIndex()) {
				case GIF:
					encoder = new GifEncoder(filt2Image.getSource(), System.out);
					break;
				case PPM:
					encoder = new PpmEncoder(filt2Image.getSource(), System.out);
					break;
			}
			encoder.encode();
		} catch(IOException e) {
			System.err.println(e.toString());
		}
	}
	
	// This must match the order of the choice items.
	private static final int NONE = 0;
	private static final int SHRINK2 = 1;
	private static final int ENLARGE2 = 2;
	private static final int SCALE051 = 3;
	private static final int SCALE199 = 4;
	private static final int OIL = 5;
	private static final int SMOOTH2 = 6;
	private static final int EDGEDETECT = 7;
	private static final int GAMMA2 = 8;
	private static final int TILE400 = 9;
	private static final int FLIPLR = 10;
	private static final int FLIPTB = 11;
	private static final int FLIPCW = 12;
	private static final int INVERT = 13;
	private static final int MARGIN = 14;
	private static final int SHEAR30 = 15;
	private static final int ROTATE30 = 16;
	
	private void filter1() {
		filt1Image = filterImage(filt1Choice.getSelectedIndex(), origImage);
		filt1ImageLabel.setImage(filt1Image);
	}
	
	private void filter2() {
		filt2Image = filterImage(filt2Choice.getSelectedIndex(), filt1Image);
		filt2ImageLabel.setImage(filt2Image);
	}
	
	private Image filterImage(int which, Image image) {
		switch(which) {
			case NONE:
				return image;
			case SHRINK2:
				return JPMUtils.filterImage(this, new Shrink(image.getSource(), 2));
			case ENLARGE2:
				return JPMUtils.filterImage(this, new Enlarge(image.getSource(), 2));
			case SCALE051:
				return JPMUtils.filterImage(this, new ScaleCopy(image.getSource(), 0.51));
			case SCALE199:
				return JPMUtils.filterImage(this, new ScaleCopy(image.getSource(), 1.99));
			case OIL:
				return JPMUtils.filterImage(this, new Oil(image.getSource()));
			case SMOOTH2:
				return JPMUtils.filterImage(this, new Smooth(image.getSource(), 2));
			case EDGEDETECT:
				return JPMUtils.filterImage(this, new EdgeDetect(image.getSource()));
			case GAMMA2:
				return JPMUtils.filterImage(this, new Gamma(image.getSource(), 2.0));
			case TILE400:
				return JPMUtils.filterImage(this, new Tile(image.getSource(), 400, 400));
			case FLIPLR:
				return JPMUtils.filterImage(this, new Flip(image.getSource(), Flip.FLIP_LR));
			case FLIPTB:
				return JPMUtils.filterImage(this, new Flip(image.getSource(), Flip.FLIP_TB));
			case FLIPCW:
				return JPMUtils.filterImage(this, new Flip(image.getSource(), Flip.FLIP_CW));
			case INVERT:
				return JPMUtils.filterImage(this, new Invert(image.getSource()));
			case MARGIN:
				return JPMUtils.filterImage(this, new Margin(image.getSource(), Color.black, 10));
			case SHEAR30:
				return JPMUtils.filterImage(this, new Shear(image.getSource(), 30.0));
			case ROTATE30:
				return JPMUtils.filterImage(this, new Rotate(image.getSource(), 30.0));
		}
		return null;
	}
	
}
