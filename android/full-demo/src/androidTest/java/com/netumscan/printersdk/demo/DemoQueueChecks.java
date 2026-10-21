// SPDX-License-Identifier: MIT
package com.netumscan.printersdk.demo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.netumscan.printersdk.CancellationContext;
import com.netumscan.printersdk.ConnectionOptions;
import com.netumscan.printersdk.DiscoveredDevice;
import com.netumscan.printersdk.ManagerResult;
import com.netumscan.printersdk.PixelImage;
import com.netumscan.printersdk.MediaMode;
import com.netumscan.printersdk.PrinterProfile;
import com.netumscan.printersdk.PrinterManager;
import com.netumscan.printersdk.RequestId;
import com.netumscan.printersdk.internal.BluetoothChannel;
import com.netumscan.printersdk.internal.IoControl;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Actual Demo Model and public Manager through JNI. The transport exists only in tests. */
public final class DemoQueueChecks {
    interface Progress { void accept(String stage); }
    private interface Condition { boolean get(); }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ConnectionOptions TARGET = namedTarget("AA:BB:CC:DD:EE:02");
    private static final LabelConfig LABEL = new LabelConfig(203, 256, 256, 128, 1, 2);
    private static final LabelConfig RECEIPT = new LabelConfig(203, 256, 256, 128, 1, 0, 0, true, 7);

