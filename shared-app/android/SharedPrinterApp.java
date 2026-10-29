import android.content.Context;
import android.graphics.Bitmap;
import com.netumscan.printersdk.*;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** Application owns one Manager and retains results independently of page lifetimes. */
public final class SharedPrinterApp implements AutoCloseable, PrinterListener {
    @FunctionalInterface public interface Completion<T> { void onComplete(ManagerResult<T> result); }
    public interface Page {
        void connection(ManagerResult<?> result);
        void configuration(ManagerResult<?> result);
        void completed(ManagerResult<Void> result);
    }
    public final class PageBinding implements AutoCloseable {
        private final Page page;
        private volatile boolean attached = true;
        private PageBinding(Page page) { this.page = page; }
        /** Only detach this page's notification target; accepted work stays application-owned. */
        @Override public void close() { attached = false; synchronized (lock) { pages.remove(this); } }
    }
    private final Object lock = new Object();
    private final PrinterManager manager;
    private final DiscoveredDevice device;
    private final boolean lowEnergy;
    private final List<PageBinding> pages = new ArrayList<>();
    private final IdentityHashMap<RequestId, Boolean> pending = new IdentityHashMap<>();
    private final IdentityHashMap<RequestId, Completion<Void>> disconnects = new IdentityHashMap<>();
    private volatile ManagerResult<Void> lastResult;
    private volatile ManagerResult<?> lastConfiguration;
    public SharedPrinterApp(Context context, DiscoveredDevice device, boolean lowEnergy) {
        this(new PrinterManager(context), device, lowEnergy);
    }
    SharedPrinterApp(PrinterManager manager, DiscoveredDevice device, boolean lowEnergy) {
        this.manager = manager; this.device = java.util.Objects.requireNonNull(device); this.lowEnergy = lowEnergy;
        manager.setListener(this);
    }
    public PageBinding bindPage(Page page) {
        PageBinding binding = new PageBinding(java.util.Objects.requireNonNull(page));
        synchronized (lock) { pages.add(binding); } return binding;
    }
    public void unbindPage(Page page) {
        for (PageBinding binding : bindings()) if (binding.page == page) binding.close();
    }
    private List<PageBinding> bindings() { synchronized (lock) { return new ArrayList<>(pages); } }
    public ManagerResult<Void> lastResult() { return lastResult; }
    public ManagerResult<?> lastConfiguration() { return lastConfiguration; }
    public int pendingCount() { synchronized (lock) { return pending.size(); } }
    public RequestId connect() {
        return connect(null);
    }
    /** The selected model is configuration, not a replacement device name. */
    public RequestId connect(PrinterModel model) {
        if (model != null) return lowEnergy ? manager.connectBle(device, model)
                                           : manager.connectSpp(device, model);
        return lowEnergy ? manager.connectBle(device)
                         : manager.connectSpp(device);
    }
    private void connectionResult(ManagerResult<?> result) {
        for (PageBinding binding : bindings()) if (binding.attached) binding.page.connection(result);
    }
    /** Configure only after connection and while no device request is pending. */
    public RequestId setPrinterModel(PrinterModel model) {
        return manager.setPrinterModel(model);
    }
    public RequestId setPaperType(PaperType type) {
        return manager.setPaperType(type);
    }
    private void configurationResult(ManagerResult<?> result) {
        lastConfiguration = result;
        for (PageBinding binding : bindings()) if (binding.attached) binding.page.configuration(result);
    }
    @Override public void onConnectionCompleted(ManagerResult<ConnectionInfo> result) { connectionResult(result); }
    @Override public void onDisconnected(ManagerResult<Void> result) { connectionResult(result); }
    @Override public void onModelConfigurationCompleted(ManagerResult<ConnectionInfo> result) { configurationResult(result); }
    @Override public void onPaperTypeCompleted(ManagerResult<Void> result) { configurationResult(result); }
    public RequestId printImage(Bitmap image, double widthMm, double heightMm) {
        synchronized (lock) {
            RequestId id = manager.printImage(image, widthMm, heightMm); pending.put(id, Boolean.TRUE); return id;
        }
    }
    public RequestId printEncodedImage(byte[] bytes, double widthMm, double heightMm) {
        synchronized (lock) {
            RequestId id = manager.printEncodedImage(bytes, widthMm, heightMm); pending.put(id, Boolean.TRUE); return id;
        }
    }
    @Override public void onPrintCompleted(ManagerResult<Void> result) {
        synchronized (lock) { pending.remove(result.requestId); lastResult = result; }
        for (PageBinding binding : bindings()) if (binding.attached) binding.page.completed(result);
    }
    public void cancel(RequestId id) { manager.cancel(id); }
    public RequestId disconnect(Completion<Void> completion) {
        synchronized (lock) {
            RequestId id = manager.disconnect(); disconnects.put(id, completion); return id;
        }
    }
    @Override public void onDisconnectCompleted(ManagerResult<Void> result) {
        Completion<Void> completion;
        synchronized (lock) { completion = disconnects.remove(result.requestId); }
        if (completion != null) completion.onComplete(result);
    }
    @Override public void close() {
        for (PageBinding binding : bindings()) binding.close();
        manager.close();
    }
}
