package mx.edu.utez.rete.estructuradatos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class ListaEnlazada<T> {
    private Nodo<T> cabeza;
    private int tamanio;

    public ListaEnlazada() {
        this.cabeza = null;
        this.tamanio = 0;
    }

    public void insertar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            Nodo<T> actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevo);
        }
        tamanio++;
    }

    public T buscar(Predicate<T> criterio) {
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (criterio.test(actual.getDato())) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null;
    }

    public ListaEnlazada<T> filtrar(Predicate<T> criterio) {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (criterio.test(actual.getDato())) {
                resultado.insertar(actual.getDato());
            }
            actual = actual.getSiguiente();
        }
        return resultado;
    }

    // Ordenamiento Burbuja sobre los nodos (ideal para demostrar algoritmos de ordenamiento)
    public void ordenar(Comparator<T> comparador) {
        if (cabeza == null || cabeza.getSiguiente() == null) return;
        boolean intercambiado;
        do {
            intercambiado = false;
            Nodo<T> actual = cabeza;
            while (actual.getSiguiente() != null) {
                if (comparador.compare(actual.getDato(), actual.getSiguiente().getDato()) > 0) {
                    T temp = actual.getDato();
                    actual.setDato(actual.getSiguiente().getDato());
                    actual.getSiguiente().setDato(temp);
                    intercambiado = true;
                }
                actual = actual.getSiguiente();
            }
        } while (intercambiado);
    }

    public int getTamanio() { return tamanio; }

    // Conversión únicamente para serializar respuesta JSON en el Controller
    public List<T> toList() {
        List<T> lista = new ArrayList<>();
        Nodo<T> actual = cabeza;
        while (actual != null) {
            lista.add(actual.getDato());
            actual = actual.getSiguiente();
        }
        return lista;
    }

    public boolean eliminar(Predicate<T> criterio) {
        if (cabeza == null) return false;

        if (criterio.test(cabeza.getDato())) {
            cabeza = cabeza.getSiguiente();
            tamanio--;
            return true;
        }

        Nodo<T> actual = cabeza;
        while (actual.getSiguiente() != null) {
            if (criterio.test(actual.getSiguiente().getDato())) {
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                tamanio--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }
}