package topicextraction.topicinf.datastruct;

/**
 * Factory for Distribution objects.
 */
public class DistributionFactory {
    private enum DistributionType {
        VALUESORTEDMAP, LAZYARRAY
    }

    //    private static final DistributionType DISTRIBUTION_TYPE = DistributionType.VALUESORTEDMAP;
    private static final DistributionType DISTRIBUTION_TYPE = DistributionType.LAZYARRAY;

    public static IDistributionInt createIntDistribution(){
        return new MediumFastDistributionInt();
    }

    public static <E> IDistribution<E> createDistribution() {

        if (DISTRIBUTION_TYPE == DistributionType.VALUESORTEDMAP) {
            return new Distribution<E>();
        } else if (DISTRIBUTION_TYPE == DistributionType.LAZYARRAY) {
            return new MediumFastDistribution<E>();
        } else {
            throw new RuntimeException("Unexpected DISTRIBUTION_TYPE (" + DISTRIBUTION_TYPE + ")");
        }
    }

    public static IDistributionInt copyDistribution(IDistributionInt copyDistr) {
        return new MediumFastDistributionInt((MediumFastDistributionInt) copyDistr);
    }
    
    public static <E> IDistribution<E> copyDistribution(IDistribution<E> copyDistr) {
        if (DISTRIBUTION_TYPE == DistributionType.VALUESORTEDMAP) {
            return new Distribution<E>(copyDistr);
        } else if (DISTRIBUTION_TYPE == DistributionType.LAZYARRAY) {
            return new MediumFastDistribution<E>(copyDistr);
        } else {
            throw new RuntimeException("Unexpected DISTRIBUTION_TYPE (" + DISTRIBUTION_TYPE + ")");
        }
    }
}