    static final class Memory implements BluetoothChannel {
        volatile boolean open, blocked;
        volatile int maxWrite = Integer.MAX_VALUE, blockAfterBytes = Integer.MAX_VALUE;
        final AtomicInteger opens = new AtomicInteger(), writes = new AtomicInteger();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        public int open(IoControl control) { opens.incrementAndGet(); open = true; return 0; }
        public int write(byte[] data, IoControl control) {
            writes.incrementAndGet();
            while ((blocked || size() >= blockAfterBytes) && open && control.error() == 0) {
                try { Thread.sleep(1); } catch (InterruptedException error) { Thread.currentThread().interrupt(); return -1005; }
            }
            if (control.error() != 0) return -control.error();
            if (!open) return -2002;
            int accepted = Math.min(data.length, maxWrite);
            synchronized (bytes) { bytes.write(data, 0, accepted); }
            return accepted;
        }
        public boolean isOpen() { return open; }
        public void close() { open = false; }
        byte[] snapshot() { synchronized (bytes) { return bytes.toByteArray(); } }
        int size() { synchronized (bytes) { return bytes.size(); } }
    }
    /** Test discovery value feeds the production name resolver; it supplies no explicit Profile. */
    static ConnectionOptions namedTarget(String address) {
        try {
            Class<?> type = Class.forName("com.netumscan.printersdk.BluetoothDiscovery$Device");
            Constructor<?> raw = type.getDeclaredConstructor(String.class, String.class, java.util.List.class, Integer.class);
            raw.setAccessible(true);
            Constructor<DiscoveredDevice> discovered = DiscoveredDevice.class.getDeclaredConstructor(type);
            discovered.setAccessible(true);
            return ConnectionOptions.spp(discovered.newInstance(raw.newInstance(address, "LT-M90_AB12", Collections.emptyList(), null)));
        } catch (ReflectiveOperationException invalid) { throw new AssertionError(invalid); }
    }
    private static final class Harness implements AutoCloseable {
        final Memory memory = new Memory();
        final PrinterManager manager;
        final DemoModel model;
        Harness(Context context) throws Exception {
            manager = manager(context, memory);
            AtomicReference<DemoModel> result = new AtomicReference<>();
            main(() -> { result.set(new DemoModel(manager)); result.get().foreground(); result.get().connect(TARGET); });
            model = result.get();
            until(() -> model.state().canWork());
        }
        @Override public void close() {
            try {
                main(model::close);
                CountDownLatch done = new CountDownLatch(1);
                manager.close(result -> done.countDown());
                check(done.await(10, TimeUnit.SECONDS), "Queue test native cleanup deadline");
                check(!memory.open, "Queue test closes its memory transport");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt(); throw new AssertionError("Queue test cleanup interrupted", error);
            } catch (Exception error) { throw new AssertionError("Queue test cleanup failed", error); }
        }
    }
    private static ExecutorService pool() {
        return Executors.newCachedThreadPool(action -> {
            Thread thread = new Thread(action, "DemoQueueChecks"); thread.setDaemon(true); return thread;
        });
    }
    static PrinterManager manager(Context context, Memory memory) throws Exception {
        Class<?> factoryType = Class.forName("com.netumscan.printersdk.PrinterManager$Factory");
        Object factory = Proxy.newProxyInstance(factoryType.getClassLoader(), new Class<?>[]{factoryType}, (proxy, method, args) -> {
            Constructor<?> constructor = Class.forName("com.netumscan.printersdk.MobilePrinter")
                .getDeclaredConstructor(BluetoothChannel.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(memory, 2);
        });
        Constructor<PrinterManager> constructor = PrinterManager.class.getDeclaredConstructor(Context.class, factoryType,
            Executor.class, ExecutorService.class, ExecutorService.class);
        constructor.setAccessible(true);
        return constructor.newInstance(context, factory, (Executor)action -> MAIN.post(action), pool(), pool());
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void main(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1); AtomicReference<Throwable> failure = new AtomicReference<>();
        MAIN.post(() -> { try { action.run(); } catch (Throwable error) { failure.set(error); } finally { done.countDown(); } });
        check(done.await(10, TimeUnit.SECONDS), "Queue test main callback deadline");
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    private static void until(Condition condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        do {
            AtomicBoolean done = new AtomicBoolean(); main(() -> done.set(condition.get()));
            if (done.get()) return;
            Thread.sleep(2);
        } while (System.nanoTime() < end);
        throw new AssertionError("Queue test completion deadline");
    }
    private static Object field(Object value, String name) throws Exception {
        Field field = value.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(value);
    }
    private static CancellationContext cancellation() throws Exception {
        Constructor<CancellationContext> constructor = CancellationContext.class.getDeclaredConstructor(long.class);
        constructor.setAccessible(true); return constructor.newInstance(Long.MAX_VALUE);
    }
    private static int sourceReferences(Object shared) {
        synchronized (shared) {
            try { return (Integer)field(shared, "references"); }
            catch (Exception error) { throw new AssertionError("Image source reference inspection", error); }
        }
    }
    private static void rejects(Runnable action, String message) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message);
    }
    private static void sampleDescriptions() throws Exception {
        for (DemoPixelSamples.Example example : DemoPixelSamples.Example.values()) {
            for (LabelConfig config : new LabelConfig[]{LABEL, RECEIPT}) {
                try (DemoPrintData data = DemoPixelSamples.create(example, config)) {
                    check(data.options != null, "Every sample builds the new unified PrintOptions");
                }
            }
        }
        byte[] rgba = DemoPixelSamples.pixels(PixelImage.RGBA32, 256, 128, () -> false);
        boolean transparent = false, translucent = false, opaque = false;
        for (int index = 3; index < rgba.length; index += 4) {
            int alpha = rgba[index] & 255;
            transparent |= alpha == 0; translucent |= alpha > 0 && alpha < 255; opaque |= alpha == 255;
        }
        check(transparent && translucent && opaque, "RGBA input demonstrates all three alpha ranges");
        rejects(() -> DemoPixelSamples.validate(null, LABEL), "Null sample must fail validation");
        rejects(() -> DemoPixelSamples.validate(DemoPixelSamples.Example.RGB, null), "Null configuration must fail validation");
        try { DemoPixelSamples.pixels(PixelImage.RGB24, 256, 128, () -> true); }
        catch (CancellationException expected) { return; }
        throw new AssertionError("Sample generation must cooperate with cancellation");
    }
    private static DemoUiState.PrintRequest find(DemoModel model, RequestId id) {
        for (DemoUiState.PrintRequest request : model.state().printRequests) if (request.requestId == id) return request;
        throw new AssertionError("Retained request identity missing");
    }
    private static void identities(DemoUiState state) {
        IdentityHashMap<RequestId, Boolean> ids = new IdentityHashMap<>(); long previous = 0;
        int pending = 0;
        for (DemoUiState.PrintRequest request : state.printRequests) {
            check(request.sequence > previous && request.description != null && !request.description.isEmpty(), "History preserves submission sequence and descriptions");
            previous = request.sequence;
            check(ids.put(request.requestId, Boolean.TRUE) == null, "Each row has one distinct RequestId");
            if (request.result == null) pending++;
            else check(request.result.kind == DemoResult.Kind.PRINT && request.result.raw.requestId == request.requestId,
                "Terminal result remains attached to the original request identity");
        }
        check(pending == state.pendingPrintCount() && pending <= 8 && state.printRequests.size() <= 32,
            "Pending and history budgets describe the actual retained requests");
    }
    private static RequestId last(DemoModel model) {
        List<DemoUiState.PrintRequest> requests = model.state().printRequests;
        return requests.get(requests.size() - 1).requestId;
    }
    private static byte[] since(Memory memory, int start) { return Arrays.copyOfRange(memory.snapshot(), start, memory.size()); }
    private static byte[] concat(byte[] first, byte[] second) {
        byte[] value = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, value, first.length, second.length); return value;
    }
    private static void configure(DemoModel model, LabelConfig config) throws Exception {
        main(() -> {
            PrinterProfile current = model.printProfile();
            PrinterProfile target = config.printProfile();
            if (current == null || current.protocol != target.protocol || current.dpi != target.dpi ||
                    current.maxPrintWidthDots != target.maxPrintWidthDots || current.dotsPerMm != target.dotsPerMm ||
                    current.labelAlignment != target.labelAlignment) model.configureProfile(target, MediaMode.GAP);
            else if (!model.configured()) model.retryInitialize();
        });
        until(() -> model.state().canWork());
    }
    private static byte[] print(Harness harness, DemoPixelSamples.Example example, LabelConfig config) throws Exception {
        configure(harness.model, config);
        int start = harness.memory.size();
        main(() -> harness.model.printSample(example, config));
        until(() -> harness.model.state().pendingPrintCount() == 0);
        main(() -> {
            DemoUiState.PrintRequest result = find(harness.model, last(harness.model));
            check(result.result.raw.code == 0 && result.result.raw.completion == ManagerResult.Completion.DATA_SENT,
                "Sample reaches real native DATA_SENT completion: " + example + ", receipt=" + config.receipt +
                ", code=" + result.result.raw.code + ", message=" + result.result.raw.message); identities(harness.model.state());
        });
        return since(harness.memory, start);
    }
    private static void receiptEncoding(byte[] encoded, LabelConfig config) {
        check(encoded.length > 6 && encoded[0] == 27 && encoded[1] == 64 && encoded[2] == 29 && encoded[3] == 80,
            "Receipt sample uses the real ESC/POS encoder");
        int cursor = 6, rasterCount = 0, imageHeight = 0, feeds = 0;
        while (cursor < encoded.length) {
            if (encoded[cursor] == 29 && cursor + 8 <= encoded.length && encoded[cursor + 1] == 118 && encoded[cursor + 2] == 48) {
                int width = (encoded[cursor + 4] & 255) | (encoded[cursor + 5] & 255) << 8;
                int height = (encoded[cursor + 6] & 255) | (encoded[cursor + 7] & 255) << 8;
                cursor += 8 + width * height; rasterCount++; imageHeight += height;
            } else if (encoded[cursor] == 27 && cursor + 3 <= encoded.length && encoded[cursor + 1] == 74) {
                feeds += encoded[cursor + 2] & 255; cursor += 3;
            } else throw new AssertionError("Unexpected command in bounded receipt sample at " + cursor);
        }
        check(cursor == encoded.length && rasterCount >= 2 && feeds > config.trailingBlankDots &&
            imageHeight + feeds == config.height + config.trailingBlankDots, "Native receipt encoding preserves multiple images and blank segments");
    }
    private static List<byte[]> labelRasters(byte[] encoded) {
        String text = new String(encoded, StandardCharsets.ISO_8859_1); List<byte[]> rasters = new ArrayList<>();
        int cursor = 0;
        while ((cursor = text.indexOf("CG ", cursor)) >= 0) {
            cursor += 3; int[] dimensions = new int[4];
            for (int index = 0; index < dimensions.length; index++) {
                int end = text.indexOf(' ', cursor);
                check(end > cursor, "CPCL raster has complete dimensions");
                dimensions[index] = Integer.parseInt(text.substring(cursor, end)); cursor = end + 1;
            }
            int size = dimensions[0] * dimensions[1];
            check(size > 0 && cursor + size <= encoded.length, "CPCL raster payload is bounded by encoded dimensions");
            rasters.add(Arrays.copyOfRange(encoded, cursor, cursor + size)); cursor += size;
        }
        return rasters;
    }
    private static void labelEncoding(byte[] encoded, DemoPixelSamples.Example example) {
        String text = new String(encoded, StandardCharsets.ISO_8859_1);
        check(text.contains("PAGE-WIDTH 256\r\n") && text.endsWith("PRINT\r\n"), "Pixel samples pass the real CPCL encoder");
        List<byte[]> rasters = labelRasters(encoded);
        int expected = example == DemoPixelSamples.Example.DITHERING ? 4 : example == DemoPixelSamples.Example.MULTI_IMAGE ? 3 : 1;
        check(rasters.size() == expected, "Every sample image reaches one bounded CPCL raster");
        if (example == DemoPixelSamples.Example.RGBA) {
            byte[] raster = rasters.get(0); int blackMiddle = 0, middlePixels = 0;
            for (int y = 0; y < 128; y++) {
                check((raster[y * 32] & 128) == 0 && (raster[y * 32 + 31] & 1) != 0,
                    "Native RGBA processing composites transparent black onto white and retains opaque black");
                for (int x = 88; x < 168; x++) {
                    if ((raster[y * 32 + x / 8] & (128 >> (x % 8))) != 0) blackMiddle++;
                    middlePixels++;
                }
            }
            check(blackMiddle > 0 && blackMiddle < middlePixels, "Native RGBA dithering preserves half-transparent coverage");
        }
        if (example == DemoPixelSamples.Example.DITHERING) {
            int different = 0;
            for (int index = 1; index < rasters.size(); index++)
                if (!Arrays.equals(rasters.get(0), rasters.get(index))) different++;
            check(different == 3, "Native algorithms produce distinct threshold and dithered gradient rasters");
        }
        if (example == DemoPixelSamples.Example.MULTI_IMAGE)
            check(Arrays.equals(rasters.get(0), rasters.get(1)), "Reused immutable pixels reach both distinct label placements");
    }
    private static void fifoAndCancellation(Harness harness, EnumMap<DemoPixelSamples.Example, byte[]> baseline) throws Exception {
        DemoModel model = harness.model; Memory memory = harness.memory;
        int before = memory.size(), writes = memory.writes.get(); memory.blocked = true;
        main(() -> model.printSample(DemoPixelSamples.Example.RGB, LABEL));
        until(() -> memory.writes.get() > writes);
        AtomicReference<DemoUiState> snapshot = new AtomicReference<>(); AtomicReference<RequestId> middle = new AtomicReference<>();
        main(() -> {
            model.printSample(DemoPixelSamples.Example.RGBA, LABEL); middle.set(last(model));
            model.printSample(DemoPixelSamples.Example.MULTI_IMAGE, LABEL); snapshot.set(model.state());
            check(model.state().pendingPrintCount() == 3 && model.state().canSubmitPrint(), "Connected Model admits multiple independent requests");
            model.cancel(middle.get());
        });
        until(() -> find(model, middle.get()).result != null);
        main(() -> check(find(model, middle.get()).result.raw.code == 1005 &&
            find(model, middle.get()).result.raw.outcome == ManagerResult.Outcome.CANCELLED,
            "Selected queued request receives its own cancellation terminal"));
        check(memory.size() == before, "Cancelling a waiting request sends no bytes while first write is gated");
        memory.blocked = false; until(() -> model.state().pendingPrintCount() == 0);
        check(Arrays.equals(since(memory, before), concat(baseline.get(DemoPixelSamples.Example.RGB), baseline.get(DemoPixelSamples.Example.MULTI_IMAGE))),
            "First and third requests retain FIFO order; cancelled second contributes zero bytes");
        main(() -> {
            identities(model.state());
            for (DemoUiState.PrintRequest request : snapshot.get().printRequests)
                if (request.requestId == middle.get()) check(request.result == null, "Published pending snapshots stay immutable after completion");
            try { snapshot.get().printRequests.add(null); } catch (UnsupportedOperationException expected) { return; }
            throw new AssertionError("Request history must be immutable");
        });
    }
    private static void budgetsAndBatch(Harness harness, byte[] rgb, Progress progress) throws Exception {
        DemoModel model = harness.model; Memory memory = harness.memory;
        int before = memory.size();
        main(() -> model.printBatch(null, LABEL, DemoPixelSamples.Example.RGB));
        until(() -> model.state().pendingPrintCount() == 0);
        check(Arrays.equals(since(memory, before), concat(concat(rgb, rgb), rgb)), "Batch submits three complete SDK requests");
        progress.accept("queue/budgets-batch-three-complete");
        memory.blocked = true; int writes = memory.writes.get();
        AtomicReference<RequestId> cancelled = new AtomicReference<>();
        main(() -> {
            model.printBatch(null, LABEL, DemoPixelSamples.Example.RGB);
            model.printBatch(null, LABEL, DemoPixelSamples.Example.RGB);
            model.printSample(DemoPixelSamples.Example.RGBA, LABEL); cancelled.set(last(model));
            model.printSample(DemoPixelSamples.Example.RGB, LABEL);
            check(model.state().pendingPrintCount() == 8 && !model.state().canSubmitPrint() && !model.state().canSubmitPrints(3),
                "Demo permits eight pending requests and exposes the capacity guard");
            int size = model.state().printRequests.size(); RequestId previous = last(model);
            model.printBatch(null, LABEL, DemoPixelSamples.Example.RGB); model.printSample(DemoPixelSamples.Example.RGB, LABEL);
            check(model.state().pendingPrintCount() == 8 && model.state().printRequests.size() == size && last(model) == previous,
                "Capacity rejection does not partially submit a batch");
            progress.accept("queue/budgets-eight-admitted");
            model.cancel(cancelled.get());
        });
        until(() -> memory.writes.get() > writes && model.state().pendingPrintCount() == 7);
        progress.accept("queue/budgets-cancelled-to-seven");
        main(() -> { model.printSample(DemoPixelSamples.Example.RGB, LABEL); check(model.state().pendingPrintCount() == 8, "A finished cancellation releases one pending slot"); });
        memory.blocked = false; until(() -> model.state().pendingPrintCount() == 0);
        progress.accept("queue/budgets-refilled-eight-complete");
        for (int index = 0; index < 34; index++) {
            print(harness, DemoPixelSamples.Example.RGB, LABEL);
            if ((index + 1) % 8 == 0) progress.accept("queue/budgets-history-" + (index + 1));
        }
        progress.accept("queue/budgets-history-thirty-four-complete");
        main(() -> { identities(model.state()); check(model.state().printRequests.size() == 32, "Terminal history evicts old entries at thirty-two"); });
        memory.blocked = true;
        main(() -> {
            for (int index = 0; index < 8; index++) model.printSample(DemoPixelSamples.Example.RGB, LABEL);
            identities(model.state()); check(model.state().pendingPrintCount() == 8 && model.state().printRequests.size() == 32,
                "History eviction retains every pending request");
        });
        memory.blocked = false; until(() -> model.state().pendingPrintCount() == 0);
        progress.accept("queue/budgets-history-eight-complete");
        main(() -> {
            int size = model.state().printRequests.size(); RequestId last = last(model);
            rejects(() -> model.printSample(null, LABEL), "Invalid sample must fail before submission");
            rejects(() -> model.printSample(DemoPixelSamples.Example.RGB, null), "Invalid sample config must fail before submission");
            check(model.state().printRequests.size() == size && last(model) == last, "Validation failure preserves all valid request rows");
        });
    }
    private static void lifecycle(Harness harness, byte[] rgb) throws Exception {
        DemoModel model = harness.model; Memory memory = harness.memory;
        int before = memory.size(), writes = memory.writes.get(), opens = memory.opens.get(); memory.blocked = true;
        main(() -> model.printBatch(null, LABEL, DemoPixelSamples.Example.RGB));
        until(() -> memory.writes.get() > writes);
        main(model::background);
        until(() -> model.state().phase == DemoUiState.Phase.OFFLINE && model.state().pendingPrintCount() == 0);
        memory.blocked = false;
        check(memory.size() == before && !memory.open, "Background cancellation joins transport cleanup without a late write");
        main(() -> { identities(model.state()); model.foreground(); check(model.state().canConnect(), "Foreground requires a new explicit connection"); });
        check(memory.opens.get() == opens && memory.size() == before, "Foreground does not reconnect or replay interrupted requests");
        main(() -> model.connect(TARGET)); until(() -> model.state().canWork());
        check(new String(since(memory, before), StandardCharsets.ISO_8859_1).contains("GAP-SENSE") &&
            !new String(since(memory, before), StandardCharsets.ISO_8859_1).contains("CG ") && memory.opens.get() == opens + 1,
            "Explicit reconnect performs only fresh initialization, never replays old print requests");
        check(Arrays.equals(print(harness, DemoPixelSamples.Example.RGB, LABEL), rgb), "A fresh request works after explicit reconnect");
        memory.blocked = true; int finalWrites = memory.writes.get(), finalSize = memory.size();
        main(() -> model.printBatch(null, LABEL, DemoPixelSamples.Example.RGB)); until(() -> memory.writes.get() > finalWrites);
        main(model::close);
        CountDownLatch done = new CountDownLatch(1); harness.manager.close(result -> done.countDown());
        check(done.await(10, TimeUnit.SECONDS), "Closing Model waits for native requests through Manager cleanup");
        memory.blocked = false;
        main(() -> {
            check(model.state().phase == DemoUiState.Phase.CLOSED, "Close is terminal for the Model");
            model.foreground(); model.connect(TARGET); model.printSample(DemoPixelSamples.Example.RGB, LABEL);
            check(model.state().phase == DemoUiState.Phase.CLOSED, "Closed Model ignores later interaction");
        });
        check(memory.size() == finalSize && memory.opens.get() == opens + 1 && !memory.open, "Closing pending requests produces neither late IO nor replay");
    }
    /** Android-only bitmap generation exercises the batch's independent source leases. */
    private static void sourceBatch(Harness harness, Progress progress) throws Exception {
        DemoModel model = harness.model; Memory memory = harness.memory;
        DemoImageSource source = DemoImageSource.builtin();
        Object shared = field(source, "shared");
        try {
            progress.accept("queue/source-baseline");
            int before = memory.size();
            main(() -> model.print(source, LABEL)); until(() -> model.state().pendingPrintCount() == 0);
            byte[] baseline = since(memory, before);
            main(() -> check(find(model, last(model)).result.raw.code == 0, "Built-in source baseline prints through JNI"));
            before = memory.size(); int writes = memory.writes.get(); memory.blocked = true;
            AtomicReference<RequestId> middle = new AtomicReference<>();
            main(() -> {
                model.printBatch(source, LABEL, null);
                List<DemoUiState.PrintRequest> requests = model.state().printRequests;
                middle.set(requests.get(requests.size() - 2).requestId);
            });
            until(() -> memory.writes.get() > writes);
            progress.accept("queue/source-batch-admitted");
            // .8 may release source reads once native jobs own their pixels, before IO completes.
            // Verify accepted work and output survival instead of a scheduler-dependent lease count.
            main(() -> check(model.state().pendingPrintCount() == 3,
                "All three accepted batch requests remain pending behind blocked IO"));
            check(memory.size() == before && sourceReferences(shared) >= 1,
                "Blocked IO has no new output and the caller still owns its source");
            source.close();
            main(() -> model.cancel(middle.get())); until(() -> find(model, middle.get()).result != null);
            main(() -> check(find(model, middle.get()).result.raw.outcome == ManagerResult.Outcome.CANCELLED &&
                find(model, middle.get()).result.raw.code == 1005,
                "Middle request receives its own cancellation result after caller source release"));
            progress.accept("queue/source-middle-cancelled");
            memory.blocked = false; until(() -> model.state().pendingPrintCount() == 0);
            progress.accept("queue/source-batch-terminal");
            check(Arrays.equals(since(memory, before), concat(baseline, baseline)),
                "Caller release and middle cancellation preserve the other two batch source reads");
            until(() -> sourceReferences(shared) == 0);
            progress.accept("queue/source-leases-released");
            main(() -> {
                identities(model.state()); RequestId prior = last(model);
                try { model.printBatch(source, LABEL, null); }
                catch (CancellationException expected) { check(last(model) == prior, "Released-source batch admits no partial request"); return; }
                throw new AssertionError("Released source cannot be submitted again");
            });
        } finally { source.close(); memory.blocked = false; }
    }
    private static void partialSendingCancellation(Context context) throws Exception {
        try (Harness harness = new Harness(context)) {
            DemoModel model = harness.model; Memory memory = harness.memory;
            int initial = memory.size(), initialWrites = memory.writes.get();
            memory.maxWrite = 16; memory.blockAfterBytes = initial + 32;
            AtomicReference<RequestId> first = new AtomicReference<>();
            main(() -> {
                model.printBatch(null, LABEL, DemoPixelSamples.Example.RGB);
                first.set(model.state().printRequests.get(0).requestId);
            });
            until(() -> memory.size() == initial + 32 && memory.writes.get() >= initialWrites + 3);
            main(() -> model.cancel(first.get()));
            until(() -> model.state().pendingPrintCount() == 0 && !model.state().connected);
            main(() -> {
                identities(model.state());
                DemoResult<Void> result = find(model, first.get()).result;
                check(result.raw.code == 1005 && result.raw.outputMayHaveOccurred,
                    "Partial-send cancellation preserves the real possible-output flag on its RequestId");
                for (DemoUiState.PrintRequest request : model.state().printRequests)
                    check(request.result.raw.code != 0, "Conservative sending cancellation terminates every affected request");
            });
            check(memory.size() == initial + 32 && memory.opens.get() == 1, "Partial-send cancellation does not write or reconnect after the cancellation boundary");
            memory.maxWrite = Integer.MAX_VALUE; memory.blockAfterBytes = Integer.MAX_VALUE;
            main(() -> model.connect(TARGET)); until(() -> model.state().canWork());
            check(memory.size() > initial + 32 && memory.opens.get() == 2 &&
                new String(since(memory, initial + 32), StandardCharsets.ISO_8859_1).contains("GAP-SENSE"),
                "Explicit reconnect reinitializes without replaying cancelled print requests");
        }
    }
    public static void main(String[] args) throws Exception {
        Context context = (Context)Class.forName("android.content.Context").getDeclaredConstructor().newInstance();
        run(context, false, stage -> { });
    }
    static void run(Context context) throws Exception { run(context, stage -> { }); }
    static void run(Context context, Progress progress) throws Exception { run(context, true, progress); }
    private static void run(Context context, boolean androidBitmaps, Progress progress) throws Exception {
        progress.accept("queue/sample-descriptions");
        sampleDescriptions();
        progress.accept("queue/connect");
        try (Harness harness = new Harness(context)) {
            EnumMap<DemoPixelSamples.Example, byte[]> baseline = new EnumMap<>(DemoPixelSamples.Example.class);
            for (DemoPixelSamples.Example example : DemoPixelSamples.Example.values()) {
                progress.accept("queue/native-sample-" + example);
                byte[] label = print(harness, example, LABEL); baseline.put(example, label);
                labelEncoding(label, example);
                receiptEncoding(print(harness, example, RECEIPT), RECEIPT);
            }
            for (int width : new int[]{129, 131}) {
                progress.accept("queue/odd-width-" + width);
                LabelConfig offset = new LabelConfig(203, 256, width, 128, 2, 1, 120, false, 9);
                LabelConfig receipt = new LabelConfig(203, 256, width, 128, 2, 0, 0, true, 11);
                for (DemoPixelSamples.Example example : DemoPixelSamples.Example.values()) {
                    check(print(harness, example, offset).length > 0 && print(harness, example, receipt).length > 0,
                        "Odd content widths preserve byte-packed boundaries, label offset, copies and receipt blanks");
                }
            }
            progress.accept("queue/fifo-cancel");
            configure(harness.model, LABEL);
            fifoAndCancellation(harness, baseline);
            progress.accept("queue/budgets-and-batch");
            budgetsAndBatch(harness, baseline.get(DemoPixelSamples.Example.RGB), progress);
            if (androidBitmaps) sourceBatch(harness, progress);
            progress.accept("queue/lifecycle");
            lifecycle(harness, baseline.get(DemoPixelSamples.Example.RGB));
            progress.accept("queue/harness-cleanup");
        }
        progress.accept("queue/partial-sending-cancel");
        partialSendingCancellation(context);
        progress.accept("queue/complete");
    }
}
