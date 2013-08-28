package com.tumri.cbo.monitor;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

abstract public interface ProblemReporter {

    public void summariseProblems
            (Writer writer,
             Map<AbstractProblem,
                 Map<AbstractMonitor, List<AbstractProblem>>> recordedProblems,
             Map<Object, String> outputKey,
             boolean htmlify, boolean admin)
	throws IOException;
}
