package com.github.easyjpa;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 
 * Using SerializableFunction for lambda expression
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public interface SerializableFunction<X, Y> extends Function<X, Y>, Serializable {

}
