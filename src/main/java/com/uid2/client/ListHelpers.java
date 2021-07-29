package com.uid2.client;

import java.util.List;
import java.util.function.BiPredicate;

class ListHelpers {
    // Returns index of the first element for which comp(value, listItem) returns false
    // If no such element is found, returns list.size()
    // Modelled after C++ std::upper_bound()
    static <T1, T2> int upperBound(List<T1> list, T2 value, BiPredicate<T2, T1> comp) {
        int it = 0;
        int first = 0;
        int count = list.size();
        int step = 0;
        while (count > 0) {
            step = count / 2;
            it = first + step;
            if (!comp.test(value, list.get(it))) {
                first = ++it;
                count -= step + 1;
            } else {
                count = step;
            }
        }
        return first;
    }
}
