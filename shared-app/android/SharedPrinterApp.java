import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.netumscan.printersdk.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Application-scoped owner for one endpoint. All public control calls belong on the main thread.
 * Pages detach their own notifications without cancelling accepted work or closing the shared device.
 * One request per page; requests from different pages share the SDK's native FIFO. No automatic retry/replay. */
public final class SharedPrinterApp implements AutoCloseable {
    public interface Ui {
        void connection(Throwable failure);
        void state(DeviceStateSnapshot snapshot);
        void completed(PrintOperation.Result result);
        void failed(Throwable failure);
        void disconnected(Throwable failure);
    }
    interface Submitter {
        PrintOperation submit(MobilePrinter printer, byte[] gray8, int width, int height,
                              CpclProfile profile, int copies, int timeoutMs);
    }
    private final Context application;
    private final String address;
    private final BleOptions ble;
    private final PrinterManager manager;
    private final ExecutorService preparations;
    private final Executor main;
    private final Submitter submitter;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicLong epoch = new AtomicLong(1);
    private final Set<PageBinding> pages = Collections.newSetFromMap(new ConcurrentHashMap<PageBinding, Boolean>());
    private final Set<PrintRequest> requests = Collections.newSetFromMap(new ConcurrentHashMap<PrintRequest, Boolean>());
    private MobilePrinter printer; // Main-thread state; workers capture an exact device and epoch.
    private PrintOperation.Result lastResult; // One immutable terminal copy; never retains source pixels.
    private boolean releasing;
    private long connectionAttempt;

