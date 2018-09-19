package structures;

public interface PeekAheadStack<E> {


    /**
     *
     * @return size of this {@link PeekAheadStack}
     */
    int size();

    /**
     * Pushes an object to this {@link PeekAheadStack}
     * @param e the object to be pushed
     */
    void push(E e);

    /**
     * Pops the top of this {@link PeekAheadStack}
     * @return the top of the stack
     * @throws java.util.NoSuchElementException
     */
    E pop();

    /**
     * Peeks without removing the top of the {@link PeekAheadStack}
     * @return the object
     * @throws java.util.EmptyStackException
     */
    E peek();

    /**
     * Peeks without removing the n-th element of the {@link PeekAheadStack}
     * @param n
     * @return the object
     * @throws java.util.EmptyStackException
     * @throws IndexOutOfBoundsException
     */
    E peek(int n);

    /**
     * Replaces an existing value for a given one in the {@link PeekAheadStack}
     * @param e
     * @param o
     */
    void replace(E e, E o);

    /**
     *
     * @return true if this {@link PeekAheadStack} is empty
     */
    default boolean isEmpty(){
        return size() == 0;
    }


}
