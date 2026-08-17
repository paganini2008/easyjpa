
package com.github.easyjpa;

/**
 * 
 * Thrown when an attribute name matches no path of the entity.
 * 
 * @Description: PathMismatchedException
 * @Author: Fred Feng
 * @Date: 24/03/2025
 * @Version 1.0.0
 */
public class PathMismatchedException extends IllegalArgumentException {

    private static final long serialVersionUID = 6298298843692196713L;

    public PathMismatchedException(String alias, String attributeName) {
        super(alias + "." + attributeName);
    }

}
