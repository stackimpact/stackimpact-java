package test.stackimpact.agent.model;

import org.junit.Test;
import com.stackimpact.agent.*;
import com.stackimpact.agent.model.*;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class BreakdownTest {

    @Test
    public void filter() throws Exception {
        Breakdown root = new Breakdown(Breakdown.TYPE_CALLGRAPH, "root");
        root.measurement = 10;

        Breakdown child1 = new Breakdown(Breakdown.TYPE_CALLSITE, "child1");
        child1.measurement = 9;
        root.addChild(child1);

        Breakdown child2 = new Breakdown(Breakdown.TYPE_CALLSITE, "child2");
        child2.measurement = 1;
        root.addChild(child2);

        Breakdown child2child1 = new Breakdown(Breakdown.TYPE_CALLSITE, "child2child1");
        child2child1.measurement = 1;
        child2.addChild(child2child1);

        root.filter(2, 3, 100);

        assertTrue(root.findChild("child1") != null);
        assertTrue(root.findChild("child2") != null);
        assertTrue(child2.findChild("child2child1") == null);
    }


    @Test
    public void depth() throws Exception {
        Breakdown root = new Breakdown(Breakdown.TYPE_CALLGRAPH, "root");

        Breakdown child1 = new Breakdown(Breakdown.TYPE_CALLSITE, "child1");
        root.addChild(child1);

        Breakdown child2 = new Breakdown(Breakdown.TYPE_CALLSITE, "child2");
        root.addChild(child2);

        Breakdown child2child1 = new Breakdown(Breakdown.TYPE_CALLSITE, "child2child1");
        child2.addChild(child2child1);

        assertEquals(3, root.depth());
        assertEquals(1, child1.depth());
        assertEquals(2, child2.depth());
    }


    @Test
    public void addChild() throws Exception {
        Breakdown root = new Breakdown(Breakdown.TYPE_CALLGRAPH, "root");

        Breakdown child1 = new Breakdown(Breakdown.TYPE_CALLSITE, "child1");
        root.addChild(child1);

        assertEquals(1, root.children.size());
    }


    @Test
    public void removeChild() throws Exception {
        Breakdown root = new Breakdown(Breakdown.TYPE_CALLGRAPH, "root");

        Breakdown child1 = new Breakdown(Breakdown.TYPE_CALLSITE, "child1");
        root.removeChild(child1.name);

        assertTrue(root.findChild("child1") == null);
    }


    @Test
    public void increment() throws Exception {
        Breakdown b = new Breakdown(Breakdown.TYPE_CALLGRAPH, "root");
        b.increment(0.1, 1);
        b.increment(0.2, 2);

        assertEquals(0.3, b.measurement, 0.1);
        assertEquals(3, b.numSamples);
    }


    @Test
    public void propagate() throws Exception {
        Breakdown root = new Breakdown(Breakdown.TYPE_CALLGRAPH, "root");
        root.measurement = 1;
        root.numSamples = 1;

        Breakdown child = new Breakdown(Breakdown.TYPE_CALLSITE, "child");
        child.measurement = 2;
        child.numSamples = 1;
        root.addChild(child);

        root.propagate();

        assertEquals(3.0, root.measurement, 0.0);
        assertEquals(2, root.numSamples);

    }


    @Test
    public void normalize() throws Exception {
        Breakdown root = new Breakdown(Breakdown.TYPE_CALLGRAPH, "root");
        root.measurement = 20;

        Breakdown child = new Breakdown(Breakdown.TYPE_CALLSITE, "child");
        child.measurement = 10;
        root.addChild(child);

        root.normalize(5);

        assertEquals(4.0, root.measurement, 0.0);
        assertEquals(2.0, child.measurement, 0.0);

    }


    @Test
    public void evaluatePercent() throws Exception {
        Breakdown root = new Breakdown(Breakdown.TYPE_CALLGRAPH, "root");
        root.numSamples = 4;

        Breakdown child = new Breakdown(Breakdown.TYPE_CALLSITE, "child");
        child.numSamples = 2;
        root.addChild(child);

        root.evaluatePercent(10);

        assertEquals(40.0, root.measurement, 0.0);
        assertEquals(20.0, child.measurement, 0.0);
    }


}
