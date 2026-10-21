import SwiftUI
import PhotosUI
import UniformTypeIdentifiers
import PrinterSDK

/// Provider URLs are copied before their callback returns. The Model accepts only the matching request.
struct DemoImagePicker: UIViewControllerRepresentable {
    let kind: DemoPickerKind
    let importer: @Sendable (URL) throws -> DemoImportedImage
    let onBegin: @MainActor @Sendable () -> Void
    let onFile: @MainActor @Sendable (Result<URL, Error>) -> Void
    let onImported: @MainActor @Sendable (Result<DemoImportedImage, DemoInputError>) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }
    func makeUIViewController(context: Context) -> UIViewController {
        if kind == .files {
            let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.png, .jpeg], asCopy: true)
            picker.allowsMultipleSelection = false; picker.delegate = context.coordinator
            return picker
        }
        var configuration = PHPickerConfiguration()
        configuration.filter = .images; configuration.selectionLimit = 1
        configuration.preferredAssetRepresentationMode = .compatible
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    static func dismantleUIViewController(_ uiViewController: UIViewController, coordinator: Coordinator) {
        coordinator.cancelIfNeeded()
    }

    @MainActor final class Coordinator: NSObject, UIDocumentPickerDelegate, PHPickerViewControllerDelegate {
        let parent: DemoImagePicker
        private var handedOff = false
        init(_ parent: DemoImagePicker) { self.parent = parent }
        func cancelIfNeeded() {
            guard !handedOff else { return }
            handedOff = true; parent.onImported(.failure(DemoInputError(1005)))
        }
        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            handedOff = true
            parent.onFile(.failure(DemoInputError(1005)))
        }
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            handedOff = true
            guard let url = urls.first else { parent.onFile(.failure(DemoInputError(1005))); return }
            parent.onBegin(); parent.onFile(.success(url))
        }
        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            handedOff = true
            guard let provider = results.first?.itemProvider else {
                parent.onImported(.failure(DemoInputError(1005))); return
            }
            let type: String
            if provider.hasItemConformingToTypeIdentifier(UTType.png.identifier) { type = UTType.png.identifier }
            else if provider.hasItemConformingToTypeIdentifier(UTType.jpeg.identifier) { type = UTType.jpeg.identifier }
            else { parent.onImported(.failure(DemoInputError(1004))); return }
            let importer = parent.importer, completion = parent.onImported
            parent.onBegin()
            provider.loadFileRepresentation(forTypeIdentifier: type) { url, _ in
                let result: Result<DemoImportedImage, DemoInputError>
                do {
                    guard let url else { throw DemoInputError(2003) }
                    // This provider URL expires on return; complete the bounded copy here.
                    result = .success(try importer(url))
                } catch { result = .failure((error as? DemoInputError) ?? DemoInputError(2003)) }
                Task { @MainActor in completion(result) }
            }
        }
    }
}
