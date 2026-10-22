import android.content.Context;
import android.graphics.Bitmap;
import com.netumscan.printersdk.*;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** Application owns one Manager and retains results independently of page lifetimes. */
public final class SharedPrinterApp implements AutoCloseable {
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
    private volatile ManagerResult<Void> lastResult;
    private volatile ManagerResult<?> lastConfiguration;
    public SharedPrinterApp(Context context, DiscoveredDevice device, boolean lowEnergy) {
        this(new PrinterManager(context), device, lowEnergy);
    }
    SharedPrinterApp(PrinterManager manager, DiscoveredDevice device, boolean lowEnergy) {
        this.manager = manager; this.device = java.util.Objects.requireNonNull(device); this.lowEnergy = lowEnergy;
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
        if (model != null) return lowEnergy ? manager.connectBle(device, model, this::connectionResult, this::connectionResult)
                                           : manager.connectSpp(device, model, this::connectionResult, this::connectionResult);
        return lowEnergy ? manager.connectBle(device, this::connectionResult, this::connectionResult)
                         : manager.connectSpp(device, this::connectionResult, this::connectionResult);
    }
    private void connectionResult(ManagerResult<?> result) {
        for (PageBinding binding : bindings()) if (binding.attached) binding.page.connection(result);
    }
    /** Configure only after connection and while no device request is pending. */
    public RequestId setPrinterModel(PrinterModel model) {
        return manager.setPrinterModel(model, this::configurationResult);
    }
    public RequestId setPaperType(PaperType type) {
        return manager.setPaperType(type, this::configurationResult);
    }
    private void configurationResult(ManagerResult<?> result) {
        lastConfiguration = result;
        for (PageBinding binding : bindings()) if (binding.attached) binding.page.configuration(result);
    }
    private interface Submit { RequestId start(PrinterManager.Callback<Void> callback); }
    public RequestId printImage(Bitmap image, double widthMm, double heightMm) {
        return submit(callback -> manager.printImage(image, widthMm, heightMm, callback));
    }
    public RequestId printEncodedImage(byte[] bytes, double widthMm, double heightMm) {
        return submit(callback -> manager.printEncodedImage(bytes, widthMm, heightMm, callback));
    }
    private RequestId submit(Submit action) {
        synchronized (lock) {
            RequestId id = action.start(result -> {
                synchronized (lock) { pending.remove(result.requestId); lastResult = result; }
                for (PageBinding binding : bindings()) if (binding.attached) binding.page.completed(result);
            });
            pending.put(id, Boolean.TRUE); return id;
        }
    }
    public void cancel(RequestId id) { manager.cancel(id); }
    public RequestId disconnect(PrinterManager.Callback<Void> completion) { return manager.disconnect(completion); }
    @Override public void close() {
        for (PageBinding binding : bindings()) binding.close();
        manager.close(result -> { });
    }
}
