package com.cloud.fizzbuzz;

public interface FizzBuzzService {
    /**
     * Takes in an argument and returns fizz if n is divisible by 3, buzz if n is divisible by 5, fizzbuzz if n is
     * divisible by both. If null is passed we use the active number of VMs instead of n.
     *
     * @param n an argument from user of API
     * @return a string based on logic above
     */
    String getDisplayText(Integer n);
}
