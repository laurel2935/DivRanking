package org.archive.a1.search.another.jsearchdemo;

import org.archive.a1.search.another.search.Algorithm;



public interface StateListener 
{

    void queueChanged(Algorithm a);

    void stateChanged();
}
