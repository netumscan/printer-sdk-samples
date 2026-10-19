package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** Uses SDK subscriptions; the Demo never polls native operations or device state. */
final class SdkSession implements PrinterController.Session {
    private final PrinterManager manager;
    private final MobilePrinter printer;
    private final AtomicBoolean closing = new AtomicBoolean();
    private PrintOperation operation;
    private Subscription stateSubscription, completionSubscription;
    private PrinterController.Observation observer;
    private Executor dispatcher;
    SdkSession(PrinterManager manager, MobilePrinter printer) { this.manager = manager; this.printer = printer; }
    private void requireOpen() { if (closing.get()) throw new PrinterException(1002); }
    public void connect() { requireOpen(); PrinterController.awaitConnection(manager.connect(printer, 10000)); }
    public DeviceStateSnapshot stateSnapshot() { requireOpen(); return printer.stateSnapshot(); }
    public ResourceSnapshot resourceSnapshot() { requireOpen(); return printer.resourceSnapshot(); }
    public Diagnostic diagnostic() { requireOpen(); return printer.diagnostic(); }
    public synchronized void observe(Executor dispatcher, PrinterController.Observation observer) {
        requireOpen(); this.dispatcher = dispatcher; this.observer = observer;
        if (stateSubscription != null) stateSubscription.close();
        stateSubscription = printer.observeState(dispatcher, snapshot -> {
            PrinterController.Observation selected;
            synchronized (this) { selected = this.observer; }
            if (selected == null || closing.get()) return;
            try { selected.state(snapshot, printer.resourceSnapshot(), printer.diagnostic()); }
            catch (PrinterException unavailable) { if (unavailable.code != 1002) selected.state(null, null, null); }
        });
    }
    public boolean supportsMedia() { return printer.implementsFeature(MobilePrinter.FEATURE_MEDIA); }
    public boolean supportsLabelPositioning() { return printer.implementsFeature(MobilePrinter.FEATURE_FEED_TO_NEXT_LABEL); }
    private long own(PrintOperation selected) {
        if (operation != null) operation.close();
        if (completionSubscription != null) completionSubscription.close();
        operation = selected; long id = selected.snapshot().id;
        if (observer != null) completionSubscription = selected.onComplete(dispatcher, result -> {
            PrinterController.Observation listener;
            synchronized (this) { listener = observer; }
            if (listener != null && !closing.get()) {
                PrintOperation.Snapshot snapshot = result.snapshot;
                listener.completed(new PrinterController.Update(snapshot.id, snapshot.state, snapshot.completion, snapshot.error,
                    snapshot.bytesSent, snapshot.bytesTotal, result.diagnostic.message.isEmpty() ? PrinterException.description(snapshot.error) : result.diagnostic.message,
                    snapshot.byteCountFlags, result.diagnostic));
            }
            synchronized (this) { if (operation == selected) { selected.close(); operation = null; completionSubscription = null; } }
        });
        return id;
    }
    public synchronized long submit(byte[] data, LabelConfig config) {
        requireOpen();
        return own(config.receipt ? printer.submitReceiptGray8(data, config.labelWidthDots, config.height, config.receiptProfile(), config.copies,
            config.trailingBlankDots, config.receiptBudget(), MobilePrinter.CUT_NONE, 120000) :
            printer.submitLabelGray8(data, config.labelWidthDots, config.height, config.profile(), config.copies, config.trailingBlankDots,
                config.xOffsetDots, 0, config.form, 120000));
    }
    public synchronized long raw(byte[] bytes, int copies) { requireOpen(); return own(printer.sendRaw(bytes, copies, 120000)); }
    public synchronized long mediaControl(int mode, LabelConfig config) {
        requireOpen(); if (config.receipt) throw new IllegalArgumentException("当前小票协议不支持介质设置或标签定位");
        switch (mode) {
        case 0: return own(printer.locateNextLabel(config.profile(), 120000));
        case 1: return own(printer.setMedia(MediaMode.GAP, config.profile(), 120000));
        case 2: return own(printer.setMedia(MediaMode.BLACK_MARK, config.profile(), 120000));
        case 3: return own(printer.setMedia(MediaMode.CONTINUOUS, config.profile(), 120000));
        default: throw new IllegalArgumentException("无效介质模式");
        }
    }
    public synchronized void cancel(long id) { if (operation != null && operation.snapshot().id == id) operation.cancel(); }
    public void close() {
        closing.set(true);
        synchronized (this) {
            observer = null; dispatcher = null;
            if (stateSubscription != null) { stateSubscription.close(); stateSubscription = null; }
            if (completionSubscription != null) { completionSubscription.close(); completionSubscription = null; }
        }
        try { PrinterController.awaitCleanup(manager.release(printer)); }
        finally { synchronized (this) { if (operation != null) { operation.close(); operation = null; } } }
    }
}
