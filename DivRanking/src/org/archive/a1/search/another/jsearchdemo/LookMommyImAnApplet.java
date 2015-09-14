package org.archive.a1.search.another.jsearchdemo;

import java.applet.*;

public class LookMommyImAnApplet extends Applet {

    public LookMommyImAnApplet() {
    }

    public void init() {
	UndirectedGraph ug = new UndirectedGraph();
	GraphPanel gp = new GraphPanel(ug);
	add(gp);
    }

}
	