    /** null BleOptions selects SPP; the App supplies permissions, endpoint selection and verified BLE options. */
    public SharedPrinterApp(Context context, String address, BleOptions ble) {
        this(application(context), address, ble, new PrinterManager(), Executors.newSingleThreadExecutor(),
            mainDispatcher(), (device, gray8, width, height, profile, copies, timeoutMs) ->
                device.submitLabelGray8(gray8, width, height, profile, copies, 0, profile.xOffsetDots, 0, profile.form, timeoutMs));
    }
    // Package-local dependencies let the host fixture control preparation/dispatcher races without SDK hooks.
    SharedPrinterApp(Context application, String address, BleOptions ble, PrinterManager manager,
                     ExecutorService preparations, Executor main, Submitter submitter) {
        this.application = application; this.address = address; this.ble = ble;
        this.manager = Objects.requireNonNull(manager); this.preparations = Objects.requireNonNull(preparations);
        this.main = Objects.requireNonNull(main); this.submitter = Objects.requireNonNull(submitter);
        try { requireMain(); printer = acquire(); }
        catch (RuntimeException | Error failure) {
            preparations.shutdown();
            try { manager.close(); } catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    private static Context application(Context context) {
        requireMain(); return Objects.requireNonNull(Objects.requireNonNull(context).getApplicationContext());
    }
    private static Executor mainDispatcher() {
        Handler handler = new Handler(Looper.getMainLooper());
        return callback -> { if (!handler.post(callback)) throw new RejectedExecutionException("Main looper unavailable"); };
    }
    private MobilePrinter acquire() { return ble == null ? manager.spp(application, address) : manager.ble(application, address, ble); }
    private void requireOpen() { if (stopped.get()) throw new IllegalStateException("Application printer owner is closed"); }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Call on the main thread");
    }
    public PageBinding bind(Ui ui) {
        requireMain(); requireOpen(); PageBinding page = new PageBinding(Objects.requireNonNull(ui)); pages.add(page);
        try {
            requireOpen();
            if (printer != null) page.watch(printer, epoch.get()); else page.dispatch(target -> target.disconnected(null));
            requireOpen(); // Dispatcher failure can stop the owner on the preparation worker.
        }
        catch (RuntimeException | Error failure) { page.detach(); throw failure; }
        return page;
    }
    /** Most recent terminal result, or null before the first completion; available after page detachment. */
    public PrintOperation.Result lastResult() { requireMain(); return lastResult; }
    public final class PageBinding implements AutoCloseable {
        private volatile Ui ui;
        private final AtomicBoolean detached = new AtomicBoolean();
        private final AtomicReference<Subscription> state = new AtomicReference<>(), connection = new AtomicReference<>();
        private final AtomicReference<PrintRequest> pending = new AtomicReference<>();
        private long generation, attempt;
        private PageBinding(Ui ui) { this.ui = ui; }
        private void requireAttached() { requireOpen(); if (detached.get()) throw new IllegalStateException("Page is detached"); }
        private void watch(MobilePrinter selected, long selectedEpoch) {
            unsubscribe(state.getAndSet(null)); generation = 0;
            Subscription next = selected.observeState(main, value -> {
                if (stopped.get() || detached.get() || selected != printer || selectedEpoch != epoch.get() || value.connectionGeneration < generation) return;
                generation = value.connectionGeneration; notifyUi(target -> target.state(value));
            });
            state.set(next); if (detached.get()) unsubscribe(state.getAndSet(null));
        }
        /** Explicit first connect or retry after failure. Closing this page only drops its callback. */
        public void connect(int timeoutMs) {
            requireMain(); requireAttached();
            try {
                if (releasing) throw new PrinterException(1002);
                if (timeoutMs <= 0) throw new PrinterException(1001);
                if (printer == null) {
                    printer = acquire();
                }
                MobilePrinter selected = printer; DeviceStateSnapshot before = selected.stateSnapshot();
                if (before.connectionState == MobilePrinter.CONNECTED) {
                    generation = Math.max(generation, before.connectionGeneration);
                    dispatch(target -> target.connection(null)); return;
                }
                if (before.connectionState == MobilePrinter.CONNECTING) throw new PrinterException(1002);
                long selectedEpoch = epoch.incrementAndGet(), selectedAttempt = connectionAttempt + 1;
                for (PageBinding page : new ArrayList<>(pages)) page.watch(selected, selectedEpoch);
                Subscription next = manager.connect(selected, timeoutMs, main, failure -> {
                    if (attempt == selectedAttempt) unsubscribe(connection.getAndSet(null));
                    if (stopped.get() || detached.get() || selected != printer || selectedEpoch != epoch.get() || selectedAttempt != connectionAttempt) return;
                    Throwable result = failure;
                    if (result == null) {
                        try {
                            DeviceStateSnapshot current = selected.stateSnapshot(); generation = Math.max(generation, current.connectionGeneration);
                            if (current.connectionState != MobilePrinter.CONNECTED) result = new PrinterException(2002);
                        } catch (RuntimeException unavailable) { result = unavailable; }
                    }
                    final Throwable reported = result; notifyUi(target -> target.connection(reported));
                });
                connectionAttempt = selectedAttempt; attempt = selectedAttempt;
                unsubscribe(connection.getAndSet(next)); if (detached.get()) unsubscribe(connection.getAndSet(null));
            } catch (RuntimeException failure) { dispatch(target -> target.failed(failure)); }
        }
        /** Transfer gray8 as read-only until this request finishes; no pixel copy or IO runs on the main thread. */
        public PrintRequest printLabelGray8(byte[] gray8, int width, int height, CpclProfile profile, int copies, int timeoutMs) {
            requireMain(); requireAttached(); Objects.requireNonNull(gray8); Objects.requireNonNull(profile);
            MobilePrinter selected = printer;
            DeviceStateSnapshot current = selected == null ? null : selected.stateSnapshot();
            if (releasing || current == null || current.connectionState != MobilePrinter.CONNECTED)
                throw new IllegalStateException("Explicit connection required");
            PrintRequest request = new PrintRequest(this, selected, epoch.get(), current.connectionGeneration);
            if (!pending.compareAndSet(null, request)) throw new IllegalStateException("This page already has a request");
            requests.add(request);
            try { preparations.execute(() -> prepare(request, gray8, width, height, profile, copies, timeoutMs)); }
            catch (RuntimeException failure) { failed(request, failure); }
            return request;
        }
        private void dispatch(UiCall callback) {
            try { main.execute(() -> notifyUi(callback)); }
            catch (RuntimeException | Error unavailable) { detach(); }
        }
        private void notifyUi(UiCall callback) {
            Ui target = ui; if (target == null || stopped.get() || detached.get()) return;
            try { callback.call(target); } catch (RuntimeException | Error failure) { Log.e("PrinterApp", "Page callback failed"); }
        }
        private void detach() {
            if (!detached.compareAndSet(false, true)) return;
            ui = null; unsubscribe(connection.getAndSet(null)); unsubscribe(state.getAndSet(null));
            PrintRequest request = pending.getAndSet(null); if (request != null) unsubscribe(request.pageCompletion.getAndSet(null));
            pages.remove(this); // Do not cancel a request or release the shared printer.
        }
        @Override public void close() { requireMain(); detach(); }
    }
    public final class PrintRequest {
        private final PageBinding page;
        private final MobilePrinter selected;
        private final long selectedEpoch, selectedGeneration;
        private final AtomicBoolean cancelled = new AtomicBoolean(), finished = new AtomicBoolean();
        private final AtomicReference<PrintOperation> operation = new AtomicReference<>();
        private final AtomicReference<Subscription> ownerCompletion = new AtomicReference<>(), pageCompletion = new AtomicReference<>();
        private PrintRequest(PageBinding page, MobilePrinter selected, long selectedEpoch, long selectedGeneration) {
            this.page = page; this.selected = selected; this.selectedEpoch = selectedEpoch; this.selectedGeneration = selectedGeneration;
        }
        public boolean isFinished() { return finished.get(); }
        /** Also covers preparation and submit-before-publication. Accepted bytes can never be recalled. */
        public void cancel() {
            requireMain(); if (finished.get()) return; cancelled.set(true);
            PrintOperation selected = operation.get();
            if (selected != null) {
                try { selected.cancel(); }
                catch (PrinterException failure) {
                    // A worker may release caller access after this method captured the operation.
                    if (failure.code != 1002 || (!finished.get() && operation.get() == selected)) throw failure;
                }
            }
        }
        private void dispose() {
            finished.set(true); unsubscribe(pageCompletion.getAndSet(null)); unsubscribe(ownerCompletion.getAndSet(null));
            PrintOperation selected = operation.getAndSet(null); if (selected != null) selected.close();
            page.pending.compareAndSet(this, null); requests.remove(this);
        }
    }
    private interface UiCall { void call(Ui ui); }
    private static void unsubscribe(Subscription subscription) { if (subscription != null) subscription.close(); }
    private void prepare(PrintRequest request, byte[] gray8, int width, int height, CpclProfile profile, int copies, int timeoutMs) {
        if (stopped.get()) { request.dispose(); return; }
        if (request.cancelled.get() || request.selectedEpoch != epoch.get()) { postFailure(request, new PrinterException(1005)); return; }
        PrintOperation next = null;
        try {
            DeviceStateSnapshot current = request.selected.stateSnapshot();
            if (current.connectionState != MobilePrinter.CONNECTED || current.connectionGeneration != request.selectedGeneration) {
                postFailure(request, new PrinterException(1005)); return;
            }
            next = submitter.submit(request.selected, gray8, width, height, profile, copies, timeoutMs);
            if (stopped.get()) { next.close(); request.dispose(); return; }
            if (request.cancelled.get() || request.selectedEpoch != epoch.get()) next.cancel();
            final PrintOperation accepted = next; main.execute(() -> publish(request, accepted));
        } catch (RuntimeException | Error failure) {
            if (next != null) {
                try { next.cancel(); } catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
                try { next.close(); } catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            }
            postFailure(request, failure);
        }
    }
    private void postFailure(PrintRequest request, Throwable failure) {
        try { main.execute(() -> failed(request, failure)); }
        catch (RuntimeException | Error unavailable) { request.dispose(); stopAfterDispatcherFailure(); }
    }
    private void failed(PrintRequest request, Throwable failure) {
        request.dispose(); request.page.notifyUi(target -> target.failed(failure));
    }
    private void publish(PrintRequest request, PrintOperation accepted) {
        if (stopped.get() || request.finished.get()) { accepted.close(); request.dispose(); return; }
        request.operation.set(accepted);
        try {
            if (!request.page.detached.get()) {
                request.pageCompletion.set(accepted.onComplete(main, result -> {
                    unsubscribe(request.pageCompletion.getAndSet(null)); request.page.pending.compareAndSet(request, null);
                    request.page.notifyUi(target -> target.completed(result));
                }));
                if (request.page.detached.get()) unsubscribe(request.pageCompletion.getAndSet(null));
            }
            request.ownerCompletion.set(accepted.onComplete(main, result -> {
                lastResult = result;
                request.finished.set(true); unsubscribe(request.ownerCompletion.getAndSet(null));
                PrintOperation selected = request.operation.getAndSet(null); if (selected != null) selected.close();
                if (request.page.detached.get()) request.page.pending.compareAndSet(request, null);
                requests.remove(request);
            }));
            if (request.cancelled.get() || request.selectedEpoch != epoch.get()) accepted.cancel();
        } catch (RuntimeException failure) {
            try { accepted.cancel(); } catch (RuntimeException cancellation) { failure.addSuppressed(cancellation); }
            failed(request, failure);
        }
    }
    /** Application-owner action: stop this device's work, wait asynchronously for release, then allow explicit reconnect. */
    public void disconnect() {
        requireMain(); requireOpen(); if (releasing) return;
        MobilePrinter selected = printer; printer = null; epoch.incrementAndGet(); connectionAttempt++;
        for (PageBinding page : new ArrayList<>(pages)) { unsubscribe(page.state.getAndSet(null)); unsubscribe(page.connection.getAndSet(null)); }
        if (selected == null) return;
        releasing = true;
        manager.release(selected, main, failure -> {
            releasing = false;
            for (PageBinding page : new ArrayList<>(pages)) page.notifyUi(target -> target.disconnected(failure));
        });
    }
    private void stopAfterDispatcherFailure() { stop(); manager.close(); }
    private void stop() {
        if (!stopped.compareAndSet(false, true)) return;
        epoch.incrementAndGet();
        for (PageBinding page : new ArrayList<>(pages)) page.detach();
        for (PrintRequest request : new ArrayList<>(requests)) request.dispose();
        preparations.shutdown(); // Queued preparation sees stopped; late accepted operations close themselves.
    }
    @Override public void close() { requireMain(); stop(); manager.close(); }
    public Subscription close(PrinterManager.LifecycleListener listener) {
        requireMain(); stop(); return manager.close(main, Objects.requireNonNull(listener));
    }
}
