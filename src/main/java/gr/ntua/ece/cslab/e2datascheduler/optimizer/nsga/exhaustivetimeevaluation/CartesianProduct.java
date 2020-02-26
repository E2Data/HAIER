package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

/**
 * Cartesian Product implementation for Java 1.8
 */
class CartesianProduct {

    static List<?> product(final List<?>[] a) {
        System.err.println("I'M IN product(); len(a) = " + a.length);
        if (a.length >= 2) {
            List<?> product = a[0];
            for (int i = 1; i < a.length; i++) {
                product = productOfTwo(product, a[i]);
                System.err.println("productOfTwo returned: " + product);
            }
            return product;
        }

        //System.err.println("RETURNING emptyList()");
        //return emptyList();
        return a[0];
    }

    private static <A, B> List<?> productOfTwo(final List<A> a, final List<B> b) {
        System.err.println("\tI'M IN productOfTwo()");
        return of(a.stream()
                .map(e1 -> of(b.stream().map(e2 -> asList(e1, e2)).collect(toList())).orElse(emptyList()))
                .flatMap(List::stream)
                .collect(toList())).orElse(emptyList());
    }

    static <A> List<A> flatten(final List<?> l) {
        List<A> ret = new ArrayList<>();
        for (Object o : l) {
            if (o instanceof List) {
                ret.addAll(flatten((List<?>) o));
            } else {
                ret.add((A) o);
            }
        }
        return ret;
    }

}
