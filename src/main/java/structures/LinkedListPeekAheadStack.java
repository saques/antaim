package structures;

import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class LinkedListPeekAheadStack<E> implements PeekAheadStack<E> {

    private List<E> l = new LinkedList<>();

    @Override
    public int size() {
        return l.size();
    }

    @Override
    public void push(E e) {
        l.add(0, e);
    }

    @Override
    public E pop() {
        if(l.isEmpty())
            throw new NoSuchElementException();
        return l.remove(0);
    }

    @Override
    public E peek() {
        return peek(0);
    }

    @Override
    public E peek(int n) {
        if(l.isEmpty())
            throw new EmptyStackException();
        if(n >= l.size())
            throw new IndexOutOfBoundsException();
        return l.get(n);
    }

    @Override
    public void replace(E e, E o) {
        int idx = l.indexOf(e);
        if(idx == -1)
            return;
        l.set(idx, o);
    }
}
