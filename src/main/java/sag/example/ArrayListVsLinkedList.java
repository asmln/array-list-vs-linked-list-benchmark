package sag.example;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput) // Измеряем количество операций в секунду
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1) // Прогрев JIT-компилятора
@Measurement(iterations = 5, time = 1) // Основные замеры
@Fork(1)
@State(Scope.Thread)
public class ArrayListVsLinkedList {
    @Param({"1000", "10000"}) // Размер коллекций для теста
    private int size;

    private List<Integer> arrayList;
    private List<Integer> linkedList;
    private int[] indices;

    static void main() throws RunnerException {
        var options = new OptionsBuilder()
                .include(ArrayListVsLinkedList.class.getSimpleName())
                .jvmArgs("--sun-misc-unsafe-memory-access=allow")
                .build();
        new Runner(options).run();
    }

    @Setup(Level.Trial)
    public void setup() {
        arrayList = new ArrayList<>();
        linkedList = new LinkedList<>();
        Random random = new Random(42);

        indices = new int[size];
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
            indices[i] = random.nextInt(size);
        }
    }

    // Наполнение ArrayList с постоянным расширением
    @Benchmark
    public List<Integer> testArrayListAdd() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(i); // O(1) амортизированно
        }
        return result;
    }

    // Наполнение LinkedList
    @Benchmark
    public List<Integer> testLinkedListAdd() {
        List<Integer> result = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            result.add(i); // ❌ O(1)
        }
        return result;
    }

    // Наполнение ArrayList с постоянным расширением через addFirst
    @Benchmark
    public List<Integer> testArrayListAddFirst() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.addFirst(i); // ❌ O(n)
        }
        return result;
    }

    // Наполнение LinkedList через addFirst
    @Benchmark
    public List<Integer> testLinkedListAddFirst() {
        List<Integer> result = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            result.addFirst(i); // O(1)
        }
        return result;
    }

    // Случайное чтение из ArrayList
    @Benchmark
    public int testArrayListRandomGet() {
        int sum = 0;
        for (int index : indices) {
            sum += arrayList.get(index); // O(1)
        }
        return sum;
    }

    // Случайное чтение из LinkedList
    @Benchmark
    public int testLinkedListRandomGet() {
        int sum = 0;
        for (int index : indices) {
            sum += linkedList.get(index); // ❌ O(N)
        }
        return sum;
    }

    // Итерирование по ArrayList
    @Benchmark
    public int testArrayListIteration() {
        int sum = 0;
        for (Integer value : arrayList) { // Использует кэш процессора
            sum += value;
        }
        return sum;
    }

    // Итерирование по LinkedList
    @Benchmark
    public int testLinkedListIteration() {
        int sum = 0;
        for (Integer value : linkedList) { // ❌ Промахи мимо кэша из-за разбросанных Node в памяти
            sum += value;
        }
        return sum;
    }
}
