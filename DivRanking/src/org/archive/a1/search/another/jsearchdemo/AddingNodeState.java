package org.archive.a1.search.another.jsearchdemo;

import java.awt.event.*;
import java.awt.*;

class AddingNodeState extends State {

    AddingNodeState(GraphRenderer gp) {
	super(gp);
    }

    public void mouseClicked(MouseEvent m) {
	Point pos = boss.getEmpty(m.getPoint());
	if (pos != null) {
	    boss.reenable();
	    boss.moveTo(pos);
	    boss.gotoPreviousState();
	}
    }

    public String toString() {
	return "Click to place new node.";
    }

}




