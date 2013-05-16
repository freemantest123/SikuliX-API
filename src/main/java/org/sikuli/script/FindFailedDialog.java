/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

//TODO:
//- (done) close --> abort
//- (done) keep the dialog box always on top
//- (done) display target image
//- display oversized target images at a proper scale
//- beautify the layout
//- (done) disable resizing
//- ensure the dialog disappears before find is reattempted so that it won't find the
//target image in the dialog box.
class FindFailedDialog extends JDialog implements ActionListener {

	JButton retryButton;
	JButton skipButton;
	JButton abortButton;
	FindFailedResponse _response;

	public <PatternString> FindFailedDialog(PatternString target) {
		setModal(true);
		//super(new Frame(),true);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		Component targetComp = createTargetComponent(target);

		panel.add(targetComp, BorderLayout.NORTH);

		JPanel buttons = new JPanel();

		retryButton = new JButton("Retry");
		retryButton.addActionListener(this);

		skipButton = new JButton("Skip");
		skipButton.addActionListener(this);

		abortButton = new JButton("Abort");
		abortButton.addActionListener(this);

		buttons.add(retryButton);
		buttons.add(skipButton);
		buttons.add(abortButton);

		panel.add(buttons, BorderLayout.SOUTH);

		add(panel);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);


		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				_response = FindFailedResponse.ABORT;
				//dispose();
			}
		});

		//pack(); // don't pack, doing so messes up AlwaysOnTop
		//setLocationRelativeTo(null);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (retryButton == e.getSource()) {
			_response = FindFailedResponse.RETRY;
		} else if (abortButton == e.getSource()) {
			_response = FindFailedResponse.ABORT;
		} else if (skipButton == e.getSource()) {
			_response = FindFailedResponse.SKIP;
		}
		dispose();
	}

	public FindFailedResponse getResponse() {
		return _response;
	}

	<PatternString> Component createTargetComponent(PatternString target) {
		Image image = null;
		JLabel c = null;
		String targetTyp = "";
		JPanel p;
		if (target instanceof Pattern) {
			Pattern pat = (Pattern) target;
			targetTyp = "pattern";
			target = (PatternString) pat.toString();
			image = pat.getImage();
		} else if (target instanceof String) {
			image = ImageLocator.getImage((String) target);
			if (image != null) {
				targetTyp = "image";
			} else {
				c = new JLabel("Sikuli cannot find text:" + (String) target);
				return c;
			}
		} else {
			return null;
		}
		p = new JPanel();
		p.setLayout(new BorderLayout());
		JLabel iconLabel = new JLabel();
		String rescale = "";
		if (image != null) {
			int w = image.getWidth(this);
			int h = image.getHeight(this);
			if (w > 500) {
				w = 500;
				h = -h;
				rescale = " (rescaled to 500x...)";
			}
			if (h > 300) {
				h = 300;
				w = -w;
				rescale = " (rescaled to ...x300)";
			}
			if (h < 0 && w < 0) {
				w = 500;
				h = 300;
				rescale = " (rescaled to 500x300)";
			}
			image = image.getScaledInstance(w, h, Image.SCALE_DEFAULT);
		}
		iconLabel.setIcon(new ImageIcon(image));
		c = new JLabel("Sikuli cannot find " + targetTyp + rescale + ".");
		p.add(c, BorderLayout.PAGE_START);
		p.add(new JLabel((String) target));
		p.add(iconLabel, BorderLayout.PAGE_END);
		return p;
	}

	@Override
	public void setVisible(boolean flag) {

		if (flag) {
			// These can not be called in the constructor.
			// Doing so somehow made it impossible to keep
			// the dialog always on top.
			requestFocus();
			setAlwaysOnTop(true);
			pack();
			setResizable(false);
			setLocationRelativeTo(this);
		}

		super.setVisible(flag);
	}
}
