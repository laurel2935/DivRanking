package org.archive.a1.search.another.jsearchdemo;

import java.awt.event.*;

class LinkingState extends State {


    LinkingState(GraphRenderer gp) {
	super (gp);
    }

    public void mouseClicked(MouseEvent m) {
	NodeRenderer target = boss.getNode(m.getPoint());
	if (target != null) {
	    boss.linkTo(target);
	}
	boss.gotoPreviousState();
    }

    public String toString() {
	return  "Select target node.";
    }
    
}
