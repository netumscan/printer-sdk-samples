package com.netumscan.printersdk.demo;

import com.netumscan.printersdk.*;

final class SdkSession implements PrinterController.Session {
    private final MobilePrinter printer;
    private final ConnectionConfig connection;
    private PrintOperation operation;
    SdkSession(MobilePrinter printer, ConnectionConfig connection) { this.printer = printer; this.connection = connection; }
    public void connect() { printer.connect(10000); }
    public boolean supportsMedia() { return printer.implementsFeature(MobilePrinter.FEATURE_MEDIA); }
    public boolean supportsLabelPositioning() { return printer.implementsFeature(MobilePrinter.FEATURE_FEED_TO_NEXT_LABEL); }
    private long own(PrintOperation selected) {
        if (operation != null) operation.close();
        operation = selected; return selected.snapshot().id;
    }
    public synchronized long submit(byte[] data, LabelConfig config) {
        if (!connection.matches(config)) throw new IllegalArgumentException("设备参数已变化；请断开后重新连接");
        int width = config.labelWidthDots, height = config.height, copies = config.copies;
        return own(config.receipt ?
            printer.submitReceiptGray8(data, width, height, copies, config.trailingBlankDots, config.receiptBudget(),
                MobilePrinter.CUT_NONE, 120000) :
            printer.submitLabelGray8(data, width, height, copies, config.trailingBlankDots, config.xOffsetDots, 0, config.form, 120000));
    }
    public synchronized long raw(byte[] bytes, int copies) { return own(printer.sendRaw(bytes, copies, 120000)); }
    public synchronized long mediaControl(int mode) {
        switch (mode) {
        case 0: return own(printer.locateNextLabel(120000));
        case 1: return own(printer.setMedia(MediaMode.GAP, 120000));
        case 2: return own(printer.setMedia(MediaMode.BLACK_MARK, 120000));
        case 3: return own(printer.setMedia(MediaMode.CONTINUOUS, 120000));
        default: throw new IllegalArgumentException("无效介质模式");
        }
    }
    public synchronized PrinterController.Update[] poll() {
        if (operation == null) return new PrinterController.Update[0];
        PrintOperation.Snapshot snapshot = operation.snapshot();
        PrinterController.Update update = new PrinterController.Update(snapshot.id, snapshot.state,
            snapshot.completion, snapshot.error, snapshot.bytesSent, snapshot.bytesTotal,
            new PrinterException(snapshot.error).getMessage(), snapshot.byteCountFlags);
        if (snapshot.terminal()) { operation.close(); operation = null; }
        return new PrinterController.Update[]{update};
    }
    public synchronized void cancel(long id) { if (operation != null && operation.snapshot().id == id) operation.cancel(); }
    public void close() {
        printer.close(); // Interrupt platform IO before waiting for session access.
        synchronized (this) { if (operation != null) { operation.close(); operation = null; } }
    }
}
