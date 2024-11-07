package util;

import java.util.ArrayList;


/**
 * generic stacks using java.util.ArrayList
 * 
 * @author Michael Siff
 */
public class Stack<T> {
    private ArrayList<T> _array;
    
    /**
     * Constructs an empty stack.
     */
    public Stack() {
        _array = new ArrayList<T>();
    }
        

    /**
     * Returns true if stack is empty, false otherwise.
     * @return whether stack is empty
     */
    public boolean isEmpty() {
        return _array.isEmpty();
    }

    
    /**
     * Pushes specified element on top of stack.
     * @param element to be pushed on top of stack
     */
    public void push(T element) {
        _array.add(element);
    }

    
    /**
     * Removes and returns element currently on top of stack, assuming
     * stack not empty. (If empty, generates "stack underflow" error.)
     * @return element on top of stack that is removed
     */
    public T pop() {
        int n = _array.size();
        if (n > 0) {
            return _array.remove(n-1);
        } else {
            throw new RuntimeException("stack underflow");
        }
    }
    
}

