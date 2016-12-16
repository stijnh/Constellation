package ibis.constellation;

import java.util.Properties;

import ibis.constellation.impl.DistributedConstellation;
import ibis.constellation.impl.MultiThreadedConstellation;
import ibis.constellation.impl.SingleThreadedConstellation;

/**
 * The <code>ConstellationFactory</code> provides several static methods to create a {@link Constellation} instance.
 */
public class ConstellationFactory {

    /**
     * Prevent instantiation of this object type.
     */
    private ConstellationFactory() {
        // nothing
    }

    /**
     * Creates a constellation instance, using the specified executor and the system properties.
     *
     * If the system property <code>ibis.constellation.distributed</code> is not set, or set to "true", a distributed
     * constellation instance is created. If not, a singlethreaded constellation is created.
     *
     * @param e
     *            the executor
     * @return the constellation instance
     * @throws IllegalArgumentException
     *             thrown when no executors are supplied, or in case of incorrect property values.
     * @throws ConstellationCreationException
     *             thrown when the constellation instance could not be created for some reason.
     */
    public static Constellation createConstellation(Executor e) throws ConstellationCreationException {
        return createConstellation(System.getProperties(), e);
    }

    /**
     * Creates a constellation instance, using the specified executor and properties.
     *
     * If the property <code>ibis.constellation.distributed</code> is not set, or set to "true", a distributed constellation
     * instance is created. If not, a singlethreaded constellation is created.
     *
     * @param p
     *            the properties to use
     * @param e
     *            the executor
     * @return the constellation instance
     * @throws IllegalArgumentException
     *             thrown when no executors are supplied, or in case of incorrect property values.
     * @throws ConstellationCreationException
     *             thrown when the constellation instance could not be created for some reason.
     */
    public static Constellation createConstellation(Properties p, Executor e) throws ConstellationCreationException {
        return createConstellation(p, new Executor[] { e });
    }

    /**
     * Creates a constellation instance, using the specified executors and the system properties.
     *
     * If the system property <code>ibis.constellation.distributed</code> is not set, or set to "true", a distributed
     * constellation instance is created. If not, depending on the number of executors, either a multithreaded constellation or a
     * singlethreaded constellation is created.
     *
     * @param e
     *            the executors
     * @return the constellation instance
     * @throws IllegalArgumentException
     *             thrown when no executors are supplied, or in case of incorrect property values.
     * @throws ConstellationCreationException
     *             thrown when the constellation instance could not be created for some reason.
     */
    public static Constellation createConstellation(Executor... e) throws ConstellationCreationException {

        return createConstellation(System.getProperties(), e);
    }

    /**
     * Creates a constellation instance, using the specified executors and properties.
     *
     * If the property <code>ibis.constellation.distributed</code> is not set, or set to "true", a distributed constellation
     * instance is created. If not, depending on the number of executors, either a multithreaded constellation or a singlethreaded
     * constellation is created.
     *
     * @param p
     *            the properties
     * @param e
     *            the executors
     * @return the constellation instance
     * @throws IllegalArgumentException
     *             thrown when no executors are supplied, or in case of incorrect property values.
     * @throws ConstellationCreationException
     *             thrown when the constellation instance could not be created for some reason.
     */
    public static Constellation createConstellation(Properties p, Executor... e) throws ConstellationCreationException {

        if (e == null || e.length == 0) {
            throw new IllegalArgumentException("Need at least one executor!");
        }

        ConstellationProperties props;

        if (p instanceof ConstellationProperties) {
            props = (ConstellationProperties) p;
        } else {
            props = new ConstellationProperties(p);
        }

        boolean needsDistributed = props.DISTRIBUTED;

        return createConstellation(needsDistributed, props, e);
    }

    /**
     * Creates a constellation instance, using the specified executors and properties.
     *
     * If the <code>needsDistributed</code> parameter is set, a distributed constellation instance is created. If it is not set,
     * depending on the number of executors, either a multithreaded constellation or a singlethreaded constellation is created.
     *
     * @param needsDistributed
     *            when set, a distributed constellation instance is created
     * @param props
     *            the properties
     * @param e
     *            the executors
     * @return the constellation instance
     * @throws IllegalArgumentException
     *             thrown when no executors are supplied, or in case of incorrect property values.
     * @throws ConstellationCreationException
     *             thrown when the constellation instance could not be created for some reason.
     */
    public static Constellation createConstellation(boolean needsDistributed, ConstellationProperties props, Executor... e)
            throws ConstellationCreationException {

        DistributedConstellation d = null;

        if (needsDistributed) {
            d = new DistributedConstellation(props);
        }

        MultiThreadedConstellation m = null;

        if (needsDistributed || e.length > 1) {
            m = new MultiThreadedConstellation(d, props);
        }

        SingleThreadedConstellation s = null;

        for (Executor element : e) {
            s = new SingleThreadedConstellation(m, element, props);
        }

        if (d != null) {
            return d.getConstellation();
        }
        if (m != null) {
            return m.getConstellation();
        }
        return s.getConstellation();
    }

}
