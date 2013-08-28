package com.tumri.cbo.monitor;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@SuppressWarnings("unused")
public class OrderByTypeProblemReporter
        extends AbstractReporter
        implements ProblemReporter {

    public void summariseProblems
            (Writer writer,
             Map<AbstractProblem,
                 Map<AbstractMonitor, List<AbstractProblem>>> recordedProblems,
             Map<Object, String> outputKey,
             boolean htmlify, boolean admin)
            throws IOException
    {
        List<AbstractProblem> types = new Vector<AbstractProblem>();
        types.addAll(recordedProblems.keySet());
        Collections.sort(types, AbstractProblem.SEVERITY_COMPARATOR);
        for(AbstractProblem prototype: types)
        {
            Map<AbstractMonitor, List<AbstractProblem>> monitorMap =
                recordedProblems.get(prototype);
            if(monitorMap != null && monitorMap.size() > 0)
            {
                nl(writer, htmlify);
                nl(writer, htmlify);
                writer.append(prototype.summaryHeading(htmlify));
                for(AbstractMonitor key: monitorMap.keySet())
                {
                    List<AbstractProblem> problems = monitorMap.get(key);
                    if(problems != null && problems.size() > 0)
                    {
                        nl(writer, htmlify);
                        writer.append(_html(key.heading(), htmlify));
                        writer.append(":");
                        for(AbstractProblem p: problems)
                        {
                            nl(writer, 4, htmlify);
                            writer.append
                                    (_html(p.summarise(htmlify), htmlify));
                        }
                    }
                }
            }
        }
    }
}
