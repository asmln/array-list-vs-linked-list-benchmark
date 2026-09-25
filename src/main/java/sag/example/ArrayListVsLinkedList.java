package sag.example;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
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
    public void setup1() {
        Random random = new Random(42);
        indices = new int[size];
        for (int i = 0; i < size; i++) {
            indices[i] = random.nextInt(size);
        }
    }

    @Setup(Level.Invocation)
    public void setup() {
        arrayList = new ArrayList<>();
        linkedList = new LinkedList<>();

        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }
    }

    // Наполнение ArrayList с постоянным расширением
    @Benchmark
    public List<Integer> a_testArrayListAdd() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(i); // 👍 O(1) амортизированно
        }
        return result;
    }

    // Наполнение LinkedList
    @Benchmark
    public List<Integer> a_testLinkedListAdd() {
        List<Integer> result = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            result.add(i); // 👎 O(1)
        }
        return result;
    }

    // Наполнение ArrayList с постоянным расширением через addFirst
    @Benchmark
    public List<Integer> b_testArrayListAddFirst() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.addFirst(i); // 👎 O(n)
        }
        return result;
    }

    // Наполнение LinkedList через addFirst
    @Benchmark
    public List<Integer> b_testLinkedListAddFirst() {
        List<Integer> result = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            result.addFirst(i); // 👍 O(1)
        }
        return result;
    }

    // Случайное чтение из ArrayList
    @Benchmark
    public int c_testArrayListRandomGet() {
        int sum = 0;
        for (int index : indices) {
            sum += arrayList.get(index); // 👍 O(1)
        }
        return sum;
    }

    // Случайное чтение из LinkedList
    @Benchmark
    public int c_testLinkedListRandomGet() {
        int sum = 0;
        for (int index : indices) {
            sum += linkedList.get(index); // 👎 O(N)
        }
        return sum;
    }

    // Итерирование по ArrayList
    @Benchmark
    public int d_testArrayListIteration() {
        int sum = 0;
        for (Integer value : arrayList) { // 👍 Использует кэш процессора
            sum += value;
        }
        return sum;
    }

    // Итерирование по LinkedList
    @Benchmark
    public int d_testLinkedListIteration() {
        int sum = 0;
        for (Integer value : linkedList) { // 👎 Промахи мимо кэша из-за разбросанных Node в памяти
            sum += value;
        }
        return sum;
    }

    @Benchmark
    public int e_testArrayListIteratorRemoval() {
        Iterator<Integer> iterator = arrayList.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            Integer value = iterator.next();
            // Удаляем каждый второй элемент
            if (value % 2 == 0) {
                iterator.remove(); // 👎 O(N) — сдвиг хвоста массива влево
                count++;
            }
        }
        return count;
    }

    @Benchmark
    public int e_testLinkedListIteratorRemoval() {
        Iterator<Integer> iterator = linkedList.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            Integer value = iterator.next();
            // Удаляем каждый второй элемент
            if (value % 2 == 0) {
                iterator.remove(); // 👍 O(1)
                count++;
            }
        }
        return count;
    }
}
