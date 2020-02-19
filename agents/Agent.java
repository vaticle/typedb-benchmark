package grakn.simulation.agents;

import grakn.client.GraknClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * An agent that will execute some logic across the
 */
public abstract class Agent<T extends AgentItem> implements AutoCloseable {

    private AgentContext agentContext;
    private Random random;
    private T item;

    private GraknClient.Transaction tx;

    private LocalDateTime today;

    protected Random random() {
        return random;
    }

    protected T item() {
        return item;
    }

    protected String tracker() {
        return item.getTracker();
    }

    protected GraknClient.Session session() {
        return agentContext.getIterationGraknSessionFor(getSessionKey());
    }

    protected GraknClient.Transaction.Builder transaction() {
        return agentContext.getIterationGraknSessionFor(getSessionKey()).transaction();
    }

    protected String getSessionKey() {
        return item.getSessionKey();
    }

    protected GraknClient.Transaction tx() {
        if (tx == null) {
            tx = transaction().write();
        }
        return tx;
    }

    protected void closeTx() {
        if (tx != null) {
            tx.close();
            tx = null;
        }
    }

    protected void setTx(GraknClient.Transaction tx) {
        closeTx();
        this.tx = tx;
    }

    protected World world() {
        return agentContext.getWorld();
    }

    protected LocalDateTime today() {
        if (today == null) {
            today = agentContext.getLocalDateTime();
        }
        return today;
    }

    protected int simulationStep() {
        return agentContext.getSimulationStep();
    }

    protected void shuffle(List<?> list) {
        Collections.shuffle(list, random);
    }

    protected <T> T pickOne(List<T> list) {
        return list.get(random().nextInt(list.size()));
    }

    @Override
    public void close() {
        closeTx();
    }

    protected abstract void iterate();
}
